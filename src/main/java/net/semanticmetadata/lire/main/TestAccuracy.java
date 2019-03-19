package net.semanticmetadata.lire.main;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import net.semanticmetadata.lire.utils.FileUtils;

public class TestAccuracy {
	private static final String DATASET_RECT = "C:/icon-dataset/rectangle";
	private static final String DATASET_CIRCLE = "C:/icon-dataset/circle";
	private static final String DATASET_UNDEF = "C:/icon-dataset/undefined";
	
	private static final int CIRCLE = 2;
	private static final int RECTANGLE = 1;
	private static final int UNDEFINED = 0;
	
	private int tp,fp,tn,fn;
	
	public TestAccuracy() {
		this.tp = 0;
		this.fp = 0;
		this.tn = 0;
		this.fn = 0;
	}
	
	public void test() throws IOException {
		ShapeDetector sd = new ShapeDetector();
		
		ArrayList<String> images_rect = FileUtils.getAllImages(new File(DATASET_RECT), true);
        int label = RECTANGLE;
		for(String img_path : images_rect) {
        	BufferedImage img = ImageIO.read(new File(img_path));
        	sd.extract(img);
        	int result = (int) sd.getFeatureVector()[0];
        	if(result == RECTANGLE && label == RECTANGLE)
        		tp++;
        	else if(result == RECTANGLE && label != RECTANGLE)
        		fp++;
        	else if(result != RECTANGLE && label == RECTANGLE)
        		fn++;
        	else if(result != RECTANGLE && label != RECTANGLE)
        		tn++;
        }
		
		ArrayList<String> images_circle = FileUtils.getAllImages(new File(DATASET_CIRCLE), true);
        label = CIRCLE;
		for(String img_path : images_circle) {
        	BufferedImage img = ImageIO.read(new File(img_path));
        	sd.extract(img);
        	int result = (int) sd.getFeatureVector()[0];
        	if(result == CIRCLE && label == CIRCLE)
        		tp++;
        	else if(result == CIRCLE && label != CIRCLE)
        		fp++;
        	else if(result != CIRCLE && label == CIRCLE)
        		fn++;
        	else if(result != CIRCLE && label != CIRCLE)
        		tn++;
        }
		
		ArrayList<String> images_undef = FileUtils.getAllImages(new File(DATASET_UNDEF), true);
        label = UNDEFINED;
		for(String img_path : images_undef) {
        	BufferedImage img = ImageIO.read(new File(img_path));
        	sd.extract(img);
        	int result = (int) sd.getFeatureVector()[0];
        	if(result == UNDEFINED && label == UNDEFINED)
        		tp++;
        	else if(result == UNDEFINED && label != UNDEFINED)
        		fp++;
        	else if(result != UNDEFINED && label == UNDEFINED)
        		fn++;
        	else if(result != UNDEFINED && label != UNDEFINED)
        		tn++;
        }
		
		double acc = (tn + tp)/(double)(tn + tp + fn + fp);
		double precision = tp / (double)(tp + fp);
		double recall = tp / (double) (tp + fn);
		System.out.println("Accuracy: "+acc);
		System.out.println("Precision: "+precision);
		System.out.println("Recall: "+recall);
	}
}
