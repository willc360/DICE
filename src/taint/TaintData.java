package taint;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

//TODO: Make this static, not a singleton
public class TaintData {

	private static TaintData self;
	
	// A source is something which describes where the data came from
	private IdentityHashMap resultSetToSourceMap;
	private ConcurrentHashMap taintStack;
	
	// This is where much of the important data comes from, aside from the TaintFinder. Tainted strings map to the ResultSetMetaData responsible for them.
	// This is fine for now as we're only concerned with data read from the database.
	private WeakIdentityHashMap dataToSourcesMap;
	
	// TODO: Probably need to change this to a WeakIdentityHashMap
	
	private TaintData() {
		resultSetToSourceMap = new IdentityHashMap();
		dataToSourcesMap = new WeakIdentityHashMap();
		taintStack = new ConcurrentHashMap();
	}
	
	public static TaintData getTaintData() {
		if (self == null) {
			self = new TaintData();
		}
		
		return self;
	}

	/*
	 * Called when a tainted object is accessed (get pointcut) to log that the current thread at
	 * a particular stack location accessed the tainted object.
	 */
	public void recordTaintAccess(Object tainted) {
		Long threadId = new Long(Thread.currentThread().getId());
		if (!taintStack.containsKey(threadId)) {
			TaintLogger.getTaintLogger().log("stackTaint failed");
		}
		else {
			((WeakIdentityHashMap)((Stack)taintStack.get(threadId)).peek()).put(tainted, null);
		}
	}
	
	/*
	 * Checks if taint found in arguments was accessed. This indicates that accessed taint came from
	 * arguments, and so it should be searched for in the last method on the stack as well.
	 */
	public void pushTaintDownStack(Object taintedArg) {
		Long threadId = new Long(Thread.currentThread().getId());
		if (!taintStack.containsKey(threadId)) {
			TaintLogger.getTaintLogger().log("taintAccessed failed");
		}
		else {
			WeakIdentityHashMap top = (WeakIdentityHashMap)((Stack)taintStack.get(threadId)).pop();
			if (top.remove(taintedArg) != null) {
				((WeakIdentityHashMap)((Stack)taintStack.get(threadId)).peek()).put(taintedArg, null);
			}
			((Stack)taintStack.get(threadId)).push(top);
		}
	}
	
	/*
	 * Checks if any taint was accessed for the current thread at a particular stack location.
	 */
	public boolean taintAccessed() {
		Long threadId = new Long(Thread.currentThread().getId());
		if (!taintStack.containsKey(threadId)) {
			TaintLogger.getTaintLogger().log("taintAccessed failed");
			return false;
		}
		else {
			return (((WeakIdentityHashMap)((Stack)taintStack.get(threadId)).peek()).size() > 0);
		}
	}
	
	/*
	 * Called on method entry, to keep track of the stack location and associate tainted object 
	 * access with that location.
	 */
	public void startCall() {
		Long threadId = new Long(Thread.currentThread().getId());
		if (!taintStack.containsKey(threadId)) {
			taintStack.put(threadId, new Stack());
//			TaintLogger.getTaintLogger().log("TID: " + threadId + " startfail: " + taintStack.contains(threadId));
		}
		((Stack)taintStack.get(threadId)).push(new WeakIdentityHashMap());
	}
	
	/*
	 * Used with startCall() to keep track of the stack location. Called on method exit.
	 */
	public void endCall() {
		Long threadId = new Long(Thread.currentThread().getId());
		if (!taintStack.containsKey(threadId)) {
//			TaintLogger.getTaintLogger().log("TID: " + threadId + " endfail ");
			taintStack.put(threadId, new Stack());
		}
		if (!((Stack)taintStack.get(threadId)).empty())
			((WeakIdentityHashMap)((Stack)taintStack.get(threadId)).pop()).clear();
	}
	
	public void mapResultSetToSource(Object resultSet, Object source) {
		resultSetToSourceMap.put(resultSet, source);
	}
	
	public Object getResultSetSource(Object resultSet) {
		if (!resultSetToSourceMap.containsKey(resultSet))
			TaintLogger.getTaintLogger().log("getResultSetSourceNULL");
		return resultSetToSourceMap.get(resultSet);
	}
	
	public void mapDataToSource(Object data, Object source) {
		int size = 0;
		if (data instanceof String || data instanceof StringBuffer || data instanceof StringBuilder || data instanceof ResultSet) {
//			size = ((String)data).length();
			size = 0;
//			TaintLogger.getTaintLogger().log("Mapping: " + data + " code: " + data.hashCode());
			dataToSourcesMap.put(data, new SizedSources(size, source));
		}
	}
	
	public void propagateSources(Object sourceData, Object targetData) {
		if (dataToSourcesMap.get(targetData) == null) {
			dataToSourcesMap.put(targetData, new SizedSources(0, null));
		}
		((SizedSources)dataToSourcesMap.get(targetData)).addSources((SizedSources)dataToSourcesMap.get(sourceData));
	}
	
	public SizedSources getDataSources(Object data) {
		return (SizedSources)dataToSourcesMap.get(data);
	}
	
	public WeakIdentityHashMap getDataToSourceMap() {
		return dataToSourcesMap;
	}
	
	public boolean isTainted(Object object) {
		return dataToSourcesMap.containsKey(object);
	}
	
	public int getTaintHashCode(Object object) {
		if (isTainted(object)) {
			return System.identityHashCode(object);
		}
		return 0;
	}
	
	class SizedSources {
		private HashMap sources;
		
		public SizedSources (int size, Object source) {
			this.sources = new HashMap();
			if (source != null) {
				this.sources.put(source, new Integer(size));
			}
		}
		
		public void addSources(SizedSources sources) {
			if (sources != null)
				this.sources.putAll(sources.getSources());
		}
		
		public HashMap getSources() {
			return sources;
		}
		
		public String toString() {
			String ret = "";
			Iterator keySet = sources.keySet().iterator();
			while (keySet.hasNext()) {
				Object source = keySet.next();
				ret = ret + (sources.get(source).toString() + '-');
				try {
					ResultSetMetaData metaData = (ResultSetMetaData) source;
					int colCount = metaData.getColumnCount();
					for (int i = 1; i <= colCount; i++) {
						ret = ret + (metaData.getCatalogName(i) + "/" + metaData.getTableName(i) + "/" + metaData.getColumnName(i) + '#');
					}
				} catch (SQLException e) {
					ret = "SQLException";
					ret = ret + ": " + e.getMessage();
					e.printStackTrace();
				}
			}
			return ret;
		}
	}
}
