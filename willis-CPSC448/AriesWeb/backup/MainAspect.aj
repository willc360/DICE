package remoting;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.*;

public aspect MainAspect {
	
	// hashtable storing all the cuts
	private Hashtable<String, List<String>> cuts_table = null;

	// Redirect URL
	private String redirect_url = "http://localhost:9080/AriesWeb/Remoting";
	
	// location of cuts.xml
	private String cuts_xml = "C:/Users/Sentinel/Desktop/UBC/CPSC448/cuts.xml";
	
	/**
	 * Pointcut and advice for ConnectionProxy
	 */
	pointcut readResolve() : 
		execution(Object readResolve() throws java.io.ObjectStreamException) &&
		within(ConnectionProxy);
	
	Object around() : readResolve() {
		System.out.println("Reached readResolve pointcut");
		Connection con = null;
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			con = DriverManager.getConnection("jdbc:mysql://localhost:3306/rubis?user=root&password=");
		} catch(Exception e) {
			e.printStackTrace();
		}
		return con;
	}
	
	/**
	 *  Populate hashtable with cuts from cuts.xml.
	 *  Must first specify the correct location of cuts.xml in the
	 *  "cuts_xml" variable above.
	 *  Key of the table would be m2, entry of each key is an ArrayList of 
	 *  associated m1's
	 */
	private void parseCutsXML() {
		
		cuts_table = new Hashtable<String,  List<String>>();
				
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
		try {
			docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse (new File(cuts_xml));
			NodeList cuts_list = doc.getElementsByTagName("cut");
			for (int i = 0; i < cuts_list.getLength(); i++) {
				Node cuts_node = cuts_list.item(i);
                if(cuts_node.getNodeType() == Node.ELEMENT_NODE) {
                	Element cuts_element = (Element)cuts_node;
                	String m1 = ((Node)(cuts_element.getElementsByTagName("m1").item(0).getChildNodes().item(0))).getNodeValue();
                	String m2 = ((Node)(cuts_element.getElementsByTagName("m2").item(0).getChildNodes().item(0))).getNodeValue();
                	
                	// Replace all the ":" in m1 and m2 with "."
                	m1 = m1.replaceAll(":", ".");
                	m2 = m2.replaceAll(":", ".");
                	
                	// m2 is the key, the entry corresponding to the key is a list of m1s
                	// that has appeared in cuts.xml
                	if (!cuts_table.contains(m2)) {
                		ArrayList<String> list = new ArrayList<String>();
                		list.add(m1);
                		cuts_table.put(m2, list);
                	}
                	else {
                		cuts_table.get(m2).add(m1);
                	}
                }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Helper function to get methods from superclass
	 * 
	 * @param target_class
	 * @param method_name
	 * @param args_classes
	 * @return
	 */
	private Method getMethodHelper(Class target_class, String method_name, Class[] args_classes) {
		// Get methods declared in super class
		while (true) {
			try {
				return target_class.getDeclaredMethod(method_name, args_classes);
			} catch (NoSuchMethodException e) {
				target_class = target_class.getSuperclass();
			}
		}
	}
	
	/**
	 * Helper function to get primitive classes (same implementation as in RemotingManager)
	 * @param type
	 * @return
	 */
	private Class getPrimitiveClass(String type) {
		if(type.equals("byte")) return byte.class;
		if(type.equals("char")) return char.class;
		if(type.equals("int")) return int.class;
		if(type.equals("long")) return long.class;
		if(type.equals("float")) return float.class;
		if(type.equals("double")) return double.class;
		throw new RuntimeException();
	}
	
	/**
	 *  Generic Aries pointcut
	 */
	pointcut aries() :
		call (* *(..)) && !cflow(call(* *.upCall(..))) && !within(remoting.*);

	/**
	 *  Main advice to redirect local calls to remote calls with RemotingManager
	 *  if applicable
	 * @return
	 */
	Object around() : aries() {
		
		// Checks if the cuts.xml has been parsed, if not parse it and populate hashtable
		if (cuts_table == null) {
			parseCutsXML();
		}

		// Get the called method name (m2)
		// thisJoinPoint.getStaticPart().getSignature().toLongString() returns a method name
		// containing the class/interface where it was originally declared.
		// Need to modified this so that the actual runtime target type is included instead
		// in the fully qualified domain name of the method since this is the one used in cuts.xml
		String method_name = "";
		if (thisJoinPoint.getTarget() != null) {
			String long_method_name = thisJoinPoint.getStaticPart().getSignature().toLongString().split("\\(")[0];
			String[] method_name_array = long_method_name.split(" ");
			method_name = method_name_array[method_name_array.length-1];
			method_name = method_name.split("\\.")[method_name.split("\\.").length-1];		
			method_name = thisJoinPoint.getTarget().getClass().getName() + "." + method_name;
		}
		else {
			// no target for static method, so just proceed with the static type + method name
			method_name = thisJoinPoint.getStaticPart().getSignature().getDeclaringTypeName() +
					thisJoinPoint.getSignature().getName();

		}
		
		// Retrieves the calling method name (m1)
		String long_calling_method_name = thisEnclosingJoinPointStaticPart.getSignature().toLongString().split("\\(")[0];
		String[] calling_method_name_array = long_calling_method_name.split(" ");
		String calling_method_name = calling_method_name_array[calling_method_name_array.length-1];
		
		// Checks if the called method name exist in cuts table. If so, redirect method
		// to be called remotely. Otherwise, just call "proceed" (continue with local call).
		if (cuts_table.containsKey(method_name) && cuts_table.get(method_name).contains(calling_method_name)) {
			RemotingManager remote_mgr = RemotingManager.getInstance();
			Object target = thisJoinPoint.getTarget();
			Object[] args = thisJoinPoint.getArgs();
			Class[] args_classes = new Class[args.length];
			String sig_long = thisJoinPoint.getSignature().toLongString();
			// Get arguments
			String[] temp = sig_long.split("\\(")[1].split("\\)");
			// Check methods with no argument
			String[] args_type = new String[0];
			if (temp.length > 0) {
				args_type = temp[0].split(", ");
			}
			
			// Assign classes, could get away with this if RemotingManager 
			// exposes getPrimitiveClass
			for (int i = 0; i < args.length; i++) {
				if (args_type[i].equals(""))
					break;
				try {
					args_classes[i] = Class.forName(args_type[i]);
				} catch (ClassNotFoundException e) {
					args_classes[i] = getPrimitiveClass(args_type[i]);
				}
			}
			
			try {
				Method m = null;
				Class target_class = null;
				if (target == null) {
					// Target is null for static class methods, thus explicitly get the
					// declaring type here.
					String classname = thisJoinPoint.getStaticPart().getSignature().getDeclaringTypeName();
					target_class = Class.forName(classname);			
				}
				else {
					target_class = target.getClass();
				}
				m = getMethodHelper(target_class, thisJoinPoint.getSignature().getName(), args_classes);

				// Get fully qualified class name for static methods as
				// required by RemotingManager.downCall
				if (Modifier.isStatic(m.getModifiers())) {
					target = target_class.getName();
				}

				return remote_mgr.downCall(redirect_url, 
						m,
						target,
						args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
		else {
			return proceed();
		}
	}
}
