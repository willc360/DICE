package aspects;

import java.util.ArrayList;

import aspects.TaintUtil.StackPath;

public class StringTracker {
    /*
     * For managing taint at the String level
	 * Pointcuts for String, StringBuilder, StringBuffer
	 * 
	 * TODO: May also need to handle CharSequence
	 * TODO: myAdvice restriction may not allow multiple advice around a single method, which would break some cases requiring two advice.
	 * TODO: do I even need to log propagation? Is it important to what I'm looking for? I'm already propagating the taint itself.
     */
	
	/*
	 * Use this to stop advice from triggering advice, which leads to infinite recursion
	 */
	pointcut myAdvice(): adviceexecution() && (within(GeneralTracker) || within(DBCPTaint) || within(RequestTracker) || within(TaintLogger) || within(TaintData) || within(TaintUtil));
    
    
    /* String */
    /* Break this one up */
    /* Can catch all constructors at once and just look for possible tainted types:
     * 		String
     * 		StringBuffer
     * 		StringBuilder
     * 		byte[]
     * 		char[]
     * 		int[]
     */
    /* Mixing, modification, propagation DONE */
    pointcut stringConstruct():
    	(call(java.lang..String.new(..)));
    
    /* Modification, propagation DONE */
    pointcut stringConstructModification():
    	(call(java.lang..String.new(byte[], int)) ||
				call(java.lang..String.new(byte[], int, int)) ||
				call(java.lang..String.new(byte[], int, int, int)) ||
				call(java.lang..String.new(byte[], int, int, String)) ||
				call(java.lang..String.new(byte[], String)) ||
				call(java.lang..String.new(char[], int, int)) ||
				call(java.lang..String.new(char[], int, int, boolean[])) || //TODO: deals with taint, remove later?
				call(java.lang..String.new(byte[], Charset)) ||
				call(java.lang..String.new(byte[], int, int, Charset)) ||
				call(java.lang..String.new(int[], int, int)));

    /* Mixing DONE */
    pointcut stringCompareTo():
    	(call(public * java.lang..String.compareTo*(String)));
    
    /* Mixing, modification, propagation DONE */
    pointcut stringConcat():
    	(call(public * java.lang..String.concat(String)));
    
    /* Propagation (data could be mixed in char[]) DONE */
    pointcut stringCopyValueOf():
    	(call(public * java.lang..String.copyValueOf(char[])));
    
    /* Propagation, modification DONE */
    pointcut stringCopyValueOfModification():
    	(call(public * java.lang..String.valueOf(char[], int, int)));
    
    /* Mixing DONE */
    pointcut stringEndsWith():
    	(call(public * java.lang..String.endsWith(..)));
    
    /* Mixing DONE */
    pointcut stringStartsWith():
    	(call(public * java.lang..String.startsWith(..)));
    
    /* Mixing DONE */
    pointcut stringEquals():
    	(call(public * java.lang..String.equals*(Object)));
	
    /* Propagation DONE */
    pointcut stringGetBytes():
    	(call(public * java.lang..String.getBytes()));
    	
	pointcut stringGetBytesModification():
    	(call(public * java.lang..String.getBytes(String)) ||
    			call(public * java.lang..String.getBytes(Charset)));	
    
    pointcut stringGetBytesNoReturn():
    	(call(public * java.lang..String.getBytes(int, int, byte[], int)));
	
    /* Propagation DONE  */
	pointcut stringGetCharsNoReturn():
		(call(public * java.lang..String.getChars(..)));
    
    /* Mixing DONE */
    pointcut stringRegionMatches():
    	(call(public * java.lang..String.regionMatches(..)));
    
    /* Modification, propagation DONE */
    pointcut stringReplace():
    	(call(public * java.lang..String.replace(char, char)));

    /* Modification, propagation DONE */
    pointcut stringSubstring():
    	(call(public * java.lang..String.substring(..)));
    
    /* Propagation DONE */
    pointcut stringToCharArray():
    	(call(public * java.lang..String.toCharArray(..)));

    /* Modification, propagation DONE */
    pointcut stringToLowerCase():
    	(call(public * java.lang..String.toLowerCase*(..)));

    /* Modification, propagation DONE */
    pointcut stringToUpperCase():
    	(call(public * java.lang..String.toUpperCase*(..)));
    
    /* Modification, propagation DONE */
    pointcut stringTrim():
    	(call(public * java.lang..String.trim()));
    	
    /* Propagation (data could be mixed in char[]. Accepts Object using toString) DONE */
    pointcut stringValueOf():
    	(call(public * java.lang..String.valueOf(char[])) ||
    			call(public * java.lang..String.valueOf(Object)));
    
    /* Propagation, modification DONE */
    pointcut stringValueOfCharModification():
    	(call(public * java.lang..String.valueOf(char[], int, int)));
    
    /* Mixing (StringBuffer or CharSequence) DONE */
    pointcut stringContentEquals():
    	(call(public * java.lang..String.contentEquals(..)));
    
    /* Mixing DONE */
    pointcut stringMatches():
    	(call(public * java.lang..String.matches(String)));
    
    /* Mixing, modification, propagation DONE */
    pointcut stringReplaceString():
    	(call(public * java.lang..String.replaceAll(String, String)) ||
    			call(public * java.lang..String.replaceFirst(String, String)) ||
    			call(public * java.lang..String.replace(CharSequence, CharSequence)));
    
    /* Modification, propagation DONE */
    pointcut stringSplit():
    	(call(public * java.lang..String.split(..)));
    
    /* Modification, propagation DONE */
    pointcut stringSubSequence():
    	(call(public * java.lang..String.subSequence(int, int)));
    
    /* Mixing DONE */
    pointcut stringContains():
    	(call(public * java.lang..String.contains(CharSequence)));
    
    /* Modification, propagation, composition DONE */
    pointcut stringFormat():
    	(call(public * java.lang..String.format(..)));
    
    
    /* StringBuilder */
    /* Expect:
     * 		String
     * 		CharSequence
     */
    /* Mixing, modification, propagation DONE */
    pointcut stringBuilderConstruct():
    	(call(java.lang..StringBuffer.new(..)));
    
    /* Mixing, modification, propagation DONE */
    pointcut stringBuilderAppend():
    	(call(public * java.lang..StringBuilder.append(char[])) ||
    			call(public * java.lang..StringBuilder.append(char)) ||
    			call(public * java.lang..StringBuilder.append(char, boolean)) ||
    			call(public * java.lang..StringBuilder.append(double)) ||
    			call(public * java.lang..StringBuilder.append(float)) ||
    			call(public * java.lang..StringBuilder.append(int)) ||
    			call(public * java.lang..StringBuilder.append(long)) ||
    			call(public * java.lang..StringBuilder.append(Object)) ||
    			call(public * java.lang..StringBuilder.append(String)) ||
    			call(public * java.lang..StringBuilder.append(StringBuffer)) ||
    			call(public * java.lang..StringBuilder.append(CharSequence)) ||
    			call(public * java.lang..StringBuilder.append(boolean)));
    
    pointcut stringBuilderAppendModification():
    	(call(public * java.lang..StringBuilder.append(char[], int, int)) ||
    			call(public * java.lang..StringBuilder.append(CharSequence, int, int)) ||
    			call(public * java.lang..StringBuilder.append(char[], int, int, boolean))); //TODO: may remove, taint
    
    /* Modification */
    pointcut stringBuilderDelete():
    	(call(public * java.lang..StringBuilder.delete*(..)));
    
    /* Propagation */
    pointcut stringBuilderGetChars():
    	(call(public * java.lang..StringBuilder.getChars(..)));
    
    /* Mixing, modification, propagation DONE */
    pointcut stringBuilderInsert():
    	(call(public * java.lang..StringBuilder.insert(int, char[])) ||
    			call(public * java.lang..StringBuilder.insert(int, char)) ||
    			call(public * java.lang..StringBuilder.insert(int, double)) ||
    			call(public * java.lang..StringBuilder.insert(int, float)) ||
    			call(public * java.lang..StringBuilder.insert(int, int)) ||
    			call(public * java.lang..StringBuilder.insert(int, long)) ||
    			call(public * java.lang..StringBuilder.insert(int, Object)) ||
    			call(public * java.lang..StringBuilder.insert(int, CharSequence)) ||
    			call(public * java.lang..StringBuilder.insert(int, boolean)) ||
    			call(public * java.lang..StringBuilder.insert(int, String)));
    
    pointcut stringBuilderInsertModification():
    	(call(public * java.lang..StringBuilder.insert(int, char[], int, int)) ||
    			call(public * java.lang..StringBuilder.insert(int, CharSequence, int, int)));
    
    /* Mixing, modification, propagation */
    pointcut stringBuilderReplace():
    	(call(public * java.lang..StringBuilder.replace(..)));
    
    /* Modification */
    pointcut stringBuilderReverse():
    	(call(public * java.lang..StringBuilder.reverse(..)));
    
    /* Modification */
    pointcut stringBuilderSetCharAt():
    	(call(public * java.lang..StringBuilder.setCharAt(..)));
    
    /* Modification */
    pointcut stringBuilderSetLength():
    	(call(public * java.lang..StringBuilder.setLength(..)));
    
    /* Propagation DONE */
    pointcut stringBuilderSubstring():
    	(call(public * java.lang..StringBuilder.substring(..)));
    
    /* Propagation DONE */
    pointcut stringBuilderToString():
    	(call(public * java.lang..StringBuilder.toString()));
    
    /* Propagation DONE */
    pointcut stringBuilderShareValue():
    	(call(public * java.lang..StringBuilder.shareValue()));
    
    /* Modification, Propagation DONE */
    pointcut stringBuilderSubSequence():
    	(call(public * java.lang..StringBuilder.subSequence(..)));
    
    /* Mixing DONE */
    pointcut stringBuilderIndexOf():
    	(call(public * java.lang..StringBuilder.indexOf(..)));
    
    /* Mixing DONE */
    pointcut stringBuilderLastIndexOf():
    	(call(public * java.lang..StringBuilder.lastIndexOf(..)));
    
    /* Modification */
    pointcut stringBuilderAppendCodePoint():
    	(call(public * java.lang..StringBuilder.appendCodePoint(..)));
    
    /* Propagation DONE */
    pointcut stringBuilderGetValue():
    	(call(public * java.lang..StringBuilder.getValue()));
    
    
    /* StringBuffer */
    /* Expect:
     * 		String
     * 		StringBuffer
     * 		CharSequence
     */
    /* Mixing, modification, propagation DONE */
    pointcut stringBufferConstruct():
    	(call(java.lang..StringBuffer.new(..)));
    
    /* Mixing, modification, propagation DONE */
    pointcut stringBufferAppend():
    	(call(public * java.lang..StringBuffer.append(char[])) ||
    			call(public * java.lang..StringBuffer.append(char)) ||
    			call(public * java.lang..StringBuffer.append(double)) ||
    			call(public * java.lang..StringBuffer.append(float)) ||
    			call(public * java.lang..StringBuffer.append(int)) ||
    			call(public * java.lang..StringBuffer.append(long)) ||
    			call(public * java.lang..StringBuffer.append(Object)) ||
    			call(public * java.lang..StringBuffer.append(String)) ||
    			call(public * java.lang..StringBuffer.append(StringBuffer)) ||
    			call(public * java.lang..StringBuffer.append(CharSequence)) ||
    			call(public * java.lang..StringBuffer.append(boolean)) );
    
    pointcut stringBufferAppendModification():
    	(call(public * java.lang..StringBuffer.append(char[], int, int)) ||
    			call(public * java.lang..StringBuffer.append(CharSequence, int, int)));
    
    /* Modification */
    pointcut stringBufferDelete():
    	(call(public * java.lang..StringBuffer.delete*(..)));
    
    /* Propagation */
    pointcut stringBufferGetChars():
    	(call(public * java.lang..StringBuffer.getChars(..)));
    
    /* Mixing, modification, propagation DONE */
    pointcut stringBufferInsert():
    	(call(public * java.lang..StringBuffer.insert(int, char[])) ||
    			call(public * java.lang..StringBuffer.insert(int, char)) ||
    			call(public * java.lang..StringBuffer.insert(int, double)) ||
    			call(public * java.lang..StringBuffer.insert(int, float)) ||
    			call(public * java.lang..StringBuffer.insert(int, int)) ||
    			call(public * java.lang..StringBuffer.insert(int, long)) ||
    			call(public * java.lang..StringBuffer.insert(int, Object)) ||
    			call(public * java.lang..StringBuffer.insert(int, CharSequence)) ||
    			call(public * java.lang..StringBuffer.insert(int, boolean)) ||
    			call(public * java.lang..StringBuffer.insert(int, String)));
    
    pointcut stringBufferInsertModification():
    	(call(public * java.lang..StringBuffer.insert(int, char[], int, int)) ||
    			call(public * java.lang..StringBuffer.insert(int, CharSequence, int, int)));
    
    /* Mixing, modification, propagation */
    pointcut stringBufferReplace():
    	(call(public * java.lang..StringBuffer.replace(..)));
    
    /* Modification */
    pointcut stringBufferReverse():
    	(call(public * java.lang..StringBuffer.reverse(..)));
    
    /* Modification */
    pointcut stringBufferSetCharAt():
    	(call(public * java.lang..StringBuffer.setCharAt(..)));
    
    /* Modification */
    pointcut stringBufferSetLength():
    	(call(public * java.lang..StringBuffer.setLength(..)));
    
    /* Propagation DONE */
    pointcut stringBufferSubstring():
    	(call(public * java.lang..StringBuffer.substring(..)));
    
    /* Propagation DONE */
    pointcut stringBufferToString():
    	(call(public * java.lang..StringBuffer.toString()));
    
    /* Propagation DONE */
    pointcut stringBufferShareValue():
    	(call(public * java.lang..StringBuffer.shareValue()));
    
    /* Modification, Propagation DONE */
    pointcut stringBufferSubSequence():
    	(call(public * java.lang..StringBuffer.subSequence(..)));
    
    /* Mixing DONE */
    pointcut stringBufferIndexOf():
    	(call(public * java.lang..StringBuffer.indexOf(..)));
    
    /* Mixing DONE */
    pointcut stringBufferLastIndexOf():
    	(call(public * java.lang..StringBuffer.lastIndexOf(..)));
    
    /* Propagation DONE */
    pointcut stringBufferGetValue():
    	(call(public * java.lang..StringBuffer.getValue()));
    
    /* Mixing */
    pointcut stringBufferTrimToSize():
    	(call(public * java.lang..StringBuffer.trimToSize()));
    
    /* Modification */
    pointcut stringBufferAppendCodePoint():
    	(call(public * java.lang..StringBuffer.appendCodePoint(..)));
    
    
    
    /* String tainting advice */
    /* 
     * Constructing advice, must detect:
     * 		-Propagation of tainted data (given when args are tainted, propagates all tainted args to newly constructed value
     * 		-Composition (multiple tainted arguments should imply this)
     * 
     */
    after() returning (Object ret): (stringConstruct() || stringBuilderConstruct() || stringBufferConstruct() || stringCopyValueOf() || stringFormat()) && !cflow(myAdvice()) {
    	Object[] args = thisJoinPoint.getArgs();
    	ArrayList composed = new ArrayList();
		StackPath location = getStackTracePath();
//    	TaintLogger.getTaintLogger().log("String construct: " + ret + " in " + location.toString());
    	if (ret != null) {
	    	for (int i = 0; i < args.length; i++) {
	        	if (args[i] instanceof String || 
	    			args[i] instanceof CharSequence || 
	    			args[i] instanceof StringBuffer || 
	    			args[i] instanceof StringBuilder ||
	    			args[i] instanceof char[] ||
	    			args[i] instanceof byte[] ||
	    			args[i] instanceof int[]) {
//	        		TaintLogger.getTaintLogger().log("Checking arg for taint: " + args[i] + " code: " + args[i].hashCode());
	        		if (TaintData.getTaintData().isTainted(args[i])) {
//		        		TaintLogger.getTaintLogger().log("taintfound");
	        			composed.add(args[i]);
	        			TaintLogger.getTaintLogger().logPropagation(location, "STRINGCONSTRUCT", args[i], ret);
	        			TaintData.getTaintData().propagateSources(args[i], ret);
	        		}
	        	}
	        }
	    	
	    	if (composed.size() > 1)
	    		TaintLogger.getTaintLogger().logComposition(location, "STRINGCONSTRUCT", composed, ret);
    	}
    }
    
	/*
	 * This advice is for all constructors which potentially modify the arguments to produce the new strings
	 * byte[], int modifies by setting high byte
	 * byte[], int, int truncates input
	 * byte[], int, int, int, truncates and sets high byte
	 * byte[], int, int, String truncates and reencodes
	 * byte[], String reencodes
	 * char[], int, int truncates
	 * char[], int, int, boolean[] truncates with taint
	 * byte[], Charset converts with charset
	 * byte[], int, int, Charset truncates and converts
	 * int[], int, int truncates
	 */
    after() returning (Object ret): stringConstructModification() && !cflow(myAdvice()) {
    	Object[] args = thisJoinPoint.getArgs();
		StackPath location = getStackTracePath();
    	
    	for (int i = 0; i < args.length; i++) {
        	if (args[i] instanceof char[] || args[i] instanceof byte[] || args[i] instanceof int[]) {
        		if (TaintData.getTaintData().isTainted(args[i]))
        			TaintLogger.getTaintLogger().logModification(location, "STRINGCONSTRUCTMOD", args[i]);
        	}
        }
    }
    
    /*
     * Static propagation without modification
     */
    after() returning (Object ret): (stringValueOf() || stringCopyValueOf()) && !cflow(myAdvice()) {
    	Object[] args = thisJoinPoint.getArgs();
		StackPath location = getStackTracePath();
    	
    	if (args[0] instanceof Object || args[0] instanceof char[]) {
    		if (TaintData.getTaintData().isTainted(args[0])) {
    			TaintLogger.getTaintLogger().logPropagation(location, "STRINGSTATICPROPNOMOD", args[0], ret);
    			TaintData.getTaintData().propagateSources(args[0], ret);
    		}
    	}
    }
    
    /*
     * Static propagation with modification
     */
    after() returning (Object ret): (stringValueOfCharModification() || stringCopyValueOfModification()) && !cflow(myAdvice()) {
    	Object[] args = thisJoinPoint.getArgs();
		StackPath location = getStackTracePath();
    	
    	if (args[0] instanceof Object || args[0] instanceof char[]) {
    		if (TaintData.getTaintData().isTainted(args[0])) {
	    		TaintLogger.getTaintLogger().logPropagation(location, "STRINGSTATICPROPMOD", args[0], ret);
	    		TaintData.getTaintData().propagateSources(args[0], ret);
	    		//Note that source is modified
	    		TaintLogger.getTaintLogger().logModification(location, "STRINGSTATICMOD", args[0]);
    		}
    	}
    }
    
    /*
     * Propagation without modification
     */
    after() returning (Object ret): 	(stringGetBytes() || stringToCharArray() || stringBuilderToString() || stringBuilderShareValue() ||
    					stringBuilderGetValue() || stringBufferToString() || stringBufferShareValue() || stringBufferGetValue()) && !cflow(myAdvice()) {
    	StackPath location = getStackTracePath();

		if (TaintData.getTaintData().isTainted(thisJoinPoint.getThis())) {
			TaintLogger.getTaintLogger().logPropagation(location, "STRINGPROPNOMOD", thisJoinPoint.getThis(), ret);
			TaintData.getTaintData().propagateSources(thisJoinPoint.getThis(), ret);
		}
    }
    
    /*
     * Propagation to arguments with modification (void return)
     */
    
    before(): (stringGetBytesNoReturn() || stringGetCharsNoReturn() || stringBuilderGetChars() || stringBufferGetChars()) && !cflow(myAdvice()) {
    	Object[] args = thisJoinPoint.getArgs();
		StackPath location = getStackTracePath();
    	
    	for (int i = 0; i < args.length; i++) {
        	if (args[i] instanceof char[] ||
    			args[i] instanceof byte[]) {
        		if (TaintData.getTaintData().isTainted(thisJoinPoint.getThis())) {
	        		TaintLogger.getTaintLogger().logPropagation(location, "STRINGPROPARGMOD", thisJoinPoint.getThis(), args[i]);
	        		TaintData.getTaintData().propagateSources(thisJoinPoint.getThis(), args[i]);
	        		//Note modification of this and args[i]
	        		TaintLogger.getTaintLogger().logModification(location, "STRINGARGMODSRC", thisJoinPoint.getThis());
        		}
        		if (TaintData.getTaintData().isTainted(args[i]))
	        		TaintLogger.getTaintLogger().logModification(location, "STRINGARGMODDEST", args[i]);
        	}
        }
    }
    
    /*
     * Propagation with modification
     */
    after() returning (Object ret): 	(stringGetBytesModification() || stringReplace() || stringSubstring() || stringToLowerCase() || stringToUpperCase()  || 
    					stringTrim() || stringSubSequence() || stringBuilderSubstring() || stringBuilderSubSequence() || stringBufferSubstring() || stringBufferSubSequence()) && !cflow(myAdvice()) {
    	StackPath location = getStackTracePath();

		if (TaintData.getTaintData().isTainted(thisJoinPoint.getThis())) {
			TaintLogger.getTaintLogger().logPropagation(location, "STRINGPROPMOD", thisJoinPoint.getThis(), ret);
			TaintData.getTaintData().propagateSources(thisJoinPoint.getThis(), ret);
			TaintLogger.getTaintLogger().logModification(location, "STRINGMOD", thisJoinPoint.getThis());
		}
    }
    
    /*
     * Propagation by splitting
     */
    after() returning (Object ret): stringSplit() && !cflow(myAdvice()) {
    	StackPath location = getStackTracePath();
		
		if (TaintData.getTaintData().isTainted(thisJoinPoint.getThis())) {
			TaintLogger.getTaintLogger().logPropagation(location, "STRINGSPLITPROP", thisJoinPoint.getThis(), ret);
			TaintData.getTaintData().propagateSources(thisJoinPoint.getThis(), ret);
			if (ret instanceof String[]) {
				String[] splitString = (String[]) ret;
				for (int i = 0; i < splitString.length; i++) {
					TaintLogger.getTaintLogger().logPropagation(location, "STRINGSPLITPROPSUB", thisJoinPoint.getThis(), splitString[i]);
					TaintData.getTaintData().propagateSources(thisJoinPoint.getThis(), splitString[i]);
				}
			}
		}
    }
    
    /*
     * Concat propagates and composes this and argument
     */
    after() returning (Object ret): stringConcat() && !cflow(myAdvice()) {
    	Object[] args = thisJoinPoint.getArgs();
    	ArrayList composed = new ArrayList();
		StackPath location = getStackTracePath();
    	
    	if (TaintData.getTaintData().isTainted(thisJoinPoint.getThis())) {
        	composed.add(thisJoinPoint.getThis());
	    	TaintLogger.getTaintLogger().logPropagation(location, "STRINGPROPCONCATTHIS", thisJoinPoint.getThis(), ret);
	    	TaintData.getTaintData().propagateSources(thisJoinPoint.getThis(), ret);
    	}
    	
    	if (args[0] != null) {
    		if (TaintData.getTaintData().isTainted(args[0])) {
	    		composed.add(args[0]);
	    		TaintLogger.getTaintLogger().logPropagation(location, "STRINGPROPCONCATARG", args[0], ret);
	    		TaintData.getTaintData().propagateSources(args[0], ret);
    		}
    	}
    	
    	// composed now contains list of composed objects
    	if (composed.size() > 1)
    		TaintLogger.getTaintLogger().logComposition(location, "STRINGCOMPCONCAT", composed, ret);
    	else if (composed.size() == 1) {
    		if (TaintData.getTaintData().isTainted(thisJoinPoint.getThis()))
    			TaintLogger.getTaintLogger().logModification(location, "STRINGMODCONCATTHIS", thisJoinPoint.getThis());
    		else
    			TaintLogger.getTaintLogger().logModification(location, "STRINGMODCONCATARG", args[0]);
    	}
    }
    
    /*
     * Currently finds composition from relaceAll, replaceFirst
     */
    after() returning (Object ret): stringReplaceString() && !cflow(myAdvice()) {
    	Object[] args = thisJoinPoint.getArgs();
    	ArrayList composed = new ArrayList();
		StackPath location = getStackTracePath();
    	
		if (TaintData.getTaintData().isTainted(thisJoinPoint.getThis())) {
	    	composed.add(thisJoinPoint.getThis());
	    	TaintLogger.getTaintLogger().logPropagation(location, "STRINGPROPREPLACETHIS", thisJoinPoint.getThis(), ret);
	    	TaintData.getTaintData().propagateSources(thisJoinPoint.getThis(), ret);
		}
    	
    	// Mixing still happens with args[0], will want to note this. Added to following aspect which handles general association
    	
    	if (args[1] != null) {
    		if (TaintData.getTaintData().isTainted(args[0])) {
    			composed.add(args[1]);
        		TaintLogger.getTaintLogger().logPropagation(location, "STRINGPROPREPLACEARG", args[1], ret);
    			TaintData.getTaintData().propagateSources(args[1], ret);
    		}
    	}
    	
    	// composed now contains list of composed objects
    	if (composed.size() > 1) {
    		TaintLogger.getTaintLogger().logComposition(location, "STRINGCOMPREPLACE", composed, ret);
    		TaintLogger.getTaintLogger().logModification(location, "STRINGMODREPLACE", thisJoinPoint.getThis());
    	}
    	else if (composed.size() == 1) {
    		if (TaintData.getTaintData().isTainted(thisJoinPoint.getThis()))
    			TaintLogger.getTaintLogger().logModification(location, "STRINGMODREPLACETHIS", thisJoinPoint.getThis());
    		else
    			TaintLogger.getTaintLogger().logModification(location, "STRINGMODREPLACEARG", args[0]);
    	}
    }
    
    /*
     * Advice for methods which cause data to associate 
     */
    after() returning (Object ret): 	(stringCompareTo() || stringEndsWith() || stringStartsWith() || stringEquals() || 
    					stringRegionMatches() || stringContentEquals() || stringMatches() || stringContains() || stringReplaceString() ||
    					stringBuilderIndexOf() || stringBuilderLastIndexOf() || stringBufferIndexOf() || stringBufferLastIndexOf()) && !cflow(myAdvice()) {
    	
    	Object[] args = thisJoinPoint.getArgs();
    	ArrayList associated = new ArrayList();
		StackPath location = getStackTracePath();
		
		if (TaintData.getTaintData().isTainted(thisJoinPoint.getThis()))
			associated.add(thisJoinPoint.getThis());
    	
    	for (int i = 0; i < args.length; i++) {
        	if (args[i] instanceof String || 
    			args[i] instanceof Object || 
    			args[i] instanceof StringBuffer || 
    			args[i] instanceof CharSequence) {
        		if (TaintData.getTaintData().isTainted(args[i]))
        			associated.add(args[i]);
        	}
        }
    	
    	// mixed now contains list of mixed objects
        if (associated.size() > 1)
        	TaintLogger.getTaintLogger().logAssociation(location, "STRINGASSOC", associated);
    }
    
    /*
     * Advice for modification of this but not arguments
     */
    after() returning (Object ret): 	(stringBuilderAppend() || stringBuilderInsert() || stringBuilderReplace() ||
    					stringBufferAppend() || stringBufferInsert() || stringBufferReplace()) && !cflow(myAdvice()) {
    	Object[] args = thisJoinPoint.getArgs();
    	ArrayList composed = new ArrayList();
		StackPath location = getStackTracePath();
		
		if (TaintData.getTaintData().isTainted(thisJoinPoint.getThis()))
			composed.add(thisJoinPoint.getThis());
    	
    	for (int i = 0; i < args.length; i++) {
        	if (args[i] instanceof String || 
    			args[i] instanceof CharSequence || 
    			args[i] instanceof StringBuffer || 
    			args[i] instanceof char[]) {
        		if (TaintData.getTaintData().isTainted(args[i])) {
	        		if (!TaintData.getTaintData().isTainted(thisJoinPoint.getThis()))
	        			TaintLogger.getTaintLogger().logModification(location, "STRINGMODARGS", args[i]);
	        		composed.add(args[i]);
	        		TaintLogger.getTaintLogger().logPropagation(location, "STRINGPROPTHIS", args[i], thisJoinPoint.getThis());
	        		TaintData.getTaintData().propagateSources(args[i], thisJoinPoint.getThis());
        		}
        	}
        }

    	// Note modification of this
    	// composed now contains list of composed objects
    	if (composed.size() > 1) {
    		TaintLogger.getTaintLogger().logComposition(location, "STRINGCOMPTHIS", composed, thisJoinPoint.getThis());
    		TaintLogger.getTaintLogger().logModification(location, "STRINGMODTHIS", thisJoinPoint.getThis());
    	}
    	else if (composed.size() == 1) {
    		if (TaintData.getTaintData().isTainted(thisJoinPoint.getThis()))
    			TaintLogger.getTaintLogger().logModification(location, "STRINGMODTHIS", thisJoinPoint.getThis());
    	}
    }
    
    /*
     * Advice for modification of this and arguments
     */
    after() returning (Object ret): 	(stringBuilderAppendModification() || stringBuilderInsertModification() ||
    					stringBufferAppendModification() || stringBufferInsertModification()) && !cflow(myAdvice()) {
    	Object[] args = thisJoinPoint.getArgs();
    	ArrayList composed = new ArrayList();
		StackPath location = getStackTracePath();
		
    	composed.add(thisJoinPoint.getThis());
    	
    	for (int i = 0; i < args.length; i++) {
        	if (args[i] instanceof String || 
    			args[i] instanceof CharSequence || 
    			args[i] instanceof StringBuffer || 
    			args[i] instanceof char[]) {
        		if (TaintData.getTaintData().isTainted(args[i])) {
	        		composed.add(args[i]);
	        		TaintLogger.getTaintLogger().logPropagation(location, "STRINGPROPTHIS", args[i], thisJoinPoint.getThis());
	        		TaintData.getTaintData().propagateSources(args[i], thisJoinPoint.getThis());
	        		TaintLogger.getTaintLogger().logModification(location, "STRINGMODARGS", args[i]);
        		}
        	}
        }

    	// Note modification of this
    	// composed now contains list of composed objects
    	if (composed.size() > 1) {
    		TaintLogger.getTaintLogger().logComposition(location, "STRINGCOMPTHIS", composed, thisJoinPoint.getThis());
    		TaintLogger.getTaintLogger().logModification(location, "STRINGMODTHIS", thisJoinPoint.getThis());
    	}
    	else if (composed.size() == 1) {
    		if (TaintData.getTaintData().isTainted(thisJoinPoint.getThis()))
    			TaintLogger.getTaintLogger().logModification(location, "STRINGMODTHIS", thisJoinPoint.getThis());
    	}
    }
    
    /* 
     * Advice for modification of this 
     */
    
    after() returning (Object ret):	(stringBuilderDelete() || stringBufferDelete() || stringBuilderReverse() || stringBufferReverse() ||
    					stringBuilderAppendCodePoint() || stringBufferAppendCodePoint()) && !cflow(myAdvice()) {
    	StackPath location = getStackTracePath();
		
    	// Note modification of this
		if (TaintData.getTaintData().isTainted(thisJoinPoint.getThis()))
			TaintLogger.getTaintLogger().logModification(location, "STRINGMODTHIS", thisJoinPoint.getThis());
    }
    
    /*
     * Advice for modification of this (void return)
     */
    
    before(): (stringBuilderSetCharAt() || stringBufferSetCharAt() || stringBuilderSetLength() || stringBufferSetLength()) && !cflow(myAdvice()) {
    	StackPath location = getStackTracePath();
		
		if (TaintData.getTaintData().isTainted(thisJoinPoint.getThis()))
			TaintLogger.getTaintLogger().logModification(location, "STRINGMODTHISVOID", thisJoinPoint.getThis());
    	// Note modification of this
    }
    
    /*
     * Monitors all method invocations and returns for fuzzy propagation
     */
    after() returning (Object ret): execution(public * org.jresearch..*.*(..)) && !cflow(myAdvice()) {
    	Object[] args = thisJoinPoint.getArgs();
		StackPath location = getStackTracePath();

		ArrayList taintedArgs = new ArrayList();
		for (int i = 0; i < args.length; i++) {
			if (args[i] instanceof String || args[i] instanceof StringBuffer || args[i] instanceof StringBuilder) { // TODO: add StringBuffer/Builder
				if (args[i] != null && TaintData.getTaintData().isTainted(args[i])) {
					taintedArgs.add(args[i]);
				}
			} 
		}
		
		if (taintedArgs.size() > 0 && ret != null) {
			if (!TaintData.getTaintData().isTainted(ret)) {
				Iterator argsIt = taintedArgs.iterator();
				while (argsIt.hasNext()) {
					Object arg = argsIt.next();
					if (TaintUtil.getLevenshteinDistance(arg.toString(), ret.toString()) < 
							Math.abs(arg.toString().length() - ret.toString().length()) + 
							Math.min(arg.toString().length(), ret.toString().length()) * 0.20 &&
							Math.min(arg.toString().length(), ret.toString().length()) > 0) {
						TaintLogger.getTaintLogger().logFuzzyPropagation(location, "FUZZYPROP", arg, ret);
						TaintData.getTaintData().propagateSources(arg, ret);
						break;
					}
				}
			}
		}
	}

	// Moving this to general tracker, has compatible pointcut
//	public Object processStringBufferTrimToSize(CallerInvocation invocation) throws Throwable {
//    	Object[] args = invocation.getArguments();
//		StackPath location = TaintUtil.getStackTracePath();
//		Object ret = invocation.invokeNext();
//
//		ArrayList<Object> taintedArgs = new ArrayList<Object>();
//		for (int i = 0; i < args.length; i++) {
//			if (args[i] instanceof String || args[i] instanceof StringBuffer || args[i] instanceof StringBuilder) { // TODO: add StringBuffer/Builder
//				if (args[i] != null && TaintData.getTaintData().isTainted(args[i])) {
//					taintedArgs.add(args[i]);
//				}
//			} 
//		}
//		
//		if (taintedArgs.size() > 0 && ret != null) {
//			if (!TaintData.getTaintData().isTainted(ret)) {
//				for (Object arg : taintedArgs) {
//					if (TaintUtil.getLevenshteinDistance(arg.toString(), ret.toString()) < 
//							Math.abs(arg.toString().length() - ret.toString().length()) + 
//							Math.min(arg.toString().length(), ret.toString().length()) * 0.20 &&
//							Math.min(arg.toString().length(), ret.toString().length()) > 0) {
//						TaintLogger.getTaintLogger().logFuzzyPropagation(location, "FUZZYPROP", arg, ret);
//						TaintData.getTaintData().propagateSources(arg, ret);
//						break;
//					}
//				}
//			}
//		}
//    	return ret;
//	}
	
}
