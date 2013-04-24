package remoting;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Creates a wrapper object which contains most of the fields required (except for the target object)
 * for the execution of a method.
 */
public class ReflectionMethodWrapper implements Serializable {
	
	private String method_name;
	private Class target_class;
	private Class[] args_classes;
	private Object[] arguments;

	/**
	 * Constructor
	 * 
	 * @param name
	 * @param t_class
	 * @param a_classes
	 * @param args
	 */
	public ReflectionMethodWrapper(String name, Class t_class, Class[] a_classes, Object[] args) {
		method_name = name;
		target_class = t_class;
		arguments = args;
		args_classes = a_classes;
	}
	
	/**
	 * Execute the method represented by this wrapper on the provided target object.
	 * 
	 * @param target
	 * @return
	 */
	public Object run(Object target) {
		try {
			Method m = target_class.getMethod(method_name, args_classes);
			return m.invoke(target, arguments);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
