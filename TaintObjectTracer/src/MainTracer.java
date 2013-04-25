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
	// This is useful in generating the CSV table columns in the end, as this set
	// contains all the database columns encountered for all files in "directory".
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

						// fields storing information retrieved from taintlog
						String srcClass = "";
						String srcMethod = "";
						String destClass = "";
						String destMethod = "";
						String col_name = "";
						String callerContextCounter = "";
						String calledContextCounter = "";
						String taintType = "Object";
						
						// Records all the taint source (database columns) which contribute
						// to the taint record in this taintlog element. (We use a list as
						// there can be several)
						ArrayList<String> taintSources = new ArrayList<>();
						
						// Retrieves the type attribute in the taintlog element
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
								
								// Obtain the source class name and method name, and
								// also the context counter
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
								
								// Obtain the destination class name and method name, and
								// also the context counter
								String[] full_dest_method = new String[0];
								if (!checkOmitDest(taintlogType)) {
									calledContextCounter = taintLogChildElem.getAttribute("calledContextCounter");
									destClass = taintLogChildElem.getAttribute("destClass");
									full_dest_method = taintLogChildElem.getAttribute("destMethod").split(" ");
									destMethod = full_dest_method[0] + "(";
								}
								// Generate the full destination method name including parameters
								for (int a = 1; a < full_dest_method.length; a++) {
									destMethod+=full_dest_method[a];
									if (a != full_dest_method.length - 1) {
										destMethod+=",";
									}
								}
								destMethod+=")";

								// If the advicetype attribute in the location element is NONTAINTRETURN
								// then we ignore it. This indicates that this element represents a non-tainted object
								if (taintLogChildElem.getAttribute("adviceType").equals("NONTAINTRETURN")) {
									break;
								}
							}
							// Check taintedObject element to get the column(s) returned
							else if (taintLogChildElem.getNodeName().equals("taintedObject")) {
								String currTaintType = taintLogChildElem.getAttribute("type");
								// Checks to ensure that type of object that is tainting the current XML element
								// As there can be multple tainted objects in one element, we ensure that
								// Strings takes priority over objects
								if (!(taintType.contains("String")) && currTaintType.contains("String")) {
									taintType = "String";
								}
								
								String taintRecord = taintLogChildElem.getAttribute("taintRecord");
								// According to Lee's thesis, this delimiter is used to indicate multiple 
								// taint sources
								String[] taints = taintRecord.split(" #RECSEP#");
								
								// Access each taint SOURCE
								for (int b = 0; b < taints.length; b++) {
									String taint = taints[b];
									
									// check whether a column is accessed or a whole resultset is returned
									// Also generates the column name
									// Format of column name: DB/TABLE/COL
									
									// Indicates a specific column has been accessed
									if(taint.contains("TARGETCOLUMN")) {
										// Performs String opertaions to generate name of column
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
										taintSources.add(col_name.trim());
									}
									// indicated an entire resultset is returned
									else if (taint.contains("CATALOG:")) {
										// whole resultset is returned, record all columns in rs
										String[] columns = taint.split("CATALOG:");
										for (int a = 1; a < columns.length; a++) {
											col_name = columns[a].substring(1).
													replace(" TABLE: ", "/").replace(" COLUMN: ", "/");
//											col_name = col_name + ": " + taintType;
											taintSources.add(col_name.trim());
										}
									}
									// This is supposed to treat taints from user input. However,
									// we have not seen any in this project.
									else {
										taintSources.add(taint.trim());
									}
								}
							}
						} // Finished iterating each child node
						
						// Record all unique columns accessed
						if (srcClass.contains(packageName) || destClass.contains(packageName)) {
							columns_set.addAll(taintSources);
							
							// Combine the class, full method name (with parameter types), and context counters
							String fullsrc = srcClass+"."+srcMethod+"-"+callerContextCounter;
							String fulldest = destClass+"."+destMethod+"-"+calledContextCounter;
							
							// Records the type of objects responsible for tainting each method
							addMethodByTaintType(fullsrc, taintType);
							addMethodByTaintType(fulldest, taintType);
							
							// Adds the src and dest methods to the tree
							addTopLvlNode(callerContextCounter, fullsrc, calledContextCounter, fulldest, taintlogType);
							
							// For each column, adds the current method name.
							// Note that each parent node can only have a single method, but can have
							// accessed multiple columns.
							for (int z = 0; z < taintSources.size(); z++) {
								String name = taintSources.get(z);
								// Case if the database column already exists
								if (col_table.containsKey(name)) {
									// Filter out non-application methods and then adds the
									// method its corresponding database column
									if (!col_table.get(name).contains(fullsrc) &&
											(srcClass.contains(packageName)))
										col_table.get(name).add(fullsrc);
									if (!col_table.get(name).contains(fulldest) &&
											(destClass.contains(packageName)))
										col_table.get(name).add(fulldest);
								}
								// If database column has not existed before, ensure the column
								// name is valid before proceeding
								else if (!name.equals("")) {
									ArrayList<String> list = new ArrayList<>();
									// Filter out non-application methods and then adds the
									// method its corresponding database column
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

				// Previously, all methods are classified to be tainted as by either 
				// String or objects. Now, we further classify object tainted methods into
				// type 1 or 3.
				classifyObjectTaintType();
				
				generateTaintTypeTableRows(logFile.getName());
				
				// Store the current request with a table of columns as keys and methods as values
				req_table.put(logFile.getName(), col_table);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		// Print out the percentage table and also the "taint by database column" table in csv format
		generateTable();
	}
	
	// Certain taintlog types indicates that source methods would not be considered.
	// This helper method returns true if the current type is one such type.
	// The types listed in the methods are obtained from the GUI tool from Lee
	private static boolean checkOmitSrc(String taintlogType) {
		return taintlogType.equals("FIELDGET") || taintlogType.equals("FIELDSET") || 
				taintlogType.equals("JAVAFIELDGET") || taintlogType.equals("STATICFIELDSTORE") ||
				taintlogType.equals("JAVAFIELDSET") || taintlogType.equals("FUZZYPROPAGATION");
	}
	
	// Certain taintlog types indicates that destination methods would not be considered.
	// This helper method returns true if the current type is one such type.
	// The types listed in the methods are obtained from the GUI tool from Lee
	private static boolean checkOmitDest(String taintlogType) {
		return (taintlogType.equals("RETURNINGINPUT"));
	}
	
	// Adds the methods to the tree
	private static void addTopLvlNode(String callerContextCounter, String fullSrcMethodName, 
			String calledContextCounter, String fullDestMethodName, String taintlogType) {

		// Note, callercontext corresponds to src method, and calledcontext correspond
		// to dest method
		
		// boolean values to check if the src or dest are omitted, as shown below
		boolean srcEmpty = true;
		boolean destEmpty = true;
		
		// Ensures that the sources method should not be omitted.
		// If so, records the contextcounter which maps to the source method
		if (!checkOmitSrc(taintlogType)) {
			contextCounterToMethodMap.put(callerContextCounter, fullSrcMethodName);
			srcEmpty = false;
		}
		
		// Ensures that the destination method should not be omitted.
		// If so, records the contextcounter which maps to the destination method
		if (!checkOmitDest(taintlogType)) {
			contextCounterToMethodMap.put(calledContextCounter, fullDestMethodName);
			destEmpty = false;
		}
		
		// Deals with cases where one of the methods are omitted.
		// (currently there is no case where both methods are omitted.)
		if (srcEmpty) {
			// If the calledContextCounter (for destination method) does not exist in tree nor as root,
			// then add this context as a root node with no children (a new tree)
			if (!treeNode.containsKey(calledContextCounter)) {
				if (!rootNode.containsKey(calledContextCounter)) {
					rootNode.put(calledContextCounter, new HashSet<String>());
				}
			}
			return;
		}
		else if (destEmpty) {
			// If the callerContextCounter (for source method) does not exist in tree nor as root,
			// then add this context as a root node with no children (a new tree)
			if (!treeNode.containsKey(callerContextCounter)) {
				if (!rootNode.containsKey(callerContextCounter)) {
					rootNode.put(callerContextCounter, new HashSet<String>());
				}
			}
			return;
		}
		
		// Deals with the case where the destination method already exists a root node
		// The goal is then to push every thing one level down, such that the source method
		// is the new root, and the destination method is on the next lower level.
		if (rootNode.containsKey(calledContextCounter)) {
			HashSet<String> set = (HashSet<String>) rootNode.get(calledContextCounter).clone();
			
			// If the tree node contains the destination method, then we combine the children
			// of the the root node and this tree node. Note that it is possible for root and tree
			// nodes to both contain the same method because the processing of the tree may not have 
			// been completed. Once completed, a method should only appear at one place
			if (treeNode.containsKey(calledContextCounter)) {
				treeNode.get(calledContextCounter).addAll(set);
			}
			else {
				// tree node does not contain the destination method, in which we just
				// push the root current root node down 1 level.
				treeNode.put(calledContextCounter, set);
			}
			
			// Remove the destincation method from the root node
			rootNode.remove(calledContextCounter);
			
			// now we deal with the src method
			HashSet<String> set2 = new HashSet<>();
			
			// If a root node already contains the src method, then add the dest method
			// to its current list of children. If not, just create a new one.
			if (rootNode.containsKey(callerContextCounter)) {
				set2.addAll(rootNode.get(callerContextCounter));
			}			
			set2.add(calledContextCounter);
			rootNode.put(callerContextCounter, set2);
		}
		// deals with the case where root contains src method but not dest method
		else if (rootNode.containsKey(callerContextCounter)) {
			HashSet<String> set = rootNode.get(callerContextCounter);

			// Only adds the dest method to the child of the root if it does not exist.
			// In this case, also needs to add entry to the tree node set.
			if (!set.contains(calledContextCounter)) {
				set.add(calledContextCounter);
				if (!treeNode.containsKey(calledContextCounter))
					treeNode.put(calledContextCounter, new HashSet<String>());
			}
		}
		// deals with the case where the tree (non-root) contains the src method
		else if (treeNode.containsKey(callerContextCounter)) {
			
			// adds the dest method to the child of the src method
			treeNode.get(callerContextCounter).add(calledContextCounter);
			
			// creates an entry for dest method in tree node set if it does not have it
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
	
	// Classify the object tainted methods into type 1 and 3.
	private static void classifyObjectTaintType() {
		Iterator<String> treeItr = rootNode.keySet().iterator();
		// Iterate the top level node in trees (level 0)
		// (in other words, iterate each tree)
		while(treeItr.hasNext()) {
			String toplvlcontextCounter = treeItr.next();
			
			// Keep track of the nodes iterated in a tree path
			// (top element in stack is the lowest level is tree so far)
			Stack<String> counterStack = new Stack<String>();
			counterStack.push(toplvlcontextCounter);
			
			// This set contains the nodes in the level 1 (root is level 0)
			HashSet<String> set = rootNode.get(toplvlcontextCounter);
			Iterator<String> lvlOneItr = rootNode.get(toplvlcontextCounter).iterator();
			
			// Iterates though all level 1 nodes
			while (lvlOneItr.hasNext()) {
				
				// Records the method accessed at level 1
				String lvlOneContextCounter = lvlOneItr.next();
				counterStack.push(lvlOneContextCounter);
				
				// lvlOneChildren == nodes in lvl 2
				HashSet<String> lvlOneChildren = treeNode.get(lvlOneContextCounter);
				
				// Checks if there are children are lvl 2
				if (lvlOneChildren.size() == 0) {
					
					// Checks if last the lvl 1 node is String tainted. Change the 
					// root taint type from type 3 to 1 if so.
					if (methodsTaintedByString.contains(lvlOneContextCounter) &&
							methodsTaintedByObject_type3.contains(toplvlcontextCounter)) {
						methodsTaintedByObject_type3.remove(toplvlcontextCounter);
						methodsTaintedByObject_type1.add(toplvlcontextCounter);
					}
					continue;
				}
				else {
					// Iterate through subtree (level 2 and further)
					// Note that information of root node and lvl1 node is recorded in counterStack,
					// so nothing is lost
					processSubTree(lvlOneContextCounter, counterStack);
				}
			}
		}
	}
	
	
	// Helper method to iterate through subtrees to classify type 1 and 3 object
	// tainted methods by recursion
	private static void processSubTree(String currCounter, Stack<String> stack) {
		
		// Clone the stack as we are going into different branches. We do not
		// want to overwrite information of other branches
		Stack<String> newStack = (Stack<String>) stack.clone();
		// records the current method
		newStack.push(currCounter);
		// Checks if there are any children of current node
		if (treeNode.get(currCounter).size() > 0) {
			
			// There are children at current node, recursively call this method again
			// for each child (branch into each child)
			Iterator<String> itr = treeNode.get(currCounter).iterator();
			
			while (itr.hasNext()) {
				processSubTree(itr.next(), newStack);
			}
		}
		else {
			// No more children, meaning we have reached the leaf of tree path
			
			// Indicator of whether a string tainted method has been encountered in the tree path
			boolean stringTaintInPath = false;
			
			// Checks current path from bottom to top by traversing the stack.
			// Checks whether a string tainted method is encountered. If so, 
			// any subsequent object tainted methods are converted to type 1
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
	
	// Helper method for generateTaintTypeTableRows
	// fill in the first row of the table containing the titles.
	private static void initializeTaintTypeTable(FileWriter writer) {
		try {
			writer.append(",Object_type1");
			writer.append(",Object_type3");
			writer.append(",String\n");
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	// Generate a CSV file which records information of the type of objects
	// which taints a specific method
	private static void generateTaintTypeTableRows(String rowName) throws Exception {
		
		try {
			FileWriter writer = new FileWriter(directory + "/taintedMethodByTypes.csv", true);
			
			// Initialize the first row
			if (!taintTypeTableInitialized) {
				taintTypeTableInitialized = true;
				initializeTaintTypeTable(writer);
			}
			
			// sanitize the table to check for any potential problems
			checkTaintTypeTable();
			
			// start writing out the methods for the specified request (rowName)
			// each column is a list of methods with the type (1 to 3) indicated in that column. They are
			// delimited by ";".
			writer.append(rowName+",");
			writer.append("\""+methodsTaintedByObject_type1.toString().replaceAll(", ", ";")+"\",");
			writer.append("\""+methodsTaintedByObject_type3.toString().replaceAll(", ", ";")+"\",");
			writer.append("\""+methodsTaintedByString.toString().replaceAll(", ", ";")+"\"\n");
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
	
	// Helper method to record the method and the type of object that is tainting it
	private static void addMethodByTaintType(String methodName, String taintType) {
		
		// only considers application methods
		if (!methodName.contains(packageName))
			return;
		
		// Records methods into appropriate sets
		// All object-tainted methods are considered as type 3 at first, late processing will classify
		// type 1 and 3
		if (taintType.equals("String")) {
			// Since method is now tainted by String, it overrides any previous recordings
			// of it being tainted by objects
			methodsTaintedByObject_type3.remove(methodName);
			methodsTaintedByString.add(methodName);
		}
		// only records method as tainted by object if it is not tainted by String also.
		else if (!methodsTaintedByString.contains(methodName)){
			methodsTaintedByObject_type3.add(methodName);
		}
	}
	
	// generates the csv table
	private static void generateTable() {
		
		// Gets the list of request to be processed
		Set<String> keys = req_table.keySet();
		Iterator<String> keys_itr = keys.iterator();
		// Get the list of columns to be processed, and then sort them alphabetically
		ArrayList<String> columns_list = new ArrayList(columns_set);
		Collections.sort(columns_list);
				
		try {
			// taints.csv contains the list of methods tainted by a specific database column
			// for each request. methods_percent.csv contains the proportion of application
			// methods that are tainted by that database column
			FileWriter writer = new FileWriter(directory + "/taints.csv", true);
			FileWriter writer2 = new FileWriter(directory + "/methods_percent.csv", true);

			// Initiate the first row, where the names of the database columns are written out
			Iterator<String> cols_names = columns_list.iterator();
			while (cols_names.hasNext()) {
				String name = cols_names.next();
				writer.append(","+name);
				writer2.append(","+name);
			}
			writer.append("\n");
			writer2.append("\n");
			
			// Start processing each request
			while(keys_itr.hasNext()) {
				String request= keys_itr.next();
				writer.append(request);
				writer2.append(request);
				// Parse for the text file containing the total number of methods executed so far for the
				// current request
				BufferedReader reader = new BufferedReader(
						new FileReader(directory + "/" + request.split(".xml")[0] + ".txt"));
				double total_methods_executed = Double.parseDouble(reader.readLine().trim());
				
				// Obtain the table containing the methods tainted for each database column
				Hashtable<String, ArrayList<String>> col = req_table.get(request);
				
				// Iterate each database column to be written
				// Note that we are iterating through ALL database columns recorded
				// for ALL XML files parsed. As a result, some columns may be empty for some requests
				// because there are no methods for that column.
				Iterator<String> curr_col = columns_list.iterator();
				while (curr_col.hasNext()) {
					// Get current column name
					String curr_col_name = curr_col.next();
					// Get the methods for the current database column
					ArrayList<String> methods = col.get(curr_col_name);
					writer.append(",\"");
					writer2.append(",");
					if (methods != null) {
						// prints out the list of methods, however replace all ", " to ";" to separate methods
						// in the list
						writer.append(methods.toString().replaceAll(", ", ";"));
						writer2.append(Double.toString(methods.size() / (total_methods_executed-offset) * 100));
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
