package remoting.oracle.jdbc;

import java.io.Serializable;

public class StatementWrapper implements Serializable {
	
	private static final long serialVersionUID = 442021362153491714L;
	
	String mQuery = null;
	Class[] mClazzArray = null;
	Object[] mObjArray = null;
	
	
	public StatementWrapper(String query, Class[] clazzArray, Object[] objArray){
		mQuery = query;
		mClazzArray = clazzArray;
		mObjArray = objArray;
	}
	
	public String getQueryString() {
		return mQuery;
	}
	public void setQueryString(String mQuery) {
		this.mQuery = mQuery;
	}
	public Class[] getClazzArray() {
		return mClazzArray;
	}
	public void setClazzArray(Class[] mClazzArray) {
		this.mClazzArray = mClazzArray;
	}
	public Object[] getObjArray() {
		return mObjArray;
	}
	public void setObjArray(Object[] mObjArray) {
		this.mObjArray = mObjArray;
	}

}
