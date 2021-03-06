package aspects;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.catalina.connector.RequestFacade;
import org.aspectj.lang.reflect.FieldSignature;

import datamanagement.HeuristicNumericTainter;
import datamanagement.ReferenceMaster;
import datamanagement.ReferenceMaster.IDdTaintSource;
import datamanagement.SimpleCommControl;
import datamanagement.StaticFieldBackTaintChecker;
import datamanagement.TaintLogger;
import datamanagement.TaintUtil;
import datamanagement.TaintUtil.StackLocation;
import datamanagement.ThreadRequestMaster;


public aspect ReferenceTracker {

	pointcut allExclude(): within(javax.management.MBeanConstructorInfo) ||
								within(javax.management.MBeanNotificationInfo) ||
								within(javax.management.MBeanFeatureInfo) ||
								within(javax.management.MBeanOperationInfo) ||
								within(javax.management.MBeanInfo) ||
								within(javax.management.MBeanNotificationInfo) ||
//								within(com.mysql.jdbc..*) ||
								within(org.apache.jasper.runtime.JspWriterImpl) ||
								within(org.hsqldb.types.Binary) ||
								within(oracle.jpub.runtime.MutableStruct) ||
								within(oracle.jpub.runtime.MutableArray) ||
								within(oracle.gss.util.JNLS) ||
								within(oracle.sql..*) ||
								within(org.postgresql..*) ||
								within(org.apache.bcel..*) ||
								within(org.apache.xalan.xsltc.compiler..*) ||
								within(org.apache.xerces.util.XMLChar) ||
								within(freemarker.core.FMParserTokenManager) ||
								within(org.apache.commons.lang.text.StrTokenizer) ||
								within(net.sourceforge.jtds.jdbc.UniqueIdentifier) ||
								within(net.sourceforge.jtds.jdbc.SQLDiagnostic) ||
								within(org.apache.log4j.spi.ThrowableInformation) ||
								within(org.apache.lucene.index.SegmentInfo) ||
								within(com.sun.mail.imap.protocol.IMAPAddress) ||
								within(com.sun.mail.imap.IMAPFolder) ||
								within(com.jhlabs.image..*) ||
								within(com.mchange.v2.cfg..*) ||
								within(com.mchange.v2.codegen.bean..*) ||

								within(org.apache.catalina.ant.*) ||
								within(org.apache.catalina.util.*) ||
								within(org.apache.catalina.ha.*) ||
								within(org.apache.catalina.tribes.*) ||
								within(org.apache.catalina.authenticator.*) ||
								within(org.apache.catalina.connector.*) ||
								within(org.apache.catalina.core.*) ||
								within(org.apache.catalina.deploy.*) ||
								within(org.apache.catalina.filters.*) ||
								within(org.apache.catalina.loaders.*) ||
								within(org.apache.catalina.manager.*) ||
								within(org.apache.catalina.mbeans.*) ||
								within(org.apache.catalina.realm.*) ||
								within(org.apache.catalina.security.*) ||
								within(org.apache.catalina.servlets.*) ||
								within(org.apache.catalina.ssi.*) ||
								within(org.apache.catalina.startup.*) ||
								within(org.apache.catalina.users.*) ||
								within(org.apache.catalina.valves.*) ||
								within(org.apache.catalina.A*) ||
								within(org.apache.catalina.C*) ||
								within(org.apache.catalina.E*) ||
								within(org.apache.catalina.G*) ||
								within(org.apache.catalina.H*) ||
								within(org.apache.catalina.I*) ||
								within(org.apache.catalina.L*) ||
								within(org.apache.catalina.M*) ||
								within(org.apache.catalina.P*) ||
								within(org.apache.catalina.R*) ||
								within(org.apache.catalina.Serv*) ||
								within(org.apache.catalina.Store*) ||
								within(org.apache.catalina.U*) ||
								within(org.apache.catalina.V*) ||
								within(org.apache.catalina.W*) ||
								
								within(org.apache.naming..*) ||
								within(org.apache.AnnotationProcessor) ||
								within(org.apache.PeriodicEventListener) ||
								within(org.apache.coyote..*) ||
								within(org.apache.jk..*) ||
								within(org.apache.tomcat..*) ||
								within(org.apache.tomcat.dbcp..*);
//	pointcut allExclude(): within(javax.ejb.AccessLocalException);
	
	pointcut myAdvice(): adviceexecution() || within(aspects.*) || within(datamanagement.*);
	pointcut tooBigErrorExcludeCollections(): within(com.mysql.jdbc.TimeUtil) ||
												within(org.apache.jasper.xmlparser.EncodingMap) ||
												within(org.apache.xerces.util.EncodingMap);
	
	// For associating threads with requests
	// TODO: Does this really belong here?
	before(): execution(* service(..)) {
		if (!SimpleCommControl.getInstance().trackingEnabled())
    		return;
    	ReferenceMaster.resetPSTaintTracking(); // Otherwise possible side effects across requests.
		Object[] args = thisJoinPoint.getArgs();
		String URI = null;
		String remoteAddr = null;
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof RequestFacade) {
				RequestFacade req = (RequestFacade)args[i];
				URI = req.getRequestURI();
				// Hack to fake different IP addresses
//				remoteAddr = req.getRemoteAddr();
				remoteAddr = SimpleCommControl.getInstance().getForcedRemoteAddr();
				break;
			}
		}
		if (URI != null)
			ThreadRequestMaster.mapThreadToRequest(URI, remoteAddr);
//			TaintLogger.getTaintLogger().dumpStack("HAS URI " + URI + " - " + Thread.currentThread().getId());
//		else
//			TaintLogger.getTaintLogger().dumpStack("NO URI - " + Thread.currentThread().getId());
	}
	
	/*
	 * Advice to propagate taint from tainted Strings to the numerictracking system
	 */
	Integer around(String arg): call(Integer.new(..)) && args(arg) {
		if (!SimpleCommControl.getInstance().trackingEnabled())
    		return proceed(arg);
		
		Integer ret = proceed(arg);
		
		if (ReferenceMaster.isPrimaryTainted(arg)) {
			boolean safeForTracking = false;
			for (IDdTaintSource source : ReferenceMaster.getDataSources(arg)) {
				if (HeuristicNumericTainter.getInstance().sourceSafeForNumericTracking(source.getTaintSource().getSource(), source.getTaintSource().getTargetColumn())) {
					safeForTracking = true;
					break;
				}
			}
			if (safeForTracking) {
				ret = ReferenceMaster.doPrimaryIntTaint(ret);
				ReferenceMaster.propagateTaintSourcesToNumeric(arg, ret);
				StackLocation location = TaintUtil.getStackTraceLocation();
				TaintLogger.getTaintLogger().logPropagation(location, "CREATEINT", arg, ret);
			}
		}
		
		return ret;
	}
	
	Double around(String arg): call(Double.new(..)) && args(arg) {
		if (!SimpleCommControl.getInstance().trackingEnabled())
    		return proceed(arg);
		
		Double ret = proceed(arg);
		
		if (ReferenceMaster.isPrimaryTainted(arg)) {
			boolean safeForTracking = false;
			for (IDdTaintSource source : ReferenceMaster.getDataSources(arg)) {
				if (HeuristicNumericTainter.getInstance().sourceSafeForNumericTracking(source.getTaintSource().getSource(), source.getTaintSource().getTargetColumn())) {
					safeForTracking = true;
					break;
				}
			}
			if (safeForTracking) {
				ret = ReferenceMaster.doPrimaryDoubleTaint(ret);
				ReferenceMaster.propagateTaintSourcesToNumeric(arg, ret);
				StackLocation location = TaintUtil.getStackTraceLocation();
				TaintLogger.getTaintLogger().logPropagation(location, "CREATEDOUBLE", arg, ret);
			}
		}
		
		return ret;
	}
	
	Float around(String arg): call(Float.new(..)) && args(arg) {
		if (!SimpleCommControl.getInstance().trackingEnabled())
    		return proceed(arg);
		
		Float ret = proceed(arg);
		
		if (ReferenceMaster.isPrimaryTainted(arg)) {
			boolean safeForTracking = false;
			for (IDdTaintSource source : ReferenceMaster.getDataSources(arg)) {
				if (HeuristicNumericTainter.getInstance().sourceSafeForNumericTracking(source.getTaintSource().getSource(), source.getTaintSource().getTargetColumn())) {
					safeForTracking = true;
					break;
				}
			}
			if (safeForTracking) {
				ret = ReferenceMaster.doPrimaryFloatTaint(ret);
				ReferenceMaster.propagateTaintSourcesToNumeric(arg, ret);
				StackLocation location = TaintUtil.getStackTraceLocation();
				TaintLogger.getTaintLogger().logPropagation(location, "CREATEFLOAT", arg, ret);
			}
		}
		
		return ret;
	}
	
	Integer around(String arg): call(* Integer.parseInt(..)) && args(arg) {
		if (!SimpleCommControl.getInstance().trackingEnabled())
    		return proceed(arg);

		TaintLogger.getTaintLogger().log("PARSEINT CALLED: " + arg);
		Integer ret = proceed(arg);
		
		if (ReferenceMaster.isPrimaryTainted(arg)) {
			TaintLogger.getTaintLogger().log("PARSEINT on TAINTED: " + arg);
			boolean safeForTracking = false;
			for (IDdTaintSource source : ReferenceMaster.getDataSources(arg)) {
				if (HeuristicNumericTainter.getInstance().sourceSafeForNumericTracking(source.getTaintSource().getSource(), source.getTaintSource().getTargetColumn())) {
					safeForTracking = true;
					break;
				}
			}
			if (safeForTracking) {
				ret = ReferenceMaster.doPrimaryIntTaint(ret);
				ReferenceMaster.propagateTaintSourcesToNumeric(arg, ret);
				TaintLogger.getTaintLogger().log("PARSE INT PROPAGATION: " + ret);
				StackLocation location = TaintUtil.getStackTraceLocation();
				TaintLogger.getTaintLogger().logPropagation(location, "PARSEINT", arg, ret);
			}
		}
		
		return ret;
	}
	
	Double around(String arg): call(* Double.parseDouble(..)) && args(arg) {
		if (!SimpleCommControl.getInstance().trackingEnabled())
    		return proceed(arg);

		TaintLogger.getTaintLogger().log("PARSEDOUBLE CALLED: " + arg);
		Double ret = proceed(arg);
		
		if (ReferenceMaster.isPrimaryTainted(arg)) {
			TaintLogger.getTaintLogger().log("PARSEDOUBLE on TAINTED: " + arg);
			boolean safeForTracking = false;
			for (IDdTaintSource source : ReferenceMaster.getDataSources(arg)) {
				if (HeuristicNumericTainter.getInstance().sourceSafeForNumericTracking(source.getTaintSource().getSource(), source.getTaintSource().getTargetColumn())) {
					safeForTracking = true;
					break;
				}
			}
			if (safeForTracking) {
				ret = ReferenceMaster.doPrimaryDoubleTaint(ret);
				ReferenceMaster.propagateTaintSourcesToNumeric(arg, ret);
				TaintLogger.getTaintLogger().log("PARSE DOUBLE PROPAGATION: " + ret);
				StackLocation location = TaintUtil.getStackTraceLocation();
				TaintLogger.getTaintLogger().logPropagation(location, "PARSEDOUBLE", arg, ret);
			}
		}
		
		return ret;
	}
	
	Float around(String arg): call(* Float.parseFloat(..)) && args(arg) {
		if (!SimpleCommControl.getInstance().trackingEnabled())
    		return proceed(arg);

		TaintLogger.getTaintLogger().log("PARSEFLOAT CALLED: " + arg);
		Float ret = proceed(arg);
		
		if (ReferenceMaster.isPrimaryTainted(arg)) {
			TaintLogger.getTaintLogger().log("PARSEFLOAT on TAINTED: " + arg);
			boolean safeForTracking = false;
			for (IDdTaintSource source : ReferenceMaster.getDataSources(arg)) {
				if (HeuristicNumericTainter.getInstance().sourceSafeForNumericTracking(source.getTaintSource().getSource(), source.getTaintSource().getTargetColumn())) {
					safeForTracking = true;
					break;
				}
			}
			if (safeForTracking) {
				ret = ReferenceMaster.doPrimaryFloatTaint(ret);
				ReferenceMaster.propagateTaintSourcesToNumeric(arg, ret);
				TaintLogger.getTaintLogger().log("PARSE FLOAT PROPAGATION: " + ret);
				StackLocation location = TaintUtil.getStackTraceLocation();
				TaintLogger.getTaintLogger().logPropagation(location, "PARSEFLOAT", arg, ret);
			}
		}
		
		return ret;
	}
	
	/*
     * Advice for new reference tracking system
     */
    after() returning(Object accessed): get(!static * *) && !(myAdvice()) && !allExclude() {
    	if (!SimpleCommControl.getInstance().trackingEnabled())
    		return;
    	StackLocation location = null;
		Field field = ((FieldSignature)thisJoinPoint.getSignature()).getField();

		Object target = thisJoinPoint.getTarget();
		if (target instanceof ResultSet || target.getClass().getName().endsWith("RowDataStatic"))
			return;
		
		if (ReferenceMaster.isPrimaryTainted(accessed)) {
			if (location == null)
				location = TaintUtil.getStackTraceLocation();
//			if (ThreadRequestMaster.checkStateful(location, accessed))
//				TaintLogger.getTaintLogger().log("STATE FOUND: " + accessed);
    		TaintLogger.getTaintLogger().logFieldGet(location, "NORMAL", accessed, field, TaintUtil.getLastContext(), TaintUtil.getContext());
    		TaintUtil.addContextAccessedTaint(accessed);
    	}
    	else {
			Set<Object> objTaint = ReferenceMaster.fullTaintCheck(field, accessed);
    		if (objTaint != null && objTaint.size() > 0) {
    			if (location == null)
    				location = TaintUtil.getStackTraceLocation();
//    			for (Object item : objTaint) {
//        			if (ThreadRequestMaster.checkStateful(location, item))
//        				TaintLogger.getTaintLogger().log("STATE FOUND: " + item);
//    			}
    			TaintLogger.getTaintLogger().logFieldGet(location, "NORMAL", accessed, objTaint, field, TaintUtil.getLastContext(), TaintUtil.getContext());
    			TaintUtil.addContextAccessedTaint(accessed, objTaint);
    		}
    	}
    }
	
    before(): set(!static * *) && !(myAdvice()) && !withincode(*.new(..)) && !allExclude() {
    	if (!SimpleCommControl.getInstance().trackingEnabled())
    		return;
    	Field field = ((FieldSignature) thisJoinPoint.getSignature()).getField();
		field.setAccessible(true);
		if (field.getType().isPrimitive() ||
				field.getType().equals(Integer.class) ||
				field.getType().equals(Double.class) ||
				field.getType().equals(Byte.class) ||
				field.getType().equals(Short.class) ||
				field.getType().equals(Long.class) ||
				field.getType().equals(Float.class) ||
				field.getType().equals(Boolean.class) ||
				field.getType().equals(Character.class)) {
			return;
		}
		Object target = thisJoinPoint.getTarget();
		// Skipping ResultSet sets, because we just taint the result set anyways. Probably don't
		// care what happens inside.
		if (target instanceof ResultSet || target.getClass().getName().endsWith("RowDataStatic"))
			return;
		Object oldValue = null;
		try {
			oldValue = field.get(target);
		} catch (Exception e) {
		}
		Object newValue = thisJoinPoint.getArgs()[0];

		ReferenceMaster.cleanupOldValue(oldValue, target);
		ReferenceMaster.setNewValue(newValue, target);
		
		StackLocation location = null;
		
		if (ReferenceMaster.isPrimaryTainted(newValue)) {
			if (location == null)
				location = TaintUtil.getStackTraceLocation();
//			if (ThreadRequestMaster.checkStateful(location, newValue))
//				TaintLogger.getTaintLogger().log("STATE FOUND: " + newValue);
			TaintLogger.getTaintLogger().logFieldSet(location, "NORMAL", newValue, field, TaintUtil.getLastContext(), TaintUtil.getContext());
		} else {
			Set<Object> objTaint = ReferenceMaster.fullTaintCheck(field, newValue);
			if (objTaint != null && objTaint.size() > 0) {
				if (location == null)
					location = TaintUtil.getStackTraceLocation();
//				for (Object item : objTaint) {
//        			if (ThreadRequestMaster.checkStateful(location, item))
//        				TaintLogger.getTaintLogger().log("STATE FOUND: " + item);
//    			}
				TaintLogger.getTaintLogger().logFieldSet(location, "NORMAL", newValue, objTaint, field, TaintUtil.getLastContext(), TaintUtil.getContext());
			}
		}
    }
    
//    before(): (execution(*.new(..)) && !within(aspects.*)) && !cflow(myAdvice()) {
//    	if (((ConstructorSignature)thisJoinPoint.getSignature()).getConstructor().getDeclaringClass().getName().contains("Connection")) {
//    		System.out.println("Starting Connection constructor " + ((ConstructorSignature)thisJoinPoint.getSignature()).getConstructor().getDeclaringClass().getName());
//    	}
//    }
    
    after(Object ret) returning: this(ret) && (execution(*.new(..)) && !within(aspects.*)) && !(myAdvice()) && !allExclude() {
    	if (!SimpleCommControl.getInstance().trackingEnabled())
    		return;
    	if (!TaintUtil.getAJLock("AFTERREFNEW" + thisJoinPoint.getSignature().toShortString()))
    		return;// scan ret for instance fields.
    	Class clazz = ret.getClass();
//    	if (ret instanceof Connection) {
//    		System.out.println("Finished Connection constructor: " + ((ConstructorSignature)thisJoinPoint.getSignature()).getConstructor().getDeclaringClass().getName());
//    	}
    	// Skipping ResultSet sets, because we just taint the result set anyways. Probably don't
			// care what happens inside.
		if (ret instanceof ResultSet || ret.getClass().getName().endsWith("RowDataStatic")) {
			TaintUtil.releaseAJLock("AFTERREFNEW" + thisJoinPoint.getSignature().toShortString());
			return;
		}
		while (clazz != null) {
			try {
				Field[] fields = clazz.getDeclaredFields();
				for (int i = 0; i < fields.length; i++) {
					//TODO: is it bad that this only scans non-static fields?
					if (!Modifier.isStatic(fields[i].getModifiers())) {
						if (fields[i].getType().equals(Double.class) ||
								fields[i].getType().equals(Byte.class) ||
								fields[i].getType().equals(Short.class) ||
								fields[i].getType().equals(Long.class) ||
								fields[i].getType().equals(Float.class) ||
								fields[i].getType().equals(Boolean.class) ||
								fields[i].getType().equals(Character.class))
							continue;
						fields[i].setAccessible(true);
						try {
							Object newValue = fields[i].get(ret);
							ReferenceMaster.setNewValue(newValue, ret);
						} catch (IllegalAccessException ex) {
							assert false;
						}
					}
				}
			}
			catch (NoClassDefFoundError e) {
				TaintLogger.getTaintLogger().log("NoClassDefFoundError on: " + clazz.toString());				
			}
			clazz = clazz.getSuperclass();
		}
		TaintUtil.releaseAJLock("AFTERREFNEW" + thisJoinPoint.getSignature().toShortString());
    }
    
    /*
     * Static get/set
     * 
     * Problem: when you get a java field to add to it, the field is modified before the get is registered.
     * Has nothing to do with call pointcut. The field is simply modified before it is gotten (from aspectJ
     * perspective)
     */
    before(): get(static * *) && !(myAdvice()) && !allExclude() {
    	if (!SimpleCommControl.getInstance().trackingEnabled())
    		return;
    	StackLocation location = null;
		Field field = ((FieldSignature)thisJoinPoint.getSignature()).getField();
		field.setAccessible(true);
		
		Object accessed = null;
		try {
			accessed = field.get(thisJoinPoint.getTarget());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		if (accessed == null)
			return;
		boolean taintFound = false;
		if (ReferenceMaster.isPrimaryTainted(accessed)) {
			if (location == null)
				location = TaintUtil.getStackTraceLocation();
			StaticFieldBackTaintChecker.addPrimary(field, accessed);
			taintFound = true;
//			if (ThreadRequestMaster.checkStateful(location, accessed))
//				TaintLogger.getTaintLogger().log("STATE FOUND: " + accessed);
    		TaintLogger.getTaintLogger().logFieldGet(location, "STATIC", accessed, field, TaintUtil.getLastContext(), TaintUtil.getContext());
    		TaintUtil.addContextAccessedTaint(accessed);
    	}
    	else {
			Set<Object> objTaint = ReferenceMaster.fullTaintCheck(field, accessed);
    		if (objTaint != null && objTaint.size() > 0) {
    			if (location == null)
    				location = TaintUtil.getStackTraceLocation();
    			StaticFieldBackTaintChecker.addComplex(field, accessed, objTaint);
    			taintFound = true;
//    			for (Object item : objTaint) {
//        			if (ThreadRequestMaster.checkStateful(location, item))
//        				TaintLogger.getTaintLogger().log("STATE FOUND: " + item);
//    			}
    			TaintLogger.getTaintLogger().logFieldGet(location, "STATIC", accessed, objTaint, field, TaintUtil.getLastContext(), TaintUtil.getContext());
    			TaintUtil.addContextAccessedTaint(accessed, objTaint);
    		}
    	}

//		if (location == null)
//			location = TaintUtil.getStackTracePath();
//    	TaintLogger.getTaintLogger().log("BEFORE STATIC GET: " + field + " taintfound: " + taintFound + " at " + location);
		if (!taintFound && ReferenceMaster.isPrimaryType(accessed)) {
			StaticFieldBackTaintChecker.addPrimary(field, accessed);
		}
		else if (!taintFound) {
			StaticFieldBackTaintChecker.addComplex(field, accessed, null);
		}
    }
    
    before(): set(static * *) && !(myAdvice()) && !allExclude() {
    	if (!SimpleCommControl.getInstance().trackingEnabled())
    		return;
    	Field field = ((FieldSignature) thisJoinPoint.getSignature()).getField();
		field.setAccessible(true);
		Object newValue = thisJoinPoint.getArgs()[0];

		StackLocation location = null;
		
		if (newValue == null)
			return;
		boolean taintFound = false;
		if (ReferenceMaster.isPrimaryTainted(newValue)) {
			if (location == null)
				location = TaintUtil.getStackTraceLocation();
			StaticFieldBackTaintChecker.addPrimary(field, newValue);
			taintFound = true;
//			if (ThreadRequestMaster.checkStateful(location, newValue))
//				TaintLogger.getTaintLogger().log("STATE FOUND: " + newValue);
			TaintLogger.getTaintLogger().logFieldSet(location, "STATIC", newValue, field, TaintUtil.getLastContext(), TaintUtil.getContext());
		} else {
			Set<Object> objTaint = ReferenceMaster.fullTaintCheck(field, newValue);
			if (objTaint != null && objTaint.size() > 0) {
				if (location == null)
					location = TaintUtil.getStackTraceLocation();
    			StaticFieldBackTaintChecker.addComplex(field, newValue, objTaint);
    			taintFound = true;
//				for (Object item : objTaint) {
//        			if (ThreadRequestMaster.checkStateful(location, item))
//        				TaintLogger.getTaintLogger().log("STATE FOUND: " + item);
//    			}
				TaintLogger.getTaintLogger().logFieldSet(location, "STATIC", newValue, objTaint, field, TaintUtil.getLastContext(), TaintUtil.getContext());
			}
		}
		
		if (!taintFound && ReferenceMaster.isPrimaryType(newValue)) {
			StaticFieldBackTaintChecker.addPrimary(field, newValue);
		}
		else if (!taintFound && newValue != null) {
			StaticFieldBackTaintChecker.addComplex(field, newValue, null);
		}
	}
    
    /*
     * Filter out collections/maps we don't want to handle specially
     */
//    pointcut collectionOp(): !(call(* java.beans.beancontext.BeanContext.*(..)) || 
//    		call(* javax.management.AttributeList.*(..)) || 
//    		call(* javax.management.relation.RoleList.*(..)) || 
//    		call(* javax.management.relation.RoleUnresolvedList.*(..)) || 
//    		call(* java.util.Vector.*(..)) ||
//    		call(* java.util.EnumSet.*(..)) ||
//    		call(* javax.print.attribute.standard.JobStateReasons.*(..))) 
//    		&& !within(aspects.*) && !cflow(myAdvice());
//    
//    pointcut mapOp(): !(call(* java.security.AuthProvider.*(..)) ||
//    		call(* java.util.EnumMap.*(..)) ||
//    		call(* java.util.LinkedHashMap.*(..)) ||
//    		call(* javax.print.attribute.standard.PrinterStateReasons.*(..)) ||
//    		call(* java.util.Properties.*(..)) ||
//    		call(* java.security.Provider.*(..)) ||
//    		call(* java.awt.RenderingHints.*(..)) ||
//    		call(* javax.script.SimpleBindings.*(..)) ||
//    		call(* javax.management.openmbean.TabularDataSupport.*(..)) ||
//    		call(* javax.swing.UIDefaults.*(..)))
//    		&& !within(aspects.*) && !cflow(myAdvice());
    
    pointcut collectionOp(): !within(aspects.*) && !myAdvice();
    
    pointcut mapOp(): !within(aspects.*) && !myAdvice();

    /*
     * Collection/Map constructors
     */
    Object around(): call(java.util.Collection+.new(..)) && collectionOp() && !tooBigErrorExcludeCollections() && !allExclude() {
    	if (!SimpleCommControl.getInstance().trackingEnabled())
    		return proceed();
    	Object ret = proceed();
    	if ((ret instanceof Collection) && 
				!(ret instanceof java.beans.beancontext.BeanContext ||  // These must agree with collectionOp pointcut in GeneralTracker
						ret instanceof javax.management.AttributeList ||
						ret instanceof javax.management.relation.RoleList ||
						ret instanceof javax.management.relation.RoleUnresolvedList ||
						ret instanceof java.util.Vector ||
						ret instanceof java.util.EnumSet ||
						ret instanceof javax.print.attribute.standard.JobStateReasons ||
						ret instanceof java.security.AuthProvider ||
						ret instanceof java.util.EnumMap ||
						ret instanceof java.util.LinkedHashMap ||
						ret instanceof javax.print.attribute.standard.PrinterStateReasons ||
						ret instanceof java.util.Properties ||
						ret instanceof java.security.Provider ||
						ret instanceof java.awt.RenderingHints ||
						ret instanceof javax.script.SimpleBindings ||
						ret instanceof javax.management.openmbean.TabularDataSupport ||
						ret instanceof javax.swing.UIDefaults ||
						ret.getClass().getName().endsWith("CursorableLinkedList"))) {
    		Object[] args = thisJoinPoint.getArgs();
        	for (int i = 0; i < args.length; i++) {
        		if (args[i] instanceof Collection) {
        			for (Object item : (Collection)args[i]) {
        				ReferenceMaster.setNewValue(item, ret);
        			}
        			break;
        		}
        	}
    	}
    	
    	return ret;
    }
	Object around(): call(java.util.Map+.new(..)) && mapOp() && !tooBigErrorExcludeCollections() && !allExclude() {
    	// look for map
		if (!SimpleCommControl.getInstance().trackingEnabled())
    		return proceed();
    	Object ret = proceed();
		if ((ret instanceof Map) && 
				!(ret instanceof java.beans.beancontext.BeanContext ||  // These must agree with collectionOp pointcut in GeneralTracker
						ret instanceof javax.management.AttributeList ||
						ret instanceof javax.management.relation.RoleList ||
						ret instanceof javax.management.relation.RoleUnresolvedList ||
						ret instanceof java.util.Vector ||
						ret instanceof java.util.EnumSet ||
						ret instanceof javax.print.attribute.standard.JobStateReasons ||
						ret instanceof java.security.AuthProvider ||
						ret instanceof java.util.EnumMap ||
						ret instanceof java.util.LinkedHashMap ||
						ret instanceof javax.print.attribute.standard.PrinterStateReasons ||
						ret instanceof java.util.Properties ||
						ret instanceof java.security.Provider ||
						ret instanceof java.awt.RenderingHints ||
						ret instanceof javax.script.SimpleBindings ||
						ret instanceof javax.management.openmbean.TabularDataSupport ||
						ret instanceof javax.swing.UIDefaults ||
						ret.getClass().getName().endsWith("CursorableLinkedList"))) {
			Object[] args = thisJoinPoint.getArgs();
	    	
	    	for (int i = 0; i < args.length; i++) {
	    		if (args[i] instanceof Map) {
	    			for (Object item : ((Map)args[i]).entrySet()) {
	    				Map.Entry entry = (Map.Entry)item;
	    				ReferenceMaster.setNewValue(entry.getKey(), ret);
	    				ReferenceMaster.setNewValue(entry.getValue(), ret);
	    			}
	    			break;
	    		}
	    	}
		}
    	
    	return ret;
    }
    
    /*
     * MERGED
     */
    after() returning (Object ret): call(* java.util.Collection+.*(..)) && collectionOp() && !tooBigErrorExcludeCollections() && !allExclude() {
    	// Object (ret bool)
    	if (!SimpleCommControl.getInstance().trackingEnabled())
    		return;
    	Object target = thisJoinPoint.getTarget();
    	if ((target instanceof Collection) && 
				!(target instanceof java.beans.beancontext.BeanContext ||  // These must agree with collectionOp pointcut in GeneralTracker
						target instanceof javax.management.AttributeList ||
						target instanceof javax.management.relation.RoleList ||
						target instanceof javax.management.relation.RoleUnresolvedList ||
						target instanceof java.util.Vector ||
						target instanceof java.util.EnumSet ||
						target instanceof javax.print.attribute.standard.JobStateReasons ||
						target instanceof java.security.AuthProvider ||
						target instanceof java.util.EnumMap ||
						target instanceof java.util.LinkedHashMap ||
						target instanceof javax.print.attribute.standard.PrinterStateReasons ||
						target instanceof java.util.Properties ||
						target instanceof java.security.Provider ||
						target instanceof java.awt.RenderingHints ||
						target instanceof javax.script.SimpleBindings ||
						target instanceof javax.management.openmbean.TabularDataSupport ||
						target instanceof javax.swing.UIDefaults ||
						target.getClass().getName().endsWith("CursorableLinkedList"))) {
    		String methodName = thisJoinPoint.getSignature().getName();
        	if (methodName.equals("add")) {
            	if ((Boolean)ret == true) {
        	    	Object[] args = thisJoinPoint.getArgs();
        	    	Object thisOb = thisJoinPoint.getTarget();
        			ReferenceMaster.setNewValue(args[0], thisOb);
            	}
        	}
        	else if (methodName.equals("addAll")) {
            	if ((Boolean)ret == true) {
        	    	Object[] args = thisJoinPoint.getArgs();
        	    	Object thisOb = thisJoinPoint.getTarget();
        	    	
        	    	if (args[0] instanceof Collection) {
            			for (Object item : (Collection)args[0]) {
            				ReferenceMaster.setNewValue(item, thisOb);
            			}
            		}
            	}
        	}
        	else if (methodName.equals("addAllAbsent")) {
            	Integer count = (Integer) ret;
            	if (count > 0) {
            		CopyOnWriteArrayList thisOb = (CopyOnWriteArrayList)thisJoinPoint.getTarget();
            		
            		for (int index = thisOb.size() - count; index < thisOb.size(); index++) {
            			ReferenceMaster.setNewValue(thisOb.get(index), thisOb);
            		}
            	}
        	}
        	else if (methodName.equals("addIfAbsent")) {
        		if ((Boolean)ret == true) {
        	    	Object[] args = thisJoinPoint.getArgs();
        	    	Object thisOb = thisJoinPoint.getTarget();
        	    	
        			ReferenceMaster.setNewValue(args[0], thisOb);
            	}
        	}
        	else if (methodName.equals("addFirst")) {
            	Deque thisOb = (Deque)thisJoinPoint.getTarget();
            	Object[] args = thisJoinPoint.getArgs();
            	if (args[0] == thisOb.getFirst()) {
            		ReferenceMaster.setNewValue(args[0], thisOb);
            	}
        	}
        	else if (methodName.equals("addLast")) {
            	Deque thisOb = (Deque)thisJoinPoint.getTarget();
            	Object[] args = thisJoinPoint.getArgs();
            	if (args[0] == thisOb.getLast()) {
            		ReferenceMaster.setNewValue(args[0], thisOb);
            	}
        	}
        	else if (methodName.equals("drainTo")) {
            	TaintLogger.getTaintLogger().log("DRAIN CALLED");
        	}
        	else if (methodName.equals("offer")) {
            	if ((Boolean)ret == true) {
        	    	Object[] args = thisJoinPoint.getArgs();
        	    	Object thisOb = thisJoinPoint.getTarget();
        	    	
        			ReferenceMaster.setNewValue(args[0], thisOb);
            	}
        	}
        	else if (methodName.equals("offerFirst")) {
            	if ((Boolean)ret == true) {
        	    	Object[] args = thisJoinPoint.getArgs();
        	    	Object thisOb = thisJoinPoint.getTarget();
        	    	
        			ReferenceMaster.setNewValue(args[0], thisOb);
            	}
        	}
        	else if (methodName.equals("offerLast")) {
            	if ((Boolean)ret == true) {
        	    	Object[] args = thisJoinPoint.getArgs();
        	    	Object thisOb = thisJoinPoint.getTarget();
        	    	
        			ReferenceMaster.setNewValue(args[0], thisOb);
            	}
        	}
        	else if (methodName.equals("poll")) {
            	if (ret != null) {
            		Object thisOb = thisJoinPoint.getTarget();
            		
            		ReferenceMaster.cleanupOldValue(ret, thisOb);
            	}
        	}
        	else if (methodName.equals("pollFirst")) {
            	if (ret != null) {
            		Object thisOb = thisJoinPoint.getTarget();
            		
            		ReferenceMaster.cleanupOldValue(ret, thisOb);
            	}
        	}
        	else if (methodName.equals("pollLast")) {
            	if (ret != null) {
            		Object thisOb = thisJoinPoint.getTarget();
            		
            		ReferenceMaster.cleanupOldValue(ret, thisOb);
            	}
        	}
        	else if (methodName.equals("pop")) {
            	if (ret != null) {
            		Object thisOb = thisJoinPoint.getTarget();
            		
            		ReferenceMaster.cleanupOldValue(ret, thisOb);
            	}
        	}
        	else if (methodName.equals("push")) {
            	if (thisJoinPoint.getTarget() instanceof Deque) {
        	    	Deque thisOb = (Deque)thisJoinPoint.getTarget();
        	    	Object[] args = thisJoinPoint.getArgs();
        	    	if (args[0] == thisOb.peek()) {
        	    		ReferenceMaster.setNewValue(args[0], thisOb);
        	    	}
            	}
            	else if (thisJoinPoint.getTarget() instanceof Stack) {
        	    	Stack thisOb = (Stack)thisJoinPoint.getTarget();
        	    	Object[] args = thisJoinPoint.getArgs();
        	    	if (args[0] == thisOb.peek()) {
        	    		ReferenceMaster.setNewValue(args[0], thisOb);
        	    	}
            	}
        	}
        	else if (methodName.equals("put")) {
            	Deque thisOb = (Deque)thisJoinPoint.getTarget();
            	Object[] args = thisJoinPoint.getArgs();
            	if (args[0] == thisOb.getLast()) {
            		ReferenceMaster.setNewValue(args[0], thisOb);
            	}
        	}
        	else if (methodName.equals("putFirst")) {
            	Deque thisOb = (Deque)thisJoinPoint.getTarget();
            	Object[] args = thisJoinPoint.getArgs();
            	if (args[0] == thisOb.getFirst()) {
            		ReferenceMaster.setNewValue(args[0], thisOb);
            	}
        	}
        	else if (methodName.equals("putLast")) {
            	Deque thisOb = (Deque)thisJoinPoint.getTarget();
            	Object[] args = thisJoinPoint.getArgs();
            	if (args[0] == thisOb.getLast()) {
            		ReferenceMaster.setNewValue(args[0], thisOb);
            	}
        	}
        	else if (methodName.equals("remove")) {
            	Object[] args = thisJoinPoint.getArgs();
        		Object thisOb = thisJoinPoint.getTarget();
            	if (args.length > 0) {
            		if ((ret instanceof Boolean || ret.getClass().isPrimitive())) {
        	    		if ((Boolean)ret == true) {
        	    			ReferenceMaster.cleanupOldValue(args[0], thisOb);
        	    		}
            		}
            		else {
            			ReferenceMaster.cleanupOldValue(ret, thisOb);
            		}
            	}
            	else {
            		ReferenceMaster.cleanupOldValue(ret, thisOb);
            	}
        	}
        	else if (methodName.equals("removeAll")) {
            	Collection arg = (Collection)thisJoinPoint.getArgs()[0];
        		Object thisOb = thisJoinPoint.getTarget();
        		if ((Boolean)ret == true) {
        			for (Object item : arg) {
        				ReferenceMaster.cleanupOldValue(item, thisOb);
        			}
        		}
        	}
        	else if (methodName.equals("removeFirst")) {
        		if (ret != null) {
            		Object thisOb = thisJoinPoint.getTarget();
            		
            		ReferenceMaster.cleanupOldValue(ret, thisOb);
            	}
        	}
        	else if (methodName.equals("removeFirstOccurrence")) {
        		if ((Boolean)ret == true) {
            		Object thisOb = thisJoinPoint.getTarget();
                	Object[] args = thisJoinPoint.getArgs();
            		
            		ReferenceMaster.cleanupOldValue(args[0], thisOb);
            	}
        	}
        	else if (methodName.equals("removeLast")) {
        		if (ret != null) {
            		Object thisOb = thisJoinPoint.getTarget();
            		
            		ReferenceMaster.cleanupOldValue(ret, thisOb);
            	}
        	}
        	else if (methodName.equals("removeLastOccurrence")) {
        		if ((Boolean)ret == true) {
            		Object thisOb = thisJoinPoint.getTarget();
                	Object[] args = thisJoinPoint.getArgs();
            		
            		ReferenceMaster.cleanupOldValue(args[0], thisOb);
            	}
        	}
        	else if (methodName.equals("take")) {
        		if (ret != null) {
            		Object thisOb = thisJoinPoint.getTarget();
            		
            		ReferenceMaster.cleanupOldValue(ret, thisOb);
            	}
        	}
        	else if (methodName.equals("takeFirst")) {
        		if (ret != null) {
            		Object thisOb = thisJoinPoint.getTarget();
            		
            		ReferenceMaster.cleanupOldValue(ret, thisOb);
            	}
        	}
        	else if (methodName.equals("takeLast")) {
        		if (ret != null) {
            		Object thisOb = thisJoinPoint.getTarget();
            		
            		ReferenceMaster.cleanupOldValue(ret, thisOb);
            	}
        	}
    	}
    }
    before(): call(* java.util.Collection+.*(..)) && !tooBigErrorExcludeCollections() && !allExclude() {
    	if (!SimpleCommControl.getInstance().trackingEnabled())
    		return;
    	Object target = thisJoinPoint.getTarget();
    	if ((target instanceof Collection) && 
				!(target instanceof java.beans.beancontext.BeanContext ||  // These must agree with collectionOp pointcut in GeneralTracker
						target instanceof javax.management.AttributeList ||
						target instanceof javax.management.relation.RoleList ||
						target instanceof javax.management.relation.RoleUnresolvedList ||
						target instanceof java.util.Vector ||
						target instanceof java.util.EnumSet ||
						target instanceof javax.print.attribute.standard.JobStateReasons ||
						target instanceof java.security.AuthProvider ||
						target instanceof java.util.EnumMap ||
						target instanceof java.util.LinkedHashMap ||
						target instanceof javax.print.attribute.standard.PrinterStateReasons ||
						target instanceof java.util.Properties ||
						target instanceof java.security.Provider ||
						target instanceof java.awt.RenderingHints ||
						target instanceof javax.script.SimpleBindings ||
						target instanceof javax.management.openmbean.TabularDataSupport ||
						target instanceof javax.swing.UIDefaults ||
						target.getClass().getName().endsWith("CursorableLinkedList"))) {
    		String methodName = thisJoinPoint.getSignature().getName();
        	if (methodName.equals("clear")) {
            	Collection thisOb = (Collection) thisJoinPoint.getTarget();
            	for (Object item : thisOb) {
            		ReferenceMaster.cleanupOldValue(item, thisOb);
            	}
        	}
        	else if (methodName.equals("retainAll")) {
        		Collection arg = (Collection)thisJoinPoint.getArgs()[0];
        		Collection thisOb = (Collection)thisJoinPoint.getTarget();
        		for (Object item : thisOb) {
        			if (!arg.contains(item)) {
        				ReferenceMaster.cleanupOldValue(item, thisOb);
        			}
        		}
        	}
        	else if (methodName.equals("set")) {
        		Object[] args = thisJoinPoint.getArgs();
            	Integer targetInt = (Integer)args[0];
            	Object newVal = (Object)args[1];
        		List thisOb = (List)thisJoinPoint.getTarget();
            	Object oldVal = thisOb.get(targetInt);
            	
            	ReferenceMaster.cleanupOldValue(oldVal, thisOb);
            	ReferenceMaster.setNewValue(newVal, thisOb);
        	}
    	}
    }

    after() returning (Object ret): call(* java.util.Map+.*(..)) && mapOp() && !tooBigErrorExcludeCollections() && !allExclude() {
    	if (!SimpleCommControl.getInstance().trackingEnabled())
    		return;
    	Object target = thisJoinPoint.getTarget();
    	if ((target instanceof Map) && 
				!(target instanceof java.beans.beancontext.BeanContext ||  // These must agree with collectionOp pointcut in GeneralTracker
						target instanceof javax.management.AttributeList ||
						target instanceof javax.management.relation.RoleList ||
						target instanceof javax.management.relation.RoleUnresolvedList ||
						target instanceof java.util.Vector ||
						target instanceof java.util.EnumSet ||
						target instanceof javax.print.attribute.standard.JobStateReasons ||
						target instanceof java.security.AuthProvider ||
						target instanceof java.util.EnumMap ||
						target instanceof java.util.LinkedHashMap ||
						target instanceof javax.print.attribute.standard.PrinterStateReasons ||
						target instanceof java.util.Properties ||
						target instanceof java.security.Provider ||
						target instanceof java.awt.RenderingHints ||
						target instanceof javax.script.SimpleBindings ||
						target instanceof javax.management.openmbean.TabularDataSupport ||
						target instanceof javax.swing.UIDefaults)) {
    		String methodName = thisJoinPoint.getSignature().getName();
        	if (methodName.equals("put")) {
        		Map thisOb = (Map)thisJoinPoint.getTarget();
            	Object[] args = thisJoinPoint.getArgs();
            	Object key = args[0];
            	Object value = args[0];
            	
            	ReferenceMaster.setNewValue(key, thisOb);
            	ReferenceMaster.setNewValue(value, thisOb);
        	}
        	else if (methodName.equals("putAll")) {
        		Map thisOb = (Map)thisJoinPoint.getTarget();
            	Object[] args = thisJoinPoint.getArgs();
            	if (args[0] instanceof Map) {
        			for (Object item : ((Map)args[0]).entrySet()) {
        				Map.Entry entry = (Map.Entry)item;
        				ReferenceMaster.setNewValue(entry.getKey(), thisOb);
        				ReferenceMaster.setNewValue(entry.getValue(), thisOb);
        			}
        		}
        	}
        	else if (methodName.equals("putValue")) {
        		Map thisOb = (Map)thisJoinPoint.getTarget();
            	Object[] args = thisJoinPoint.getArgs();
            	String key = (String)args[0];
            	String value = (String)args[0];
            	
            	ReferenceMaster.setNewValue(key, thisOb);
            	ReferenceMaster.setNewValue(value, thisOb);
        	}
        	else if (methodName.equals("pollFirstEntry")) {
        		if (ret != null) {
            		Object thisOb = thisJoinPoint.getTarget();
            		Map.Entry entry = (Map.Entry)ret;
            		
            		ReferenceMaster.cleanupOldValue(entry.getKey(), thisOb);
            		ReferenceMaster.cleanupOldValue(entry.getValue(), thisOb);
            	}
        	}
        	else if (methodName.equals("pollLastEntry")) {
        		if (ret != null) {
            		Object thisOb = thisJoinPoint.getTarget();
            		Map.Entry entry = (Map.Entry)ret;
            		
            		ReferenceMaster.cleanupOldValue(entry.getKey(), thisOb);
            		ReferenceMaster.cleanupOldValue(entry.getValue(), thisOb);
            	}
        	}
    	}
    }
    before(): call(* java.util.Map+.*(..)) && mapOp() && !tooBigErrorExcludeCollections() && !allExclude() {
    	if (!SimpleCommControl.getInstance().trackingEnabled())
    		return;
    	Object target = thisJoinPoint.getTarget();
    	if ((target instanceof Map) && 
				!(target instanceof java.beans.beancontext.BeanContext ||  // These must agree with collectionOp pointcut in GeneralTracker
						target instanceof javax.management.AttributeList ||
						target instanceof javax.management.relation.RoleList ||
						target instanceof javax.management.relation.RoleUnresolvedList ||
						target instanceof java.util.Vector ||
						target instanceof java.util.EnumSet ||
						target instanceof javax.print.attribute.standard.JobStateReasons ||
						target instanceof java.security.AuthProvider ||
						target instanceof java.util.EnumMap ||
						target instanceof java.util.LinkedHashMap ||
						target instanceof javax.print.attribute.standard.PrinterStateReasons ||
						target instanceof java.util.Properties ||
						target instanceof java.security.Provider ||
						target instanceof java.awt.RenderingHints ||
						target instanceof javax.script.SimpleBindings ||
						target instanceof javax.management.openmbean.TabularDataSupport ||
						target instanceof javax.swing.UIDefaults)) {
    		String methodName = thisJoinPoint.getSignature().getName();
        	if (methodName.equals("clear")) {
        		Map thisOb = (Map)thisJoinPoint.getTarget();
        		for (Object item : thisOb.entrySet()) {
        			Map.Entry entry = (Map.Entry)item;
        			ReferenceMaster.cleanupOldValue(entry.getKey(), thisOb);
        			ReferenceMaster.cleanupOldValue(entry.getValue(), thisOb);
        		}
        	}
        	else if (methodName.equals("putIfAbsent")) {
        		Map thisOb = (Map)thisJoinPoint.getTarget();
            	Object[] args = thisJoinPoint.getArgs();
            	String key = (String)args[0];
            	String value = (String)args[0];
            	if (!thisOb.containsKey(key)) {    	
        	    	ReferenceMaster.setNewValue(key, thisOb);
        	    	ReferenceMaster.setNewValue(value, thisOb);
            	}
        	}
        	else if (methodName.equals("remove")) {
        		Map thisOb = (Map)thisJoinPoint.getTarget();
        		Object[] args = thisJoinPoint.getArgs();
        		Object key = args[0];
        		if (args.length > 1) {
        			Object value = args[1];
        			if (thisOb.get(key) == value) {
        				ReferenceMaster.cleanupOldValue(key, thisOb);
        				ReferenceMaster.cleanupOldValue(thisOb.get(key), thisOb);
        			}
        		}
        		else {
        			ReferenceMaster.cleanupOldValue(key, thisOb);
        			ReferenceMaster.cleanupOldValue(thisOb.get(key), thisOb);
        		}
        	}
        	else if (methodName.equals("replace")) {
        		Map thisOb = (Map)thisJoinPoint.getTarget();
        		Object[] args = thisJoinPoint.getArgs();
        		Object key = args[0];
        		Object newValue = args[1];
        		if (args.length > 2) {
        			Object oldValue = args[2];
        			if (thisOb.get(key) == oldValue && thisOb.get(key) != null) {
        				ReferenceMaster.cleanupOldValue(thisOb.get(key), thisOb);
        				ReferenceMaster.setNewValue(newValue, thisOb);
        			}
        		}
        		else {
        			if (thisOb.get(key) != null) {
        				ReferenceMaster.cleanupOldValue(thisOb.get(key), thisOb);
        				ReferenceMaster.setNewValue(newValue, thisOb);
        			}
        		}
        	}
    	}
    }
    /*
     * END MERGED
     */
    
    /*
     * MERGE SOURCE
     */
//    after() returning (Object ret): call(* java.util.Collection+.add(..)) && collectionOp() {
//    	// Object (ret bool)
//    	if ((Boolean)ret == true) {
//	    	Object[] args = thisJoinPoint.getArgs();
//	    	Object thisOb = thisJoinPoint.getTarget();
//	    	
//			ReferenceMaster.setNewValue(args[0], thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.addAll(..)) && collectionOp() {
//    	// Collection (ret bool)
//    	if ((Boolean)ret == true) {
//	    	Object[] args = thisJoinPoint.getArgs();
//	    	Object thisOb = thisJoinPoint.getTarget();
//	    	
//	    	if (args[0] instanceof Collection) {
//    			for (Object item : (Collection)args[0]) {
//    				ReferenceMaster.setNewValue(item, thisOb);
//    			}
//    		}
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.addAllAbsent(..)) && collectionOp() {
//    	// Collection (ret int)
//    	Integer count = (Integer) ret;
//    	if (count > 0) {
//    		CopyOnWriteArrayList thisOb = (CopyOnWriteArrayList)thisJoinPoint.getTarget();
//    		
//    		for (int index = thisOb.size() - count; index < thisOb.size(); index++) {
//    			ReferenceMaster.setNewValue(thisOb.get(index), thisOb);
//    		}
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.addIfAbsent(..)) && collectionOp() {
//    	// Object (ret bool)
//    	if ((Boolean)ret == true) {
//	    	Object[] args = thisJoinPoint.getArgs();
//	    	Object thisOb = thisJoinPoint.getTarget();
//	    	
//			ReferenceMaster.setNewValue(args[0], thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.addFirst(..)) && collectionOp() {
//    	// Object
//    	Deque thisOb = (Deque)thisJoinPoint.getTarget();
//    	Object[] args = thisJoinPoint.getArgs();
//    	if (args[0] == thisOb.getFirst()) {
//    		ReferenceMaster.setNewValue(args[0], thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.addLast(..)) && collectionOp() {
//    	// Object
//    	Deque thisOb = (Deque)thisJoinPoint.getTarget();
//    	Object[] args = thisJoinPoint.getArgs();
//    	if (args[0] == thisOb.getLast()) {
//    		ReferenceMaster.setNewValue(args[0], thisOb);
//    	}
//    }
//    before(): call(* java.util.ArrayList+.clear(..)) {
//    	Collection thisOb = (Collection) thisJoinPoint.getTarget();
//    	for (Object item : thisOb) {
//    		ReferenceMaster.cleanupOldValue(item, thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.drainTo(..)) && collectionOp() {
//    	TaintLogger.getTaintLogger().log("DRAIN CALLED");
//    }
//    after() returning (Object ret): call(* java.util.Collection+.offer(..)) && collectionOp() {
//    	// Object (ret bool) || Object, timeout, unit (ret bool)
//    	if ((Boolean)ret == true) {
//	    	Object[] args = thisJoinPoint.getArgs();
//	    	Object thisOb = thisJoinPoint.getTarget();
//	    	
//			ReferenceMaster.setNewValue(args[0], thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.offerFirst(..)) && collectionOp() {
//    	// Object (ret bool) || Object, timeout, unit (ret bool)
//    	if ((Boolean)ret == true) {
//	    	Object[] args = thisJoinPoint.getArgs();
//	    	Object thisOb = thisJoinPoint.getTarget();
//	    	
//			ReferenceMaster.setNewValue(args[0], thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.offerLast(..)) && collectionOp() {
//    	// Object (ret bool) || Object, timeout, unit (ret bool)
//    	if ((Boolean)ret == true) {
//	    	Object[] args = thisJoinPoint.getArgs();
//	    	Object thisOb = thisJoinPoint.getTarget();
//	    	
//			ReferenceMaster.setNewValue(args[0], thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.poll(..)) && collectionOp() {
//    	// timeout (ret removed)
//    	if (ret != null) {
//    		Object thisOb = thisJoinPoint.getTarget();
//    		
//    		ReferenceMaster.cleanupOldValue(ret, thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.pollFirst(..)) && collectionOp() {
//    	// timeout (ret removed)
//    	if (ret != null) {
//    		Object thisOb = thisJoinPoint.getTarget();
//    		
//    		ReferenceMaster.cleanupOldValue(ret, thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.pollLast(..)) && collectionOp() {
//    	//timeout (ret removed)
//    	if (ret != null) {
//    		Object thisOb = thisJoinPoint.getTarget();
//    		
//    		ReferenceMaster.cleanupOldValue(ret, thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.pop(..)) && collectionOp() {
//    	// ret Object
//    	if (ret != null) {
//    		Object thisOb = thisJoinPoint.getTarget();
//    		
//    		ReferenceMaster.cleanupOldValue(ret, thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.push(..)) && collectionOp() {
//    	// Object
//    	if (thisJoinPoint.getTarget() instanceof Deque) {
//	    	Deque thisOb = (Deque)thisJoinPoint.getTarget();
//	    	Object[] args = thisJoinPoint.getArgs();
//	    	if (args[0] == thisOb.peek()) {
//	    		ReferenceMaster.setNewValue(args[0], thisOb);
//	    	}
//    	}
//    	else if (thisJoinPoint.getTarget() instanceof Stack) {
//	    	Stack thisOb = (Stack)thisJoinPoint.getTarget();
//	    	Object[] args = thisJoinPoint.getArgs();
//	    	if (args[0] == thisOb.peek()) {
//	    		ReferenceMaster.setNewValue(args[0], thisOb);
//	    	}
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.put(..)) && collectionOp() {
//    	// Object
//    	Deque thisOb = (Deque)thisJoinPoint.getTarget();
//    	Object[] args = thisJoinPoint.getArgs();
//    	if (args[0] == thisOb.getLast()) {
//    		ReferenceMaster.setNewValue(args[0], thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.putFirst(..)) && collectionOp() {
//    	// Object
//    	Deque thisOb = (Deque)thisJoinPoint.getTarget();
//    	Object[] args = thisJoinPoint.getArgs();
//    	if (args[0] == thisOb.getFirst()) {
//    		ReferenceMaster.setNewValue(args[0], thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.putLast(..)) && collectionOp() {
//    	// Object
//    	Deque thisOb = (Deque)thisJoinPoint.getTarget();
//    	Object[] args = thisJoinPoint.getArgs();
//    	if (args[0] == thisOb.getLast()) {
//    		ReferenceMaster.setNewValue(args[0], thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.remove(..)) && collectionOp() {
//    	// Object (ret bool) || blank (returns Removed)
//    	Object[] args = thisJoinPoint.getArgs();
//		Object thisOb = thisJoinPoint.getTarget();
//    	if (args.length > 0) {
//    		if (args[0] instanceof Object) {
//	    		if ((Boolean)ret == true) {
//	    			ReferenceMaster.cleanupOldValue(args[0], thisOb);
//	    		}
//    		}
//    		else {
//    			ReferenceMaster.cleanupOldValue(ret, thisOb);
//    		}
//    	}
//    	else {
//    		ReferenceMaster.cleanupOldValue(ret, thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.removeAll(..)) && collectionOp() {
//    	// Collection (ret bool)
//    	Collection arg = (Collection)thisJoinPoint.getArgs()[0];
//		Object thisOb = thisJoinPoint.getTarget();
//		if ((Boolean)ret == true) {
//			for (Object item : arg) {
//				ReferenceMaster.cleanupOldValue(item, thisOb);
//			}
//		}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.removeFirst(..)) && collectionOp() {
//    	// ret object (first)
//    	if (ret != null) {
//    		Object thisOb = thisJoinPoint.getTarget();
//    		
//    		ReferenceMaster.cleanupOldValue(ret, thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.removeFirstOccurrence(..)) && collectionOp() {
//    	// Object (ret bool)
//    	if ((Boolean)ret == true) {
//    		Object thisOb = thisJoinPoint.getTarget();
//        	Object[] args = thisJoinPoint.getArgs();
//    		
//    		ReferenceMaster.cleanupOldValue(args[0], thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.removeLast(..)) && collectionOp() {
//    	// ret object (last)
//    	if (ret != null) {
//    		Object thisOb = thisJoinPoint.getTarget();
//    		
//    		ReferenceMaster.cleanupOldValue(ret, thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.removeLastOccurrence(..)) && collectionOp() {
//    	// Object (ret bool)
//    	if ((Boolean)ret == true) {
//    		Object thisOb = thisJoinPoint.getTarget();
//        	Object[] args = thisJoinPoint.getArgs();
//    		
//    		ReferenceMaster.cleanupOldValue(args[0], thisOb);
//    	}
//    }
//    before(): call(* java.util.Collection+.retainAll(..)) && collectionOp() {
//    	// Collection (ret bool)
//    	Collection arg = (Collection)thisJoinPoint.getArgs()[0];
//		Collection thisOb = (Collection)thisJoinPoint.getTarget();
//		for (Object item : thisOb) {
//			if (!arg.contains(item)) {
//				ReferenceMaster.cleanupOldValue(item, thisOb);
//			}
//		}
//    }
//    before(): call(* java.util.Collection+.set(..)) && collectionOp() {
//    	// int, Object
//    	Object[] args = thisJoinPoint.getArgs();
//    	Integer target = (Integer)args[0];
//    	Object newVal = (Object)args[1];
//		List thisOb = (List)thisJoinPoint.getTarget();
//    	Object oldVal = thisOb.get(target);
//    	
//    	ReferenceMaster.cleanupOldValue(oldVal, thisOb);
//    	ReferenceMaster.setNewValue(newVal, thisOb);
//    }
//    after() returning (Object ret): call(* java.util.Collection+.take(..)) && collectionOp() {
//    	// blank (ret removed)
//    	if (ret != null) {
//    		Object thisOb = thisJoinPoint.getTarget();
//    		
//    		ReferenceMaster.cleanupOldValue(ret, thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.takeFirst(..)) && collectionOp() {
//    	// blank (ret removed)
//    	if (ret != null) {
//    		Object thisOb = thisJoinPoint.getTarget();
//    		
//    		ReferenceMaster.cleanupOldValue(ret, thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Collection+.takeLast(..)) && collectionOp() {
//    	// blank (ret removed)
//    	if (ret != null) {
//    		Object thisOb = thisJoinPoint.getTarget();
//    		
//    		ReferenceMaster.cleanupOldValue(ret, thisOb);
//    	}
//    }
    

//    before(): call(* java.util.Map+.clear(..)) && mapOp() {
//		Map thisOb = (Map)thisJoinPoint.getTarget();
//		for (Object item : thisOb.entrySet()) {
//			Map.Entry entry = (Map.Entry)item;
//			ReferenceMaster.cleanupOldValue(entry.getKey(), thisOb);
//			ReferenceMaster.cleanupOldValue(entry.getValue(), thisOb);
//		}
//    }
//    after() returning (Object ret): call(* java.util.Map+.put(..)) && mapOp() {
//    	// Object key, Object value
//    	Map thisOb = (Map)thisJoinPoint.getTarget();
//    	Object[] args = thisJoinPoint.getArgs();
//    	Object key = args[0];
//    	Object value = args[0];
//    	
//    	ReferenceMaster.setNewValue(key, thisOb);
//    	ReferenceMaster.setNewValue(value, thisOb);
//    }
//    after() returning (Object ret): call(* java.util.Map+.putAll(..)) && mapOp() {
//    	// Map 
//    	Map thisOb = (Map)thisJoinPoint.getTarget();
//    	Object[] args = thisJoinPoint.getArgs();
//    	if (args[0] instanceof Map) {
//			for (Object item : ((Map)args[0]).entrySet()) {
//				Map.Entry entry = (Map.Entry)item;
//				ReferenceMaster.setNewValue(entry.getKey(), thisOb);
//				ReferenceMaster.setNewValue(entry.getValue(), thisOb);
//			}
//		}
//    }
//    after() returning (Object ret): call(* java.util.Map+.putValue(..)) && mapOp() {
//    	// String key, String value
//    	Map thisOb = (Map)thisJoinPoint.getTarget();
//    	Object[] args = thisJoinPoint.getArgs();
//    	String key = (String)args[0];
//    	String value = (String)args[0];
//    	
//    	ReferenceMaster.setNewValue(key, thisOb);
//    	ReferenceMaster.setNewValue(value, thisOb);
//    }
//    before(): call(* java.util.Map+.putIfAbsent(..)) && mapOp() {
//    	// Object key, Object value
//    	Map thisOb = (Map)thisJoinPoint.getTarget();
//    	Object[] args = thisJoinPoint.getArgs();
//    	String key = (String)args[0];
//    	String value = (String)args[0];
//    	if (!thisOb.containsKey(key)) {    	
//	    	ReferenceMaster.setNewValue(key, thisOb);
//	    	ReferenceMaster.setNewValue(value, thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Map+.pollFirstEntry(..)) && mapOp() {
//    	// ret Map.Entry
//    	if (ret != null) {
//    		Object thisOb = thisJoinPoint.getTarget();
//    		Map.Entry entry = (Map.Entry)ret;
//    		
//    		ReferenceMaster.cleanupOldValue(entry.getKey(), thisOb);
//    		ReferenceMaster.cleanupOldValue(entry.getValue(), thisOb);
//    	}
//    }
//    after() returning (Object ret): call(* java.util.Map+.pollLastEntry(..)) && mapOp() {
//    	// ret Map.Entry
//    	if (ret != null) {
//    		Object thisOb = thisJoinPoint.getTarget();
//    		Map.Entry entry = (Map.Entry)ret;
//    		
//    		ReferenceMaster.cleanupOldValue(entry.getKey(), thisOb);
//    		ReferenceMaster.cleanupOldValue(entry.getValue(), thisOb);
//    	}
//    }
//    before(): call(* java.util.Map+.remove(..)) && mapOp() {
//    	// Object key || Object key, Object value
//    	Map thisOb = (Map)thisJoinPoint.getTarget();
//		Object[] args = thisJoinPoint.getArgs();
//		Object key = args[0];
//		if (args.length > 1) {
//			Object value = args[1];
//			if (thisOb.get(key) == value) {
//				ReferenceMaster.cleanupOldValue(key, thisOb);
//				ReferenceMaster.cleanupOldValue(thisOb.get(key), thisOb);
//			}
//		}
//		else {
//			ReferenceMaster.cleanupOldValue(key, thisOb);
//			ReferenceMaster.cleanupOldValue(thisOb.get(key), thisOb);
//		}
//    }
//    before(): call(* java.util.Map+.replace(..)) && mapOp() {
//    	// Object key, Object value || Object key, Object value, Object oldValue
//    	Map thisOb = (Map)thisJoinPoint.getTarget();
//		Object[] args = thisJoinPoint.getArgs();
//		Object key = args[0];
//		Object newValue = args[1];
//		if (args.length > 2) {
//			Object oldValue = args[2];
//			if (thisOb.get(key) == oldValue && thisOb.get(key) != null) {
//				ReferenceMaster.cleanupOldValue(thisOb.get(key), thisOb);
//				ReferenceMaster.setNewValue(newValue, thisOb);
//			}
//		}
//		else {
//			if (thisOb.get(key) != null) {
//				ReferenceMaster.cleanupOldValue(thisOb.get(key), thisOb);
//				ReferenceMaster.setNewValue(newValue, thisOb);
//			}
//		}
//    }
    /*
     * END MERGE SOURCE
     */
	
}
