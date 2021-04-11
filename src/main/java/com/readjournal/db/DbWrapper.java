package com.readjournal.db;

import java.util.Date;

import com.readjournal.db.DbPool.PDB;
import com.readjournal.util.DateUtil;

public class DbWrapper {
	private PDB db;
	private Date lastAccessTime;
	private Date borrowTime;
	private boolean locked;
	private boolean reapable;
	public DbWrapper(DB db) {
		this.db = (PDB)db;
		syncValues();
	}

	public void syncValues() {
		this.lastAccessTime = db.getLastAccessTime();
		this.borrowTime = db.getBorrowTime();
		this.locked = db.isLocked();
		this.reapable = db.isReapable();
	}

	public DB getDb() {
		return db;
	}
	public int getId() {
		return db.getId();
	}
	public boolean isLocked() {
		return locked;
	}
	public boolean isReapable() {
		return reapable;
	}
	public Date getLastAccessTime() {
		return lastAccessTime;
	}
	public long getBorrowDuration() {
		return Math.max(0, locked ? System.currentTimeMillis() - borrowTime.getTime() : 0);
	}
	public String getFormattedBorrowDuration() {
		return locked ? DateUtil.formatDuration( System.currentTimeMillis() - borrowTime.getTime() ) : null;
	}
}
