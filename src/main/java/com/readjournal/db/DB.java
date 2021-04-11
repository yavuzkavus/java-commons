package com.readjournal.db;

import java.io.Reader;
import java.lang.reflect.Array;
import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.time.FastDateFormat;

import com.readjournal.util.CollectionUtil;
import com.readjournal.util.StringUtil;
import com.readjournal.util.Utils;

public class DB {
	public static final FastDateFormat DATE_FDF = FastDateFormat.getInstance("yyyyMMdd");
	public static final FastDateFormat TIMESTAMP_FDF = FastDateFormat.getInstance("yyyyMMdd HH:mm:ss.SSS");
	public static final FastDateFormat TIME_FDF = FastDateFormat.getInstance("HH:mm:ss");

	public static final DateTimeFormatter LOCALDATE_DTF = DateTimeFormatter.ofPattern("yyyyMMdd");
	public static final DateTimeFormatter LOCALDATETIME_DTF = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss.SSS");
	public static final DateTimeFormatter LOCALTIME_DTF = DateTimeFormatter.ofPattern("HH:mm:ss");

	private static final AtomicInteger dbIdGenerator = new AtomicInteger(1);
	private static final Pattern selectPattern = Pattern.compile("(?misu)^\\s*SELECT\\s+(.+?)\\s+FROM\\s+(.+)\\s+ORDER BY\\s+(.+)$");

	public static final int QUERY_TIMEOUT	= 60; //seconds
	public static final int FETCH_SIZE 		= 60;

	public static final int SCROLL_TYPE					= ResultSet.TYPE_FORWARD_ONLY;
	public static final int CONCURENCY_TYPE				= ResultSet.CONCUR_READ_ONLY;
	public static final int TRANSACTION_ISOLATION_LEVEL	= Connection.TRANSACTION_READ_COMMITTED;

	private static String NAME_PREFIX = "RJ-jTDS-";

	private final int id;
	private final String name;

	private volatile int queryTimeout; //seconds
	private volatile int fetchSize;
	private volatile int scrollType;
	private volatile int concurencyType;

	private final Connection connection;
	private final List<Statement> unclosedStatements = new ArrayList<Statement>(1);
	private volatile Statement staticStmt;
	private volatile Statement lastStmt;
	private volatile Date lastAccessTime;

	public DB(String cString, String userName, String password) {
		try {
			id = dbIdGenerator.getAndIncrement();
			cString = cString.trim();
			if( !cString.endsWith(";") )
				cString += ";";
			name = NAME_PREFIX + id;
			cString += "appName="+name+";progName="+name+";";
			connection = DriverManager.getConnection(cString,userName,password);
			setDefaults();
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	public DB(String cString) {
		this(cString, "", "");
	}

	public void close() {
		try {
			connection.close();
		}
		catch(Exception ex) {
			throw Utils.runtime(ex);
		}
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public static void setNamePrefix(String namePrefix) {
		NAME_PREFIX = namePrefix;
	}

	public int getQueryTimeout() {
		return queryTimeout;
	}
	public void setQueryTimeout(int queryTimeout) {
		this.queryTimeout = queryTimeout;
		if( staticStmt!=null ) {
			try {
				staticStmt.setQueryTimeout(queryTimeout);
			}
			catch(Exception ex) { }
		}
	}
	public void resetQueryTimeout() {
		setQueryTimeout(QUERY_TIMEOUT);
	}

	public int getFetchSize() {
		return fetchSize;
	}
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
		if( staticStmt!=null ) {
			try {
				staticStmt.setFetchSize(fetchSize);
			}
			catch(Exception ex) { }
		}
	}
	public void resetFetchSize() {
		setFetchSize(FETCH_SIZE);
	}

	public int getScrollType() {
		return this.scrollType;
	}
	public void setScrollType(int scrollType) {
		this.scrollType = scrollType;
	}
	public void resetScrollType() {
		setScrollType(SCROLL_TYPE);
	}

	public int getConcurencyType() {
		return concurencyType;
	}
	public void setConcurencyType(int concurencyType) {
		this.concurencyType = concurencyType;
	}
	public void resetConcurencyType() {
		setConcurencyType(CONCURENCY_TYPE);
	}

	public void setTransactionIsolation(int transactionIsolation) {
		try {
			if( connection.getTransactionIsolation()!=transactionIsolation )
				connection.setTransactionIsolation(transactionIsolation);
		}
		catch (SQLException e) {
			throw Utils.runtime(e);
		}
	}
	public int getTransactionIsolation() {
		try {
			return connection.getTransactionIsolation();
		}
		catch (SQLException e) {
			throw Utils.runtime(e);
		}
	}
	public void resetTransactionIsolation() {
		setTransactionIsolation(TRANSACTION_ISOLATION_LEVEL);
	}

	public Date getLastAccessTime() {
		return lastAccessTime;
	}
	public void setLastAccessTime(Date lastAccessTime) {
		this.lastAccessTime = lastAccessTime;
	}

	protected void checkLock() {
		resetLastUsed();
	}

	private void resetLastUsed() {
		setLastAccessTime( new Date() );
	}

	public void closeStatements() {
		Iterator<Statement> iter = unclosedStatements.iterator();
		while( iter.hasNext() ) {
			Utils.closeSilently(iter.next());
			iter.remove();
		}
		Utils.closeSilently(staticStmt);
		staticStmt = null;
	}

	public void closeLastStatement() {
		if( lastStmt!=null ) {
			Utils.closeSilently(lastStmt);
			unclosedStatements.remove(lastStmt);
			lastStmt = null;
		}
	}

	private Statement getStaticStmt() throws SQLException {
		if( staticStmt==null || staticStmt.isClosed() ) {
			staticStmt = connection.createStatement();
		}
		lastStmt = null; //we wont close staticStmt
		staticStmt.setQueryTimeout(queryTimeout);
		staticStmt.setFetchSize(fetchSize);
		return staticStmt;
	}

	private Statement createQueryStatement() throws SQLException {
		Statement stmt = connection.createStatement(scrollType, concurencyType );
		stmt.setQueryTimeout(queryTimeout);
		stmt.setFetchSize(fetchSize);
		unclosedStatements.add(stmt);
		lastStmt = stmt;
		return stmt;
	}

	private PreparedStatement createQueryPrepared(String sql) throws SQLException {
		PreparedStatement pstmt = connection.prepareStatement(sql, scrollType, concurencyType);
		pstmt.setQueryTimeout(queryTimeout);
		pstmt.setFetchSize(fetchSize);
		unclosedStatements.add(pstmt);
		lastStmt = pstmt;
		return pstmt;
	}

	private CallableStatement createQueryCallable(String sql) throws SQLException {
		CallableStatement cstmt = connection.prepareCall(sql, scrollType, concurencyType );
		cstmt.setQueryTimeout(queryTimeout);
		cstmt.setFetchSize(fetchSize);
		unclosedStatements.add(cstmt);
		lastStmt = cstmt;
		return cstmt;
	}

	public Date getServerDate() {
		return getSingleResult("SELECT {fn now()} AS NOW", Timestamp.class);
	}

	@SuppressWarnings("unchecked")
	private static <T> T getArray(ResultSet rs, int from, int len, Class<T> type) throws SQLException {
		Class<?> elType = type.getComponentType();
		T result = (T)Array.newInstance(elType, len-from);
		for(int i=from; i<len; i++)
			Array.set(result, i-from, getSqlValue(rs, i+1, elType));
		return result;
	}

	@SuppressWarnings("unchecked")
	public <T> T getSingleResult(String sql) {
		return (T)getSingleResult(sql, Object.class);
	}

	public <T> T getSingleResult(String sql, Class<T> type) {
		checkLock();
		try(ResultSet rs = getStaticStmt().executeQuery(sql)) {
			return getSingleResult(rs, type);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public <T> T getSingleResult(String sql, Function<ResultSet, T> mapper) {
		checkLock();
		try(ResultSet rs = getStaticStmt().executeQuery(sql)) {
			rs.setFetchSize(1);
			T result = rs.next() ? mapper.apply(rs) : null;
			return result;
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public <T> T getSingleResult(String sql, List<Parameter> params) {
		return getSingleResult(sql, params.toArray(Parameter.EMPTY_ARRAY));
	}

	@SuppressWarnings("unchecked")
	public <T> T getSingleResult(String sql, Parameter[] params) {
		return (T)getSingleResult(sql, params, Object.class);
	}

	public <T> T getSingleResult(String sql, List<Parameter> params, Class<T> type) {
		return getSingleResult(sql, params.toArray(Parameter.EMPTY_ARRAY), type);
	}

	public <T> T getSingleResult(String sql, Parameter[] params, Class<T> type) {
		checkLock();
		try(ResultSet rs = executePreparedQuery(sql, params)) {
			return getSingleResult(rs, type);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			closeLastStatement();
			resetLastUsed();
		}
	}

	public <T> T getSingleResult(String sql, List<Parameter> params, Function<ResultSet, T> mapper) {
		return getSingleResult(sql, params.toArray(Parameter.EMPTY_ARRAY), mapper);
	}

	public <T> T getSingleResult(String sql, Parameter[] params, Function<ResultSet, T> mapper) {
		checkLock();
		try(ResultSet rs = executePreparedQuery(sql, params)) {
			rs.setFetchSize(1);
			T result = rs.next() ? mapper.apply(rs) : null;
			return result;
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			closeLastStatement();
			resetLastUsed();
		}
	}

	private static <T> T getSingleResult(ResultSet rs, Class<T> type) throws SQLException {
		T result = null;
		rs.setFetchSize(1);
		if( type.isArray() ) {
			if( rs.next() ) {
				int len = rs.getMetaData().getColumnCount();
				result = getArray(rs, 0, len, type);
			}
		}
		else {
			if( rs.next() )
				result = getSqlValue(rs, type);
			else
				result = getDefaultSqlValue(rs, type);
		}
		return result;
	}

	public <T> List<T> getResultList(String sql, Class<T> type) {
		return getResultList(sql, type, null);
	}

	public <T> List<T> getResultList(String sql, Class<T> type, List<T> list) {
		checkLock();
		try(ResultSet rs = getStaticStmt().executeQuery(sql)) {
			return getResultList(rs, type, list);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public <T> List<T> getResultList(String sql, List<Parameter> params, Class<T> type) {
		return getResultList(sql, params, type, null);
	}

	public <T> List<T> getResultList(String sql, Parameter[] params, Class<T> type) {
		return getResultList(sql, params, type, null);
	}

	public <T> List<T> getResultList(String sql, List<Parameter> params, Class<T> type, List<T> list) {
		return getResultList(sql, params.toArray(Parameter.EMPTY_ARRAY), type, list);
	}

	public <T> List<T> getResultList(String sql, Parameter[] params, Class<T> type, List<T> list) {
		checkLock();
		try(ResultSet rs = executePreparedQuery(sql, params)) {
			return getResultList(rs, type, list);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			closeLastStatement();
			resetLastUsed();
		}
	}

	private static <T> List<T> getResultList(ResultSet rs, Class<T> type, List<T> list) throws SQLException {
		if( !rs.next() ) {
			return list==null ? Collections.emptyList() : list;
		}
		else {
			if( list==null )
				list = new ArrayList<>();
			if( type.isArray() ) {
				int len = rs.getMetaData().getColumnCount();
				do {
					list.add(getArray(rs, 0, len, type));
				} while(rs.next());
			}
			else {
				do {
					list.add(getSqlValue(rs, type));
				} while(rs.next());
			}
			return list;
		}
	}

	public <K, V> Map<K, V> getResultMap(String sql, Class<K> keyType, Class<V> valueType) {
		return getResultMap(sql, keyType, valueType, null);
	}

	public <K, V> Map<K, V> getResultMap(String sql, Class<K> keyType, Class<V> valueType, Map<K, V> map) {
		checkLock();
		try(ResultSet rs = getStaticStmt().executeQuery(sql)) {
			return getResultMap(rs, keyType, valueType, map);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public <K, V> Map<K, V> getResultMap(String sql, List<Parameter> params, Class<K> keyType, Class<V> valueType) {
		return getResultMap(sql, params, keyType, valueType, null);
	}

	public <K, V> Map<K, V> getResultMap(String sql, Parameter[] params, Class<K> keyType, Class<V> valueType) {
		return getResultMap(sql, params, keyType, valueType, null);
	}

	public <K, V> Map<K, V> getResultMap(String sql, List<Parameter> params, Class<K> keyType, Class<V> valueType, Map<K, V> map) {
		return getResultMap(sql, params.toArray(Parameter.EMPTY_ARRAY), keyType, valueType, map);
	}

	public <K, V> Map<K, V> getResultMap(String sql, Parameter[] params, Class<K> keyType, Class<V> valueType, Map<K, V> map) {
		checkLock();
		try(ResultSet rs = executePreparedQuery(sql, params)) {
			return getResultMap(rs, keyType, valueType, map);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			closeLastStatement();
			resetLastUsed();
		}
	}

	private static <K, V> Map<K, V> getResultMap(ResultSet rs, Class<K> keyType, Class<V> valueType, Map<K, V> map) throws SQLException {
		if( !rs.next() ) {
			return map==null ? Collections.emptyMap() : map;
		}
		else {
			if( map==null )
				map = new LinkedHashMap<>();
			if( valueType.isArray() ) {
				int len = rs.getMetaData().getColumnCount();
				do {
					map.put(getSqlValue(rs, 1, keyType), getArray(rs, 1, len, valueType));
				} while(rs.next());
			}
			else {
				do {
					map.put(getSqlValue(rs, 1, keyType), getSqlValue(rs, 2, valueType));
				} while(rs.next());
			}
			return map;
		}
	}

	public <K, V> Map<K, List<V>> getResultListMap(String sql, Class<K> keyType, Class<V> valueType) {
		checkLock();
		try(ResultSet rs = getStaticStmt().executeQuery(sql)) {
			return getResultListMap(rs, keyType, valueType);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public <K, V> Map<K, List<V>> getResultListMap(String sql, List<Parameter> params, Class<K> keyType, Class<V> valueType) {
		return getResultListMap(sql, params.toArray(Parameter.EMPTY_ARRAY), keyType, valueType);
	}

	public <K, V> Map<K, List<V>> getResultListMap(String sql, Parameter[] params, Class<K> keyType, Class<V> valueType) {
		checkLock();
		try(ResultSet rs = executePreparedQuery(sql, params)) {
			return getResultListMap(rs, keyType, valueType);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			closeLastStatement();
			resetLastUsed();
		}
	}

	private static <K, V> Map<K, List<V>> getResultListMap(ResultSet rs, Class<K> keyType, Class<V> valueType) throws SQLException {
		if( !rs.next() ) {
			return Collections.emptyMap();
		}
		else {
			Map<K, List<V>> map = new LinkedHashMap<>();
			if( valueType.isArray() ) {
				int len = rs.getMetaData().getColumnCount();
				do {
					K key = getSqlValue(rs, 1, keyType);
					List<V> valueList = map.get(key);
					if( valueList==null )
						map.put(key, valueList = new ArrayList<>());
					valueList.add(getArray(rs, 1, len, valueType));
				} while(rs.next());
			}
			else {
				do {
					K key = getSqlValue(rs, 1, keyType);
					List<V> valueList = map.get(key);
					if( valueList==null )
						map.put(key, valueList = new ArrayList<>());
					valueList.add(getSqlValue(rs, 2, valueType));
				} while(rs.next());
			}
			return map;
		}		
	}

	private <T> List<T> mapToList(ResultSet rs, Function<ResultSet, T> mapper) throws SQLException {
		if( !rs.next() ) {
			return Collections.emptyList();
		}
		else {
			List<T> list = new ArrayList<>(20);
			do {
				T ret = mapper.apply(rs);
				if( ret!=null )
					list.add( ret );
			} while( rs.next() );
			return list;
		}
	}

	private void consume(ResultSet rs, Consumer<ResultSet> consumer) throws SQLException {
		while( rs.next() ){
			consumer.accept(rs);
		}
	}

	public ResultSet executeQuery(String sql) {
		checkLock();
		try{
			return createQueryStatement().executeQuery(sql);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public <T> List<T> executeQuery(String sql, Function<ResultSet, T> mapper) {
		try(ResultSet rs = getStaticStmt().executeQuery(sql)) {
			return mapToList(rs, mapper);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public void executeQuery(String sql, Consumer<ResultSet> consumer) {
		try(ResultSet rs = getStaticStmt().executeQuery(sql)) {
			consume(rs, consumer);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public ResultSet executeQuery(String sql, int offset, int limit) {
		return executeQuery(sql, offset, limit, (OutParameter<Integer>)null);
	}

	public <T> List<T> executeQuery(String sql, int offset, int limit, Function<ResultSet, T> mapper) {
		return executeQuery(sql, offset, limit, (OutParameter<Integer>)null, mapper);
	}

	public void executeQuery(String sql, int offset, int limit, Consumer<ResultSet> consumer) {
		executeQuery(sql, offset, limit, (OutParameter<Integer>)null, consumer);
	}

	public ResultSet executeQuery(String sql, int offset, int limit, OutParameter<Integer> totalCount) {
		return executeQuery(sql, offset, limit, totalCount, false);
	}

	public <T> List<T> executeQuery(String sql, int offset, int limit, OutParameter<Integer> totalCount, Function<ResultSet, T> mapper) {
		try(ResultSet rs = executeQuery(sql, offset, limit, totalCount, true)) {
			return mapToList(rs, mapper);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public void executeQuery(String sql, int offset, int limit, OutParameter<Integer> totalCount, Consumer<ResultSet> consumer) {
		try(ResultSet rs = executeQuery(sql, offset, limit, totalCount, true)) {
			consume(rs, consumer);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	private ResultSet executeQuery(String sql, int offset, int limit, OutParameter<Integer> totalCount, boolean useStatic) {
		checkLock();
		try{
			sql = sql.replaceAll("\\s+", " ");
			Matcher matcher = selectPattern.matcher(sql);
			if( !matcher.find() ) {
				throw new SQLException("query needs SELECT and ORDER BY statements : [" + sql + "]");
			}
			Statement stmt;
			if( useStatic ) {
				stmt = getStaticStmt();
			}
			else {
				stmt = createQueryStatement();
				stmt.setFetchSize(limit);
			}
			int totalCountInt = 0;
			if( totalCount!=null ) {
				sql = matcher.replaceAll("SELECT COUNT(*) FROM $2");
				try(ResultSet rs = stmt.executeQuery(sql)) {
					rs.next();
					totalCountInt = rs.getInt(1);
				}
			}
			//"SELECT $1 FROM (SELECT => is problematic. When there is such a "case when then" it gives error.
			sql = matcher.replaceAll("SELECT * FROM (SELECT $1, ROW_NUMBER() OVER(ORDER BY $3) AS ROW_NUM FROM $2) INNER_TABLE WHERE ROW_NUM BETWEEN " + (offset+1) + " AND " + (offset+limit));
			ResultSet rs = stmt.executeQuery(sql);
			if( totalCount!=null )
				totalCount.setValue(totalCountInt);
			return rs;
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public ResultSet executePreparedQuery(String sql, List<Parameter> params) {
		return executePreparedQuery(sql, params.toArray(Parameter.EMPTY_ARRAY));
	}

	public <T> List<T> executePreparedQuery(String sql, List<Parameter> params, Function<ResultSet, T> mapper) {
		return executePreparedQuery(sql, params.toArray(Parameter.EMPTY_ARRAY), mapper);
	}

	public void executePreparedQuery(String sql, List<Parameter> params, Consumer<ResultSet> consumer) {
		executePreparedQuery(sql, params.toArray(Parameter.EMPTY_ARRAY), consumer);
	}

	public ResultSet executePreparedQuery(String sql, Parameter[] params) {
		checkLock();
		try{
			PreparedStatement ps = createQueryPrepared(sql);
			if( params!=null ) {
				for(int i=0; i<params.length; i++) {
					Parameter param = params[i];
					if(param.isInput() ) {
						ps.setObject(i+1, param.getObject(), param.getArgType() );
					}
				}
			}
			return ps.executeQuery();
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public <T> List<T> executePreparedQuery(String sql, Parameter[] params, Function<ResultSet, T> mapper) {
		try {
			ResultSet rs = executePreparedQuery(sql, params);
			return mapToList(rs, mapper);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			closeLastStatement();
			resetLastUsed();
		}
	}

	public void executePreparedQuery(String sql, Parameter[] params, Consumer<ResultSet> consumer) {
		try {
			ResultSet rs = executePreparedQuery(sql, params);
			consume(rs, consumer);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			closeLastStatement();
			resetLastUsed();
		}
	}

	public ResultSet executePreparedQuery(String sql, List<Parameter> params, int offset, int limit) {
		return executePreparedQuery(sql, params.toArray(Parameter.EMPTY_ARRAY), offset, limit);
	}

	public <T> List<T> executePreparedQuery(String sql, List<Parameter> params, int offset, int limit, Function<ResultSet, T> mapper) {
		return executePreparedQuery(sql, params.toArray(Parameter.EMPTY_ARRAY), offset, limit, mapper);
	}

	public void executePreparedQuery(String sql, List<Parameter> params, int offset, int limit, Consumer<ResultSet> consumer) {
		executePreparedQuery(sql, params.toArray(Parameter.EMPTY_ARRAY), offset, limit, consumer);
	}

	public ResultSet executePreparedQuery(String sql, Parameter[] params, int offset, int limit) {
		checkLock();
		try{
			sql = sql.replaceAll("\\s+", " ");
			Matcher matcher = selectPattern.matcher(sql);
			if( !matcher.find() ) {
				throw new SQLException("query needs SELECT and ORDER BY and ASC and DESC statements : [" + sql + "]");
			}
			sql = matcher.replaceAll("SELECT $1 FROM  (SELECT $1, ROW_NUMBER() OVER(ORDER BY $3) AS ROW_NUM FROM $2) INNER_TABLE WHERE ROW_NUM BETWEEN ? AND ?");

			Parameter offsetParam = new Parameter(offset+1, Types.INTEGER);
			Parameter limitParam = new Parameter(offset+limit, Types.INTEGER);
			Parameter params2[];
			if( params==null || params.length==0 ) {
				params2 = new Parameter[] {offsetParam, limitParam};
			}
			else {
				params2 = new Parameter[ params.length + 2 ];
				System.arraycopy(params, 0, params2, 0, params.length);
				params2[params.length] = offsetParam;
				params2[params.length+1] = limitParam;
			}
			params = params2;
			PreparedStatement ps  = createQueryPrepared(sql);
			ps.setFetchSize(limit);
			if( params!=null ) {
				for(int i=0; i<params.length; i++) {
					Parameter param = params[i];
					if( param.isInput() ) {
						ps.setObject(i+1, param.getObject(), param.getArgType() );
					}
				}
			}
			return ps.executeQuery();
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public <T> List<T> executePreparedQuery(String sql, Parameter[] params, int offset, int limit, Function<ResultSet, T> mapper) {
		try {
			ResultSet rs = executePreparedQuery(sql, params, offset, limit);
			return mapToList(rs, mapper);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			closeLastStatement();
			resetLastUsed();
		}
	}

	public void executePreparedQuery(String sql, Parameter[] params, int offset, int limit, Consumer<ResultSet> consumer) {
		try{
			ResultSet rs = executePreparedQuery(sql, params, offset, limit);
			consume(rs, consumer);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			closeLastStatement();
			resetLastUsed();
		}
	}

	public ResultSet executeCallable(String sql, Parameter[] params) {
		checkLock();
		ResultSet rs = null;
		try{
			CallableStatement cstmt = createQueryCallable(sql);
			if( params!=null ) {
				for(int i=0; i<params.length; i++) {
					Parameter param = params[i];
					if( param.isInput() ) {
						cstmt.setObject(i+1, param.getObject(), param.getArgType() );
					}
					if( param.isOutput() ) {
						cstmt.registerOutParameter(i+1, param.getArgType() );
					}
				}
			}

			boolean hasResultSet = cstmt.execute();
			if( hasResultSet ) {
				rs = cstmt.getResultSet();
				cstmt.getMoreResults(Statement.KEEP_CURRENT_RESULT );
			}
			if( params!=null ) {
				for(int i=0; i<params.length; i++) {
					Parameter param = params[i];
					if( param.isOutput() ) {
						param.setObject( cstmt.getObject(i+1) );
					}
				}
			}

			if( !hasResultSet ) {
				closeLastStatement();
			}
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
		return rs;
	}

	public <T> List<T> executeCallable(String sql, Parameter[] params, Function<ResultSet, T> mapper) {
		try {
			ResultSet rs = executeCallable(sql, params);
			return mapToList(rs, mapper);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			closeLastStatement();
			resetLastUsed();
		}
	}

	public void executeCallable(String sql, Parameter[] params, Consumer<ResultSet> consumer) {
		try {
			ResultSet rs = executeCallable(sql, params);
			consume(rs, consumer);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			closeLastStatement();
			resetLastUsed();
		}
	}

	private long _executeUpdate(String sql, boolean returnIdentity) {
		checkLock();
		try{
			long result;
			if( returnIdentity ) {
				// CAREFULL! use lowercase insert instead of uppercase INSERT, because of a bug in jtds
				// fixed
				String newSql = sql.trim();
				if( newSql.startsWith("I") ) {
					newSql = "i" + newSql.substring(1);
				}
				getStaticStmt().executeUpdate(newSql, Statement.RETURN_GENERATED_KEYS);
				try(ResultSet rs = staticStmt.getGeneratedKeys()) {
					rs.next();
					result = rs.getLong(1);
				}
			}
			else {
				result = getStaticStmt().executeUpdate(sql);
			}
			return result;
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public int executeUpdate(String sql) {
		return (int)_executeUpdate(sql, false);
	}

	public long executeUpdateAndReturnId(String sql) {
		return _executeUpdate(sql, true);
	}

	public int[] executeBatch(String... sqls) {//only update and delete
		if( sqls.length==1 )
			return new int[] { executeUpdate(sqls[0]) };
		checkLock();
		Statement stmt = null;
		try {
			stmt = getStaticStmt();
			stmt.clearBatch();
			for(String sql : sqls ) {
				stmt.addBatch(sql);
			}
			return stmt.executeBatch();
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			try { stmt.clearBatch(); } catch(Exception inex) { }
			resetLastUsed();
		}
	}

	public int[] executeBatch(List<String> sqls) {//only update and delete
		return executeBatch(sqls.toArray(new String[0]));
	}

	private static final Parameter[][] EMPTY_PARAM_LIST = new Parameter[0][];
	public int[] executePreparedBatch(String sql, List<Parameter[]> paramList) {//only update and delete
		return executePreparedBatch(sql, paramList.toArray(EMPTY_PARAM_LIST));
	}

	public int[] executePreparedBatch(String sql, Parameter[][] paramList) {//only update and delete
		if( paramList.length==1 )
			return new int[] { executePreparedUpdate(sql, paramList[0]) };
		checkLock();
		try(PreparedStatement pstmt = createQueryPrepared(sql)) {
			if( paramList!=null ) {
				for(Parameter[] params :  paramList) {
					for(int i=0; i<params.length; i++) {
						pstmt.setObject(i+1, params[i].getObject(), params[i].getArgType() );
					}
					pstmt.addBatch();
				}
			}
			return pstmt.executeBatch();
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	private long _executePreparedUpdate(String sql, Parameter[] params, boolean returnIdentity) {
		checkLock();
		// because of jtds
		String newSql = sql.trim();
		if( newSql.startsWith("I") ) {
			newSql = "i" + newSql.substring(1);
		}
		try(PreparedStatement pstmt = connection.prepareStatement(newSql,returnIdentity ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {
			long result;
			pstmt.setQueryTimeout(queryTimeout);

			if( params!=null ) {
				for(int i=0; i<params.length; i++) {
					Parameter param = params[i];
					if( param.isInput() ) {
						pstmt.setObject(i+1, param.getObject(), param.getArgType() );
					}
				}
			}
			if( returnIdentity ) {
				pstmt.executeUpdate();
				try(ResultSet rs = pstmt.getGeneratedKeys()) {
					rs.next();
					result = rs.getLong(1);
				}
			}
			else {
				result = pstmt.executeUpdate();
			}
			lastStmt = null;
			return result;
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public int executePreparedUpdate(String sql, List<Parameter> params) {
		return (int)_executePreparedUpdate(sql, params.toArray(new Parameter[0]), false);
	}

	public int executePreparedUpdate(String sql, Parameter[] params) {
		return (int)_executePreparedUpdate(sql, params, false);
	}

	public long executePreparedUpdateAndReturnId(String sql, Parameter[] params) {
		return _executePreparedUpdate(sql, params, true);
	}

	public void rollbackSilently() {
		try { rollback(); } catch(Exception ex) { /*ignore*/}
	}

	public void rollback() {
		checkLock();
		if( !hasActiveTransaction() )
			return;
		try {
			connection.rollback();
			connection.setAutoCommit(true);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public void commit() {
		checkLock();
		if( !hasActiveTransaction() )
			return;
		try {
			connection.commit();
			connection.setAutoCommit(true);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public void setAutoCommit(boolean autoCommit) {
		try {
			connection.setAutoCommit(autoCommit);
		}
		catch(SQLException ex) {
			throw Utils.runtime(ex);
		}
		finally {
			resetLastUsed();
		}
	}

	public void beginTransaction() {
		if( !hasActiveTransaction() )
			setAutoCommit(false);
	}

	public boolean hasActiveTransaction() {
		try {
			return !connection.getAutoCommit();
		}
		catch (SQLException e) {
			throw Utils.runtime(e);
		}
	}

	public boolean isClosed() {
		try {
			return connection==null || connection.isClosed();
		} catch (SQLException e) {
			return false;
		}
	}

	public static String escapeString(String field) {
		if( field==null )
			return "";
		StringBuilder sb = new StringBuilder( field );
		for(int i=0; i<sb.length(); i++) {
			char ch = sb.charAt(i);
			switch(ch) {
				case '\'' :sb.insert(i, '\'');
							i++;
							break;
			}
		}
		return sb.toString();
	}

	public static String escapeStringForLike(String field) {
		if( field==null )
			return "";
		StringBuilder sb = new StringBuilder( field );
		for(int i=0; i<sb.length(); i++) {
			char ch = sb.charAt(i);
			if( ch=='\'' )
				sb.insert(i++, '\'');
			else if( ch=='%' || ch=='_' || ch=='[')
				sb.insert(i++, '/');
		}
		return sb.toString();
	}

	/*
	 * Example :
	 * sql : SELECT {ID}, {NAME} FROM {TABLE} WHERE {ID} = 5
	 * map : {ID:"COLUMN_ID", NAME:"COLUMN_NAME", TABLE:"TABLE_NAME"
	 * return : SELECT COLUMN_ID, COLUMN_NAME FROM TABLE_NAME WHERE COLUMN_ID = 5
	 */
	public static String rewriteSql(String sql, Map<String, String> map) {
		if(StringUtil.empty(sql) || map==null)
			return null;
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		CharacterIterator iter = new StringCharacterIterator(sql);
		char ch = iter.current();
		do {
			if(ch=='{') {
				sb2.setLength(0);
				while( (ch=iter.next())!='}' ) {
					if( ch==CharacterIterator.DONE ) {
						throw new RuntimeException("malformed sql");
					}
					sb2.append(ch);
				}
				if( !map.containsKey(sb2.toString()) )
					throw new RuntimeException("map does not containe value for : " + sb2.toString());
				String value = map.get( sb2.toString() );
				sb.append(value);
			}
			else {
				sb.append(ch);
			}
		} while( (ch=iter.next())!=CharacterIterator.DONE );
		return sb.toString();
	}

	public static String forSql(Collection<?> collect, String methodName) {
		if( collect==null || collect.isEmpty() )
			return null;
		try {
			List<?> list = CollectionUtil.propertyList(collect, methodName);
			return forSql(list);
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	public static String forSql(Collection<?> collect) {
		if( collect==null || collect.isEmpty() )
			return null;
		Class<?> clas = collect.iterator().next().getClass();
		Set<String> set = new HashSet<String>( collect.size() );

		if( String.class.isAssignableFrom(clas) ) {
			for( Object t : collect ) {
				set.add( forSql((String)t ) );
			}
		}
		else if( Number.class.isAssignableFrom(clas) ) {
			for( Object t : collect ) {
				set.add( forSql((Number)t) );
			}
		}
		else if( Character.class.isAssignableFrom(clas) ) {
			for( Object t : collect ) {
				set.add( forSql((char)t) );
			}
		}
		else if( Boolean.class.isAssignableFrom(clas) ) {
			for( Object t : collect ) {
				set.add( forSql((boolean)t) );
			}
		}
		else if( java.sql.Date.class.isAssignableFrom(clas) ) {
			for( Object t : collect ) {
				set.add( forSql((java.sql.Date)t ) );
			}
		}
		else if( LocalDate.class.isAssignableFrom(clas) ) {
			for( Object t : collect ) {
				set.add( forSql((LocalDate)t ) );
			}
		}
		else if( Time.class.isAssignableFrom(clas) ) {
			for( Object t : collect ) {
				set.add( forSql((Time)t ) );
			}
		}
		else if( LocalTime.class.isAssignableFrom(clas) ) {
			for( Object t : collect ) {
				set.add( forSql((LocalTime)t ) );
			}
		}
		else if( Timestamp.class.isAssignableFrom(clas) ) {
			for( Object t : collect ) {
				set.add( forSql((Timestamp)t ) );
			}
		}
		else if( LocalDateTime.class.isAssignableFrom(clas) ) {
			for( Object t : collect ) {
				set.add( forSql((LocalDateTime)t ) );
			}
		}
		else if( Date.class.isAssignableFrom(clas) ) {
			for( Object t : collect ) {
				set.add( forSql(new Timestamp( ((Date)t).getTime() ) ) );
			}
		}
		else {
			throw new RuntimeException("Unsupported type : " + clas.getCanonicalName());
		}

		StringBuilder sb = new StringBuilder();
		sb.append('(');
		for( String t : set ) {
			sb.append( t ).append(',');
		}
		sb.setCharAt(sb.length()-1, ')');
		return sb.toString();
	}

	public static String forSql(Character ch) {
		return "N'" + ch + "'";
	}

	public static String forSql(char ch) {
		return "N'" + ch + "'";
	}

	public static String forSql(Boolean bool) {
		return bool ? "1" : "0";
	}

	public static String forSql(boolean bool) {
		return bool ? "1" : "0";
	}

	public static String forSql(Number number) {
		return String.valueOf(number);
	}

	public static String forSql(Number number1, Number number2) {
		return "BETWEEN " + number1 +" AND " + number2;
	}

	public static String forSql(String field) {
		if( StringUtil.empty(field) )
			return "NULL";
		return "N'" + escapeString(field) + '\'';
	}

	public static String forSql(java.sql.Date date) {
		return "'" + DATE_FDF.format(date) + "'";
	}

	public static String forSql(java.sql.Date date1, java.sql.Date date2) {
		return "BETWEEN '" + DATE_FDF.format(date1) + "' AND '" + DATE_FDF.format(date2) + "'";
	}

	public static String forSql(LocalDate date) {
		return "'" + date.format(LOCALDATE_DTF) + "'";
	}

	public static String forSql(LocalDate date1, LocalDate date2) {
		return "BETWEEN '" + date1.format(LOCALDATE_DTF) + "' AND '" + date2.format(LOCALDATE_DTF) + "'";
	}

	public static String forSql(Time time) {
		return "'" + TIME_FDF.format(time) + "'";
	}

	public static String forSql(Time time1, Time time2) {
		return "BETWEEN '" + TIME_FDF.format(time1) + "' AND '" + TIME_FDF.format(time2) + "'";
	}

	public static String forSql(LocalTime time) {
		return "'" + time.format(LOCALTIME_DTF) + "'";
	}

	public static String forSql(LocalTime time1, LocalTime time2) {
		return "BETWEEN '" + time1.format(LOCALTIME_DTF) + "' AND '" + time2.format(LOCALTIME_DTF) + "'";
	}

	public static String forSql(Timestamp timestamp) {
		return "'" + TIMESTAMP_FDF.format(timestamp) + "'";
	}

	public static String forSql(Timestamp timestamp1, Timestamp timestamp2) {
		return "BETWEEN '" + TIMESTAMP_FDF.format(timestamp1) + "' AND '" + TIMESTAMP_FDF.format(timestamp2) + "'";
	}

	public static String forSql(LocalDateTime date) {
		return "'" + date.format(LOCALDATETIME_DTF) + "'";
	}

	public static String forSql(LocalDateTime dateTime1, LocalDateTime dateTime2) {
		return "BETWEEN '" + dateTime1.format(LOCALDATETIME_DTF) + "' AND '" + dateTime2.format(LOCALDATETIME_DTF) + "'";
	}

	public static String forSql(Object obj) {
		if( obj==null )
			return "NULL";
		if( obj instanceof String )
			return forSql((String)obj);
		if( obj instanceof Number )
			return forSql((Number)obj);
		if( obj instanceof java.sql.Date )
			return forSql((java.sql.Date)obj);
		if( obj instanceof LocalDate )
			return forSql((LocalDate)obj);
		if( obj instanceof Time )
			return forSql((Time)obj);
		if( obj instanceof LocalTime )
			return forSql((LocalTime)obj);
		if( obj instanceof Timestamp )
			return forSql((Timestamp)obj);
		if( obj instanceof LocalDateTime )
			return forSql((LocalDateTime)obj);
		if( obj instanceof Date )
			return forSql((new Timestamp(((Date)obj).getTime())));
		if( obj instanceof Boolean )
			return forSql((boolean)obj);
		if( obj instanceof Character )
			return forSql((char)obj);
		if( obj instanceof Collection<?> )
			return forSql((Collection<?>) obj);
		if( obj.getClass().isArray() )
			return forSqlUsingArray(obj);
		throw new IllegalArgumentException(obj.getClass() + " : " + obj);
	}

	private static String forSqlUsingArray(Object arr) {
		int len = Array.getLength(arr);
		if( len==0 )
			return null;
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		for(int i=0; i<len; i++) {
			sb.append( forSql( Array.get(arr, i) ) ).append(',');
		}
		sb.setCharAt(sb.length()-1, ')');
		return sb.toString();
	}

	public static String forLike(String field) {
		return "LIKE N'%" + escapeStringForLike(field) + "%' ESCAPE '/'";
	}

	public static String forLikeFromEnd(String field) {
		return "LIKE N'" + escapeStringForLike(field) + "%' ESCAPE '/'";
	}

	public static String forLikeFromBegin(String field) {
		return "LIKE N'%" + escapeStringForLike(field) + "' ESCAPE '/'";
	}

	public static String getClobData(Clob clob) {
		if(clob==null)
			return "";
		try {
			Reader reader = clob.getCharacterStream();
			if(reader==null)
				return "";
			StringBuilder sb = new StringBuilder( );
			char[] buff = new char[2048];
			int read;
			while((read=reader.read(buff))>-1)
				sb.append(buff, 0, read);
			return sb.toString();
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		}
		catch(Exception ex) { }
	}

	public boolean validate() {
		try {
			getStaticStmt().executeQuery("SELECT 1");
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	public static int getSqlType(Class<?> type) {
		if( short.class==type || int.class==type || byte.class==type ||
				Short.class==type || Integer.class==type || Byte.class==type)
			return Types.INTEGER;
		if( long.class==type || Long.class==type || BigInteger.class==type )
			return Types.BIGINT;
		if( boolean.class==type || Boolean.class==type )
			return Types.BOOLEAN;
		if( String.class==type || char.class==type || Character.class==type )
			return Types.VARCHAR;
		else if( float.class==type || Float.class==type )
			return Types.FLOAT;
		else if( double.class==type || Double.class==type )
			return Types.DOUBLE;
		else if( java.sql.Date.class==type || LocalDate.class==type )
			return Types.DATE;
		else if( Time.class==type || LocalTime.class==type )
			return Types.TIME;
		else if( Timestamp.class==type || Date.class==type || LocalDateTime.class==type )
			return Types.TIMESTAMP;
		return Types.OTHER;
	}

	public static <T> T getSqlValue(ResultSet rs, Class<T> type) throws SQLException {
		return getSqlValue(rs, 1, type);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getSqlValue(ResultSet rs, int ind, Class<T> type) throws SQLException {
		Object result = null;
		if( String.class==type )
			result = rs.getString(ind);
		else if( short.class==type || Short.class==type )
			result = rs.getShort(ind);
		else if( int.class==type || Integer.class==type )
			result = rs.getInt(ind);
		else if( long.class==type || Long.class==type )
			result = rs.getLong(ind);
		else if( float.class==type || Float.class==type )
			result = rs.getFloat(ind);
		else if( double.class==type || Double.class==type )
			result = rs.getDouble(ind);
		else if( boolean.class==type || Boolean.class==type )
			result = rs.getBoolean(ind);
		else if( byte.class==type || Byte.class==type )
			result = rs.getByte(ind);
		else if( char.class==type || Character.class==type )
			result = rs.getString(ind).charAt(0);
		else if( java.sql.Date.class==type )
			result = rs.getDate(ind);
		else if( LocalDate.class==type )
			result = rs.getDate(ind).toLocalDate();
		else if( java.sql.Time.class==type )
			result = rs.getTime(ind);
		else if( LocalTime.class==type )
			result = rs.getTime(ind).toLocalTime();
		else if( Timestamp.class==type || Date.class==type )
			result = rs.getTimestamp(ind);
		else if( LocalDateTime.class==type )
			result = rs.getTimestamp(ind).toLocalDateTime();
		else
			result = rs.getObject(ind);
		return (T)result;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getSqlValue(ResultSet rs, String colName, Class<T> type) throws SQLException {
		Object result = null;
		if( String.class==type )
			result = rs.getString(colName);
		else if( short.class==type || Short.class==type )
			result = rs.getShort(colName);
		else if( int.class==type || Integer.class==type )
			result = rs.getInt(colName);
		else if( long.class==type || Long.class==type )
			result = rs.getLong(colName);
		else if( float.class==type || Float.class==type )
			result = rs.getFloat(colName);
		else if( double.class==type || Double.class==type )
			result = rs.getDouble(colName);
		else if( boolean.class==type || Boolean.class==type )
			result = rs.getBoolean(colName);
		else if( byte.class==type || Byte.class==type )
			result = rs.getByte(colName);
		else if( char.class==type || Character.class==type )
			result = rs.getString(colName).charAt(0);
		else if( java.sql.Date.class==type )
			result = rs.getDate(colName);
		else if( LocalDate.class==type )
			result = rs.getDate(colName).toLocalDate();
		else if( Time.class==type )
			result = rs.getTime(colName);
		else if( LocalTime.class==type )
			result = rs.getTime(colName).toLocalTime();
		else if( Timestamp.class==type || Date.class==type )
			result = rs.getTimestamp(colName);
		else if( LocalDateTime.class==type )
			result = rs.getTimestamp(colName).toLocalDateTime();
		else
			result = rs.getObject(colName);
		return (T)result;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getDefaultSqlValue(ResultSet rs, Class<T> type) throws SQLException {
		Object result = null;
		if( short.class==type )
			result = (short)0;
		else if( int.class==type )
			result = (int)0;
		else if( long.class==type )
			result = 0l;
		else if( float.class==type )
			result = 0f;
		else if( double.class==type )
			result = 0d;
		else if( boolean.class==type )
			result = Boolean.FALSE;
		else if( byte.class==type)
			result = (byte)0;
		else if( char.class==type)
			result = (char)0;
		return (T)result;
	}

	public static int getIntValue(ResultSet rs, String name, int def) throws SQLException {
		int result = rs.getInt(name);
		return rs.wasNull() ? def : result;
	}

	public static long getLongValue(ResultSet rs, String name, long def) throws SQLException {
		long result = rs.getLong(name);
		return rs.wasNull() ? def : result;
	}

	public static boolean getBooleanValue(ResultSet rs, String name, boolean def) throws SQLException {
		boolean result = rs.getBoolean(name);
		return rs.wasNull() ? def : result;
	}

	public void setDefaults() {
		setAutoCommit(true);
		resetQueryTimeout();
		resetFetchSize();
		resetScrollType();
		resetConcurencyType();
		resetTransactionIsolation();
	}

	public static void registerDriver() {
		try {
			Class.forName("net.sourceforge.jtds.jdbc.Driver");
		}
		catch (Exception e) {
			throw Utils.runtime(e);
		}
	}

	public static void unregisterDriver() {
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		while (drivers.hasMoreElements()) {
			try {
				DriverManager.deregisterDriver(drivers.nextElement());
			} catch (SQLException e) { }
		}
	}
}