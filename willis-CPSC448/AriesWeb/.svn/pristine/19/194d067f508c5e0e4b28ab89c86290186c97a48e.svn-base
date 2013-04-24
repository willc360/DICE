package org.apache.aries.samples.ariestrader.web;

import java.text.SimpleDateFormat;
import java.util.*;
import java.io.* ;
 
/**
 * Classe de suppervision permettant de suivre l'état de certains objets
 * c'est un classe <b>static</b>
 *
 * @author rberthou
 *
 */
public class MonitorJsp {
	public static final String version = "MonitorJsp V1.00 du 4 Septembre 2008";
	
	static protected Hashtable map = new Hashtable(100);

	static protected String NEW_LINE = System.getProperty("line.separator") ; 
	static protected SimpleDateFormat sf= new SimpleDateFormat ("dd/MM/yyyy - HH:mm:ss");

	static final int ACT_ADD = 1 ;
	static final int ACT_DEL = 2 ;
	static final int ACT_ERR = 5 ;

    public static synchronized void action(int act, String key, long delay) {
    	
    	ItemMonitor itm = (ItemMonitor)map.get(key) ;
    	if ( itm == null ) {
    		if (act == ACT_ADD) {
    			itm = new ItemMonitor() ;
    			map.put(key, itm) ;
    		} 
    	} else {
			if (act == ACT_ADD) itm.add() ; 		else
   			if (act == ACT_DEL) itm.remove(delay) ; else
   			if (act == ACT_ERR) itm.error(delay) ; 
    	}
    }
    
    public static synchronized void reset(){
    	map = new Hashtable(100);
    }
    
    public static synchronized void rotateFile(String filename) {
    	
        try {
        	java.io.FileWriter fw = new FileWriter(filename, true ) ;

        	Iterator iter     = map.keySet().iterator();
        	String k = null ;
        	
        	fw.write("# sauvegarde statistique du " + sf.format(new java.util.Date()) + NEW_LINE) ;
        	while ( iter.hasNext() ) {
        		k = (String)iter.next();
        		ItemMonitor itm = (ItemMonitor)map.get(k) ;
        		
        		fw.write(k + " ; " + itm.toString() + NEW_LINE) ;
        		itm.reset() ;
        	}
        	fw.write("# fin sauvegarde " + NEW_LINE) ;

        	fw.flush() ;
        	fw.close() ;
        } catch (Exception e1) {
        	System.out.println("--> sauvegarde impossible") ;
        }
        
    }

    public static Object get(String key) {
    	return map.get(key); 
    }
    
    public static Object cloneIt(String k) {
    	return map.clone(); 
    }
  }