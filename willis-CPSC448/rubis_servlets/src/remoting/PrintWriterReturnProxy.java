package remoting;

import java.io.ObjectStreamException;
import java.io.OutputStream;

public class PrintWriterReturnProxy extends PrintWriter implements Identifiable {

	private String id; 
	private byte[] buffer;
	
	public PrintWriterReturnProxy() {
	}
	
	public PrintWriterReturnProxy(String id, byte[] buffer) {
		this.id = id;
		this.buffer = buffer;
	}
	
	@Override
	public String getID() {
		return id;
	}
	
	public byte[] getBuffer() {
		return buffer;
	}
	
	protected Object readResolve() throws ObjectStreamException {
		PrintWriter pw = (PrintWriter) RemotingManager.getInstance().pullClientReference(id);
		pw.print(new String(buffer));
		return this;
	}

}
