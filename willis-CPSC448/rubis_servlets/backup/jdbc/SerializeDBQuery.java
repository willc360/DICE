package remoting.oracle.jdbc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.rowset.CachedRowSet;

import com.sun.rowset.CachedRowSetImpl;

//import net.jforum.JForumExecutionContext;
//import net.jforum.exceptions.DatabaseException;
//import net.jforum.util.DbUtils;
//import oracle.jdbc.rowset.OracleCachedRowSet;
import remoting.RemotingConstants;
import remoting.RemotingManager;

public class SerializeDBQuery {
	
	public static ResultSet downExecuteQuery(StatementWrapper stmt){
		
		Object obj = null;
		try
		{
		  obj = RemotingManager.getInstance().downCall(RemotingConstants.URL, SerializeDBQuery.class.getMethod("upExecuteQuery", 
				  new Class[] { StatementWrapper.class }), 
		    SerializeDBQuery.class.getName(), new Object[] { stmt });
		}
		catch (Exception e) {
		  e.printStackTrace();
		}
		return (obj != null) ? (ResultSet)obj : null;
		
	}
	
	public static ResultSet upExecuteQuery(StatementWrapper stmt) throws Exception {
		
		PreparedStatement p = null;
		ResultSet rs = null;
		try {
			
			p = DBUtil.getConnection().prepareStatement(stmt.getQueryString());

			for (int i = 0; i < stmt.getClazzArray().length; i++){
				
				if (stmt.getClazzArray()[i].isPrimitive()){
					
					String methodName = getMethodName(stmt.getClazzArray()[i].getName());
					Method m = p.getClass().getMethod(methodName, new Class[] {int.class, stmt.getClazzArray()[i]});
					m.setAccessible(Boolean.TRUE);
					m.invoke(p, new Object[]{ i+1 , stmt.getObjArray()[i]});
				}
			}

			rs = p.executeQuery();

			// modified for mysql
			CachedRowSet crset = new CachedRowSetImpl();
			crset.populate(rs);
			return crset;
		}
		catch (SQLException e) {
			throw new Exception(e);
		}
//		finally {
//			DbUtils.close(rs, p);
//		}
	}
	
	private static String getMethodName(String type){
		
		if(type.equals("byte")) return "setByte";
		if(type.equals("int")) return "setInt";
		if(type.equals("long")) return "setLong";
		if(type.equals("float")) return "setFloat";
		if(type.equals("double")) return "setDouble";
		if(type.equals("boolean")) return "setBoolean";
		if(type.equals("java.sql.Arrayy")) return "setArray";
		if(type.equals("java.math.BigDecimal")) return "setBigDecimal";
		if(type.equals("java.sql.Blob")) return "setBlob";
		if(type.equals("java.sql.Date")) return "setDate";
		if(type.equals("java.lang.Object")) return "setObject";
		if(type.equals("java.sql.Ref")) return "setRef";
		if(type.equals("short")) return "setShort";
		if(type.equals("java.lang.String")) return "setString";
		if(type.equals("java.sql.Timestamp")) return "setTimestamp";
		
		throw new RuntimeException("Object type not found");
	}
	
	public static void main(String args[]){
		System.out.println(int.class.getName());
	}

}
