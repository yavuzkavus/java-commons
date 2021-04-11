package com.readjournal.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

import com.readjournal.cache.TimedCache;
import com.readjournal.util.DateUtil;
import com.readjournal.util.Utils;

public class DbPool {
	private final String url;
	private final String userName;
	private final String password;

	private volatile int timeOut = 600_000; //10 minutes
	private volatile int poolSize = 10;
	
	private volatile int queryTimeout				= DB.QUERY_TIMEOUT; //seconds
	private volatile int fetchSize					= DB.FETCH_SIZE;
	private volatile int scrollType					= DB.SCROLL_TYPE;
	private volatile int concurencyType				= DB.CONCURENCY_TYPE;
	private volatile int transactionIsolationLevel	= DB.TRANSACTION_ISOLATION_LEVEL;

	private volatile Set<PDB> dbs;
	private volatile Map<Thread, PDB> sessionDbs;
	
	public DbPool(	final String url,
					final String userName,
					final String password  ) {
		dbs = Collections.synchronizedSet( new HashSet<>() );
		sessionDbs = Collections.synchronizedMap(new WeakHashMap<>());
		this.url = url;
		this.userName = userName;
		this.password = password;
	}

	public String getUrl() {
		return url;
	}
	public String getUserName() {
		return userName;
	}
	public String getPassword() {
		return password;
	}
	public int getTimeOut() {
		return timeOut;
	}
	public void setTimeOut(final int timeOut) {
		if( timeOut<=0 )
			throw new IllegalArgumentException("Timeout should be positive integer");
		this.timeOut = timeOut;
	}
	public int getPoolSize() {
		return poolSize;
	}
	public void setPoolSize(int poolSize) {
		if( poolSize<=0 )
			throw new IllegalArgumentException("Pool size should be positive integer");
		this.poolSize = poolSize;
	}

	public int getQueryTimeout() {
		return queryTimeout;
	}
	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
	}

	public int getFetchSize() {
		return fetchSize;
	}
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	public void setScrollType(int scrollType) {
		this.scrollType = scrollType;
	}
	public int getScrollType() {
		return this.scrollType;
	}

	public void setConcurencyType(int concurencyType) {
		this.concurencyType = concurencyType;
	}
	public int getConcurencyType() {
		return concurencyType;
	}
	
	public int getTransactionIsolationLevel() {
		return transactionIsolationLevel;
	}
	public void setTransactionIsolationLevel(int transactionIsolationLevel) {
		this.transactionIsolationLevel = transactionIsolationLevel;
	}

	/*
	 * when borrowing a db by calling one of getSessionDB methods
	 * dont call releaseDB on borrowed db
	 * because db will be released in rjfilter
	 */
	public DB getSessionDB(){
		return getSessionDB(true);
	}

	public DB getSessionDB(boolean reapable) {
		PDB db = sessionDbs.get(Thread.currentThread());
		if( db==null ) {
			db = (PDB)borrowDB(reapable);
			sessionDbs.put(Thread.currentThread(), db);
		}
		else {
			if( db.isReapable() && !reapable ) {
				db.setReapable(reapable);
			}
		}
		return db;
	}

	public void releaseSessionDB() {
		PDB db = sessionDbs.remove(Thread.currentThread());
		if( db!=null ) {
			releaseDB(db);
		}
	}

	public DB borrowDB(boolean reapable) {
		PDB db = null;
		do {
			synchronized (dbs) {
				for(PDB _db : dbs) {
					if( !_db.isLocked() ) {
						db = _db;
						db.setLocked(true);
						break;
					}
				}
			}
			if( db!=null && !db.validate() ) {
				removeDB(db);
				db = null;
			}
			else {
				break;
			}
		} while(true);
		
		if( db==null ) {
			try {
				db = new PDB();
				setDefaults(db);
				db.setLocked(true);
				dbs.add(db);
			} catch (SQLException e) {
				throw Utils.runtime(e);
			}
		}
		db.setReapable(reapable);
		db.setLastAccessTime( new Date() );
		db.setBorrowTime( new Date() );
		return db;
	}

	public DB borrowDB() {
		return borrowDB(true);
	}

	public void releaseDB(DB db) {
		PDB pdb = (PDB)db;
		pdb.closeStatements();
		if( pdb.isClosed() ) {
			dbs.remove(db);
		}
		else {
			setDefaults(pdb);
			
			//pdb.setBorrowTime(null);
			pdb.setLocked(false);
			pdb.setReapable(true);
		}
	}

	public void closeDbs() {
		synchronized (dbs) {
			Iterator<PDB> iterator = dbs.iterator();
			while( iterator.hasNext() ) {
				iterator.next().close();
				iterator.remove();
			}
		}
	}
	
	private void setDefaults(PDB db) {
		db.setAutoCommit(true);
		db.setQueryTimeout(queryTimeout);
		db.setFetchSize(fetchSize);
		db.setScrollType(scrollType);
		db.setConcurencyType(concurencyType);
		db.setTransactionIsolation(transactionIsolationLevel);
	}

	/**
	 * database server and web server time difference
	 * to work synchorizely two servers
	 */
	private final TimedCache<Long> dateDiffCache = TimedCache.<Long>newBuilder()
											.timeout(1, TimeUnit.MINUTES)
											.supplier(()-> {
												try {
													long serverMillis = getSessionDB().getServerDate().getTime();
													return System.currentTimeMillis() - serverMillis;
												}
												catch(Exception ex) {
													return 0L;
												}
											})
											.build();
	public long getDateDiff() {
		return dateDiffCache.get();
	}

	/**
	 * get current time millis at database server
	 * @return current time millis at database server
	 */
	public long getCurrentMillis() {
		return System.currentTimeMillis() - getDateDiff();
	}

	public void reapDbs() {
		List<PDB> releaseds = new ArrayList<>(5);
		synchronized (dbs) {
			Iterator<PDB> iterator = dbs.iterator();
			while(iterator.hasNext()) {
				PDB db = iterator.next();
				if( db.isClosed() ) {
					iterator.remove();
				}
				else 
				if( db.isReapable() ) {
					if( db.isLocked() &&
								(System.currentTimeMillis() - timeOut) > db.getLastAccessTime().getTime() ) {
						releaseDB(db);
						releaseds.add(db);
					}
					else if( dbs.size()>poolSize && !db.isLocked() ) {
						iterator.remove();
						try { db.close(); } catch(Exception ex) { }
					}
				}
			}
		}
		
		synchronized (sessionDbs) {
			Iterator<PDB> iter = sessionDbs.values().iterator();
			while( iter.hasNext() ) {
				if( releaseds.contains(iter.next()) ) {
					iter.remove();
				}
			}
		}
 	}

	public void removeDB(DB db) {
		releaseDB(db);
		try {
			db.close();
		} catch(Exception ex) { }
		dbs.remove(db);
	}

	public Set<DB> getDbs() {
		return Utils.cast(dbs);
	}

	public Thread getThread(DB db) {
		synchronized (sessionDbs) {
			for( Entry<Thread, PDB> entry : sessionDbs.entrySet() ) {
				if( db==entry.getValue())
					return entry.getKey();
			}
		}
		return null;
	}
	
	public DB getDB(Thread thread) {
		return sessionDbs.get( thread );
	}

	public class PDB extends DB {
		/**
		 * next fields, constructor and methods are for pooling
		 */
		private boolean locked;
		private boolean reapable = true;
		private Date borrowTime;
	
		public PDB() throws SQLException {
			super(getUrl(), getUserName(), getPassword());
		}
	
		public boolean isLocked() {
			return locked;
		}
		public void setLocked(boolean locked) {
			this.locked = locked;
		}
		public Date getBorrowTime() {
			return locked ? borrowTime : null;
		}
		public void setBorrowTime(Date borrowTime) {
			this.borrowTime = borrowTime;
		}
		public long getBorrowDuration() {
			return locked ? System.currentTimeMillis() - borrowTime.getTime() : 0;
		}
		public String getFormattedBorrowDuration() {
			return locked ? DateUtil.formatDuration( System.currentTimeMillis() - borrowTime.getTime() ) : null;
		}
		public boolean isReapable() {
			return reapable;
		}
		public void setReapable(boolean reapable) {
			this.reapable = reapable;
		}

		@Override
		protected void checkLock() {
			super.checkLock();
			if( !locked ) {
				RuntimeException ex = new RuntimeException("Using unlocked db!");
				throw ex;
			}
		}
	}
}