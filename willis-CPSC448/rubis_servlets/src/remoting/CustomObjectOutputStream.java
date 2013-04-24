package remoting;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CustomObjectOutputStream extends ObjectOutputStream {

	public CustomObjectOutputStream(OutputStream out) throws IOException  {
		super(out);
		super.enableReplaceObject(true);
	}
	
	@Override
	protected Object replaceObject(Object obj) throws IOException {
		if(obj instanceof Connection) {
			return new ConnectionProxy();
		} if(obj instanceof java.io.PrintWriter) {
			return new remoting.PrintWriter((java.io.PrintWriter) obj);
		} else if(obj instanceof java.sql.PreparedStatement) {
			// All PreparedStatements have been replaced by PreparedStatementWrapper
			// at creation time by the MainAspect. This creates the proxy
			// object based on the wrapper.
			PreparedStatementWrapper wrap = (PreparedStatementWrapper)obj;
			return new remoting.PreparedStatementProxy(wrap.getQueryString(), 
													   wrap.getStoredConnection(),
													   wrap.getMethodList());
		} else {
			return obj;
		}
	}

}
