package net.semanticmetadata.lire.main;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.lucene.document.Document;

public class Main {
	
	public static void search(String img_path, String save_path) throws IOException{
        BufferedImage img = null;
        boolean passed = false;
        File f = new File(img_path);
        if (f.exists()) {
                img = ImageIO.read(f);
                passed = true;
        }
        
        if (!passed) {
            System.out.println("No image given as second argument.");
            System.exit(1);
        }
        
		//initialize searcher and indexer
    	Searcher searcher = new Searcher();
    	Indexer indexer = new Indexer();
		
		Map<String, Double> results = null;
		
    	//convert image to index format
    	Document query = indexer.createDocument(img);

		results = searcher.search(query);

		
    	if(results!=null) {
    		searcher.printResults(results);
			searcher.saveResults(img_path,save_path, results);
    	}
    	else
    		throw new IOException();
	}
	
	public static void index(String images_path) throws IOException {
		Indexer indexer = new Indexer();
		
		indexer.createIndex(images_path);
	}
	
	
    public static void main(String[] args) {
    	// args:
    	// 0 - usage case: index, search
    	// 1 - folder path to images being indexed OR image to search for similiar images
    	// 2 - (if searching) folder path to where store the results
    	
    	if (args.length < 1) {
    		System.out.println("usage: <usage index or search> <image folder path OR image to search> <in case of search, folder to save results>");
    		System.exit(1);
    	}
    	
    	if(args[0].equals("index")) {
    		try {
    			if(args.length > 0) {
    				File f = new File(args[1]);
    				if(f.exists() && f.isDirectory())
    					index(args[1]);
    			}
    		} catch (IOException e) {
    			e.printStackTrace();
    		} 
    		
    	} else if(args[0].equals("search")) {
            try {
				search(args[1], args[2]);
			} catch (IOException e) {
				e.printStackTrace();
			}
            
    	}
    	

    }
}
