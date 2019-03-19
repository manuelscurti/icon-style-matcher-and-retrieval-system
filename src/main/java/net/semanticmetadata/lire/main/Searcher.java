package net.semanticmetadata.lire.main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import net.semanticmetadata.lire.builders.DocumentBuilder;
import net.semanticmetadata.lire.filters.RerankFilter;
import net.semanticmetadata.lire.imageanalysis.features.global.CEDD;
import net.semanticmetadata.lire.imageanalysis.features.global.FCTH;
import net.semanticmetadata.lire.searchers.GenericFastImageSearcher;
import net.semanticmetadata.lire.searchers.ImageSearchHits;
import net.semanticmetadata.lire.searchers.ImageSearcher;

public class Searcher {
	
	public Searcher() {
		
	}
	
	/**
	 * Searches for similiar images. takes as input a Document object representing the indexed image
	 * Returns a sorted set of key-value pairs ordered by relevance. 
	 * @param query
	 * @return search results
	 * @throws IOException
	 */
	public Map<String, Double> search(Document query) throws IOException{
		Map<String, Double> results = new HashMap<>();

        IndexReader ir = DirectoryReader.open(FSDirectory.open(Paths.get("index")));
        //search images
        ImageSearcher searcher = new GenericFastImageSearcher(100, FCTH.class);
        ImageSearchHits hits = searcher.search(query, ir);
        
        //re-rank based on other features
        RerankFilter filter_cedd = new RerankFilter(CEDD.class, DocumentBuilder.FIELD_NAME_CEDD);
        hits = filter_cedd.filter(hits, ir, query);
        
        RerankFilter filter_shape = new RerankFilter(ShapeDetector.class, "ShapeDetector");
        hits = filter_shape.filter(hits, ir, query);

        for (int i = 0; i < hits.length(); i++) {
            String fileName = ir.document(hits.documentID(i)).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];
            if(!results.containsKey(fileName))
            	results.put(fileName, hits.score(i));
        }

        //sort the results
	    Map<String, Double> sorted = results
	            .entrySet()
	            .stream()
	            .sorted(Map.Entry.comparingByValue())
	            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (e1, e2) -> e2,LinkedHashMap::new));
        
        return sorted;
	}
	
	/**
	 * Utility function to print the results in a human readable format
	 * @param results
	 */
	public void printResults(Map<String, Double> results) {

	    for(Map.Entry<String, Double> e : results.entrySet()) 
	    	System.out.println(e.getKey() + " - score: "+e.getValue());
	    
	}
	
	/**
	 * Save results in HTML format in the specified path folder
	 * @param searchimg - name of the query image
	 * @param path - folder path where to save the result
	 * @param results - set of key value pairs representing the results of the search
	 * @throws IOException
	 */
	public void saveResults(String searchimg, String path, Map<String, Double> results) throws IOException {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());

		BufferedWriter writer = new BufferedWriter(new FileWriter(path+"results-"+timestamp.getTime()+".html"));
		
		writer.write("<html><body>Query:<br><img src='file:///"+searchimg+"' width=100 height=100></img><br>Results:<br><table><tr>");
		int j = 0;
	    for(Map.Entry<String, Double> e : results.entrySet()) { 
	    	if(j < 5) {
	    		writer.write("<td>"+Math.round(e.getValue())+"<img src='file:///"+e.getKey()+"' width=100 height=100></img></td>");
	    		j++;
	    	} else {
	    		j = 0;
	    		writer.write("</tr><tr>");
	    	}
	    }
	    writer.write("</tr></table></body></html>");
	    
	    writer.close();
	    
	}
	
}
