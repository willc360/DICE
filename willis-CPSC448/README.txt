The taint analysis packages, aspects and datamanagement, are originally obtained from DICE\aspect\ajTracker\src.

The pointcut which untaints objects as described in the report is located in *application_name*\src\aspects\GeneralTracker.aj at line 196-212, labeled "Untainter"

*appliation_name*/src/aspect/TaintedMethodTracker.aj is used to calculate the total number of application methods executed. Be aware that startup and turning off the application also counts.
Use the "offset" parameter in MainTracer.java to deal with this.

Hacks:

In *application_name*\src\datamanagement/TaintLogger:

in addLocationElement(MyElement root, StackLocation location, String adviceType) (line 734, labeled "Added by Willis"):
Statements are added to set stub values to requestCounter, requestURI, and requestRemoteAddr. If not set, the taints will not show up in the Lee's GUI tool.

In logReturning(StackLocation location, String adviceType, Object taintSource, Long executionTime, Object calling, Object called) (line 506, labeld "Added by Willis"):
An if statement is added to remove xml elements recorded for untainted objects. This does not affect the tracking of tainted objects. The purpose is to reduce the number of elements in the log file for easier viewing and parsing. 

In Object around(): execution(public * *ResultSet.getInt(..)) of AriesWeb\src\aspects\DBCPTaint.aj:
This section is commented out for Aries only. We encountered problems in running Aries with this section enabled