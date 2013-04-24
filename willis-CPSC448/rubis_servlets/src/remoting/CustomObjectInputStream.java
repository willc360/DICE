package remoting;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class CustomObjectInputStream extends ObjectInputStream {

	public CustomObjectInputStream(InputStream arg0) throws IOException {
		super(arg0);
		super.enableResolveObject(true);
	}
	
	protected Object resolveObject(Object obj) {
		if(obj instanceof remoting.PrintWriter) {
			remoting.PrintWriter pw = (remoting.PrintWriter) obj;
			return new java.io.PrintWriter(pw.getChars());
		}
		Object service = RemotingManager.getInstance().getService(obj.getClass().getName());
		return (service == null) ? obj : service;
	}
}
