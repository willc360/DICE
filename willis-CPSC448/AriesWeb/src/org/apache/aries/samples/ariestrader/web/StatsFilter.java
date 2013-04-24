package org.apache.aries.samples.ariestrader.web;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
 
public final class StatsFilter implements Filter {
    private FilterConfig filterConfig = null;
    private String appli = "" ;
 
    public void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {
        if (filterConfig == null)  return;

        
    	RemoteStatNotifier.start(((HttpServletRequest)request).getParameter("action"));
        String key = "" ;
    	long startTime = System.currentTimeMillis();
    	
        try {
    		key = appli + ((HttpServletRequest)request).getServletPath()+"/"+((HttpServletRequest)request).getParameter("action");
    		if (key.contains("monitor.jsp") && ((HttpServletRequest)request).getParameter("action") != null &&
    				((HttpServletRequest)request).getParameter("action").equalsIgnoreCase("reset")){
    			MonitorJsp.reset();
    			((HttpServletResponse) response).sendRedirect("/ariestrader/monitor.jsp");
    			return;
    		}
    		// On ajoute la page dans la liste des pages en cours
    		MonitorJsp.action( MonitorJsp.ACT_ADD, key, 0) ;
    		chain.doFilter(request, response);
    		// On supprime la page dans la liste des pages en cours (+temsp d'execution)
    		MonitorJsp.action( MonitorJsp.ACT_DEL, key, (System.currentTimeMillis() - startTime) ) ;
    		RemoteStatNotifier.stop(((HttpServletRequest)request).getParameter("action"), startTime);
        } catch (IOException io) {
    		// Une erreur ce produit => on la trace puis on la renvoie
    		MonitorJsp.action( MonitorJsp.ACT_ERR, key, (System.currentTimeMillis() - startTime) ) ;
    		RemoteStatNotifier.stop(((HttpServletRequest)request).getParameter("action"), startTime);
    		throw io ;
        } catch (ServletException se) {
    		// Une erreur ce produit => on la trace puis on la renvoie
    		MonitorJsp.action( MonitorJsp.ACT_ERR, key, (System.currentTimeMillis() - startTime) ) ;
    		RemoteStatNotifier.stop(((HttpServletRequest)request).getParameter("action"), startTime);
    		throw se ;
        }
        
        
    }
    /**
     * Lecture des paramètres d'exécution (dans web.xml)
     *  Ici uniquement la zone APPLICATION
     */
    public void init(FilterConfig filterConfig) throws ServletException {
	this.filterConfig = filterConfig;
	appli = filterConfig.getInitParameter("APPLICATION") ;
    }
 
    public void destroy() {
	this.filterConfig = null;
    }
}
