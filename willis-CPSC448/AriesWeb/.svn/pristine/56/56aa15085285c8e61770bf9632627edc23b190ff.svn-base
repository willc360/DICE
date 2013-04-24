package org.apache.aries.samples.ariestrader.web;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class RemoteStatNotifier {
	
	public static final String STAT_SERVER = "142.103.25.22";
	public static final int STAT_PORT = 8787;
	
	/**
     * sends the given message to the given server:port.
     * returns true iff there were no *detectable* errors.
     * (note that this doesn't mean there weren't errors!)
     */
    static boolean send(String command, String server, int port) {
        try {
            Socket socket = new Socket(server, port);
            OutputStream os = socket.getOutputStream();
            BufferedOutputStream out = new BufferedOutputStream(os);
            out.write(command.getBytes());
            out.write('\r');
            out.flush();
            socket.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    static void start(String action){
    	if (!Boolean.parseBoolean(System.getProperty("remote.stats")))
    		return;
    	String command = "start " + (action == null ? "null" :
    		action) + " " + System.currentTimeMillis();
    	RemoteStatNotifier.send(command, RemoteStatNotifier.STAT_SERVER, 
    			RemoteStatNotifier.STAT_PORT);
    }
    
    static void stop(String action, long startTime){
    	if (!Boolean.parseBoolean(System.getProperty("remote.stats")))
    		return;
    	String command = "stop " + (action == null ? "null" :
    		action) + " " + System.currentTimeMillis() + " " + (System.currentTimeMillis() - startTime);
    	RemoteStatNotifier.send(command, RemoteStatNotifier.STAT_SERVER, 
    			RemoteStatNotifier.STAT_PORT);
    }

}
