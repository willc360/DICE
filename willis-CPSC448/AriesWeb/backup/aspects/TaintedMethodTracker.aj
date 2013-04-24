package aspects;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.aspectj.lang.Signature;

import datamanagement.ReferenceMaster;

public aspect TaintedMethodTracker {
	
	Set<Signature> accessed_methods_set = new HashSet<Signature>();
	String location = "C:/Users/Sentinel/Desktop/taintlog/test.txt";
	
	pointcut myAdvice(): adviceexecution() || within(aspects.*) || within(datamanagement.*);
			
	before(): execution(* *(..))  && !(myAdvice()) {
		try {
			String method_name = thisJoinPoint.getSignature().toLongString();
			if (method_name.contains("edu.rice.rubis.servlets")) {
				accessed_methods_set.add(thisJoinPoint.getSignature());
			}
			
			FileWriter writer = new FileWriter(location);
			writer.write(Integer.toString(accessed_methods_set.size()));
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
