package net.semanticmetadata.lire.main;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import net.semanticmetadata.lire.builders.GlobalDocumentBuilder;
import net.semanticmetadata.lire.imageanalysis.features.global.CEDD;
import net.semanticmetadata.lire.imageanalysis.features.global.FCTH;
import net.semanticmetadata.lire.utils.FileUtils;

public class Indexer {
	private GlobalDocumentBuilder globalDocumentBuilder;
	
	public Indexer() {
		this.globalDocumentBuilder = new GlobalDocumentBuilder(false,false);
        //load feature extractors to be used to create the index
		globalDocumentBuilder.addExtractor(CEDD.class);
        globalDocumentBuilder.addExtractor(FCTH.class);
        globalDocumentBuilder.addExtractor(ShapeDetector.class); //in the image path should be present an image called background.png 400x400 transparent
       
	}
	
	/**
	 * Creates a Lucene index for the images contained in the specified folder
	 * saves it into the index folder inside the Lucene installation folder
	 * @param folder_path
	 * @throws IOException
	 */
	public void createIndex(String folder_path) throws IOException {
		File f = new File(folder_path);
		if (!f.exists() || !f.isDirectory())
			throw new IOException("Specified path is not a folder");
		
		ArrayList<String> images = FileUtils.getAllImages(f, true);
        
		IndexWriterConfig conf = new IndexWriterConfig(new WhitespaceAnalyzer());
        IndexWriter iw = new IndexWriter(FSDirectory.open(Paths.get("index")), conf);
        // Iterating through images building the low level features
        for (Iterator<String> it = images.iterator(); it.hasNext(); ) {
            String imageFilePath = it.next();
            System.out.println("Indexing " + imageFilePath);
            try {
                BufferedImage img = ImageIO.read(new FileInputStream(imageFilePath));
                Document document = globalDocumentBuilder.createDocument(img, imageFilePath);
                iw.addDocument(document);
            } catch (Exception e) {
                System.err.println("Error reading image or indexing it.");
                e.printStackTrace();
            }
        }
        // closing the IndexWriter
        iw.close();
        System.out.println("Finished indexing.");
	}
	
	/**
	 * Converts a BufferedImage object into a Lucene Document
	 * it is used when searching by image 
	 * @param img
	 * @return converted image into Document object
	 */
	public Document createDocument(BufferedImage img) {
		return globalDocumentBuilder.createDocument(img, "query_image");
	}
	
}
