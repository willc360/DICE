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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
	private static String directory = "C:/Users/Sentinel/Desktop/taintlog/Aries";
	private static HashSet<String> columns_set;
	private static String packageName = "org.apache.aries.samples.ariestrader";
	
	private static int offset = 7;
	
	private static HashSet<String> methodsTaintedByString;
	private static HashSet<String> methodsTaintedByObject_type1;
	private static HashSet<String> methodsTaintedByObject_type3;
	
	private static boolean taintTypeTableInitialized = false;
	
	private static String[] types = {"CALLING", "OUTPUT", "RETURNING", "RETURNINGINPUT", "STATICFIELDSTORE", 
		"FIELDSET", "FIELDGET", "JAVAFIELDSET", "JAVAFIELDGET", "FUZZYPROPAGATION", "COMPOSITION", "ASSOCIATION"};  

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		req_table = new Hashtable<>();
		columns_set = new HashSet<String>();
		
		List<String> typesList = Arrays.asList(types);

		// iterate through each request xml
		File dir = new File(directory);
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
		for (File logFile : logFiles) {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder;
			
			methodsTaintedByString = new HashSet<String>();
			methodsTaintedByObject_type1 = new HashSet<String>();
			methodsTaintedByObject_type3 = new HashSet<String>();
			try {
				docBuilder = docBuilderFactory.newDocumentBuilder();
				Document doc = docBuilder.parse(logFile);

				Element docRoot = doc.getDocumentElement();
				NodeList childNodes = docRoot.getChildNodes();

				// Stores list of methods (value) accessed for each column (key)
				Hashtable<String, ArrayList<String>> col_table = new Hashtable<String, ArrayList<String>>();
				
				// iterate each node
				for (int i = 0, length = childNodes.getLength(); i < length; i++ ) {
					if (childNodes.item(i) instanceof Element) {
						Element taintLogElem = (Element) childNodes.item(i);
						NodeList taintLogChildNodes = taintLogElem.getChildNodes();

						String srcClass = "";
						String srcMethod = "";
						String destClass = "";
						String destMethod = "";
						String col_name = "";
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
								if (!(taintlogType.equals("FIELDGET") || taintlogType.equals("FIELDSET") || 
										taintlogType.equals("JAVAFIELDGET") || taintlogType.equals("STATICFIELDSTORE") ||
										taintlogType.equals("JAVAFIELDSET") || taintlogType.equals("FUZZYPROPAGATION"))) {
									srcClass = taintLogChildElem.getAttribute("srcClass");								
									full_src_method = taintLogChildElem.getAttribute("srcMethod").split(" ");
									srcMethod = full_src_method[0] + "(";
								}
								for (int a = 1; a < full_src_method.length; a++) {
									srcMethod+=full_src_method[a];
									if (a != full_src_method.length - 1) {
										srcMethod+=",";
									}
								}
								srcMethod+=")";
								
								String[] full_dest_method = new String[0];
								if (!(taintlogType.equals("RETURNINGINPUT"))) {
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
							
							addMethodByTaintType(srcClass+"."+srcMethod, taintType);
							addMethodByTaintType(destClass+"."+destMethod, taintType);
												
							// For each column, add the current method name.
							// Note that each parent node can only have a single method, but can have
							// accessed multiple columns.
							for (int z = 0; z < taintedObjects.size(); z++) {
								String name = taintedObjects.get(z);
								if (col_table.containsKey(name)) {
									// Filter out non-Aries methods
									if (!col_table.get(name).contains(srcClass+"."+srcMethod) &&
											(srcClass.contains(packageName)))
										col_table.get(name).add(srcClass+"."+srcMethod);
									if (!col_table.get(name).contains(destClass+"."+destMethod) &&
											(destClass.contains(packageName)))
										col_table.get(name).add(destClass+"."+destMethod);
								}
								else if (!name.equals("")) {
									ArrayList<String> list = new ArrayList<>();
									if (srcClass.contains(packageName))
										list.add(srcClass+"."+srcMethod);
									if (destClass.contains(packageName))
										list.add(destClass+"."+destMethod);
									col_table.put(name, list);
								}
							}
						}
					}        	
				}
				
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
			
			writer.append(rowName+",");
			writer.append("\""+methodsTaintedByObject_type1.toString()+"\",");
			writer.append("\""+methodsTaintedByObject_type3.toString()+"\",");
			writer.append("\""+methodsTaintedByString.toString()+"\"\n");
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
			if (methodsTaintedByString.contains(str))
				throw new Exception("Method tainted by both objects and Strings");
		}
		
		if (methodsTaintedByObject_type3.contains("") || methodsTaintedByString.contains(""))
			throw new Exception("Tainted method should not be an empty string");
	}
	
	private static void addMethodByTaintType(String methodName, String taintType) {
		if (!methodName.contains(packageName))
			return;
		
		if (taintType.equals("String")) {
			methodsTaintedByString.add(methodName);
			methodsTaintedByObject_type3.remove(methodName);
			methodsTaintedByObject_type1.addAll(methodsTaintedByObject_type3);
			methodsTaintedByObject_type3.clear();
		}
		else if (!methodsTaintedByString.contains(methodName) || !methodsTaintedByObject_type1.contains(methodName)){
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
						writer.append(methods.toString());
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
