package remoting;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.sql.Connection;

public class CustomObjectOutputStream extends ObjectOutputStream {

	public CustomObjectOutputStream(OutputStream out) throws IOException  {
		super(out);
		super.enableReplaceObject(true);
	}
	
	@Override
	protected Object replaceObject(Object obj) throws IOException {
		if(obj instanceof Connection) {
			return new ConnectionProxy();
		} else {
			return obj;
		}
	}

}
