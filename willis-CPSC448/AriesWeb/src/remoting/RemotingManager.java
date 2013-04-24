package remoting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class RemotingManager extends HttpServlet {
	private static RemotingManager instance = new RemotingManager();
	private Map<String, Reference> referenceObjects = new HashMap<String, Reference>();
	
	public String getID() {
		return UUID.randomUUID().toString();
	}
	
	public Reference addReference(ReferenceObject object) {
		Reference reference = new Reference(object);
		referenceObjects.put(reference.getID(), reference);
		return reference;
	}
	
	public Reference pullReference(String id) {
		return referenceObjects.remove(id);
	}
	
	public static RemotingManager getInstance() {
		return instance;
	}
	
	public Object downCall(String url, Method method, Object target, Object ... args) throws SecurityException, IOException, ClassNotFoundException {
		System.out.println("Dispatching request to the host: " + url + "METHOD [ " + 
				target.getClass().getName() + ":" + 
				method.getName() + "]");
		
		HttpClient client = new DefaultHttpClient();
		HttpPost post = createDownCallPost(url, method, target, args);
		HttpResponse response = client.execute(post);
		HttpEntity responseEntity = response.getEntity();
		InputStream inputStream = responseEntity.getContent();
		ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
		return objectInputStream.readObject();
	}
	
	private HttpPost createDownCallPost(String url, Method method, Object target, Object[] args) throws IOException {
		HttpPost post = new HttpPost(url);
		HttpEntity postEntity = createDownCallEntity(method, target, args);
		post.setEntity(postEntity);
		return post;
	}
	
	private String createSignatureString(Method method) {
		String retVal = "";
		Class[] types = method.getParameterTypes();
		for(int i=0;i < types.length; i++) {
			retVal += types[i].getName();
			if(i < (types.length -1)) {
				retVal += ',';
			}
		}
		return retVal;
	}
	
	private HttpEntity createDownCallEntity(Method method, Object target, Object[] args) throws IOException {
		ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
		ObjectOutputStream out = new CustomObjectOutputStream(bytesOut);
		out.writeObject(method.getName());
		out.writeObject(target);
		out.writeObject(createSignatureString(method));
		out.writeInt(args.length);
		for(Object arg : args) {
			out.writeObject(arg);
		}
		byte[] bytes = bytesOut.toByteArray();
		HttpEntity postEntity = new ByteArrayEntity(bytes);
		return postEntity;
	}
	
	public void upCall(HttpServletRequest request, HttpServletResponse response) throws IOException, ClassNotFoundException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		InputStream input = request.getInputStream();
		ObjectInputStream objectInputStream = new ObjectInputStream(input);
		String methodName = (String) objectInputStream.readObject();
		Object target = objectInputStream.readObject();
		String signatureString = (String) objectInputStream.readObject();
		int numArgs = 0;
		try {
			numArgs = objectInputStream.readInt();
		}catch(Exception e){}
		Object[] args = new Object[numArgs];
		for(int i=0;i < numArgs; i++) {
			args[i] = objectInputStream.readObject();
		}
		upCall(response.getOutputStream(), methodName, target, signatureString, args);
	}
	
	private Class getPrimitiveClass(String type) {
		 if(type.equals("byte")) return byte.class;
		 if(type.equals("char")) return char.class;
		 if(type.equals("int")) return int.class;
		 if(type.equals("long")) return long.class;
		 if(type.equals("float")) return float.class;
		 if(type.equals("double")) return double.class;
		 throw new RuntimeException();
	}
	
	private void upCall(OutputStream output, String methodName, Object target, String signatureString, Object ... args) throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, IOException, ClassNotFoundException {
		@SuppressWarnings("rawtypes")
		Class[] argTypes = new Class[args.length];
		String[] argTypesStrings = signatureString.split(",");
		int i=0;
		for(String type : argTypesStrings) {
			try {
				if (type.equals(""))
					break;
				argTypes[i] = Class.forName(type);
			} catch(ClassNotFoundException e) {
				argTypes[i] = getPrimitiveClass(type);
			}
			i++;
		}
		Method method = target.getClass().getDeclaredMethod(methodName, argTypes);
		method.setAccessible(true);
		Object returnValue = method.invoke(target, args);
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(output);
		objectOutputStream.writeObject(returnValue);
	}
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		System.out.println("Request received");
		try {
			long start = System.currentTimeMillis();
			upCall(req, resp);
			System.out.println("ExecTime: " + (System.currentTimeMillis() - start));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
}
