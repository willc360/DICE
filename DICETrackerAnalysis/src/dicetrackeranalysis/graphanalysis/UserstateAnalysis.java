/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package dicetrackeranalysis.graphanalysis;

import dicetrackeranalysis.graphhandling.AnalysisMainWindow;
import dicetrackeranalysis.graphhandling.GraphBuilder;
import dicetrackeranalysis.graphhandling.TaintEdge;
import dicetrackeranalysis.graphhandling.TaintIDTreeNode;
import dicetrackeranalysis.graphhandling.TaintNode;
import edu.uci.ics.jung.graph.Graph;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import javax.swing.JTextArea;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author lee
 */
public class UserstateAnalysis {
    private GraphBuilder gb;
    private AnalysisMainWindow analysisMainWindow;
    private JTextArea out;
    private HashMap<String, LinkedList<DefaultMutableTreeNode>> taintIDToTreeNodeMap;

    public UserstateAnalysis(GraphBuilder gb, DefaultMutableTreeNode taintIDTree, AnalysisMainWindow analysisMainWindow, JTextArea out) {
        this.out = out;
        this.gb = gb;
        this.analysisMainWindow = analysisMainWindow;

        taintIDToTreeNodeMap = new HashMap<String, LinkedList<DefaultMutableTreeNode>>();
        Enumeration<DefaultMutableTreeNode> childNodes = taintIDTree.depthFirstEnumeration();
        while (childNodes.hasMoreElements()) {
            DefaultMutableTreeNode childNode = childNodes.nextElement();
            if (childNode.getUserObject() instanceof TaintIDTreeNode) {
                String taintID = ((TaintIDTreeNode)childNode.getUserObject()).getTaintID();
                LinkedList<DefaultMutableTreeNode> list = taintIDToTreeNodeMap.get(taintID);
                if (list == null) {
                    list = new LinkedList<DefaultMutableTreeNode>();
                    taintIDToTreeNodeMap.put(taintID, list);
                }
                list.add(childNode);
            }
        }
    }

    public void analyze() {
        out.append("Starting User State Analysis\n");

        /*
         * New Algorithnm
         *
         * Get common taint ids
         *  -get request graphs
         *  -get all taint in all edges
         *  -figure out what is persistent
         *
         * Go through common taint, remove what is derived
         *
         * For each taintID, find which req graphs have it, return earlier edges which carry it, for each edge build a propagation graph
         *  build taintID -> reqID -> read edges, propagation pairs
         *
         * For each taintID in map, for each req, for each read edge
         *
         * call
         *  expand(Set<edge>, currentEdge, propGraph, path, set of reqID map for taintID)
         *
         */

        // Group taintIDs by request
        LinkedList<TaintEdge> sortedEdges = gb.getOrderedEdgeList();
        LinkedHashMap<String, HashSet<String>> requestToTaintIDMap = new LinkedHashMap<String, HashSet<String>>();
        for (TaintEdge edge : sortedEdges) {
            HashSet<String> requestTaintIDs = requestToTaintIDMap.get(edge.getRequestCounter());
            if (requestTaintIDs == null) {
                requestTaintIDs = new HashSet<String>();
                requestToTaintIDMap.put(edge.getRequestCounter(), requestTaintIDs);
            }
            requestTaintIDs.addAll(edge.getAllTaintIDs());
        }

        System.out.println("GROUPED REQTAINTIDS #GROUPS: " + requestToTaintIDMap.size());

        // Find what taint exists in multiple requests
        ArrayList<HashSet<String>> taintIDSets = new ArrayList<HashSet<String>>(requestToTaintIDMap.values());
        HashSet<String> persistentTaintIDs = new HashSet<String>();
        for (int i = 0; i < taintIDSets.size(); i++) {
            for (int j = i + 1; j < taintIDSets.size(); j++) {
                HashSet<String> compA = new HashSet<String>(taintIDSets.get(i));
                HashSet<String> compB = new HashSet<String>(taintIDSets.get(j));

                compA.retainAll(compB);
                persistentTaintIDs.addAll(compA);
            }
        }

        System.out.println("PERSISTENT TAINT IDS: " + persistentTaintIDs.size());
        for (String persistentID : persistentTaintIDs) {
            if (persistentID.contains("user_name"))
                System.out.println("\t" + persistentID);
        }
        
        // Remove any derived taint from the persistent set, so that we only look at original taint access
        HashSet<String> derivedTaintIDs = new HashSet<String>();
        for (String taintID : persistentTaintIDs) {
            LinkedList<DefaultMutableTreeNode> nodes = taintIDToTreeNodeMap.get(taintID);
            for (DefaultMutableTreeNode node : nodes) {
                Enumeration<DefaultMutableTreeNode> childNodes = node.depthFirstEnumeration();
                while (childNodes.hasMoreElements()) {
                    DefaultMutableTreeNode childNode = childNodes.nextElement();
                    if (childNode.getUserObject() instanceof TaintIDTreeNode) {
                        String subTaintID = ((TaintIDTreeNode)childNode.getUserObject()).getTaintID();
                        if (subTaintID != null && !subTaintID.equals(taintID))
                            derivedTaintIDs.add(subTaintID);
                    }
                }
            }
        }

        persistentTaintIDs.removeAll(derivedTaintIDs);
        System.out.println("NON DERIVED PERSISTENT TAINT IDS: " + persistentTaintIDs.size());
        for (String persistentID : persistentTaintIDs) {
            if (persistentID.contains("user_name"))
                System.out.println("\t" + persistentID);
        }

        /* For each taintID, find which req graphs have it, return earlier edges which carry it, for each edge build a propagation graph
         *  build taintID -> reqID -> read edge, propagation pairs
         */
        HashMap<String, HashMap<String, LinkedList<EdgePropagationGraphPair>>> masterMap = new HashMap<String, HashMap<String, LinkedList<EdgePropagationGraphPair>>>();
        HashMap<GraphBuilder, String> byRequestGraphBuilders = GraphBuilder.getByRequestGraphBuilders(gb);
        for (String taintID : persistentTaintIDs) {
            for (GraphBuilder requestGraphBuilder : byRequestGraphBuilders.keySet()) {
                Graph<TaintNode, TaintEdge> requestGraph = requestGraphBuilder.getMultiGraph();

                LinkedList<TaintEdge> requestTargetTaintEdges = new LinkedList<TaintEdge>();
                for (TaintEdge requestEdge : requestGraph.getEdges()) {
                    if (requestEdge.getAllUsedTaintIDs().contains(taintID)) {
                        requestTargetTaintEdges.add(requestEdge);
                    }
                }

                if (requestTargetTaintEdges.size() > 0) {
                    TaintEdge lowEdge = Collections.min(requestTargetTaintEdges);
                    TaintNode originalNode = lowEdge.getCallingNode();
//                    System.out.println("ORIGINAL NODE: " + originalNode);
                    for (TaintEdge originalEdge : requestTargetTaintEdges) {
                        if (originalEdge.getCallingNode() == originalNode) {
                            LinkedList<DefaultMutableTreeNode> nodes = taintIDToTreeNodeMap.get(taintID);
                            HashSet<String> subTaintIDs = new HashSet<String>();
                            subTaintIDs.add(taintID);
                            for (DefaultMutableTreeNode node : nodes) {
                                Enumeration<DefaultMutableTreeNode> childNodes = node.depthFirstEnumeration();
                                while (childNodes.hasMoreElements()) {
                                    DefaultMutableTreeNode childNode = childNodes.nextElement();
                                    if (childNode.getUserObject() instanceof TaintIDTreeNode) {
                                        String subTaintID = ((TaintIDTreeNode)childNode.getUserObject()).getTaintID();
                                        subTaintIDs.add(subTaintID);
                                    }
                                }
                            }
                            EdgePropagationGraphPair requestTaintPropagation = new EdgePropagationGraphPair(originalEdge, requestGraph, subTaintIDs);
                            if (requestTaintPropagation.getPropagationGraph() != null) {
                                HashMap<String, LinkedList<EdgePropagationGraphPair>> reqEdgeMap = masterMap.get(taintID);
                                if (reqEdgeMap == null) {
                                    reqEdgeMap = new HashMap<String, LinkedList<EdgePropagationGraphPair>>();
                                    masterMap.put(taintID, reqEdgeMap);
                                }
                                LinkedList<EdgePropagationGraphPair> edgePropagationPairList = reqEdgeMap.get(byRequestGraphBuilders.get(requestGraphBuilder));
                                if (edgePropagationPairList == null) {
                                    edgePropagationPairList = new LinkedList<EdgePropagationGraphPair>();
                                    reqEdgeMap.put(byRequestGraphBuilders.get(requestGraphBuilder), edgePropagationPairList);
                                }
                                edgePropagationPairList.add(requestTaintPropagation);
                            }
                        }
                    }
                }
            }
        }

        System.out.println("MASTERMAP SIZE: " + masterMap.size());
        
        /* For each taintID in map, for each req, for each read edge
         *
         * call
         *  expand(Set<edge>, currentEdge, propGraph, path, set of reqID map for taintID)
         *
         */

        int counter = 0;
        for (String taintID : masterMap.keySet()) {
            HashMap<String, LinkedList<EdgePropagationGraphPair>> reqEdgeMap = masterMap.get(taintID);
//            System.out.println("TID: " + taintID);
            
            for (String reqID : reqEdgeMap.keySet()) {
                LinkedList<EdgePropagationGraphPair> pairList = reqEdgeMap.get(reqID);
                for (EdgePropagationGraphPair pair : pairList) {
//                    System.out.println("\tRID: " + reqID + " edge: " + pair.getEdge());
                    HashMap<String, LinkedList<EdgePropagationGraphPair>> leftOverReqEdgeMap = new HashMap<String, LinkedList<EdgePropagationGraphPair>>(reqEdgeMap);
                    leftOverReqEdgeMap.remove(reqID);

                    HashSet<TaintEdge> userStateEdges = new HashSet<TaintEdge>();
                    LinkedList<TaintEdge> startPath = new LinkedList<TaintEdge>();
                    startPath.add(pair.getEdge());

                    System.out.println("Finding state");
                    findSingleUserState(new HashSet<TaintEdge>(), userStateEdges, pair.getPropagationGraph(), startPath, true, leftOverReqEdgeMap);
                    System.out.println("Done finding state");

                    // Do something with the userStateEdges, which is basically the result of our computations
                    if (userStateEdges.size() > 0) {
                        GraphBuilder userStateGraphBuilder = GraphBuilder.getBuilderFromEdges(gb, userStateEdges);
                        userStateGraphBuilder.colorNode(pair.getEdge().getCallingNode(), 5);
//                        System.out.println("ADDING USER STATE GRAPH " + counter);
                        analysisMainWindow.addAnalysisGraphBuilder(userStateGraphBuilder, "USER STATE " + (counter++) + "[" + userStateEdges.size() + "]", "");
                    }
                }
            }
        }

        System.out.println("Finished User State Analysis\n");
        out.append("Finished User State Analysis\n");
    }

    boolean debugMode;

    private void findSingleUserState(HashSet<TaintEdge> visited, HashSet<TaintEdge> foundEdges, Graph<TaintNode, TaintEdge> graph, LinkedList<TaintEdge> path, boolean isForward, HashMap<String, LinkedList<EdgePropagationGraphPair>> reqEdgeMap) {
        if (visited.contains(path.getLast()))
            return;
        visited.add(path.getLast());

        boolean check = false;

        // Use the match path check here, have it on the propagation pair
        outer:
        for (LinkedList<EdgePropagationGraphPair> pairList : reqEdgeMap.values()) {
            for (EdgePropagationGraphPair testPair : pairList) {
                if (testPair.matchPath(path)) {
                    check = true;
                    if (!testPair.getEdge().getRequestRemoteAddr().equals(path.getLast().getRequestRemoteAddr())) {
                        check = false;
                        break outer;
                    }
                }
                debugMode = false;
            }
        }
        if (!check)
            return;

        foundEdges.add(path.getLast());

        TaintNode nextNode = null;
        if (isForward) {
            nextNode = graph.getDest(path.getLast());
        }
        else {
            nextNode = graph.getSource(path.getLast());
        }

        if (graph.getOutEdges(nextNode) != null) {
            for (TaintEdge nextEdge : graph.getOutEdges(nextNode)) {
                LinkedList<TaintEdge> newPath = new LinkedList<TaintEdge>(path);
                newPath.add(nextEdge);
                findSingleUserState(visited, foundEdges, graph, newPath, true, reqEdgeMap);
            }
        }
        if (graph.getInEdges(nextNode) != null) {
            for (TaintEdge nextEdge : graph.getInEdges(nextNode)) {
                LinkedList<TaintEdge> newPath = new LinkedList<TaintEdge>(path);
                newPath.add(nextEdge);
                findSingleUserState(visited, foundEdges, graph, newPath, false, reqEdgeMap);
            }
        }
    }

    public class EdgePropagationGraphPair {
        private TaintEdge edge;
        private Graph<TaintNode, TaintEdge> propagationGraph;

        public EdgePropagationGraphPair(TaintEdge startEdge, Graph<TaintNode, TaintEdge> inputGraph, HashSet<String> targetTaintIDs) {
            HashSet<TaintEdge> propagationEdges = new HashSet<TaintEdge>();
            forwardContextExpand(new HashSet<TaintEdge>(), propagationEdges, startEdge, inputGraph, true, targetTaintIDs);

            GraphBuilder propagationGraphBuilder = GraphBuilder.getBuilderFromEdges(gb, propagationEdges);
            this.propagationGraph = propagationGraphBuilder.getMultiGraph();
            this.edge = startEdge;
        }

        public TaintEdge getEdge() {
            return this.edge;
        }

        public Graph<TaintNode, TaintEdge> getPropagationGraph() {
            return this.propagationGraph;
        }

        public boolean matchPath(LinkedList<TaintEdge> path) {
            // recurse down self graph (current edge marker), until current matches targetPath
            // recurse down graph further, make smaller copy of target path, try to match that.
            // If you can't find anything, stop recursing.

            //Iterative version
            // Have list of edges, a path, and a graph.
            // EASY WAY
            /*
             * look through graph edges for match, scan forward from there along with path to try and match to end, if fail give up.
             */
            TaintEdge firstPathEdge = path.getFirst();

            // Look through graph for edges which match the start edge of the path.
            boolean matched = false;
            for (TaintEdge startEdge : this.propagationGraph.getEdges()) {
                if (startEdge.getCallingNode().getName().equals(firstPathEdge.getCallingNode().getName()) &&
                        startEdge.getCalledNode().getName().equals(firstPathEdge.getCalledNode().getName())) {
                    // Once a match has been found with the start of the path, try to traverse graph to match path all the way to the end

                    int pathPosition = 1;
                    TaintEdge checkEdge = startEdge;

                    while (pathPosition < path.size()) {
                        boolean found = false;
                        if (propagationGraph.getIncidentEdges(checkEdge.getCalledNode()) != null) {
                            for (TaintEdge nextEdge : propagationGraph.getIncidentEdges(checkEdge.getCalledNode())) {
                                if (nextEdge != checkEdge && nextEdge.getCallingNode().getName().equals(path.get(pathPosition).getCallingNode().getName()) &&
                                        nextEdge.getCalledNode().getName().equals(path.get(pathPosition).getCalledNode().getName())) {
                                    found = true;
                                    checkEdge = nextEdge;
                                    break;
                                }
                            }
                        }
                        if (!found && propagationGraph.getIncidentEdges(checkEdge.getCallingNode()) != null) {
                            for (TaintEdge nextEdge : propagationGraph.getIncidentEdges(checkEdge.getCallingNode())) {
                                if (nextEdge != checkEdge && nextEdge.getCallingNode().getName().equals(path.get(pathPosition).getCallingNode().getName()) &&
                                        nextEdge.getCalledNode().getName().equals(path.get(pathPosition).getCalledNode().getName())) {
                                    found = true;
                                    checkEdge = nextEdge;
                                    break;
                                }
                            }
                        }
                        if (found)
                            pathPosition++;
                        else
                            break;
                    }

                    if (pathPosition == path.size()) {
                        matched = true;
                        break;
                    }
                }
            }

            return matched;
        }

        private void forwardContextExpand(HashSet<TaintEdge> visited, HashSet<TaintEdge> foundEdges, TaintEdge current, Graph<TaintNode, TaintEdge> graph, boolean isForward, HashSet<String> targetTaintIDs) {
            // Looking for taint in high level object
            if (visited.contains(current))
                return;
            visited.add(current);

            boolean foundTaint = false;
            for (String currentTaintID : current.getAllTaintIDs()) {
                if (targetTaintIDs.contains(currentTaintID)) {
                    foundTaint = true;
                    break;
                }
            }
            if (!foundTaint)
                return;

            foundEdges.add(current);

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

            for (TaintEdge nextEdge : graph.getOutEdges(nextNode)) {
                if ((nextEdge.getOutputContextCounter().equals(context) || (nextEdge.getType().equals("FIELDGET") && nextEdge.getRequestCounter().equals(current.getRequestCounter()))) && (nextEdge.getCounter() > current.getCounter() || (current.getType().equals("SUPPLEMENTARY") && !nextEdge.getType().equals("SUPPLEMENTARY")))) {
                    forwardContextExpand(visited, foundEdges, nextEdge, graph, true, targetTaintIDs);
                }
            }
            for (TaintEdge nextEdge : graph.getInEdges(nextNode)) {
                if (nextEdge.getInputContextCounter().equals(context) && (nextEdge.getCounter() > current.getCounter() || current.getType().equals("SUPPLEMENTARY") && !nextEdge.getType().equals("SUPPLEMENTARY"))) {
                    forwardContextExpand(visited, foundEdges, nextEdge, graph, false, targetTaintIDs);
                }
            }
        }
    }
}
