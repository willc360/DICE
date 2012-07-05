package datamanagement;

import java.io.File;
import java.util.HashMap;
import java.util.Random;


public class HeuristicIntTainter {

	private static HeuristicIntTainter self;
	private DataSourceInfoBuilder dataInfoBuilder;
	private Random rg;
	private HashMap<Integer, Integer> newValOldValMap;
	
	private HeuristicIntTainter() {
		dataInfoBuilder = new DataSourceInfoBuilder(new File("/home/lee/DICE/rubisDataInfo.xml"));
		rg = new Random();
		newValOldValMap = new HashMap<Integer, Integer>();
	}
	
	public static HeuristicIntTainter getInstance() {
		if (self == null)
			self = new HeuristicIntTainter();
		
		return self;
	}
	
	public boolean sourceSafeForIntTracking(String catalog, String table, String column) {
		DataSourceInfo info = dataInfoBuilder.getMatchingInfo(catalog, table, column);
		if (info != null)
			return info.intTracking();
		return false;
	}
	
	public boolean sourceSafeForIntTracking(String uri, String parameter) {
		DataSourceInfo info = dataInfoBuilder.getMatchingInfo(uri, parameter);
		return info.intTracking();
	}
	
	public int taintInt(Integer toTaint) {
		Integer newVal = 1000000 + rg.nextInt(Integer.MAX_VALUE - 1000000);
		
		newValOldValMap.put(newVal, toTaint);
//		TaintLogger.getTaintLogger().log("INT TAINTING: " + toTaint + " to " + newVal);
		return newVal;
	}
	
	public int getRealValue(Integer newVal) {
		int oldVal = newValOldValMap.get(newVal);
//		TaintLogger.getTaintLogger().log("INT GETTING OLD: " + newVal + " was: " + oldVal);
		return oldVal;
	}
	
}