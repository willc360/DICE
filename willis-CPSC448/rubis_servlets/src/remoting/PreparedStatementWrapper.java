package remoting;

import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class PreparedStatementWrapper implements PreparedStatement, Serializable {

	private transient PreparedStatement stmt;
	
	// The connection of this PreparedStatement
	private Connection conn;
	
	// Query string of the PreparedStatement
	private String query;
	
	// The method list of this PreparedStatement. Contains all the executed methods 
	// which could have changed the state of the statement object
	private ArrayList<ReflectionMethodWrapper> method_list;
	
	private static final Class P_STMT_WRAPPER_CLASS = PreparedStatementWrapper.class;
	
	/**
	 * Constructor
	 * 
	 * @param query
	 * @param ps
	 * @param conn
	 */
	public PreparedStatementWrapper(String query, Object ps, Object conn) {
		stmt = (PreparedStatement)ps;
		this.query = query;
		this.conn = (Connection)conn;
		method_list = new ArrayList<ReflectionMethodWrapper>();
	}
	
	/**
	 * Returns the connection object
	 * @return
	 */
	public Connection getStoredConnection() {
		return conn;
	}
	
	/**
	 * Returns the query string
	 * @return
	 */
	public String getQueryString() {
		return query;
	}
	
	/**
	 * Returns the method list
	 * @return
	 */
	public List<ReflectionMethodWrapper> getMethodList() {
		return method_list;
	}
	 
	@Override
	public void addBatch(String sql) throws SQLException {
		stmt.addBatch(sql);
		method_list.add(new ReflectionMethodWrapper("addBatch", P_STMT_WRAPPER_CLASS, 
				new Class[] {String.class}, new Object[] {sql}));
	}

	@Override
	public void cancel() throws SQLException {
		stmt.cancel();
		method_list.add(new ReflectionMethodWrapper("cancel", P_STMT_WRAPPER_CLASS, 
				new Class[] {}, new Object[] {}));
	}

	@Override
	public void clearBatch() throws SQLException {
		stmt.clearBatch();
		method_list.add(new ReflectionMethodWrapper("clearBatch", P_STMT_WRAPPER_CLASS, 
				new Class[] {}, new Object[] {}));
	}

	@Override
	public void clearWarnings() throws SQLException {
		stmt.clearWarnings();
		method_list.add(new ReflectionMethodWrapper("clearWarnings", P_STMT_WRAPPER_CLASS, 
				new Class[] {}, new Object[] {}));
	}

	@Override
	public void close() throws SQLException {
		stmt.close();
		method_list.add(new ReflectionMethodWrapper("close", P_STMT_WRAPPER_CLASS, 
				new Class[] {}, new Object[] {}));
	}

	@Override
	public void closeOnCompletion() throws SQLException {
		stmt.closeOnCompletion();
		method_list.add(new ReflectionMethodWrapper("closeOnCompletion", P_STMT_WRAPPER_CLASS, 
				new Class[] {}, new Object[] {}));
	}

	@Override
	public boolean execute(String sql) throws SQLException {
		return stmt.execute(sql);
	}

	@Override
	public boolean execute(String sql, int autoGeneratedKeys)
			throws SQLException {
		return stmt.execute(sql, autoGeneratedKeys);
	}

	@Override
	public boolean execute(String sql, int[] columnIndexes) throws SQLException {
		return stmt.execute(sql, columnIndexes);
	}

	@Override
	public boolean execute(String sql, String[] columnNames)
			throws SQLException {
		return stmt.execute(sql, columnNames);
	}

	@Override
	public int[] executeBatch() throws SQLException {
		return stmt.executeBatch();
	}

	@Override
	public ResultSet executeQuery(String sql) throws SQLException {
		return stmt.executeQuery();
	}

	@Override
	public int executeUpdate(String sql) throws SQLException {
		return stmt.executeUpdate(sql);
	}

	@Override
	public int executeUpdate(String sql, int autoGeneratedKeys)
			throws SQLException {
		return stmt.executeUpdate(sql, autoGeneratedKeys);
	}

	@Override
	public int executeUpdate(String sql, int[] columnIndexes)
			throws SQLException {
		return stmt.executeUpdate(sql, columnIndexes);
	}

	@Override
	public int executeUpdate(String sql, String[] columnNames)
			throws SQLException {
		return stmt.executeUpdate(sql, columnNames);
	}

	@Override
	public Connection getConnection() throws SQLException {
		return stmt.getConnection();
	}

	@Override
	public int getFetchDirection() throws SQLException {
		return stmt.getFetchDirection();
	}

	@Override
	public int getFetchSize() throws SQLException {
		return stmt.getFetchSize();
	}

	@Override
	public ResultSet getGeneratedKeys() throws SQLException {
		return stmt.getGeneratedKeys();
	}

	@Override
	public int getMaxFieldSize() throws SQLException {
		return stmt.getMaxFieldSize();
	}

	@Override
	public int getMaxRows() throws SQLException {
		return stmt.getMaxRows();
	}

	@Override
	public boolean getMoreResults() throws SQLException {
		return stmt.getMoreResults();
	}

	@Override
	public boolean getMoreResults(int current) throws SQLException {
		return stmt.getMoreResults(current);
	}

	@Override
	public int getQueryTimeout() throws SQLException {
		return stmt.getQueryTimeout();
	}

	@Override
	public ResultSet getResultSet() throws SQLException {
		return stmt.getResultSet();
	}

	@Override
	public int getResultSetConcurrency() throws SQLException {
		return stmt.getResultSetConcurrency();
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return stmt.getResultSetHoldability();
	}

	@Override
	public int getResultSetType() throws SQLException {
		return stmt.getResultSetType();
	}

	@Override
	public int getUpdateCount() throws SQLException {
		return stmt.getUpdateCount();
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return stmt.getWarnings();
	}

	@Override
	public boolean isCloseOnCompletion() throws SQLException {
		return stmt.isCloseOnCompletion();
	}

	@Override
	public boolean isClosed() throws SQLException {
		return stmt.isClosed();
	}

	@Override
	public boolean isPoolable() throws SQLException {
		return stmt.isPoolable();
	}

	@Override
	public void setCursorName(String name) throws SQLException {
		stmt.setCursorName(name);
		method_list.add(new ReflectionMethodWrapper("setCursorName", P_STMT_WRAPPER_CLASS, 
				new Class[] {String.class}, new Object[] {name}));
	}

	@Override
	public void setEscapeProcessing(boolean enable) throws SQLException {
		stmt.setEscapeProcessing(enable);
		method_list.add(new ReflectionMethodWrapper("setEscapeProcessing", P_STMT_WRAPPER_CLASS, 
				new Class[] {boolean.class}, new Object[] {enable}));
	}

	@Override
	public void setFetchDirection(int direction) throws SQLException {
		stmt.setFetchDirection(direction);
		method_list.add(new ReflectionMethodWrapper("setFetchDirection", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class}, new Object[] {direction}));
	}

	@Override
	public void setFetchSize(int rows) throws SQLException {
		stmt.setFetchSize(rows);
		method_list.add(new ReflectionMethodWrapper("setFetchSize", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class}, new Object[] {rows}));
	}

	@Override
	public void setMaxFieldSize(int max) throws SQLException {
		stmt.setMaxFieldSize(max);
		method_list.add(new ReflectionMethodWrapper("setMaxFieldSize", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class}, new Object[] {max}));
	}

	@Override
	public void setMaxRows(int max) throws SQLException {
		stmt.setMaxRows(max);
		method_list.add(new ReflectionMethodWrapper("setMaxRows", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class}, new Object[] {max}));
	}

	@Override
	public void setPoolable(boolean poolable) throws SQLException {
		stmt.setPoolable(poolable);
		method_list.add(new ReflectionMethodWrapper("setPoolable", P_STMT_WRAPPER_CLASS, 
				new Class[] {boolean.class}, new Object[] {poolable}));
	}

	@Override
	public void setQueryTimeout(int seconds) throws SQLException {
		stmt.setQueryTimeout(seconds);
		method_list.add(new ReflectionMethodWrapper("setQueryTimeout", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class}, new Object[] {seconds}));
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return stmt.isWrapperFor(iface);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return stmt.unwrap(iface);
	}

	@Override
	public void addBatch() throws SQLException {
		stmt.addBatch();
		method_list.add(new ReflectionMethodWrapper("addBatch", P_STMT_WRAPPER_CLASS, 
				new Class[] {}, new Object[] {}));
	}

	@Override
	public void clearParameters() throws SQLException {
		stmt.clearParameters();
		method_list.add(new ReflectionMethodWrapper("clearParameters", P_STMT_WRAPPER_CLASS, 
				new Class[] {}, new Object[] {}));
	}

	@Override
	public boolean execute() throws SQLException {
		return stmt.execute();
	}

	@Override
	public ResultSet executeQuery() throws SQLException {
		return stmt.executeQuery();
	}

	@Override
	public int executeUpdate() throws SQLException {
		return stmt.executeUpdate();
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return stmt.getMetaData();
	}

	@Override
	public ParameterMetaData getParameterMetaData() throws SQLException {
		return stmt.getParameterMetaData();
	}

	@Override
	public void setArray(int parameterIndex, Array x) throws SQLException {
		// TODO Auto-generated method stub
		stmt.setArray(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setArray", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, Array.class}, new Object[] {parameterIndex, x}));
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x)
			throws SQLException {
		// TODO Auto-generated method stub
		stmt.setAsciiStream(parameterIndex, x);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		// TODO Auto-generated method stub
		stmt.setAsciiStream(parameterIndex, x, length);
	}

	@Override
	public void setAsciiStream(int parameterIndex, InputStream x, long length)
			throws SQLException {
		// TODO Auto-generated method stub
		stmt.setAsciiStream(parameterIndex, x, length);
	}

	@Override
	public void setBigDecimal(int parameterIndex, BigDecimal x)
			throws SQLException {
		stmt.setBigDecimal(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setBigDecimal", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, BigDecimal.class}, new Object[] {parameterIndex, x}));
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x)
			throws SQLException {
		// TODO Auto-generated method stub
		stmt.setBinaryStream(parameterIndex, x);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		// TODO Auto-generated method stub
		stmt.setBinaryStream(parameterIndex, x, length);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x, long length)
			throws SQLException {
		// TODO Auto-generated method stub
		stmt.setBinaryStream(parameterIndex, x, length);
	}

	@Override
	public void setBlob(int parameterIndex, Blob x) throws SQLException {
		// TODO Auto-generated method stub
		stmt.setBlob(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setBlob", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, Blob.class}, new Object[] {parameterIndex, x}));
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream)
			throws SQLException {
		// TODO Auto-generated method stub
		stmt.setBlob(parameterIndex, inputStream);
	}

	@Override
	public void setBlob(int parameterIndex, InputStream inputStream, long length)
			throws SQLException {
		// TODO Auto-generated method stub
		stmt.setBlob(parameterIndex, inputStream, length);
	}

	@Override
	public void setBoolean(int parameterIndex, boolean x) throws SQLException {
		stmt.setBoolean(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setBoolean", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, boolean.class}, new Object[] {parameterIndex, x}));
	}

	@Override
	public void setByte(int parameterIndex, byte x) throws SQLException {
		stmt.setByte(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setByte", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, byte.class}, new Object[] {parameterIndex, x}));
	}

	@Override
	public void setBytes(int parameterIndex, byte[] x) throws SQLException {
		stmt.setBytes(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setBytes", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, byte[].class}, new Object[] {parameterIndex, x}));
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader)
			throws SQLException {
		// TODO Auto-generated method stub
		stmt.setCharacterStream(parameterIndex, reader);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader, int length)
			throws SQLException {
		// TODO Auto-generated method stub
		stmt.setCharacterStream(parameterIndex, reader, length);
	}

	@Override
	public void setCharacterStream(int parameterIndex, Reader reader,
			long length) throws SQLException {
		// TODO Auto-generated method stub
		stmt.setCharacterStream(parameterIndex, reader, length);
	}

	@Override
	public void setClob(int parameterIndex, Clob x) throws SQLException {
		// TODO Auto-generated method stub
		stmt.setClob(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setClob", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, Clob.class}, new Object[] {parameterIndex, x}));
	}

	@Override
	public void setClob(int parameterIndex, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		stmt.setClob(parameterIndex, reader);
	}

	@Override
	public void setClob(int parameterIndex, Reader reader, long length)
			throws SQLException {
		// TODO Auto-generated method stub
		stmt.setClob(parameterIndex, reader, length);
	}

	@Override
	public void setDate(int parameterIndex, Date x) throws SQLException {
		stmt.setDate(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setDate", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, Date.class}, new Object[] {parameterIndex, x}));
	}

	@Override
	public void setDate(int parameterIndex, Date x, Calendar cal)
			throws SQLException {
		stmt.setDate(parameterIndex, x, cal);
		method_list.add(new ReflectionMethodWrapper("setDate", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, Date.class, Calendar.class}, 
				new Object[] {parameterIndex, x, cal}));
	}

	@Override
	public void setDouble(int parameterIndex, double x) throws SQLException {
		stmt.setDouble(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setDouble", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, double.class}, 
				new Object[] {parameterIndex, x}));
	}

	@Override
	public void setFloat(int parameterIndex, float x) throws SQLException {
		stmt.setFloat(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setFloat", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, float.class}, 
				new Object[] {parameterIndex, x}));
	}

	@Override
	public void setInt(int parameterIndex, int x) throws SQLException {
		stmt.setInt(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setInt", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, int.class}, 
				new Object[] {parameterIndex, x}));
	}

	@Override
	public void setLong(int parameterIndex, long x) throws SQLException {
		stmt.setLong(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setLong", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, long.class}, 
				new Object[] {parameterIndex, x}));
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value)
			throws SQLException {
		// TODO Auto-generated method stub
		stmt.setNCharacterStream(parameterIndex, value);
	}

	@Override
	public void setNCharacterStream(int parameterIndex, Reader value,
			long length) throws SQLException {
		// TODO Auto-generated method stub
		stmt.setNCharacterStream(parameterIndex, value, length);
	}

	@Override
	public void setNClob(int parameterIndex, NClob value) throws SQLException {
		// TODO Auto-generated method stub
		stmt.setNClob(parameterIndex, value);
		method_list.add(new ReflectionMethodWrapper("setNClob", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, NClob.class}, 
				new Object[] {parameterIndex, value}));
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader) throws SQLException {
		// TODO Auto-generated method stub
		stmt.setNClob(parameterIndex, reader);
	}

	@Override
	public void setNClob(int parameterIndex, Reader reader, long length)
			throws SQLException {
		// TODO Auto-generated method stub
		stmt.setNClob(parameterIndex, reader, length);
	}

	@Override
	public void setNString(int parameterIndex, String value)
			throws SQLException {
		stmt.setNString(parameterIndex, value);
		method_list.add(new ReflectionMethodWrapper("setNString", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, String.class}, 
				new Object[] {parameterIndex, value}));
	}

	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		stmt.setNull(parameterIndex, sqlType);
		method_list.add(new ReflectionMethodWrapper("setNull", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, int.class}, 
				new Object[] {parameterIndex, sqlType}));
	}

	@Override
	public void setNull(int parameterIndex, int sqlType, String typeName)
			throws SQLException {
		stmt.setNull(parameterIndex, sqlType, typeName);
		method_list.add(new ReflectionMethodWrapper("setNull", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, int.class, String.class}, 
				new Object[] {parameterIndex, sqlType, typeName}));
	}

	@Override
	public void setObject(int parameterIndex, Object x) throws SQLException {
		stmt.setObject(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setObject", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, Object.class}, 
				new Object[] {parameterIndex, x}));
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType)
			throws SQLException {
		stmt.setObject(parameterIndex, x, targetSqlType);
		method_list.add(new ReflectionMethodWrapper("setObject", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, Object.class, int.class}, 
				new Object[] {parameterIndex, x, targetSqlType}));
	}

	@Override
	public void setObject(int parameterIndex, Object x, int targetSqlType,
			int scaleOrLength) throws SQLException {
		stmt.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
		method_list.add(new ReflectionMethodWrapper("setObject", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, Object.class, int.class, int.class}, 
				new Object[] {parameterIndex, x, targetSqlType, scaleOrLength}));
	}

	@Override
	public void setRef(int parameterIndex, Ref x) throws SQLException {
		stmt.setRef(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setRef", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, Ref.class}, 
				new Object[] {parameterIndex, x}));
	}

	@Override
	public void setRowId(int parameterIndex, RowId x) throws SQLException {
		stmt.setRowId(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setRowId", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, RowId.class}, 
				new Object[] {parameterIndex, x}));
	}

	@Override
	public void setSQLXML(int parameterIndex, SQLXML xmlObject)
			throws SQLException {
		stmt.setSQLXML(parameterIndex, xmlObject);
	}

	@Override
	public void setShort(int parameterIndex, short x) throws SQLException {
		stmt.setShort(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setShort", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, short.class}, 
				new Object[] {parameterIndex, x}));
	}

	@Override
	public void setString(int parameterIndex, String x) throws SQLException {
		stmt.setString(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setString", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, String.class}, 
				new Object[] {parameterIndex, x}));
	}

	@Override
	public void setTime(int parameterIndex, Time x) throws SQLException {
		stmt.setTime(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setTime", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, Time.class}, 
				new Object[] {parameterIndex, x}));
	}

	@Override
	public void setTime(int parameterIndex, Time x, Calendar cal)
			throws SQLException {
		stmt.setTime(parameterIndex, x, cal);
		method_list.add(new ReflectionMethodWrapper("setTime", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, Time.class, Calendar.class}, 
				new Object[] {parameterIndex, x, cal}));
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x)
			throws SQLException {
		stmt.setTimestamp(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setTimestamp", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, Timestamp.class}, 
				new Object[] {parameterIndex, x}));
	}

	@Override
	public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
			throws SQLException {
		stmt.setTimestamp(parameterIndex, x, cal);
		method_list.add(new ReflectionMethodWrapper("setTimestamp", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, Timestamp.class, Calendar.class}, 
				new Object[] {parameterIndex, x, cal}));
	}

	@Override
	public void setURL(int parameterIndex, URL x) throws SQLException {
		stmt.setURL(parameterIndex, x);
		method_list.add(new ReflectionMethodWrapper("setURL", P_STMT_WRAPPER_CLASS, 
				new Class[] {int.class, URL.class}, 
				new Object[] {parameterIndex, x}));
	}

	@Override
	public void setUnicodeStream(int parameterIndex, InputStream x, int length)
			throws SQLException {
		// TODO Auto-generated method stub
		stmt.setUnicodeStream(parameterIndex, x, length);
	}

}
