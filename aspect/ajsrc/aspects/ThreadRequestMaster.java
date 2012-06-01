package aspects;

import java.util.Enumeration;
import java.util.IdentityHashMap;

import javax.servlet.http.HttpServletRequest;

import aspects.TaintUtil.StackPath;

public class ThreadRequestMaster {

	private static int counter = 0;
	private static IdentityHashMap<Thread, CounterURIPair> threadToRequestMap = new IdentityHashMap<Thread, CounterURIPair>(); 
	private static IdentityHashMap<Object, Integer> objToRequestMap = new IdentityHashMap<Object, Integer>(); 
	
	public static void mapThreadToRequest(String URI) {
		threadToRequestMap.put(Thread.currentThread(), new CounterURIPair(counter++, URI));
	}
	
	public static CounterURIPair getMappedRequestCounter() {
		return threadToRequestMap.get(Thread.currentThread());
	}
	
	public static class CounterURIPair {
		private int counter;
		private String URI;
		
		public CounterURIPair(int counter, String URI) {
			this.counter = counter;
			this.URI = URI;
		}
		
		public int getCounter() {
			return this.counter;
		}
		
		public String getURI() {
			return this.URI;
		}
	}
	
//	public static boolean checkStateful(StackPath location, Object obj) {
//		Integer oldMapping = objToRequestMap.get(obj);
//		Integer newMapping = getMappedRequest();
//		if (newMapping == null) {
//			TaintLogger.getTaintLogger().log("REQUEST MAPPING FAIL");
//			return false;
//		}
//		
//		objToRequestMap.put(obj, newMapping);
//		
//		if (oldMapping != null && oldMapping != newMapping)
//			return true;
//		
//		return false;
//	}
}
