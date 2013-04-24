package remoting.oracle.jdbc;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * The core/debug methods for the table. Also contains the default connection method.
 */
public class DBUtil {
	
	private static Connection con = null;
	
	/**
	 * If no connection exists, creates a new connection to the cs410 database, with
	 * username "root" and password "410"
	 * @return a Connection object
	 */
	public static Connection getConnection() {
		if(con == null) {
			try {
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				con = DriverManager.getConnection("jdbc:mysql://localhost:3306/rubis?user=root&password=");
			} catch(Exception e) {
				System.out.println("DBUtil Exception");
				e.printStackTrace();
			}
		}
		return con;
	}
}