package remoting;

import java.io.Serializable;
import java.util.UUID;

public class Reference implements Serializable {
	private transient ReferenceObject ref;
	private String id = UUID.randomUUID().toString();
	private String className = ref.getClass().getName();
	
	public Reference(ReferenceObject ref) {
		this.ref = ref;
	}
	
	public String getID() {
		return id;
	}
	
	public Object getReference() {
		return ref;
	}

}
