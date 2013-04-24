<%@ page import="org.apache.aries.samples.ariestrader.web.*,java.util.*"%>
<form name="input" action="/ariestrader/monitor.jsp" method="get">
	<input type="hidden" name="action" value="reset"/>
	<input type="submit" value="Reset" />
</form>
<%
	// Get iterator for keys in HashMap
	Hashtable monitorSession = (Hashtable)MonitorJsp.cloneIt("b");
	Iterator sessionIter     = monitorSession.keySet().iterator();
	String id = "" ;
	ItemMonitor itm = null ;
%>
<table><thead><tr>
  <th>id</th><th>act</th><th>run</th><th>err</th><th>last</th><th>min/max</th><th>Sum</th>
</tr></thead>
<tbody><%
while(sessionIter.hasNext() ) {
  id = (String)sessionIter.next();
  itm = (ItemMonitor)monitorSession.get(id);
  try
  {
		%><tr valign="top"><td ><%=id%></td><%=itm.toHtmlString()%></tr><%
  }
  catch ( java.lang.IllegalStateException ie ) {
  }
}
%>
</tbody></table>