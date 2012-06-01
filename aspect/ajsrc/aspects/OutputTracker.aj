package aspects;

import java.util.Set;

import aspects.TaintUtil.StackPath;


public aspect OutputTracker {
	pointcut allExclude(): within(javax.management.MBeanConstructorInfo) ||
							within(javax.management.MBeanNotificationInfo) ||
							within(javax.management.MBeanFeatureInfo) ||
							within(javax.management.MBeanOperationInfo) ||
							within(javax.management.MBeanInfo) ||
							within(javax.management.MBeanNotificationInfo);

	pointcut myAdvice(): adviceexecution() || within(aspects.*);

	before(): (call(* org.apache.catalina.connector.CoyoteWriter+.println(..)) || call(* org.apache.catalina.connector.CoyoteWriter+.print(..)))&& !within(aspects.*) && !(myAdvice()) && !allExclude() {
    	if (!TaintUtil.getAJLock())
    		return;
		TaintUtil.pushContext(thisJoinPoint.getTarget(), thisJoinPoint.getSignature());
		StackPath location = null;
        Object[] args = thisJoinPoint.getArgs();
        //TODO: ADD CONTEXT MANAGEMENT!!!
//        if (thisJoinPoint.getTarget().getClass().getName().contains("org.apache.catalina.connector.CoyoteWriter")) {
        boolean taintOutput = false;
    	for (int i = 0; i < args.length; i++) {
        	//TODO: Deal with the fact that I added ResultSet here
        	if (ReferenceMaster.isPrimaryTainted(args[i])) {
    			if (location == null)
    				location = TaintUtil.getStackTracePath();
    			TaintLogger.getTaintLogger().logOutputStringArg(location, "EXECUTESTRINGARG", args[i], TaintUtil.getLastContext(), thisJoinPoint.getTarget());
    			taintOutput = true;
        	}
        	else if (args[i] != null) {
        		Set<Object> objTaint = ReferenceMaster.fullTaintCheck(args[i]);
        		if (objTaint != null && objTaint.size() > 0) {
        			if (location == null)
        				location = TaintUtil.getStackTracePath();
        			/*
        			 * TODO: add to taintedArgs here as well
        			 */
    				TaintLogger.getTaintLogger().logOutputObjectArg(location, "EXECUTEOBJECTARG", args[i], objTaint, TaintUtil.getLastContext(), thisJoinPoint.getTarget());
    				taintOutput = true;
        		}
        	}
        }
    	if (!taintOutput) {
    		if (location == null)
				location = TaintUtil.getStackTracePath();
    		if (args.length > 0 && args[0] != null)
    			TaintLogger.getTaintLogger().logNonTaintOutputStringArg(location, "NONTAINTOUTPUT", args[0], TaintUtil.getLastContext(), thisJoinPoint.getTarget());
    		else if (args.length == 0)
    			TaintLogger.getTaintLogger().logNonTaintOutputStringArg(location, "NONTAINTOUTPUT", null, TaintUtil.getLastContext(), thisJoinPoint.getTarget());
    	}
    	
        TaintUtil.releaseAJLock();
    }
	
	after(): (call(* org.apache.catalina.connector.CoyoteWriter+.println(..)) || call(* org.apache.catalina.connector.CoyoteWriter+.print(..)))&& !within(aspects.*) && !(myAdvice()) && !allExclude() {
		if (!TaintUtil.getAJLock())
    		return;
		StackPath location = TaintUtil.getStackTracePath();
		TaintLogger.getTaintLogger().logReturning(location, "NONTAINTRETURN", null, null, TaintUtil.getLastContext(), thisJoinPoint.getTarget());
		TaintUtil.popContext();
		TaintUtil.releaseAJLock();
	}
}
