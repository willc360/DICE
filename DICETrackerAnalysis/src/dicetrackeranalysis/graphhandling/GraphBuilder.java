/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dicetrackeranalysis.graphhandling;

import dicetrackeranalysis.datasourceinfo.DataSourceInfoBuilder;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.NodeList;
import sun.java2d.SunGraphicsEnvironment.T1Filter;

/**
 *
 * @author lee
 */
public class GraphBuilder {

    private HashSet<TaintEdge> edgeList;
    public HashMap<String, TaintNode> nodeMap;
    public LinkedHashMap<String, String> taintIDs;
    public HashMap<String, LinkedList<String>> propagationMap;
    public LinkedList<TaintIDPropagationPair> taintIDPropagations;
    public HashMap<String, RequestCounterURIPair> requestCounters;
    public HashMap<TaintNode, Integer> nodeColors;
    public HashMap<TaintEdge, Integer> edgeColors;

    private HashMap<String, TaintNode> taintIDToInputMap;

    public String debugName;

    private Graph<TaintNode, TaintEdge> unfilteredMultiGraph = null;
    private Graph<TaintNode, TaintEdge> unfilteredLightMultiGraph = null;
    private Graph<TaintNode, TaintEdge> unfilteredGraph = null;

    public class TaintTypeValue {
        public String type;
        public String value;

        public TaintTypeValue(String type, String value) {
            this.type = type;
            this.value = value;
        }
    }

    private void init() {
        taintIDs = new LinkedHashMap<String, String>();
        taintIDPropagations = new LinkedList<TaintIDPropagationPair>();
        taintIDToInputMap = new HashMap<String, TaintNode>();
        requestCounters = new HashMap<String, RequestCounterURIPair>();
        nodeColors = new HashMap<TaintNode, Integer>();
        edgeColors = new HashMap<TaintEdge, Integer>();
        propagationMap = new HashMap<String, LinkedList<String>>();
    }

    public GraphBuilder(File input) {
        edgeList = new HashSet<TaintEdge>(500000);
        nodeMap = new HashMap<String, TaintNode>();
        init();
        fillEdgeListFromFile(input);

        for (TaintIDPropagationPair propagationPair : taintIDPropagations) {
            LinkedList<String> propagations = propagationMap.get(propagationPair.getSourceID());
            if (propagations == null) {
                propagations = new LinkedList<String>();
                propagationMap.put(propagationPair.getSourceID(), propagations);
            }
            propagations.add(propagationPair.getDestID());
        }

        connectReadsToResultSets();
        addSupplementaryEdges();
        RINRETExcludePass();
        // Does this follow supplementary edges correctly? Should be able to track use even through session attribute
        // Supplementary edge can probably be though of almost as a return

        // Sometimes this takes a looooong time. Anything with around 60K edges and up is a bad idea until this is repaired.
        markUnusedSubTaint();
        setObjectFieldNames();
    }

    public GraphBuilder(GraphBuilder input) {
        edgeList = new HashSet<TaintEdge>(input.edgeList);
        nodeMap = new HashMap<String, TaintNode>(input.nodeMap);
        init();
    }

    public GraphBuilder(GraphBuilder input, boolean noEdgeCopy) {
        edgeList = new HashSet<TaintEdge>(500000);
        nodeMap = new HashMap<String, TaintNode>(input.nodeMap);
        init();
    }

    private void RINRETExcludePass() {
        FilterByRINRETExclude filter = new FilterByRINRETExclude();
        LinkedList<TaintEdge> sortedEdgeList = this.getOrderedEdgeList();
        for (TaintEdge edge : sortedEdgeList) {
            if (edge.getAdviceType().equals("NONTAINTRETURN") || edge.getType().equals("COMPOSITION") ||
                    edge.getType().equals("ASSOCIATION")) {
                continue;
            }

            TaintNode callingNode = edge.getCallingNode();
            TaintNode calledNode = edge.getCalledNode();
            if (callingNode != null && calledNode != null) {
                if (!filter.pass(edge)) {
                    this.edgeList.remove(edge);
                }
            }
        }
    }

    // In the graph data tainted data can come from resultsets. These resultsets themselves are also tainted and flow in the graph.
    // This method modifies the graph to indicate how the read data actually comes from these result sets.
    private void connectReadsToResultSets() {
        Graph<TaintNode, TaintEdge> fullGraph = this.getMultiGraph();

        // Simply look for resultset get string nodes, see where they go, in context, look for input resultset
        HashSet<TaintEdge> deleteEdges = new HashSet<TaintEdge>();
        for (TaintNode node : fullGraph.getVertices()) {
//            System.out.println("NODE NAME: " + node.getName());
            if (node.getName().startsWith("java.sql.ResultSet:getString") ||
                    node.getName().startsWith("com.mysql.jdbc.ResultSet:getString") ||
                    node.getName().startsWith("com.mysql.jdbc.ResultSet:getInt") ||
                    node.getName().startsWith("java.sql.ResultSet:getInt")) {
                String resultSetID = node.getID();
                for (TaintEdge dataEdge : fullGraph.getOutEdges(node)) {
                    TaintNode destNode = fullGraph.getDest(dataEdge);

                    String dataContext = dataEdge.getInputContextCounter();

                    for (TaintEdge rsEdge : fullGraph.getInEdges(destNode)) {
                        if (rsEdge != dataEdge) {
                            if (rsEdge.carriesTarget(resultSetID)) {
                                String rsContext = rsEdge.getInputContextCounter();

                                if (rsContext.equals(dataContext)) {
                                    // need to change rsEdge destination to be the data source for the getString
                                    TaintNode newDest = GraphBuilder.walkToSource(fullGraph, node, new HashSet<TaintNode>());
//                                    System.out.println("RECONNECTING GRAPH: edge: " + rsEdge + " to new destnode: " + newDest + " walked from: " + node);

                                    TaintEdge newEdge = rsEdge.copyEdge();
                                    newEdge.setCalledNode(newDest);
                                    newEdge.setIsPostProcessingEdge();
                                    edgeList.add(newEdge);
                                    deleteEdges.add(rsEdge);
                                    // ADD Edges, don't reconnect, and then remove later.
                                }
                            }
                        }
                    }
                }
            }
        }

        for (TaintEdge deleteEdge : deleteEdges) {
            edgeList.remove(deleteEdge);
//            System.out.println("DELETING: " + deleteEdge);
        }
    }

    private static TaintNode walkToSource(Graph<TaintNode, TaintEdge> graph, TaintNode start, HashSet<TaintNode> visited) {
        if (visited.contains(start))
            return null;
        visited.add(start);

        Collection<TaintEdge> inEdges = graph.getInEdges(start);
        if (inEdges == null || inEdges.isEmpty())
            return start;

        TaintNode found = null;

        for (TaintEdge inEdge : inEdges) {
            TaintNode check = walkToSource(graph, graph.getSource(inEdge), visited);
            if (check != null) {
                found = check;
                break;
            }
        }

        return found;
    }

    private void addSupplementaryEdges() {
        Graph<TaintNode, TaintEdge> fullGraph = this.getMultiGraph();

        HashMap<String, LinkedList<TaintNode>> groupedByObjectID = new HashMap<String, LinkedList<TaintNode>>();
        for (TaintNode node : fullGraph.getVertices()) {
            if (!node.getClassID().startsWith("java.") && !node.getName().startsWith("com.mysql.jdbc.PreparedStatement:set"))
                continue;
            String key = node.getID();
            if (key == null || key.isEmpty())
                continue;
            
            LinkedList<TaintNode> groupedNodes = groupedByObjectID.get(key);
            if (groupedNodes == null) {
                groupedNodes = new LinkedList<TaintNode>();
                groupedByObjectID.put(key, groupedNodes);
            }
            groupedNodes.add(node);
        }

        for (LinkedList<TaintNode> groupedNodes : groupedByObjectID.values()) {
            for (int i = 0; i < groupedNodes.size(); i++) {
                for (int j = 0; j < groupedNodes.size(); j++) {
                    if (i != j) {
                        TaintNode inNode = groupedNodes.get(i);
                        TaintNode outNode = groupedNodes.get(j);

                        if (inNode.getClassID().contains("ResultSet") || outNode.getClassID().contains("ResultSet"))
                            continue;

                        LinkedList<TaintEdge> inEdges = new LinkedList<TaintEdge>(fullGraph.getInEdges(inNode));
                        LinkedList<TaintEdge> outEdges = new LinkedList<TaintEdge>(fullGraph.getOutEdges(outNode));

                        boolean alreadyConnected = false;
                        boolean nonTerminus = false;
                        for (TaintEdge checkEdge : fullGraph.getOutEdges(inNode)) {
                            TaintNode destNode = fullGraph.getDest(checkEdge);
                            if (destNode == outNode) {
                                alreadyConnected = true;
                                break;
                            }
                            if (checkEdge.getType().equals("CALLING") && !checkEdge.getAdviceType().endsWith("POSTARG")) {
                                nonTerminus = true;
                                break;
                            }
                        }
                        if (alreadyConnected || nonTerminus)
                            continue;

                        for (TaintEdge inEdge : inEdges) {
                            for (TaintEdge outEdge : outEdges) {
                                // MAKE SURE NEITHER IS AN IMPLICATION EDGE
                                if (inEdge.getType().equals("SUPPLEMENTARY") || outEdge.getType().equals("SUPPLEMENTARY"))
                                    continue;
                                if (outEdge.getCounter() > inEdge.getCounter()) {
                                    if (fullGraph.getSource(inEdge) == fullGraph.getSource(outEdge) &&
                                            fullGraph.getDest(inEdge) == fullGraph.getDest(outEdge))
                                        continue;

                                    HashSet<String> inTaintIDs = getPropagatedTaintIDs(inEdge.getAllTaintIDs());
                                    HashSet<String> outTaintIDs = outEdge.getAllTaintIDs();

                                    boolean match = false;
                                    outer:
                                    for (String outTaintID : outTaintIDs) {
                                        for (String inTaintID : inTaintIDs) {
                                            if (taintIDListMatch(inTaintID, outTaintID)) {
                                                match = true;
                                                break outer;
                                            }
                                        }
                                    }

                                    /*
                                     * Probably don't need this block anymore, being handled by previous block with better taintID matching
                                     * and additional tainting of resultset return from PS in tracking code. COMMENTED OUT
                                     */
//                                    if (inNode.getMethodName().startsWith("set") &&
//                                            fullGraph.getOutEdges(inNode).isEmpty() && // change to allow PAT
//                                            outNode.getMethodName().startsWith("executeQuery")) {
//                                        match = true;
//                                    }
                                    
                                    if (match) {
                                        // TODO: mostly does an edge copy, could probably be replaced with an edge clone method
                                        TaintEdge newEdge = new TaintEdge();
                                        newEdge.setType("SUPPLEMENTARY");
                                        newEdge.setRequestCounter(inEdge.getRequestCounter());
                                        newEdge.setRequestURI(inEdge.getRequestURI());
                                        newEdge.setSrcClass(inEdge.getSrcClass());
                                        newEdge.setSrcMethod(inEdge.getSrcMethod());
                                        newEdge.setDestClass(inEdge.getDestClass());
                                        newEdge.setDestMethod(inEdge.getDestMethod());
                                        newEdge.setAdviceType("SUPPLEMENTARY");

                                        for (TaintedObject taintedObject : inEdge.getTaintedObjects())
                                            newEdge.addTaintedObject(taintedObject.copy());
                                        
                                        newEdge.setOutputObject(inEdge.getOutputObject());
                                        newEdge.setCallingObject(inEdge.getCallingObject());
                                        newEdge.setCalledObject(inEdge.getCalledObject());
                                        newEdge.setSourceObject(inEdge.getSourceObject());
                                        newEdge.setDestObject(inEdge.getDestObject());
                                        newEdge.setCallingNode(inNode);
                                        newEdge.setCalledNode(outNode);

                                        newEdge.setOutputContextCounter(inEdge.getInputContextCounter());
                                        newEdge.setInputContextCounter(outEdge.getOutputContextCounter());

                                        newEdge.setIsPostProcessingEdge();
                                        edgeList.add(newEdge);
//                                        System.out.println("SPL EDGE: " + newEdge + " from in: " + inEdge + " out: " + outEdge);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * Given two taintIDs which are actually a more complex list of taintIDs (due to propagation),
     * this returns true if there are any matching taintID components between the two inputs.
     */
    private boolean taintIDListMatch(String taintIDA, String taintIDB) {
        LinkedList<String> aComponents = new LinkedList<String>();
        LinkedList<String> bComponents = new LinkedList<String>();
        if (taintIDA == null || taintIDB == null)
            return false;

        for (String item : taintIDA.split(",")) {
            for (String subitem : item.split(" ")) {
                if (!subitem.startsWith("[")) {
                    aComponents.add(subitem);
                }
            }
        }
        for (String item : taintIDB.split(",")) {
            for (String subitem : item.split(" ")) {
                if (!subitem.startsWith("[")) {
                    bComponents.add(subitem);
                }
            }
        }
        boolean match = false;
        for (String component : aComponents) {
            if (bComponents.contains(component)) {
                match = true;
                break;
            }
        }
        return match;
    }

    private boolean taintIDMultiMatch(HashSet<String> taintListA, HashSet<String> taintListB) {
        for (String taintIDA : taintListA) {
            for (String taintIDB : taintListB) {
                if (taintIDListMatch(taintIDA, taintIDB))
                    return true;
            }
        }

        return false;
    }

    private void setObjectFieldNames() {
        Graph<TaintNode, TaintEdge> fullGraph = this.getMultiGraph();

        HashMap<String, String> idToFieldNameMap = new HashMap<String, String>();

        for (TaintEdge edge : fullGraph.getEdges()) {
            if (edge.getType().equals("FIELDGET")) {
                String objectID = edge.getTaintedObjects().getFirst().getObjectID();
                String fieldName = edge.getFieldName();
                idToFieldNameMap.put(objectID, fieldName);
//                System.out.println("Mapping " + objectID + " to " + fieldName);
            }
        }

        for (TaintNode node : fullGraph.getVertices()) {
            String fieldName = idToFieldNameMap.get(node.getID());
            if (fieldName != null)
                node.setObjectFieldName(fieldName);
        }
    }

    private HashSet<String> getPropagatedTaintIDs(HashSet<String> taintIDs) {
        HashSet<String> output = new HashSet<String>(taintIDs);
        for (String taintID : taintIDs) {
            LinkedList<String> propagations = propagationMap.get(taintID);
            addPropagationHelper(output, propagationMap, propagations, new HashSet<String>());
        }

        return output;
    }

    private void addPropagationHelper(HashSet<String> output, HashMap<String, LinkedList<String>> propagationMap, LinkedList<String> propagations, HashSet<String> visited) {
        if (propagations != null) {
            output.addAll(propagations);
            for (String propagation : propagations) {
                if (visited.contains(propagation))
                    continue;
                visited.add(propagation);
                LinkedList<String> deeperPropagations = propagationMap.get(propagation);
                addPropagationHelper(output, propagationMap, deeperPropagations, visited);
            }
        }
    }

    private void fillEdgeListFromFile(File input) {
        TaintEdge.resetCounter();
        edgeList.clear();
        nodeMap.clear();
        taintIDs.clear();
        taintIDPropagations.clear();
        requestCounters.clear();
        taintIDs.put("", "");
        Runtime runtime = Runtime.getRuntime();

        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(input);

            Element docRoot = doc.getDocumentElement();
            NodeList childNodes = docRoot.getChildNodes();

            HashSet<String> rinLog = new HashSet<String>();

            // Iterate through taintlog records
//            ProgressDialog monitor = new ProgressDialog(AnalysisMainWindow.mainWindow, "Building Graph", childNodes.getLength() * 2);
//            int progCounter = 0;
            int lineCounter = 0;
            outer:
            for (int i = 0, length = childNodes.getLength(); i < length; i++ ) {
//                if (progCounter++ % 10000 == 0)
//                    monitor.setValue(progCounter);
                if (childNodes.item(i) instanceof Element) {
                    lineCounter++;
//                    if (lineCounter % 1000 == 0)
//                        System.out.println("LineCounter: " + lineCounter + " used: " + (runtime.totalMemory() - runtime.freeMemory()) + " remain: " + runtime.freeMemory());
                    TaintEdge taintEdge = new TaintEdge();
                    Element taintLogElem = (Element) childNodes.item(i);
                    taintEdge.setType(taintLogElem.getAttribute("type"));
                    taintEdge.setExecutionTime(taintLogElem.getAttribute("executionTime"));
                    taintEdge.setDebugID(taintLogElem.getAttribute("debugID"));

                    NodeList taintLogChildNodes = taintLogElem.getChildNodes();
                    for (int j = 0; j < taintLogChildNodes.getLength(); j++) {
                        Element taintLogChildElem = (Element) taintLogChildNodes.item(j);
                        if (taintLogChildElem.getNodeName().equals("location")) {
                            taintEdge.setSrcClass(taintLogChildElem.getAttribute("srcClass"));
                            taintEdge.setSrcMethod(taintLogChildElem.getAttribute("srcMethod"));
                            taintEdge.setDestClass(taintLogChildElem.getAttribute("destClass"));
                            taintEdge.setDestMethod(taintLogChildElem.getAttribute("destMethod"));
                            taintEdge.setCallerContextCounter(taintLogChildElem.getAttribute("callerContextCounter"));
                            taintEdge.setCalledContextCounter(taintLogChildElem.getAttribute("calledContextCounter"));
                            taintEdge.setAdviceType(taintLogChildElem.getAttribute("adviceType"));
                            String requestCounter = taintLogChildElem.getAttribute("requestCounter");
                            // Don't bother if it doesn't have a request counter, otherwise causes some problems later
                            if (requestCounter == null || requestCounter.isEmpty())
                                continue outer;
//                            if (taintEdge.getCounter() == 3890) {
//                                System.out.println("CREATING 3890: req counter: " + requestCounter);
//                            }
                            taintEdge.setRequestCounter(requestCounter);
                            taintEdge.setRequestURI(taintLogChildElem.getAttribute("requestURI"));
                            taintEdge.setRequestRemoteAddr(taintLogChildElem.getAttribute("requestRemoteAddr"));
                        }
                        else if (taintLogChildElem.getNodeName().equals("taintedObject")) {
                            TaintedObject taintedObject = new TaintedObject();
                            taintedObject.setObjectID(taintLogChildElem.getAttribute("objectID"));
                            String taintID = taintLogChildElem.getAttribute("taintID");
                            taintedObject.setTaintID(taintID);
                            taintedObject.setType(taintLogChildElem.getAttribute("type"));
                            taintedObject.setValue(taintLogChildElem.getAttribute("value"));
                            taintedObject.setTaintRecord(taintLogChildElem.getAttribute("taintRecord"));
                            taintIDs.put(taintID, taintedObject.getType());

                            NodeList taintLogChildChildNodes = taintLogChildElem.getChildNodes();
                            for (int k = 0; k < taintLogChildChildNodes.getLength(); k++) {
                                Element taintLogChildChildElem = (Element) taintLogChildChildNodes.item(k);
                                if (taintLogChildChildElem.getNodeName().equals("subTaintedObject")) {
                                    TaintedObject subTaintedObject = new TaintedObject();
                                    subTaintedObject.setObjectID(taintLogChildChildElem.getAttribute("objectID"));
                                    String subTaintID= taintLogChildChildElem.getAttribute("taintID");
                                    subTaintedObject.setTaintID(subTaintID);
                                    subTaintedObject.setType(taintLogChildChildElem.getAttribute("type"));
                                    subTaintedObject.setValue(taintLogChildChildElem.getAttribute("value"));
                                    subTaintedObject.setTaintRecord(taintLogChildChildElem.getAttribute("taintRecord"));
                                    taintIDs.put(subTaintID, subTaintedObject.getType());
                                    taintedObject.addTaintedObject(subTaintedObject);
                                }
                            }

                            taintEdge.addTaintedObject(taintedObject);
                        }
                        else if (taintLogChildElem.getNodeName().equals("accessedTaint") && taintEdge.getType().equals("OUTPUTNONTAINT")) {
                            TaintedObject taintedObject = new TaintedObject();
                            taintedObject.setObjectID(taintLogChildElem.getAttribute("objectID"));
                            String taintID = taintLogChildElem.getAttribute("taintID");
                            taintedObject.setTaintID(taintID);
                            taintedObject.setType(taintLogChildElem.getAttribute("type"));
                            taintedObject.setValue(taintLogChildElem.getAttribute("value"));
                            taintedObject.setTaintRecord(taintLogChildElem.getAttribute("taintRecord"));

                            taintEdge.addTaintedObject(taintedObject);
                        }
                        else if (taintLogChildElem.getNodeName().equals("composedObjects")) {
                            NodeList taintLogChildChildNodes = taintLogChildElem.getChildNodes();
                            for (int k = 0; k < taintLogChildChildNodes.getLength(); k++) {
                                Element taintLogChildChildElem = (Element) taintLogChildChildNodes.item(k);
                                if (taintLogChildChildElem.getNodeName().equals("composedObject")) {
                                    TaintedObject composedObject = new TaintedObject();
                                    composedObject.setObjectID(taintLogChildChildElem.getAttribute("objectID"));
                                    String subTaintID= taintLogChildChildElem.getAttribute("taintID");
                                    composedObject.setTaintID(subTaintID);
                                    composedObject.setType(taintLogChildChildElem.getAttribute("type"));
                                    composedObject.setValue(taintLogChildChildElem.getAttribute("value"));
                                    composedObject.setTaintRecord(taintLogChildChildElem.getAttribute("taintRecord"));
                                    taintIDs.put(subTaintID, composedObject.getType());
                                    taintEdge.addComposedObject(composedObject);
                                }
                            }

                        }
                        else if (taintLogChildElem.getNodeName().equals("associatedObjects")) {
                            NodeList taintLogChildChildNodes = taintLogChildElem.getChildNodes();
                            for (int k = 0; k < taintLogChildChildNodes.getLength(); k++) {
                                Element taintLogChildChildElem = (Element) taintLogChildChildNodes.item(k);
                                if (taintLogChildChildElem.getNodeName().equals("composedObject")) {
                                    TaintedObject associatedObject = new TaintedObject();
                                    associatedObject.setObjectID(taintLogChildChildElem.getAttribute("objectID"));
                                    String subTaintID= taintLogChildChildElem.getAttribute("taintID");
                                    associatedObject.setTaintID(subTaintID);
                                    associatedObject.setType(taintLogChildChildElem.getAttribute("type"));
                                    associatedObject.setValue(taintLogChildChildElem.getAttribute("value"));
                                    associatedObject.setTaintRecord(taintLogChildChildElem.getAttribute("taintRecord"));
                                    taintIDs.put(subTaintID, associatedObject.getType());
                                    taintEdge.addTaintedObject(associatedObject);
                                }
                            }

                        }
                        else if (taintLogChildElem.getNodeName().equals("sourceObject")) {
                            TaintedObject taintedObject = new TaintedObject();
                            taintedObject.setObjectID(taintLogChildElem.getAttribute("objectID"));
                            String taintID = taintLogChildElem.getAttribute("taintID");
                            taintedObject.setTaintID(taintID);
                            taintedObject.setType(taintLogChildElem.getAttribute("type"));
                            taintedObject.setValue(taintLogChildElem.getAttribute("value"));
                            taintedObject.setTaintRecord(taintLogChildElem.getAttribute("taintRecord"));
                            taintIDs.put(taintID, taintedObject.getType());

                            taintEdge.setSourceObject(taintedObject);
                        }
                        else if (taintLogChildElem.getNodeName().equals("destObject")) {
                            TaintedObject taintedObject = new TaintedObject();
                            taintedObject.setObjectID(taintLogChildElem.getAttribute("objectID"));
                            String taintID = taintLogChildElem.getAttribute("taintID");
                            taintedObject.setTaintID(taintID);
                            taintedObject.setType(taintLogChildElem.getAttribute("type"));
                            taintedObject.setValue(taintLogChildElem.getAttribute("value"));

//                            if (taintEdge.getType().equals("PROPAGATION"))
//                                System.out.println("destobject taintid: " + taintID + " type: " + taintedObject.getType() + " value: " + taintedObject.getValue() + " line: " + lineCounter);
                            
                            taintedObject.setTaintRecord(taintLogChildElem.getAttribute("taintRecord"));
                            taintIDs.put(taintID, taintedObject.getType());

                            taintEdge.setDestObject(taintedObject);
                        }
                        else if (taintLogChildElem.getNodeName().equals("outputObject")) {
                            TargetObject targetObject = new TargetObject();
                            targetObject.setObjectID(taintLogChildElem.getAttribute("objectID"));
                            targetObject.setType(taintLogChildElem.getAttribute("type"));
                            targetObject.setValue(taintLogChildElem.getAttribute("value"));
                            taintEdge.setOutputObject(targetObject);
                        }
                        else if (taintLogChildElem.getNodeName().equals("callingObject")) {
                            TargetObject targetObject = new TargetObject();
                            targetObject.setObjectID(taintLogChildElem.getAttribute("objectID"));
                            targetObject.setType(taintLogChildElem.getAttribute("type"));
                            targetObject.setValue(taintLogChildElem.getAttribute("value"));
                            taintEdge.setCallingObject(targetObject);
                        }
                        else if (taintLogChildElem.getNodeName().equals("calledObject")) {
                            TargetObject targetObject = new TargetObject();
                            targetObject.setObjectID(taintLogChildElem.getAttribute("objectID"));
                            targetObject.setType(taintLogChildElem.getAttribute("type"));
                            targetObject.setValue(taintLogChildElem.getAttribute("value"));
                            taintEdge.setCalledObject(targetObject);
                        }
                        else if (taintLogChildElem.getNodeName().equals("field")) {
                            TaintedField taintedField = new TaintedField();
                            taintedField.setTargetClass(taintLogChildElem.getAttribute("targetClass"));
                            taintedField.setTargetField(taintLogChildElem.getAttribute("targetField"));
                            taintEdge.setField(taintedField);
                        }
                    }


                    if (taintEdge.getSimpleSource() == null)
                        continue;

                    String srcName = taintEdge.getTargettedSource();
                    String destName = taintEdge.getTargettedDest();
                    String fieldName = taintEdge.getFieldName();

                    if (taintEdge.getType().equals("RETURNINGINPUT")) {
//                        System.out.println("Relabeling " + destName + " to " + taintEdge.getDataSourceDest());
                        destName = taintEdge.getDataSourceDest();

                        // Parse out redundant RINs
                        String firstTaintID = taintEdge.getFirstTaintedObjectTaintID();
                        if (rinLog.contains(firstTaintID))
                            continue;
                        rinLog.add(firstTaintID);
                    }

                    if (!nodeMap.containsKey(srcName))
                        nodeMap.put(srcName, new TaintNode(taintEdge.getSrcClass(), taintEdge.getSrcMethod(), taintEdge.getSourceID()));
                    // Consider adding targetted objects later

                    if (!nodeMap.containsKey(destName)) {
                        if (taintEdge.getType().equals("RETURNINGINPUT"))
                            nodeMap.put(destName, new TaintNode(taintEdge.getDataSourceDest()));
                        else
                            nodeMap.put(destName, new TaintNode(taintEdge.getDestClass(), taintEdge.getDestMethod(), taintEdge.getDestID()));

                    }
                        
                    if (fieldName != null && !nodeMap.containsKey(fieldName)) {
                        nodeMap.put(fieldName, new TaintNode(fieldName));
                    }
                    edgeList.add(taintEdge);
                    if (taintEdge.getDebugID().equals("topicinput")) {
                        System.out.println("TOPIC INPUT FOUND");
                    }

                    if (taintEdge.getAdviceType().startsWith("NONTAINTRETURN"))
                        TaintEdge.decrementCounter();
                    
                    if (taintEdge.getType().equals("PROPAGATION")) {
                        if (taintEdge.getSourceObject() == null || taintEdge.getDestObject() == null) {
                            System.out.println("NULL SRC||DEST: " + lineCounter);
                        }
                        TaintIDPropagationPair pair = new TaintIDPropagationPair(taintEdge.getSourceObject().getTaintID(),
                                taintEdge.getSourceObject().getValue(), taintEdge.getSourceObject().getType(),
                                taintEdge.getDestObject().getTaintID(),
                                taintEdge.getDestObject().getValue(), taintEdge.getDestObject().getType());
                        taintIDPropagations.add(pair);
                    }
                }
            }

            postProcessTaintIDPropagations();

            for (TaintEdge edge : edgeList) {
//                if (progCounter++ % 10000 == 0)
//                    monitor.setValue(progCounter);
                TaintNode callingNode = null;
                TaintNode calledNode = null;

                if (edge.getType().equals("CALLING")) {
                    if (edge.getAdviceType().endsWith("POSTARG")) {
                        callingNode = nodeMap.get(edge.getTargettedDest());
                        calledNode = nodeMap.get(edge.getTargettedSource());
                        edge.setInputContextCounter(edge.getCallerContextCounter());
                        edge.setOutputContextCounter(edge.getCalledContextCounter());
                    } else {
                        callingNode = nodeMap.get(edge.getTargettedSource());
                        calledNode = nodeMap.get(edge.getTargettedDest());
                        edge.setInputContextCounter(edge.getCalledContextCounter());
                        edge.setOutputContextCounter(edge.getCallerContextCounter());
                    }
                }
                else if (edge.getType().equals("OUTPUT"))  {
                    callingNode = nodeMap.get(edge.getTargettedSource());
                    calledNode = nodeMap.get(edge.getTargettedDest());
                    edge.setInputContextCounter(edge.getCalledContextCounter());
                    edge.setOutputContextCounter(edge.getCallerContextCounter());
                }
                else if (edge.getType().equals("RETURNING")) {
                    callingNode = nodeMap.get(edge.getTargettedDest());
                    calledNode = nodeMap.get(edge.getTargettedSource());
                    edge.setInputContextCounter(edge.getCallerContextCounter());
                    edge.setOutputContextCounter(edge.getCalledContextCounter());
                }
                else if (edge.getType().equals("RETURNINGINPUT")) {
                    callingNode = nodeMap.get(edge.getDataSourceDest());
//                    callingNode = nodeMap.get(edge.getTargettedDest());
                    calledNode = nodeMap.get(edge.getTargettedSource());
                    edge.setInputContextCounter(edge.getCallerContextCounter());
                    edge.setOutputContextCounter(edge.getCalledContextCounter());

                    taintIDToInputMap.put(edge.getTaintedObjects().getFirst().getTaintID(), callingNode);
                }
                else if (edge.getType().equals("STATICFIELDSTORE")) {
                    callingNode = nodeMap.get(edge.getTargettedDest());
                    calledNode = nodeMap.get(edge.getFieldName());
                    edge.setInputContextCounter(edge.getCalledContextCounter());
                    edge.setOutputContextCounter(edge.getCalledContextCounter());
                }
                else if (edge.getType().equals("FIELDSET")) {
                    callingNode = nodeMap.get(edge.getTargettedDest());
                    calledNode = nodeMap.get(edge.getFieldName());
                    edge.setInputContextCounter(edge.getCalledContextCounter());
                    edge.setOutputContextCounter(edge.getCalledContextCounter());
                }
                else if (edge.getType().equals("FIELDGET")) {
                    callingNode = nodeMap.get(edge.getFieldName());
                    calledNode = nodeMap.get(edge.getTargettedDest());
//                    System.out.println("FIELDGET EDGE " + edge + " FROM " + callingNode + " to " + calledNode);
                    edge.setInputContextCounter(edge.getCalledContextCounter());
                    edge.setOutputContextCounter(edge.getCalledContextCounter());
                }
                else if (edge.getType().equals("JAVAFIELDSET")) {
                    callingNode = nodeMap.get(edge.getTargettedDest());
                    calledNode = nodeMap.get(edge.getFieldName());
                    edge.setInputContextCounter(edge.getCalledContextCounter());
                    edge.setOutputContextCounter(edge.getCalledContextCounter());
                }
                else if (edge.getType().equals("JAVAFIELDGET")) {
                    callingNode = nodeMap.get(edge.getFieldName());
                    calledNode = nodeMap.get(edge.getTargettedDest());
                    edge.setInputContextCounter(edge.getCalledContextCounter());
                    edge.setOutputContextCounter(edge.getCalledContextCounter());
                }
                else if (edge.getType().equals("FUZZYPROPAGATION")) {
                    callingNode = nodeMap.get(edge.getTargettedDest());
                    calledNode = nodeMap.get(edge.getTargettedDest());
                    //TODO: Not sure about this input context
                    edge.setInputContextCounter(edge.getCalledContextCounter());
                    edge.setOutputContextCounter(edge.getCalledContextCounter());
                }
                else if (edge.getType().equals("COMPOSITION")) {
                    callingNode = nodeMap.get(edge.getTargettedSource());
                    calledNode = nodeMap.get(edge.getTargettedDest());
                    edge.setInputContextCounter(edge.getCalledContextCounter());
                    edge.setOutputContextCounter(edge.getCallerContextCounter());
                    callingNode.addDataMixingEdge(edge);
                }
                else if (edge.getType().equals("ASSOCIATION")) {
                    callingNode = nodeMap.get(edge.getTargettedSource());
                    calledNode = nodeMap.get(edge.getTargettedDest());
                    edge.setInputContextCounter(edge.getCalledContextCounter());
                    edge.setOutputContextCounter(edge.getCallerContextCounter());
                    callingNode.addDataMixingEdge(edge);
                }

                if (callingNode != null && calledNode != null) {
                    if (!edge.getAdviceType().startsWith("NONTAINTRETURN")) {
                        RequestCounterURIPair counterURI = new RequestCounterURIPair(edge.getRequestCounter(), edge.getRequestURI());
                        requestCounters.put(counterURI.toString(), counterURI);
                    }
                    edge.setCallingNode(callingNode);
                    edge.setCalledNode(calledNode);
//                    if (edge.getCallingNode().toString().contains("fillUser") && edge.getCalledNode().toString().contains("fillUser")) {
//                        System.out.println("FOUND fillUser EDGE: " + edge + " from " + edge.getCallingNode() + " to " + edge.getCalledNode());
//                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Used: " + (runtime.totalMemory() - runtime.freeMemory()) + " Remain: " + runtime.freeMemory());

    }

    public class TempCounter {
        public int counter = 0;
    }

    private void markUnusedSubTaint() {
        Runtime runtime = Runtime.getRuntime();
        LinkedList<TaintEdge> orderedEdges = getOrderedEdgeList();

        // Loop over
        // For each edge...
        Graph<TaintNode, TaintEdge> fullGraph = getMultiGraph();
        HashSet<TaintEdge> visited = new HashSet<TaintEdge>();
//        HashSet<TaintedObject> markedObjects = new HashSet<TaintedObject>();


        // If we know a subtaint is not used... when if it occurs in same context later, already marked
        TaintEdge edge = null;
        for (int i = 0; i < orderedEdges.size(); i++) {
            System.out.println("Marking Unused " + i + " of: " + orderedEdges.size());
            edge = orderedEdges.get(i);
            for (TaintedObject taintedObject : edge.getTaintedObjects()) {
//                System.out.println("OVERSIZE " + edge.getTaintedObjects().size());
                if (taintedObject.getSubTaintedObjects() == null)
                    continue;
                int scounter = 0;
                for (TaintedObject subTaintedObject : taintedObject.getSubTaintedObjects()) {
                    // Want to check if the sub tainted object is useless.
                    // Want to scan forward in context
                    // Have a subTaintedObject, with an id, and an edge to start from
                    visited.clear();
                    if (subTaintedObject.isMarked())
                        continue;
//                    System.out.println("SIZE: " + taintedObject.getSubTaintedObjects().size() + " at: " + (scounter++));
                    boolean subFound = forwardContextSearch(visited, edge, fullGraph, true, subTaintedObject.getTaintID(), new TempCounter());

                    // subFound also if enclosing object is saved somewhere, like in a static or in a setAttribute
                    if (!subFound) {
//                        System.out.println("SET UNUSED ON " + subTaintedObject.getType());
//                        if (edge.getCalledNode().toString().contains("get Object:18147857") &&
//                                edge.getCallingNode().toString().contains("put Object Object:18147857") &&
//                                edge.getType().contains("SUPP"))
//                            System.out.println("PRIMARY UNUSED SUPP: " + subTaintedObject.getValue());
//                        if (edge.getCounter() == 807)
//                            System.out.println("UNUSED TAINT IN 807: " + subTaintedObject.getValue());
//                        if (edge.getCounter() == 568)
//                            System.out.println("UNUSED TAINT IN 568: " + subTaintedObject.getValue());
                        
                        subTaintedObject.setUnused();
                        subTaintedObject.setMarked();
                        // This subTaintedObject is not used (basically, its taint was not found.)
                        // Scan ahead in graph, whereever subtaint of same id is found, mark it in list
//                        forwardContextQuickMark(visited, edge, fullGraph, true, subTaintedObject.getTaintID());
                    }
                }
            }
        }
    }

    private boolean forwardContextSearch(HashSet<TaintEdge> visited, TaintEdge current, Graph<TaintNode, TaintEdge> graph, boolean isForward, String targetTaintID, TempCounter tcounter) {
        // Looking for taint in high level object
        if (visited.contains(current))
            return false;
        visited.add(current);

        tcounter.counter++;
        if ((tcounter.counter % 1000) == 0) {
            System.out.println("FCS: " + tcounter.counter);
        }

//        if (targetTaintID.contains("28412859:forumtitle:73")) {
//            System.out.println("TSCAN: " + current);
//
//        }

        for (TaintedObject taintedObject : current.getTaintedObjects()) {
            if (taintedObject.getTaintID() != null && taintedObject.getTaintID().equals(targetTaintID))
                return true;
            for (TaintedObject subTaintedObject : taintedObject.getSubTaintedObjects()) {
                if (subTaintedObject.getTaintID() != null && subTaintedObject.getTaintID().equals(targetTaintID)) {
                    if (subTaintedObject.isMarked()) {
                        if (subTaintedObject.isUnused())
                            return false;
                        else
                            return true;
                    }
                }
            }
        }

        // Check last edge. If it didn't return true, we want to see if this one has any potential. Like maybe

        TaintNode nextNode = null;
        String context = null;
        if (isForward) {
            nextNode = graph.getDest(current);
            context = current.getInputContextCounter();
        }
        else {
            nextNode = graph.getSource(current);
            context = current.getOutputContextCounter();
        }

        boolean found = false;
        for (TaintEdge nextEdge : graph.getOutEdges(nextNode)) {
            if (nextEdge.getOutputContextCounter() == null)
                System.out.println("A FAIL");
            if (nextEdge.getRequestCounter() == null) {
                System.out.println("B FAIL on edge " + nextEdge);
            }
            if ((nextEdge.getOutputContextCounter().equals(context) ||
                    (nextEdge.getType().equals("FIELDGET") && nextEdge.getRequestCounter().equals(current.getRequestCounter())))
                    && (nextEdge.getCounter() > current.getCounter() ||
                    (current.getType().equals("SUPPLEMENTARY") && !nextEdge.getType().equals("SUPPLEMENTARY")))) {
                if (forwardContextSearch(visited, nextEdge, graph, true, targetTaintID, tcounter)) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            for (TaintEdge nextEdge : graph.getInEdges(nextNode)) {
                if (nextEdge.getInputContextCounter().equals(context) &&
                        (nextEdge.getCounter() > current.getCounter() || current.getType().equals("SUPPLEMENTARY") && !nextEdge.getType().equals("SUPPLEMENTARY"))) {
                    if (forwardContextSearch(visited, nextEdge, graph, false, targetTaintID, tcounter)) {
                        found = true;
                        break;
                    }
                }
            }
        }

        if (!found) {
            // Did not find target, should mark all subobjects with target as false;
            for (TaintedObject taintedObject : current.getTaintedObjects()) {
                for (TaintedObject subTaintedObject : taintedObject.getSubTaintedObjects()) {
                    if (!subTaintedObject.isMarked() && subTaintedObject.getTaintID().equals(targetTaintID)) {
                        subTaintedObject.setUnused();
                        subTaintedObject.setMarked();
                    }
                }
            }
        }
        else {
            for (TaintedObject taintedObject : current.getTaintedObjects()) {
                for (TaintedObject subTaintedObject : taintedObject.getSubTaintedObjects()) {
                    if (!subTaintedObject.isMarked() && subTaintedObject.getTaintID().equals(targetTaintID)) {
                        subTaintedObject.setMarked();
                    }
                }
            }
        }

        return found;
    }

    public GraphBuilder getForwardContextGraphBuilder(TaintEdge startEdge) {
        HashSet<TaintEdge> foundEdges = new HashSet<TaintEdge>();
        getForwardContextEdges(new HashSet<TaintEdge>(), startEdge, foundEdges, getMultiGraph(), true);

        return GraphBuilder.getBuilderFromEdges(this, foundEdges);
    }

    private void getForwardContextEdges(HashSet<TaintEdge> visited, TaintEdge current, HashSet<TaintEdge> found, Graph<TaintNode, TaintEdge> graph, boolean isForward) {
        if (visited.contains(current))
            return;
        visited.add(current);

        found.add(current);

        TaintNode nextNode = null;
        String context = null;
        if (isForward) {
            nextNode = graph.getDest(current);
            context = current.getInputContextCounter();
        }
        else {
            nextNode = graph.getSource(current);
            context = current.getOutputContextCounter();
        }

        for (TaintEdge nextEdge : graph.getOutEdges(nextNode)) {
            if ((nextEdge.getOutputContextCounter().equals(context) || (nextEdge.getType().equals("FIELDGET") && nextEdge.getRequestCounter().equals(current.getRequestCounter()))) && (nextEdge.getCounter() > current.getCounter() || (current.getType().equals("SUPPLEMENTARY") && !nextEdge.getType().equals("SUPPLEMENTARY")))) {
                getForwardContextEdges(visited, nextEdge, found, graph, true);
            }
        }
        for (TaintEdge nextEdge : graph.getInEdges(nextNode)) {
            if (nextEdge.getInputContextCounter().equals(context) && (nextEdge.getCounter() > current.getCounter() || current.getType().equals("SUPPLEMENTARY") && !nextEdge.getType().equals("SUPPLEMENTARY"))) {
                getForwardContextEdges(visited, nextEdge, found, graph, false);
            }
        }
    }

    public GraphBuilder getForwardTaintContextGraphBuilder(TaintEdge startEdge) {
        HashSet<TaintEdge> foundEdges = new HashSet<TaintEdge>();
        getForwardTaintContextEdges(new HashSet<TaintEdge>(), startEdge, foundEdges, getMultiGraph(), true);

        return GraphBuilder.getBuilderFromEdges(this, foundEdges);
    }

    public HashSet<TaintNode> getForwardTaintContextNodes(TaintEdge startEdge) {
        HashSet<TaintEdge> foundEdges = new HashSet<TaintEdge>();
        getForwardTaintContextEdges(new HashSet<TaintEdge>(), startEdge, foundEdges, getMultiGraph(), true);

        HashSet<TaintNode> foundNodes = new HashSet<TaintNode>();

        for (TaintEdge edge : foundEdges) {
            foundNodes.add(edge.getCalledNode());
            foundNodes.add(edge.getCallingNode());
        }

        return foundNodes;
    }

    private void getForwardTaintContextEdges(HashSet<TaintEdge> visited, TaintEdge current, HashSet<TaintEdge> found, Graph<TaintNode, TaintEdge> graph, boolean isForward) {
        if (visited.contains(current))
            return;
        visited.add(current);

        found.add(current);

        TaintNode nextNode = null;
        String context = null;
        if (isForward) {
            nextNode = graph.getDest(current);
            context = current.getInputContextCounter();
        }
        else {
            nextNode = graph.getSource(current);
            context = current.getOutputContextCounter();
        }

        HashSet<String> currentTaintIDPropagations = getPropagatedTaintIDs(current.getAllTaintIDs());

        for (TaintEdge nextEdge : graph.getOutEdges(nextNode)) {
            if ((nextEdge.getOutputContextCounter().equals(context) || (nextEdge.getType().equals("FIELDGET") && nextEdge.getRequestCounter().equals(current.getRequestCounter()))) && (nextEdge.getCounter() > current.getCounter() || (current.getType().equals("SUPPLEMENTARY") && !nextEdge.getType().equals("SUPPLEMENTARY")))) {
                HashSet<String> nextEdgeTaintIDs = nextEdge.getAllTaintIDs();
                if (taintIDMultiMatch(currentTaintIDPropagations, nextEdgeTaintIDs)) {
                    getForwardTaintContextEdges(visited, nextEdge, found, graph, true);
                }
            }
        }
        for (TaintEdge nextEdge : graph.getInEdges(nextNode)) {
            if (nextEdge.getInputContextCounter().equals(context) && (nextEdge.getCounter() > current.getCounter() || current.getType().equals("SUPPLEMENTARY") && !nextEdge.getType().equals("SUPPLEMENTARY"))) {
                HashSet<String> nextEdgeTaintIDs = nextEdge.getAllTaintIDs();
                if (taintIDMultiMatch(currentTaintIDPropagations, nextEdgeTaintIDs)) {
                    getForwardTaintContextEdges(visited, nextEdge, found, graph, false);
                }
            }
        }
    }

    private void getForwardTaintOriginContextEdges(HashSet<TaintEdge> visited, TaintEdge current, HashSet<TaintEdge> found, Graph<TaintNode, TaintEdge> graph, boolean isForward) {
        if (visited.contains(current))
            return;
        visited.add(current);

        found.add(current);

        TaintNode nextNode = null;
        String context = null;
        if (isForward) {
            nextNode = graph.getDest(current);
            context = current.getInputContextCounter();
        }
        else {
            nextNode = graph.getSource(current);
            context = current.getOutputContextCounter();
        }

        HashSet<String> currentTaintIDPropagations = getPropagatedTaintIDs(current.getAllTaintIDs());

        for (TaintEdge nextEdge : graph.getOutEdges(nextNode)) {
            if ((nextEdge.getOutputContextCounter().equals(context) || (nextEdge.getType().equals("FIELDGET") && nextEdge.getRequestCounter().equals(current.getRequestCounter()))) && (nextEdge.getCounter() > current.getCounter() || (current.getType().equals("SUPPLEMENTARY") && !nextEdge.getType().equals("SUPPLEMENTARY")))) {
                HashSet<String> nextEdgeTaintIDs = nextEdge.getAllTaintIDs();
                if (taintIDMultiMatch(currentTaintIDPropagations, nextEdgeTaintIDs)) {
                    getForwardTaintOriginContextEdges(visited, nextEdge, found, graph, true);
                }
            }
        }
        for (TaintEdge nextEdge : graph.getInEdges(nextNode)) {
            if (nextEdge.getInputContextCounter().equals(context) && (nextEdge.getCounter() > current.getCounter() || current.getType().equals("SUPPLEMENTARY") && !nextEdge.getType().equals("SUPPLEMENTARY"))) {
                HashSet<String> nextEdgeTaintIDs = nextEdge.getAllTaintIDs();
                if (taintIDMultiMatch(currentTaintIDPropagations, nextEdgeTaintIDs)) {
                    getForwardTaintOriginContextEdges(visited, nextEdge, found, graph, false);
                }
            }
        }
    }

    public String getFullOutput() {
        LinkedList<TaintEdge> orderedEdges = getOrderedEdgeList();
        String ret = "";

        String currentReq = null;
        for (TaintEdge edge : orderedEdges) {
            if (currentReq == null || !edge.getRequestName().equals(currentReq)) {
                currentReq = edge.getRequestName();
                ret += "REQ: " + currentReq + "\n";
            }
            if (edge.getType().equals("OUTPUT")) {
                for (TaintedObject taintedObject : edge.getTaintedObjects()) {
                    ret += taintedObject.getValue() + " [TAINTED: " + taintedObject.getTaintID() + " - " + taintedObject.getTaintRecord() + " - " + edge.getCalledNode() + "]\n";
                }
            } else if (edge.getType().equals("OUTPUTNONTAINT")) {
                ret += edge.getOutputObject().getValue() + " [NONTAINTED]\n";
                for (TaintedObject taintedObject : edge.getAccessedTaint()) {
                    ret += "\t" + taintedObject.getTaintRecord() + "\n";
                }
            }
            
        }

        return ret;
    }

    public HashSet<TaintEdge> getEdgeList() {
        return this.edgeList;
    }

    public LinkedList<TaintEdge> getOrderedEdgeList() {
        LinkedList<TaintEdge> output = new LinkedList<TaintEdge>(this.edgeList);
        Collections.sort(output);
        return output;
    }

    public LinkedList<TaintEdge> getPropagationEdges() {
        LinkedList<TaintEdge> propEdges = new LinkedList<TaintEdge>();

        for (TaintEdge edge : edgeList) {
            if (edge.getType().equals("PROPAGATION"))
                propEdges.add(edge);
        }

        return propEdges;
    }

    public HashSet<TaintNode> getInputNodesFromTaintIDs(HashSet<String> taintIDs) {
        HashSet<TaintNode> output = new HashSet<TaintNode>();

        for (String taintID : taintIDs) {
            output.add(this.taintIDToInputMap.get(taintID));
        }

        return output;
    }

    public HashSet<TaintNode> getInputNodes() {
        HashSet<TaintNode> output = new HashSet<TaintNode>();

        output.addAll(taintIDToInputMap.values());

        return output;
    }

    public HashMap<String, RequestCounterURIPair> getRequestCounters() {
        return requestCounters;
    }

    public LinkedHashSet<String> getTaintIDs() {
        return new LinkedHashSet<String>(taintIDs.keySet());
    }

    public LinkedHashMap<String, String> getTaintIDsWithTypes() {
        return taintIDs;
    }

    public LinkedList<TaintIDPropagationPair> getTaintIDPropagations() {
        return taintIDPropagations;
    }

    public void colorNode(TaintNode node, int color) {
        nodeColors.put(node, color);
    }

    public void colorEdge(TaintEdge edge, int color) {
        edgeColors.put(edge, color);
    }

    public Integer getNodeColor(TaintNode node) {
        return nodeColors.get(node);
    }

    public void resetNodeColors() {
        nodeColors.clear();
    }

    public void colorPathBetween(Graph<TaintNode, TaintEdge> graph, TaintNode start, TaintNode end) {
        System.out.println("COLOR BETWEEN: " + start + " and " + end);
        HashSet<TaintNode> path = colorPathBetweenHelper(new HashSet<TaintNode>(), graph, start, end);
        for (TaintNode pathNode : path) {
            System.out.println("Coloring Node " + pathNode);
            colorNode(pathNode, 10);
        }
    }

    public HashSet<TaintNode> getPathBetween(Graph<TaintNode, TaintEdge> graph, TaintNode start, TaintNode end) {
        HashSet<TaintNode> path = colorPathBetweenHelper(new HashSet<TaintNode>(), graph, start, end);
        return path;
    }

    public HashSet<TaintNode> colorPathBetweenHelper(HashSet<TaintNode> visited, Graph<TaintNode, TaintEdge> graph, TaintNode current, TaintNode target) {
        /*
         * recursively expand, looking for target, when you find the target, return found of nothing, unify
         */
        HashSet<TaintNode> ret = new HashSet<TaintNode>();
        if (current == target) {
            ret.add(current);
            System.out.println("TARGET FOUND");
            return ret;
        }
        
        if (visited.contains(current))
            return new HashSet<TaintNode>();
        visited.add(current);

        

        int presize = ret.size();
        for (TaintEdge edge : graph.getOutEdges(current)) {
            TaintNode nextNode = graph.getOpposite(current, edge);
            
            ret.addAll(colorPathBetweenHelper(visited, graph, nextNode, target));
        }

        if (ret.size() > presize)
            ret.add(current);
        return ret;
    }

    public Graph<TaintNode, TaintEdge> getGraph() {
        if (unfilteredGraph != null)
            return unfilteredGraph;

        unfilteredGraph = getGraph(null);
        return unfilteredGraph;
    }

    public Graph<TaintNode, TaintEdge> getMultiGraph() {
//        if (unfilteredMultiGraph != null)
//            return unfilteredMultiGraph;

//        unfilteredMultiGraph = ;
        return getMultiGraph(new LinkedList<EdgeFilter>());
    }

    public Graph<TaintNode, TaintEdge> getLightMultiGraph() {
        if (unfilteredLightMultiGraph != null)
            return unfilteredLightMultiGraph;
        LinkedList<EdgeFilter> filters = new LinkedList<EdgeFilter>();
        
        filters.add(new FilterByIsUniqueEdge());
        unfilteredLightMultiGraph = getMultiGraph(filters);
        return unfilteredLightMultiGraph;
    }

    public Graph<TaintNode, TaintEdge> getLightMultiGraph(LinkedList<EdgeFilter> filters) {
        if (filters == null)
            filters = new LinkedList<EdgeFilter>();
        filters.add(new FilterByIsUniqueEdge());
        return getMultiGraph(filters);
    }

    public Graph<TaintNode, TaintEdge> getGraph(LinkedList<EdgeFilter> filters) {
        Graph<TaintNode, TaintEdge> g = new DirectedSparseGraph<TaintNode, TaintEdge>();
        loadGraph(g, filters);

        return g;
    }

    public Graph<TaintNode, TaintEdge> getMultiGraph(LinkedList<EdgeFilter> filters) {
        filters.add(new FilterByRINRETExclude());
        
        Graph<TaintNode, TaintEdge> g = new DirectedSparseMultigraph<TaintNode, TaintEdge>();
//        System.out.println("CALLING LOADGRAPH");
        loadGraph(g, filters);
        return g;
    }

    private void generateTaintIDPropagationsAndRequestCounters() {
        LinkedList<TaintEdge> sortedEdgeList = new LinkedList<TaintEdge>(edgeList);
        Collections.sort(sortedEdgeList);
        for (TaintEdge edge : sortedEdgeList) {
            if (edge.getType().equals("PROPAGATION")) {
                taintIDPropagations.add(new TaintIDPropagationPair(edge.getSourceObject().getTaintID(),
                        edge.getSourceObject().getValue(), edge.getSourceObject().getType(),
                        edge.getDestObject().getTaintID(),
                        edge.getDestObject().getValue(), edge.getDestObject().getType()));
            }
            taintIDs.putAll(edge.getAllTaintIDsWithTypes());

            if (edge.getCallingNode() != null && edge.getCalledNode() != null) {
                if (!edge.getAdviceType().startsWith("NONTAINTRETURN")) {
                    RequestCounterURIPair counterURI = new RequestCounterURIPair(edge.getRequestCounter(), edge.getRequestURI());
                    requestCounters.put(counterURI.toString(), counterURI);
                }
            }
        }
    }

    // Run a propagation list through this to fix up problems when multiple taint id sources are combined
    private void postProcessTaintIDPropagations() {
        HashMap<String, LinkedList<TaintIDPropagationPair>> proppedTo = new HashMap<String, LinkedList<TaintIDPropagationPair>>();

        LinkedList<TaintIDPropagationPair> toAdd = new LinkedList<TaintIDPropagationPair>();
        for (TaintIDPropagationPair pair : taintIDPropagations) {
//            System.out.println("pair dest id: " + pair.getDestID() + " pair dest type: " + pair.getDestType());
            if (pair.getDestID() != null) {
                String destObjectId = pair.getDestID().substring(pair.getDestID().lastIndexOf('[') + 1, pair.getDestID().lastIndexOf(']'));
    //            System.out.println("DESTOBJECTID: " + destObjectId);
                LinkedList<TaintIDPropagationPair> mappedPairs = proppedTo.get(destObjectId);
                if (mappedPairs == null) {
                    mappedPairs = new LinkedList<TaintIDPropagationPair>();
                    proppedTo.put(destObjectId, mappedPairs);
                }
                else {
                    for (TaintIDPropagationPair mappedPair : mappedPairs) {
                        if (mappedPair.getDestID().equals(pair.getDestID()) && mappedPair.getDestValue().equals(pair.getDestValue()))
                            continue;
                        TaintIDPropagationPair newPair = new TaintIDPropagationPair(mappedPair.getDestID(), mappedPair.getDestValue(), mappedPair.getDestType(),
                                                                                    pair.getDestID(), pair.getDestValue(), pair.getDestType());
    //                    System.out.println("ADDING NEW PAIR: " + newPair);
                        newPair.setIsPostProcessed();
                        toAdd.add(newPair);
                    }
                }
                mappedPairs.add(pair);
            }
        }

        taintIDPropagations.addAll(toAdd);
    }

    private void loadGraph(Graph<TaintNode, TaintEdge> g, LinkedList<EdgeFilter> filters) {
        // Want this filter in all graphs for now
        int skipCount = 0;

        LinkedList<TaintEdge> sortedEdgeList = this.getOrderedEdgeList();
        for (TaintEdge edge : sortedEdgeList) {
            if (edge.getAdviceType().equals("NONTAINTRETURN") || edge.getType().equals("COMPOSITION") ||
                    edge.getType().equals("ASSOCIATION")) {
                skipCount++;
                continue;
            }

            TaintNode callingNode = edge.getCallingNode();
            TaintNode calledNode = edge.getCalledNode();

            Integer callingColor = nodeColors.get(callingNode);
            Integer calledColor = nodeColors.get(calledNode);
            Integer edgeColor = edgeColors.get(edge);

            if (callingColor != null)
                callingNode.colorValue = callingColor;
            else if (callingNode != null)
                callingNode.colorValue = 0;
            if (calledColor != null)
                calledNode.colorValue = calledColor;
            else if (calledNode != null)
                calledNode.colorValue = 0;
            if (edgeColor != null)
                edge.colorValue = edgeColor;
            else
                edge.colorValue = 4;

            if (callingNode != null && calledNode != null) {
                if (filters != null) {

                    boolean pass = true;
                    for (EdgeFilter filter : filters) {
                        if (!filter.pass(edge)) {
//                            if (edge.getCounter() == 2549) {
//                                System.out.println("PRELOADING 2549: " + edge + " from " + filter);
//                            }
//                            if (edge.getCounter() == 14)
//                                System.out.println("14 Rejected on " + filter);
                            pass = false;
                            if (edge.getDebugID() != null && !edge.getDebugID().isEmpty()) {
                                System.out.println("FILTERING OUT DEBUG EDGE " + filter + " " + edge.toDebugString());
                            }
                            break;
                        }
                    }

                    if (pass) {
//                        if (edge.getCounter() == 18)
//                            System.out.println("Adding edge 18");
//                        if (edge.getCounter() == 14)
//                            System.out.println("Adding edge 14");
                        if (edge.getDebugID() != null && !edge.getDebugID().isEmpty()) {
                            System.out.println("ADDING DEBUG EDGE " + edge.toDebugString());
                        }
                        g.addEdge(edge, callingNode, calledNode);
                    }
                }
                else {
                    if (edge.getDebugID() != null && !edge.getDebugID().isEmpty()) {
                        System.out.println("ADDING DEBUG EDGE " + edge.toDebugString());
                    }
                    g.addEdge(edge, callingNode, calledNode);
                }
            }
        }
    }

    public void colorInputs() {
        for (TaintEdge edge : this.edgeList) {
            if (edge.getType().equals("RETURNINGINPUT")) {
                colorNode(edge.getCallingNode(), 7);
            }
        }
    }

    public void colorOutputs() {
        for (TaintEdge edge : this.edgeList) {
            if (edge.getType().equals("OUTPUT")) {
                colorNode(edge.getCalledNode(), 8);
            }
        }
    }

    public static Graph<TaintNode, TaintEdge> copyGraph(Graph<TaintNode, TaintEdge> fullInput) {
        Graph<TaintNode, TaintEdge> derivedGraph = null;
        try {
            derivedGraph = fullInput.getClass().newInstance();

            for (TaintNode node : fullInput.getVertices()) {
                derivedGraph.addVertex(node);
            }
            for (TaintEdge edge : fullInput.getEdges()) {
                derivedGraph.addEdge(edge, fullInput.getSource(edge), fullInput.getDest(edge));
            }
        } catch (InstantiationException ex) {
            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }

        return derivedGraph;
    }

    public static GraphBuilder copyGraphBuilder(GraphBuilder input) {
        GraphBuilder output = new GraphBuilder(input);

        output.generateTaintIDPropagationsAndRequestCounters();
        output.postProcessTaintIDPropagations();

        return output;
    }

    public static GraphBuilder getBuilderFromEdges(GraphBuilder input, HashSet<TaintEdge> sourceEdges) {
        GraphBuilder output = new GraphBuilder(input, true);

        output.getEdgeList().addAll(sourceEdges);

        for (TaintEdge edge : output.getEdgeList()) {
            output.taintIDs.putAll(edge.getAllTaintIDsWithTypes());

            if (edge.getCallingNode() != null && edge.getCalledNode() != null) {
                if (!edge.getAdviceType().startsWith("NONTAINTRETURN")) {
                    RequestCounterURIPair counterURI = new RequestCounterURIPair(edge.getRequestCounter(), edge.getRequestURI());
                    output.requestCounters.put(counterURI.toString(), counterURI);
                }
            }
        }

        LinkedList<TaintEdge> sortedEdges = new LinkedList<TaintEdge>(input.getPropagationEdges());
        Collections.sort(sortedEdges);
        for (TaintEdge edge : sortedEdges) {
            // No propagation edges moved over. No constructor copies full propagation list. It's added from edges. Propagation
            // edges are never filtered out by taintRecord.
            if (output.taintIDs.containsKey(edge.getSourceObject().getTaintID()) &&
                    output.taintIDs.containsKey(edge.getDestObject().getTaintID())) {
                output.taintIDPropagations.add(new TaintIDPropagationPair(edge.getSourceObject().getTaintID(),
                        edge.getSourceObject().getValue(), edge.getSourceObject().getType(),
                        edge.getDestObject().getTaintID(),
                        edge.getDestObject().getValue(), edge.getDestObject().getType()));
            }
        }
        output.postProcessTaintIDPropagations();

        return output;
    }

    public static GraphBuilder pruneToOnlyStableGraphBuilder(GraphBuilder input, DataSourceInfoBuilder dsib) {
        GraphBuilder output = new GraphBuilder(input);

        Graph<TaintNode, TaintEdge> inputGraph = input.getMultiGraph();

        LinkedList<TaintEdge> edges = new LinkedList<TaintEdge>(output.edgeList);
        for (TaintEdge edge : edges) {
            if (inputGraph.getSource(edge) != null && inputGraph.getSource(edge).getName().startsWith("java.sql.PreparedStatement:executeQuery"))
                continue;
            for (String record : edge.getAllTaintRecords()) {
                if (dsib.checkTaintRecordMatchesVariability(record, "PREDICTABLE") ||
                        dsib.checkTaintRecordMatchesVariability(record, "RANDOM")) {
                    output.edgeList.remove(edge);

                    // This now removes all edges on the same node. Instead, should only remove in matching context.
                    Collection<TaintEdge> calledInEdges = inputGraph.getInEdges(edge.getCalledNode());
                    if (calledInEdges != null) {
                        for (TaintEdge removeEdge : calledInEdges) {
                            if (edge.getInputContextCounter().equals(removeEdge.getInputContextCounter())) {
                                output.edgeList.remove(removeEdge);
                            }
                        }
                    }
                    Collection<TaintEdge> calledOutEdges = inputGraph.getOutEdges(edge.getCalledNode());
                    if (calledOutEdges != null) {
                        for (TaintEdge removeEdge : calledOutEdges) {
                            if (edge.getInputContextCounter().equals(removeEdge.getOutputContextCounter())) {
                                output.edgeList.remove(removeEdge);
                            }
                        }
                    }
                    Collection<TaintEdge> callingInEdges = inputGraph.getInEdges(edge.getCallingNode());
                    if (callingInEdges != null) {
                        for (TaintEdge removeEdge : callingInEdges) {
                            if (edge.getOutputContextCounter().equals(removeEdge.getInputContextCounter())) {
                                output.edgeList.remove(removeEdge);
                            }
                        }
                    }
                    Collection<TaintEdge> callingOutEdges = inputGraph.getOutEdges(edge.getCallingNode());
                    if (callingOutEdges != null) {
                        for (TaintEdge removeEdge : callingOutEdges) {
                            if (edge.getOutputContextCounter().equals(removeEdge.getOutputContextCounter())) {
                                output.edgeList.remove(removeEdge);
                            }
                        }
                    }
                }
            }
        }

        output.generateTaintIDPropagationsAndRequestCounters();
        output.postProcessTaintIDPropagations();

        return output;
    }

    public static GraphBuilder pruneToPredictableGraphBuilder(GraphBuilder input, DataSourceInfoBuilder dsib, AnalysisMainWindow window) {
        GraphBuilder output = new GraphBuilder(input);

        Graph<TaintNode, TaintEdge> inputGraph = input.getMultiGraph();

        GraphBuilder onlyStable = GraphBuilder.pruneToOnlyStableGraphBuilder(input, dsib);

        LinkedList<TaintEdge> edges = new LinkedList<TaintEdge>(output.edgeList);
        for (TaintEdge edge : edges) {
            // Check top-level taint
            // Check the rest for mixed
            boolean remove = false;

            // Total hack:
            if ((edge.getCallingNode() != null && (edge.getCallingNode().toString().contains("fillThreadList") || edge.getCallingNode().toString().contains("setLastMessage"))) ||
                    (edge.getCalledNode() != null && (edge.getCalledNode().toString().contains("fillThreadList") || edge.getCalledNode().toString().contains("setLastMessage"))))
                continue;

            for (TaintedObject taintedObject : edge.getTaintedObjects()) {
                String record = taintedObject.getTaintRecord();
                if (record != null && !dsib.checkTaintRecordMatchesVariability(record, "PREDICTABLE") &&
                        !dsib.checkTaintRecordMatchesVariability(record, "STABLE")) {
                    remove = true;
                    break;
                }
            }


            for (TaintedObject taintedObject : edge.getTaintedObjectsFlattened()) {
                String record = taintedObject.getTaintRecord();
                if (record != null && !taintedObject.isUnused()) {
                    if ((!dsib.checkTaintRecordMatchesVariability(record, "PREDICTABLE") &&
                            !dsib.checkTaintRecordMatchesVariability(record, "STABLE")) ||
                            dsib.checkTaintRecordMatchesVariability(record, "RANDOM")) {
                        remove = true;
                        break;
                    }
                }
            }

            if (remove) {
                Collection<TaintEdge> calledInEdges = inputGraph.getInEdges(edge.getCalledNode());
                if (calledInEdges != null) {
                    for (TaintEdge removeEdge : calledInEdges) {
                        if (edge.getInputContextCounter().equals(removeEdge.getInputContextCounter())) {
                            output.edgeList.remove(removeEdge);
                        }
                    }
                }
                Collection<TaintEdge> calledOutEdges = inputGraph.getOutEdges(edge.getCalledNode());
                if (calledOutEdges != null) {
                    for (TaintEdge removeEdge : calledOutEdges) {
                        if (edge.getInputContextCounter().equals(removeEdge.getOutputContextCounter())) {
                            output.edgeList.remove(removeEdge);
                        }
                    }
                }
                Collection<TaintEdge> callingInEdges = inputGraph.getInEdges(edge.getCallingNode());
                if (callingInEdges != null) {
                    for (TaintEdge removeEdge : callingInEdges) {
                        if (edge.getOutputContextCounter().equals(removeEdge.getInputContextCounter())) {
                            output.edgeList.remove(removeEdge);
                        }
                    }
                }
                Collection<TaintEdge> callingOutEdges = inputGraph.getOutEdges(edge.getCallingNode());
                if (callingOutEdges != null) {
                    for (TaintEdge removeEdge : callingOutEdges) {
                        if (edge.getOutputContextCounter().equals(removeEdge.getOutputContextCounter())) {
                            output.edgeList.remove(removeEdge);
                        }
                    }
                }
            }
        }


//        window.addAnalysisGraphBuilder(output, "TESTPRE A", "Done");
        // Changed from using inputGraph to remove edges (getIncidentEdges called on input instead of stable), to using stable
//        Graph<TaintNode, TaintEdge> stableGraph = onlyStable.getMultiGraph();
//        for (TaintNode node : stableGraph.getVertices()) {
//            for (TaintEdge removeEdge : stableGraph.getIncidentEdges(node)) {
//                output.edgeList.remove(removeEdge);
//            }
//        }

        output.generateTaintIDPropagationsAndRequestCounters();
        output.postProcessTaintIDPropagations();

        return output;
    }

//    public static Graph<TaintNode, TaintEdge> pruneToNotRandomGraph(Graph<TaintNode, TaintEdge> fullInput, DataSourceInfoBuilder dsib) {
//        Graph<TaintNode, TaintEdge> outputGraph = GraphBuilder.copyGraph(fullInput);
//
//        LinkedList<TaintEdge> edges = new LinkedList<TaintEdge>(outputGraph.getEdges());
//        for (TaintEdge edge : edges) {
//            for (String record : edge.getAllTaintRecords()) {
//                if (dsib.checkTaintRecordMatchesVariability(record, "RANDOM")) {
//                    TaintNode source = outputGraph.getSource(edge);
//                    TaintNode dest = outputGraph.getDest(edge);
//                    outputGraph.removeEdge(edge);
//                    outputGraph.removeVertex(source);
//                    outputGraph.removeVertex(dest);
//                }
//            }
//        }
//
//        removeOrphanNodes(outputGraph);
//        return outputGraph;
//    }

//    public static Graph<TaintNode, TaintEdge> pruneToOnlyRandomGraph(Graph<TaintNode, TaintEdge> fullInput, DataSourceInfoBuilder dsib) {
//        Graph<TaintNode, TaintEdge> outputGraph = GraphBuilder.copyGraph(fullInput);
//
//        LinkedList<TaintEdge> edges = new LinkedList<TaintEdge>(outputGraph.getEdges());
//        for (TaintEdge edge : edges) {
//            for (String record : edge.getAllTaintRecords()) {
//                if (!dsib.checkTaintRecordMatchesVariability(record, "RANDOM")) {
//                    TaintNode source = outputGraph.getSource(edge);
//                    TaintNode dest = outputGraph.getDest(edge);
//                    outputGraph.removeEdge(edge);
//                    outputGraph.removeVertex(source);
//                    outputGraph.removeVertex(dest);
//                }
//            }
//        }
//
//        removeOrphanNodes(outputGraph);
//        return outputGraph;
//    }

    public static GraphBuilder pruneToRandomGraphBuilder(GraphBuilder input, DataSourceInfoBuilder dsib) {
        GraphBuilder output = new GraphBuilder(input);

        Graph<TaintNode, TaintEdge> inputGraph = input.getMultiGraph();

        LinkedList<TaintEdge> edges = new LinkedList<TaintEdge>(output.edgeList);
        for (TaintEdge edge : edges) {
            for (String record : edge.getAllTaintRecords()) {
                if (!dsib.checkTaintRecordMatchesVariability(record, "RANDOM")) {
                    output.edgeList.remove(edge);
                }
            }
        }

        output.generateTaintIDPropagationsAndRequestCounters();
        output.postProcessTaintIDPropagations();

        return output;
    }

//    private static void removeOrphanNodes(Graph<TaintNode, TaintEdge> input) {
//        LinkedList<TaintNode> nodes = new LinkedList<TaintNode>(input.getVertices());
//
//        for (TaintNode node : nodes) {
//            if (input.getIncidentEdges(node) == null || input.getIncidentEdges(node).isEmpty())
//                input.removeVertex(node);
//        }
//    }


    public static HashMap<GraphBuilder, String> getByRequestGraphBuilders(GraphBuilder input) {
        HashMap<GraphBuilder, String> outputs = new HashMap<GraphBuilder, String>();

        for (String key : input.getRequestCounters().keySet()) {
            RequestCounterURIPair pair = input.getRequestCounters().get(key);
            String counter = pair.getRequestCounter();

            GraphBuilder output = new GraphBuilder(input);

            LinkedList<TaintEdge> edges = new LinkedList<TaintEdge>(output.edgeList);
            for (TaintEdge edge : edges) {
                if (edge.getRequestCounter() == null || !edge.getRequestCounter().equals(counter)) {
                    output.edgeList.remove(edge);
                }
            }

            output.generateTaintIDPropagationsAndRequestCounters();
            output.postProcessTaintIDPropagations();

            outputs.put(output, counter);
        }

        return outputs;
    }

    public static LinkedList<GraphBuilder> getConnectedSubGraphBuilders(GraphBuilder input) {
        LinkedList<GraphBuilder> outputs = new LinkedList<GraphBuilder>();

        // Get graph, split graph. Use graph to generate new edge lists, and taint ID lists.
        Graph<TaintNode, TaintEdge> inputGraph = input.getMultiGraph();
        LinkedList<TaintNode> inputNodes = new LinkedList<TaintNode>(inputGraph.getVertices());

        HashSet<TaintNode> visited = new HashSet<TaintNode>();
        for (TaintNode node : inputNodes) {

            Graph<TaintNode, TaintEdge> subGraph = null;
            if (visited.contains(node))
                continue;
            subGraph = new DirectedSparseMultigraph<TaintNode, TaintEdge>();

            LinkedList<TaintNode> toVisit = new LinkedList<TaintNode>();
            toVisit.add(node);

            while (!toVisit.isEmpty()) {
                TaintNode visit = toVisit.removeFirst();
                if (visited.contains(visit))
                    continue;
                visited.add(visit);

                subGraph.addVertex(visit);
                if (inputGraph.getIncidentEdges(visit) != null) {
                    for (TaintEdge edge : inputGraph.getIncidentEdges(visit)) {
                        subGraph.addEdge(edge, inputGraph.getSource(edge), inputGraph.getDest(edge));
                        toVisit.add(inputGraph.getSource(edge));
                        toVisit.add(inputGraph.getDest(edge));
                    }
                }
            }
            
            if (subGraph.getVertexCount() > 0) {
                GraphBuilder output = new GraphBuilder(input, true);
                output.edgeList.addAll(subGraph.getEdges());

                for (TaintEdge edge : output.edgeList) {
                    output.taintIDs.putAll(edge.getAllTaintIDsWithTypes());

                    if (edge.getCallingNode() != null && edge.getCalledNode() != null) {
                        if (!edge.getAdviceType().startsWith("NONTAINTRETURN")) {
                            RequestCounterURIPair counterURI = new RequestCounterURIPair(edge.getRequestCounter(), edge.getRequestURI());
                            output.requestCounters.put(counterURI.toString(), counterURI);
                        }
                    }
                }

                LinkedList<TaintEdge> sortedEdges = new LinkedList<TaintEdge>(input.getPropagationEdges());
                Collections.sort(sortedEdges);
                for (TaintEdge edge : sortedEdges) {
                    // No propagation edges moved over. No constructor copies full propagation list. It's added from edges. Propagation
                    // edges are never filtered out by taintRecord.
                    if (output.taintIDs.containsKey(edge.getSourceObject().getTaintID()) &&
                            output.taintIDs.containsKey(edge.getDestObject().getTaintID())) {
                        output.taintIDPropagations.add(new TaintIDPropagationPair(edge.getSourceObject().getTaintID(),
                                edge.getSourceObject().getValue(), edge.getSourceObject().getType(),
                                edge.getDestObject().getTaintID(),
                                edge.getDestObject().getValue(), edge.getDestObject().getType()));
                    }
                }
                output.postProcessTaintIDPropagations();

                outputs.add(output);
            }
        }

        return outputs;
    }

    public static HashSet<TaintNode> getNonCoyoteOutputs(GraphBuilder input) {
        HashSet<TaintNode> output = new HashSet<TaintNode>();

        Graph<TaintNode, TaintEdge> fullGraph = input.getMultiGraph();

        for (TaintEdge edge : fullGraph.getEdges()) {
            if (edge.getType().equals("OUTPUT")) {
                TaintNode outputNode = fullGraph.getDest(edge);
                if (!outputNode.getName().contains("CoyoteWriter"))
                    output.add(outputNode);
            }
        }

        return output;
    }
    
    public HashSet<TaintEdge> getUserOutputEdges() {
        HashSet<TaintEdge> output = new HashSet<TaintEdge>();

        Graph<TaintNode, TaintEdge> fullGraph = getMultiGraph();

        for (TaintEdge edge : fullGraph.getEdges()) {
            if (edge.getType().equals("OUTPUT")) {
                TaintNode outputNode = fullGraph.getDest(edge);
                if (outputNode.getName().contains(":write") || outputNode.getName().contains("CoyoteWriter:println"))
                    output.add(edge);
            }
        }

        return output;
    }

    public HashSet<TaintEdge> getDBOutputEdges() {
        HashSet<TaintEdge> output = new HashSet<TaintEdge>();

        Graph<TaintNode, TaintEdge> fullGraph = getMultiGraph();

        for (TaintEdge edge : fullGraph.getEdges()) {
            if (edge.getType().equals("OUTPUT")) {
                TaintNode outputNode = fullGraph.getDest(edge);
                if (outputNode.getName().contains(":execute"))
                    output.add(edge);
            }
            if (fullGraph.getDest(edge).toFullString().contains("com.mysql.jdbc.PreparedStatement$ParseInfo")) {
//                System.out.println("Adding: " + fullGraph.getDest(edge).toFullString());
                output.add(edge);
            }
        }

        return output;
    }

    // Compares a subset graph with a superset to find all outputs from the subset into the superset. Uses context of subset
    public static HashMap<TaintNode, LinkedList<TaintEdge>> getOutputs(Graph<TaintNode, TaintEdge> subset, Graph<TaintNode, TaintEdge> superset) {
        HashMap<TaintNode, LinkedList<TaintEdge>> outputs = new HashMap<TaintNode, LinkedList<TaintEdge>>();

        for (TaintNode node : subset.getVertices()) {
            for (TaintEdge inEdge : subset.getInEdges(node)) {
                if (superset.getOutEdges(node) != null) {
                    for (TaintEdge outEdge : superset.getOutEdges(node)) {
                        if (outEdge.getOutputContextCounter().equals(inEdge.getInputContextCounter())) {
                            TaintNode outNode = superset.getDest(outEdge);
                            if (outEdge.getType().equals("OUTPUT")) {
                                TaintNode subOutNode = subset.getDest(outEdge);
                                LinkedList<TaintEdge> outEdges = outputs.get(subOutNode);
                                if (outEdges == null) {
                                    outEdges = new LinkedList<TaintEdge>();
                                    outputs.put(subOutNode, outEdges);
                                }
//                                System.out.println("ADDING OUTPUT EDGE " + outEdge);
                                if (!outEdges.contains(outEdge))
                                    outEdges.add(outEdge);
                            }
                            else if(!subset.containsVertex(outNode)) {
                                LinkedList<TaintEdge> outEdges = outputs.get(node);
                                if (outEdges == null) {
                                    outEdges = new LinkedList<TaintEdge>();
                                    outputs.put(node, outEdges);
                                }
//                                System.out.println("ADDING OUTPUT EDGE " + outEdge + " to node " + outNode);
                                if (!outEdges.contains(outEdge))
                                    outEdges.add(outEdge);
                            }
                        }
                    }
                }
            }
        }

        return outputs;
    }

//    public static HashMap<TaintNode, LinkedList<TaintEdge>> getInputs(Graph<TaintNode, TaintEdge> graph, TaintNode startNode) {
//        HashMap<TaintNode, LinkedList<TaintEdge>> inputs = new HashMap<TaintNode, LinkedList<TaintEdge>>();
//
//        HashSet<TaintNode> inputsNodes = new HashSet<TaintNode>();
//        getInputsHelper(graph, inputsNodes, new HashSet<TaintNode>(), startNode);
//
//
//        return inputs;
//    }
//
//    public static void getInputsHelper(Graph<TaintNode, TaintEdge> graph, HashSet<TaintNode> inputs, HashSet<TaintNode> visited, TaintNode currentNode) {
//        if (visited.contains(currentNode))
//            return;
//        visited.add(currentNode);
//
//        boolean hasInputs = false;
//        for (TaintEdge inEdge : graph.getInEdges(currentNode)) {
//            hasInputs = true;
//            getInputsHelper(graph, inputs, visited, graph.getSource(inEdge));
//        }
//
//        if (!hasInputs) {
//            inputs.add(currentNode);
//        }
//    }

    // Compares a subset graph with a superset to find all inputs from the superset into the subset
    public static HashMap<TaintNode, LinkedList<TaintEdge>> getInputs(Graph<TaintNode, TaintEdge> subset, Graph<TaintNode, TaintEdge> superset) {
        HashMap<TaintNode, LinkedList<TaintEdge>> inputs = new HashMap<TaintNode, LinkedList<TaintEdge>>();

        for (TaintNode subNode : subset.getVertices()) {
            for (TaintEdge outEdge : subset.getInEdges(subNode)) {
                for (TaintEdge inEdge : superset.getInEdges(subNode)) {
                    if (outEdge.getOutputContextCounter().equals(inEdge.getInputContextCounter())) {
                        TaintNode inNode = superset.getSource(inEdge);
                        if (!subset.containsVertex(inNode)) {
                            LinkedList<TaintEdge> inEdges = inputs.get(subNode);
                            if (inEdges == null) {
                                inEdges = new LinkedList<TaintEdge>();
                                inputs.put(subNode, inEdges);
                            }
                            inEdges.add(inEdge);
                        }
                    }
                    if (inEdge.getType().equals("RETURNINGINPUT")) {
                        TaintNode innerInputNode = superset.getSource(inEdge);
                        LinkedList<TaintEdge> inEdges = inputs.get(innerInputNode);
                        if (inEdges == null) {
                            inEdges = new LinkedList<TaintEdge>();
                            inputs.put(innerInputNode, inEdges);
                        }
                        inEdges.add(inEdge);
                    }
                }
            }
        }

        return inputs;
    }

    public static LinkedList<TaintNode> getInputs(Graph<TaintNode, TaintEdge> fullInput) {
        LinkedList<TaintNode> inputs = new LinkedList<TaintNode>();

//        for (TaintNode node : fullInput.getVertices()) {
//            if (fullInput.getInEdges(node) == null || fullInput.getInEdges(node).isEmpty()) {
//                inputs.add(node);
//            }
//        }

        if (inputs.isEmpty()) {
            LinkedList<TaintEdge> edges = new LinkedList<TaintEdge>(fullInput.getEdges());
            Collections.sort(edges);

            inputs.add(fullInput.getSource(edges.getFirst()));
        }

        return inputs;
    }

}
