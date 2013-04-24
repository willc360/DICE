package org.apache.aries.samples.ariestrader.web;

public class ItemMonitor {
	private long nbrun  = 0  ;
	private long nbact = 0 ;
	private long nberr = 0 ;
	private long mints = 0 ;
	private long maxts = 0 ;
	private long lastAcc = 0 ;
	private long sumts   = 0 ;
	
	/**
	 * Constructeur
	 */
	public ItemMonitor() {
		this.add() ;
	}

	public void reset() {
		nbrun  = 0  ;
		nberr = 0 ;
		mints = 0 ;
		maxts = 0 ;
		sumts   = 0 ;
	}
	
	public void add() {
		nbact++ ;
		lastAcc = System.currentTimeMillis() ;
	}
	
	public void remove(long delay) {
		nbrun++ ;
		nbact-- ;

		sumts += delay ; 
		if (mints < 1 || delay < mints)	mints = delay ;
		if (delay > maxts) maxts = delay ;
	}

	public void error(long delay) {
		nberr++ ;
		nbact-- ;

		sumts += delay ; 
	}

	public String toString() {
		StringBuffer sb = new StringBuffer(50) ;

		sb.append(nbact) ;
		sb.append(" ; " ) ;
		sb.append(nbrun) ;
		sb.append(" ; " ) ;
		sb.append(nberr) ;
		sb.append(" ; " ) ;
		sb.append((new java.util.Date(lastAcc)).toString()) ;
		sb.append(" ; " ) ;
		sb.append(mints) ;
		sb.append(" ; " ) ;
		sb.append(maxts) ;
		sb.append(" ; " ) ;
		sb.append(sumts) ;
		sb.append(" ; " ) ;
		
		return sb.toString() ;
	}
	
	public String toHtmlString() {
		StringBuffer sb = new StringBuffer(50) ;

		sb.append("<td class=tdr>") ;
		sb.append(nbact) ;
		sb.append("</td><td class=tdr>") ;
		sb.append(nbrun) ;
		sb.append("</td><td class=tdr>") ;
		sb.append(nberr) ;
		sb.append("</td><td>") ;
		sb.append((new java.util.Date(lastAcc)).toString()) ;
		sb.append("</td><td>") ;
		sb.append(mints) ;
		sb.append("/") ;
		sb.append(maxts) ;
		sb.append("</td><td class=tdr>") ;
		sb.append(sumts) ;
		sb.append("</td>") ;
		
		return sb.toString() ;
	}
	
	public long getNbrun() {
		return nbrun;
	}
	public long getNbact() {
		return nbact;
	}
	public long getMints() {
		return mints;
	}
	public long getMaxts() {
		return maxts;
	}
	public long getLastAcc() {
		return lastAcc;
	}
	public java.util.Date getLastDt() {
		return new java.util.Date(lastAcc);
	}
	public long getSumts() {
		return sumts;
	}
}