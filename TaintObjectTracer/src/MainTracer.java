import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.print.attribute.standard.Destination;
import javax.swing.JTable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class MainTracer {
	
	private static Hashtable<String, Hashtable<String, ArrayList<String>>> req_table;
	
	// The directory to parse the taintlog files
	private static String directory = "C:/Users/Sentinel/Desktop/taintlog/Aries fixed fieldget";
	
	// A set of database columns to consider
	private static HashSet<String> columns_set;
	
	// The package name where the "application methods" are defined
	private static String packageName = "org.apache.aries.samples.ariestrader";
	
	// Method executed on startup that should not be counted as part of the %
	// 7 for aries, 3 for rubis
	private static int offset = 7;
	
	// Set of methods tainted by String
	private static HashSet<String> methodsTaintedByString;
	// Set of methods tainted by object, but one of the subsequent methods called is tainted by String
	private static HashSet<String> methodsTaintedByObject_type1;
	// Set of methods tainted by object, but none of the subsequent methods called is tainted by String
	// ie. the tainted object can be stripped of its tainted string members such that the method
	// can be treated as untainted
	private static HashSet<String> methodsTaintedByObject_type3;
	
	// ---------------------------------------------------- //
	// data structures used to identify type 1 and 3 objects
	
	// Each key represents a root node (ie. a separate tree)
	// Each key of the map and also each element in the HashSet entry is a contextcounter
	// Each key-entry pair indicates the root node (String) and its children (HashSet)
	private static HashMap<String, HashSet<String>> rootNode;
	
	// As in rootNode above, the Strings are contextCounters. Each key-entry pair
	// represents an edge in the tree. None of the keys in this map is a root node.
	private static HashMap<String, HashSet<String>> treeNode;
	
	// A list to map contextCounter (both callerContextCounter and calledContextCounter) as specified in a
	// "location" element within each "taintlog" element to its corresponding method name
	private static HashMap<String, String> contextCounterToMethodMap;
	// ---------------------------------------------------- //
	
	private static boolean taintTypeTableInitialized = false;
	
	// The list of taintlog types for which the taint recorded in log
	// file is considered. (The list is obtained from Lee Beckman's TaTAMi GUI tool)
	private static String[] types = {"CALLING", "OUTPUT", "RETURNING", "RETURNINGINPUT", "STATICFIELDSTORE", 
		"FIELDSET", "FIELDGET", "JAVAFIELDSET", "JAVAFIELDGET", "FUZZYPROPAGATION", "COMPOSITION", "ASSOCIATION"};  

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		req_table = new Hashtable<>();
		columns_set = new HashSet<String>();
		
		List<String> typesList = Arrays.asList(types);

		File dir = new File(directory);
		
		// Obtain the list of xml files in the specified directory
		File[] logFiles = dir.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				String filename = pathname.getName();
				String extension = filename.substring(filename.lastIndexOf(".") + 1);
				if (extension.equals("xml"))
					return true;
				return false;
			}
		});
		
		// iterate through each request xml
		for (File logFile : logFiles) {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder;
			
			// Initialize the data structures for each request XML
			methodsTaintedByString = new HashSet<String>();
			methodsTaintedByObject_type1 = new HashSet<String>();
			methodsTaintedByObject_type3 = new HashSet<String>();
			
			contextCounterToMethodMap = new HashMap<>();
			
			treeNode = new HashMap<String, HashSet<String>>();
			rootNode = new HashMap<String, HashSet<String>>();
			
			// Starts parsing each XML
			try {
				docBuilder = docBuilderFactory.newDocumentBuilder();
				Document doc = docBuilder.parse(logFile);

				Element docRoot = doc.getDocumentElement();
				NodeList childNodes = docRoot.getChildNodes();

				// Stores list of methods (value) accessed for each column (key)
				Hashtable<String, ArrayList<String>> col_table = new Hashtable<String, ArrayList<String>>();
				
				// iterate through each "taintlog" element
				for (int i = 0, length = childNodes.getLength(); i < length; i++ ) {
					if (childNodes.item(i) instanceof Element) {
						Element taintLogElem = (Element) childNodes.item(i);
						NodeList taintLogChildNodes = taintLogElem.getChildNodes();

						String srcClass = "";
						String srcMethod = "";
						String destClass = "";
						String destMethod = "";
						String col_name = "";
						String callerContextCounter = "";
						String calledContextCounter = "";
						String taintType = "Object";
						ArrayList<String> taintedObjects = new ArrayList<>();
						
						String taintlogType = "";
						if (taintLogElem.getNodeName().equals("taintlog")) {
							taintlogType = taintLogElem.getAttribute("type");
						}
						
						// Iterate each CHILD node
						for (int j = 0; j < taintLogChildNodes.getLength(); j++) {
							Element taintLogChildElem = (Element) taintLogChildNodes.item(j);
							
							// Check location node to obtain accessed method							
							if (taintLogChildElem.getNodeName().equals("location")) {
								if (!typesList.contains(taintlogType)) {
									break;
								}
								
								String[] full_src_method = new String[0];
								if (!checkOmitSrc(taintlogType)) {
									srcClass = taintLogChildElem.getAttribute("srcClass");
									callerContextCounter = taintLogChildElem.getAttribute("callerContextCounter");
									// Split the retrieved source method into names and parameters
									// The first element in the array is the name, rest are params
									full_src_method = taintLogChildElem.getAttribute("srcMethod").split(" ");
									srcMethod = full_src_method[0] + "(";
								}
								// Generate the full source method name including parameters
								for (int a = 1; a < full_src_method.length; a++) {
									srcMethod+=full_src_method[a];
									if (a != full_src_method.length - 1) {
										srcMethod+=",";
									}
								}
								srcMethod+=")";
								
								String[] full_dest_method = new String[0];
								if (!checkOmitDest(taintlogType)) {
									calledContextCounter = taintLogChildElem.getAttribute("calledContextCounter");
									destClass = taintLogChildElem.getAttribute("destClass");
									full_dest_method = taintLogChildElem.getAttribute("destMethod").split(" ");
									destMethod = full_dest_method[0] + "(";
								}
								for (int a = 1; a < full_dest_method.length; a++) {
									destMethod+=full_dest_method[a];
									if (a != full_dest_method.length - 1) {
										destMethod+=",";
									}
								}
								destMethod+=")";

								// Ignore nontaintreturn (non taint objects)
								if (taintLogChildElem.getAttribute("adviceType").equals("NONTAINTRETURN")) {
									break;
								}
							}
							// Check taintedobject node to get the column(s) returned
							else if (taintLogChildElem.getNodeName().equals("taintedObject")) {
								String currTaintType = taintLogChildElem.getAttribute("type");
//								if(logFile.getName().contains("Account") && 
//										(srcMethod.contains("getAccountData(String") || destMethod.contains("getAccountData(String")))
//									System.out.println(taintLogChildElem.getAttribute("value") + taintlogType + currTaintType);
								if (!(taintType.contains("String")) && currTaintType.contains("String")) {
									taintType = "String";
								}
								
								String taintRecord = taintLogChildElem.getAttribute("taintRecord");
								// According to thesis, this is used to indicate multiple sourses
								String[] taints = taintRecord.split(" #RECSEP#");
								
								// Access each taint SOURCE
								for (int b = 0; b < taints.length; b++) {
									String taint = taints[b];
									
									// check whether a column is accessed or a whole resultset is returned
									// Format of column name: DB/TABLE/COL
									if(taint.contains("TARGETCOLUMN")) {
										// Parse string to generate name of column
										String rest = taint.substring(taint.indexOf("CATALOG"));
										col_name = taint.substring(taint.indexOf(" ")+1, 
												taint.indexOf("CATALOG")-1);
										String[] columns = rest.split("CATALOG:");
										for (int a = 1; a < columns.length; a++) {
											String curr_col_name = columns[a].substring(1).
													replace(" TABLE: ", "/").replace(" COLUMN: ", "/");
											if (curr_col_name.split("/")[2].contains(col_name)) {											
												col_name = curr_col_name;
												break;
											}
										}
//										col_name = col_name + ": " + taintType;
										taintedObjects.add(col_name.trim());
									}
									else if (taint.contains("CATALOG:")) {
										// whole resultset is returned, record all columns in rs
										String[] columns = taint.split("CATALOG:");
										for (int a = 1; a < columns.length; a++) {
											col_name = columns[a].substring(1).
													replace(" TABLE: ", "/").replace(" COLUMN: ", "/");
//											col_name = col_name + ": " + taintType;
											taintedObjects.add(col_name.trim());
										}
									}
									// Supposed to treat user input, but don't see any???
									else {
										taintedObjects.add(taint.trim());
									}
								}
							}
						}
						
						// Record all unique columns accessed
						if (srcClass.contains(packageName) || destClass.contains(packageName)) {
							columns_set.addAll(taintedObjects);
							
							String fullsrc = srcClass+"."+srcMethod+"-"+callerContextCounter;
							String fulldest = destClass+"."+destMethod+"-"+calledContextCounter;
							
							addMethodByTaintType(fullsrc, taintType);
							addMethodByTaintType(fulldest, taintType);
							
							addTopLvlNode(callerContextCounter, fullsrc, calledContextCounter, fulldest, taintlogType);
							
							// For each column, add the current method name.
							// Note that each parent node can only have a single method, but can have
							// accessed multiple columns.
							for (int z = 0; z < taintedObjects.size(); z++) {
								String name = taintedObjects.get(z);
								if (col_table.containsKey(name)) {
									// Filter out non-Aries methods
									if (!col_table.get(name).contains(fullsrc) &&
											(srcClass.contains(packageName)))
										col_table.get(name).add(fullsrc);
									if (!col_table.get(name).contains(fulldest) &&
											(destClass.contains(packageName)))
										col_table.get(name).add(fulldest);
								}
								else if (!name.equals("")) {
									ArrayList<String> list = new ArrayList<>();
									if (srcClass.contains(packageName))
										list.add(fullsrc);
									if (destClass.contains(packageName))
										list.add(fulldest);
									col_table.put(name, list);
								}
							}
						}
					}        	
				}
				System.out.println(rootNode);
				System.out.println(treeNode);
				System.out.println("end");
				classifyObjectTaintType();
				
				generateTaintTypeTableRows(logFile.getName());
				
				// Store the current request with a table of columns as keys and methods as values
				req_table.put(logFile.getName(), col_table);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Print out table in csv format
		generateTable();
	}
	
	private static boolean checkOmitSrc(String taintlogType) {
		return taintlogType.equals("FIELDGET") || taintlogType.equals("FIELDSET") || 
				taintlogType.equals("JAVAFIELDGET") || taintlogType.equals("STATICFIELDSTORE") ||
				taintlogType.equals("JAVAFIELDSET") || taintlogType.equals("FUZZYPROPAGATION");
	}
	
	private static boolean checkOmitDest(String taintlogType) {
		return (taintlogType.equals("RETURNINGINPUT"));
	}
	
	private static void addTopLvlNode(String callerContextCounter, String fullSrcMethodName, 
			String calledContextCounter, String fullDestMethodName, String taintlogType) {
		// throw exp?
		
		// Simply checks if it is a non-empty method
		boolean srcEmpty = true;
		boolean destEmpty = true;
		
		if (!checkOmitSrc(taintlogType)) {
			contextCounterToMethodMap.put(callerContextCounter, fullSrcMethodName);
			srcEmpty = false;
		}
		
		if (!checkOmitDest(taintlogType)) {
			contextCounterToMethodMap.put(calledContextCounter, fullDestMethodName);
			destEmpty = false;
		}
		
		if (srcEmpty) {
			if (!treeNode.containsKey(calledContextCounter)) {
				if (!rootNode.containsKey(calledContextCounter)) {
					rootNode.put(calledContextCounter, new HashSet<String>());
				}
			}
			return;
		}
		else if (destEmpty) {
			if (!treeNode.containsKey(callerContextCounter)) {
				if (!rootNode.containsKey(callerContextCounter)) {
					rootNode.put(callerContextCounter, new HashSet<String>());
				}
			}
			return;
		}
		
		// deals with the case where the called method is a root node
		if (rootNode.containsKey(calledContextCounter)) {
			HashSet<String> set = (HashSet<String>) rootNode.get(calledContextCounter).clone();
			if (treeNode.containsKey(calledContextCounter)) {
				treeNode.get(calledContextCounter).addAll(set);
			}
			else {
				treeNode.put(calledContextCounter, set);
			}
			rootNode.remove(calledContextCounter);
			
			HashSet<String> set2 = new HashSet<>();
			if (rootNode.containsKey(callerContextCounter)) {
				set2.addAll(rootNode.get(callerContextCounter));
			}			
			set2.add(calledContextCounter);
			rootNode.put(callerContextCounter, set2);
		} 
		else if (rootNode.containsKey(callerContextCounter)) {
			HashSet<String> set = rootNode.get(callerContextCounter);

			if (!set.contains(calledContextCounter)) {
				set.add(calledContextCounter);
				if (!treeNode.containsKey(calledContextCounter))
					treeNode.put(calledContextCounter, new HashSet<String>());
			}
		}
		else if (treeNode.containsKey(callerContextCounter)) {
			treeNode.get(callerContextCounter).add(calledContextCounter);
			if (!treeNode.containsKey(calledContextCounter)) {
				treeNode.put(calledContextCounter, new HashSet<String>());
			}
		}
		else {
			HashSet<String> set = new HashSet<>();
			set.add(calledContextCounter);
			rootNode.put(callerContextCounter, set);
			treeNode.put(calledContextCounter, new HashSet<String>());
		}
	}
	
	private static void classifyObjectTaintType() {
		Iterator<String> treeItr = rootNode.keySet().iterator();
		// Iterate the top level node in trees (level 0)
		// (in other words, iterate each tree)
		while(treeItr.hasNext()) {
			String toplvlcontextCounter = treeItr.next();
			
			// Keep track of the nodes iterated in a tree path
			Stack<String> counterStack = new Stack<String>();
			counterStack.push(toplvlcontextCounter);
			
			// This set contains the nodes in the level 1 (root is level 0)
			HashSet<String> set = rootNode.get(toplvlcontextCounter);
			Iterator<String> lvlOneItr = rootNode.get(toplvlcontextCounter).iterator();
			
			while (lvlOneItr.hasNext()) {
				String lvlOneContextCounter = lvlOneItr.next();
				counterStack.push(lvlOneContextCounter);
				
				// lvlOneChildren == nodes in lvl 2
				HashSet<String> lvlOneChildren = treeNode.get(lvlOneContextCounter);
				if (lvlOneChildren.size() == 0) {
					if (methodsTaintedByString.contains(lvlOneContextCounter) &&
							methodsTaintedByObject_type3.contains(toplvlcontextCounter)) {
						methodsTaintedByObject_type3.remove(toplvlcontextCounter);
						methodsTaintedByObject_type1.add(toplvlcontextCounter);
					}
					continue;
				}
				else {
					// Iterate through subtree (level 2 and further)
					
					processSubTree(lvlOneContextCounter, counterStack);
				}
			}
		}
	}
	
	private static void processSubTree(String currCounter, Stack<String> stack) {
		Stack<String> newStack = (Stack<String>) stack.clone();
		newStack.push(currCounter);
		if (treeNode.get(currCounter).size() > 0) {
			Iterator<String> itr = treeNode.get(currCounter).iterator();
			
			while (itr.hasNext()) {
				processSubTree(itr.next(), newStack);
			}
		}
		else {
			// reach the leaf of tree path
			boolean stringTaintInPath = false;
			
			// Check current path
			while (!newStack.empty()) {
				String counter = newStack.pop();
				String method = contextCounterToMethodMap.get(counter);
				
				if (methodsTaintedByString.contains(method)) {
					stringTaintInPath = true;
				}
				else if (stringTaintInPath) {
					methodsTaintedByObject_type3.remove(method);
					methodsTaintedByObject_type1.add(method);
				}
			}
		}
	}
	
	private static void initializeTaintTypeTable(FileWriter writer) {
		try {
			writer.append(",Object_type1");
			writer.append(",Object_type3");
			writer.append(",String\n");
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	private static void generateTaintTypeTableRows(String rowName) throws Exception {
		
		try {
			FileWriter writer = new FileWriter(directory + "/taintedMethodByTypes.csv", true);
			
			if (!taintTypeTableInitialized) {
				taintTypeTableInitialized = true;
				initializeTaintTypeTable(writer);
			}
			
			checkTaintTypeTable();
			System.out.println(rowName);
			
			writer.append(rowName+",");
			writer.append("\""+methodsTaintedByObject_type1.toString()+"\",");
			writer.append("\""+methodsTaintedByObject_type3.toString()+"\",");
			writer.append("\""+methodsTaintedByString.toString()+"\"\n");
			System.out.println(methodsTaintedByString.size());
			writer.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	// Helper method to check whether the constructed table have
	// any problems in the end.
	private static void checkTaintTypeTable() throws Exception {
		
		Iterator<String> itr = methodsTaintedByObject_type3.iterator();
		while(itr.hasNext()) {
			String str = itr.next();
			if (methodsTaintedByString.contains(str)) {
				throw new Exception("Method "+str+" tainted by both objects and Strings");
			}
		}
		
		if (methodsTaintedByObject_type3.contains("") || methodsTaintedByString.contains(""))
			throw new Exception("Tainted method should not be an empty string");
	}
	
	private static void addMethodByTaintType(String methodName, String taintType) {
		if (!methodName.contains(packageName))
			return;
		
		if (taintType.equals("String")) {
			methodsTaintedByObject_type3.remove(methodName);
			methodsTaintedByString.add(methodName);
		}
		else if (!methodsTaintedByString.contains(methodName)){
			methodsTaintedByObject_type3.add(methodName);
		}
	}
	
	private static void generateTable() {
		
		Set<String> keys = req_table.keySet();
		Iterator<String> keys_itr = keys.iterator();
		ArrayList<String> columns_list = new ArrayList(columns_set);
		Collections.sort(columns_list);
				
		try {
			FileWriter writer = new FileWriter(directory + "/taints.csv", true);
			FileWriter writer2 = new FileWriter(directory + "/methods_percent.csv", true);

			Iterator<String> cols_names = columns_list.iterator();
			while (cols_names.hasNext()) {
				String name = cols_names.next();
				writer.append(","+name);
				writer2.append(","+name);
			}
			writer.append("\n");
			writer2.append("\n");
			
			while(keys_itr.hasNext()) {
				String request= keys_itr.next();
				writer.append(request);
				writer2.append(request);
				BufferedReader reader = new BufferedReader(
						new FileReader(directory + "/" + request.split(".xml")[0] + ".txt"));
				double count = Double.parseDouble(reader.readLine().trim());
				
				Hashtable<String, ArrayList<String>> col = req_table.get(request);
				Iterator<String> curr_col = columns_list.iterator();
				while (curr_col.hasNext()) {
					String curr_col_name = curr_col.next();
					ArrayList<String> methods = col.get(curr_col_name);
					writer.append(",\"");
					writer2.append(",");
					if (methods != null) {
						// prints out the list of methods, however replace all ", " to ";" to separate methods
						// in the list
						writer.append(methods.toString().replaceAll(", ", ";"));
						writer2.append(Double.toString(methods.size() / (count-offset) * 100));
					}
					writer.append("\"");
				}
				writer.append("\n");
				writer2.append("\n");
				reader.close();
			}

			writer.close();
			writer2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
