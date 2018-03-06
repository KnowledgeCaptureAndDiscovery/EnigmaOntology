package orcid;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Orcid {
	
	public static void main(String[] args) throws Exception {
        createCSV(args[0], args[1]);
	}
	
	// Creates document object from string
	public static Document loadXMLFromString(String xmlString) throws Exception {
	    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder builder = factory.newDocumentBuilder();
	    InputSource is = new InputSource(new StringReader(xmlString));
	    return builder.parse(is);
	}
	
	// Finds orcid IDs associated with passed "fname" and "lname"
	public static String getOrcidID(String fname, String lname) throws Exception {
		/*** Make API response from orcid ***/
		String urlToRead = "https://pub.orcid.org/v2.1/search/?q=family-name:"+lname+"+AND+given-names:"+fname;
		
	    StringBuilder result = new StringBuilder();
		
		URL url = new URL(urlToRead);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		
		String line;
		while ((line = rd.readLine()) != null) {
	        result.append(line);
		}
		
		/*** Parse XML response for orcid IDs and return them ***/
		Document xmlDoc = loadXMLFromString(result.toString());
		
		String orcidIDs = "";
        NodeList nList = xmlDoc.getElementsByTagName("common:orcid-identifier");
        for (int i = 0; i < nList.getLength(); i++) {
            Node node = nList.item(i);
            
            if (node.getNodeType() == Node.ELEMENT_NODE) {
               Element eElement = (Element) node;
               orcidIDs += eElement.getElementsByTagName("common:uri").item(0).getTextContent() + ";";
            }
        }
        
        if (orcidIDs != "" && orcidIDs.charAt(orcidIDs.length() - 1) == ';') {
        	orcidIDs = orcidIDs.substring(0, orcidIDs.length() - 1);
        }
        
        return orcidIDs;
	}
	
	// Creates CSV file as output, containing names and orcid IDs
	public static void createCSV(String csv, String fileDir ) throws Exception {
	    OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(fileDir), "ISO-8859-1");

	    StringBuilder sb = new StringBuilder();
		
		String splitBy = ",";
	    BufferedReader br = new BufferedReader((new InputStreamReader(new FileInputStream(csv), "ISO-8859-1")));   
	    String line;
		
		/*** Read name from csv and write to new csv ***/
	    
	    // Loop through input file, parsing each name for API call
	    while((line = br.readLine()) != null) {
	    	line = line.replaceAll("\"", "");
	    	line = line.replaceAll(" ", "");
	    	
	        String[] cells = line.split(splitBy);
	        String fname = cells[0];
	        String lname = cells[1];
	        
		    String utfFname = URLEncoder.encode(fname,"UTF-8");
		    String utfLname = URLEncoder.encode(lname,"UTF-8");
	        
		    // API call to get orcid id
	        String orcidIDs = getOrcidID(utfFname, utfLname);

	        // Prepare string builder to write to output file
	        sb.append(fname);
	        sb.append(',');
	        sb.append(lname);
	        sb.append(',');
	        sb.append(orcidIDs);
	        sb.append('\n');	        	        
	    }
	    
	    // Write to output file
	    writer.write(sb.toString());
	    writer.flush();
		writer.close();
		br.close();
	}
	
}
