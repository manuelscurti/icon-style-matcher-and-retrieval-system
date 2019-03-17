package net.semanticmetadata.lire.main;


import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import net.semanticmetadata.lire.imageanalysis.features.GlobalFeature;
import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import net.semanticmetadata.lire.utils.SerializationUtils;

public class ShapeDetector implements GlobalFeature{
	
	private static final String PATH_TO_TRASP_BACKGROUND = "res/background.png";
	private static final String PATH_TO_OPENCV_LIB = "C:/opencv/build/java/x64/opencv_java401.dll";
	protected int shape_detected = 0; // 0 undefined, 1 rectangle, 2 circle
	protected byte[] shape_byte;
	
	public ShapeDetector() {
		System.load(PATH_TO_OPENCV_LIB);
		this.shape_byte = new byte[1];
		this.shape_byte[0] = 0;
		this.shape_detected = 0;
	}
	
	/**
	 * utility function to show an image with a JPanel
	 * @param img
	 */
	public static void showResult(Mat img) {
	    MatOfByte matOfByte = new MatOfByte();
	    Imgcodecs.imencode(".jpg", img, matOfByte);
	    byte[] byteArray = matOfByte.toArray();
	    BufferedImage bufImage = null;
	    try {
	        InputStream in = new ByteArrayInputStream(byteArray);
	        bufImage = ImageIO.read(in);
	        JFrame frame = new JFrame();
	        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	        frame.getContentPane().add(new JLabel(new ImageIcon(bufImage)));
	        frame.pack();
	        frame.setVisible(true);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	// detect circle, if it's detected returns the area of the circle
	// uses HoughCircles technique, implemented in openCV
	public double detect_circle(Mat img) {
		Mat circles = new Mat();

        Imgproc.HoughCircles(img, circles, Imgproc.CV_HOUGH_GRADIENT, 1.0,
                (double)img.rows()/16, // change this value to detect circles with different distances to each other
                100.0, 30.0, 1, 300); // change the last two parameters
                // (min_radius & max_radius) to detect larger circles
        
        int max_radius = 0;
        
        for (int x = 0; x < circles.cols(); x++) {
            double[] c = circles.get(0, x);
            int radius = (int) Math.round(c[2]);
            if(radius > max_radius)
            	max_radius = radius;
        }
        
        if(max_radius > 0)
        	return max_radius*max_radius*Math.PI;
        return 0;
	}
	
	// detect rectangle, if it's detected returns the area of the rectangle
	// uses findContours method from openCV
	public double detect_rectangle(Mat img){	
		List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(img,contours,new Mat(),Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = 0;
        MatOfPoint max_contour = new MatOfPoint();

        Iterator<MatOfPoint> iterator = contours.iterator();
        while (iterator.hasNext()){
        	MatOfPoint contour = iterator.next();
            double area = Imgproc.contourArea(contour);
            if(area > maxArea){
            	maxArea = area;
                max_contour = contour;
            }
        }
        
        MatOfPoint2f c = new MatOfPoint2f(max_contour.toArray());
		
		double peri = Imgproc.arcLength(c, true);
		MatOfPoint2f approx = new MatOfPoint2f();
		Imgproc.approxPolyDP(c,approx,0.04*peri,true);
		long count = approx.total();
		
		
		
		if (count >= 4) {
			return Imgproc.contourArea(c);
		} 
		return 0;
	}
	
	//preprocess consists of:
	// - resize img
	// - paste img into bigger transparent background
	// - apply gray scale transformation, gaussian blur and thresholding
	public Mat preprocess(Mat img) {
		
		Mat proc = new Mat();
		img.copyTo(proc);
		
		if(img.cols() > 300)
			Imgproc.resize(img, proc, new Size(300, 300));
		
		Mat background = Imgcodecs.imread(PATH_TO_TRASP_BACKGROUND, Imgcodecs.IMREAD_UNCHANGED);
		proc = overlayImage(background, proc, new Point(50,50));
		//showResult(proc);
		
		Imgproc.cvtColor(proc, proc, Imgproc.COLOR_BGR2GRAY); //gray scale
		Imgproc.GaussianBlur(proc, proc, new Size(5,5), 0); // gaussian filter
		Imgproc.adaptiveThreshold(proc, proc, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 21, 2);
		
		return proc;
	}
	
	/**
	 * overlays the foreground into the background image at the specified location
	 * @param background
	 * @param foreground
	 * @param location
	 * @return
	 */
	public Mat overlayImage(Mat background,Mat foreground, Point location){
		Mat output = new Mat();
		background.copyTo(output);

		for(int y = (int) Math.max(location.y , 0); y < background.rows(); ++y){

		   int fY = (int) (y - location.y);

		   if(fY >= foreground.rows())
		          break;

		      for(int x = (int) Math.max(location.x, 0); x < background.cols(); ++x){
		          int fX = (int) (x - location.x);
		          if(fX >= foreground.cols()){
		           break;
		          }

		           double opacity;
		           double[] finalPixelValue = new double[4];
		           
		           try {
		        	   opacity = foreground.get(fY , fX)[3];
		           } catch(ArrayIndexOutOfBoundsException e) {
		        	   opacity = 255;
		           }
		           finalPixelValue[0] = background.get(y, x)[0];
		           finalPixelValue[1] = background.get(y, x)[1];
		           finalPixelValue[2] = background.get(y, x)[2];
		           finalPixelValue[3] = background.get(y, x)[3];

		           for(int c = 0;  c < output.channels(); ++c){
		               if(opacity > 0){
		            	   double foregroundPx, backgroundPx;
		            	   try {
		            		   foregroundPx =  foreground.get(fY, fX)[c];
		            	   } catch(ArrayIndexOutOfBoundsException e) {
		            		   foregroundPx =  0;
				           }
		            	   
		            	   try {
		            		   backgroundPx =  background.get(fY, fX)[c];
		            	   } catch(ArrayIndexOutOfBoundsException e) {
		            		   backgroundPx =  0;
				           }
		            	   
		                   float fOpacity = (float) (opacity / 255);
		                   finalPixelValue[c] = ((backgroundPx * ( 1.0 - fOpacity)) + (foregroundPx * fOpacity));
		                   if(c==3){
		                	   try {
		                       finalPixelValue[c] = foreground.get(fY,fX)[3];
		                	   }catch(ArrayIndexOutOfBoundsException e) {
			            		   finalPixelValue[c] =  0;
					           }
		                   }
		               }
		           }
		           output.put(y, x,finalPixelValue);
		      }
		} 
		return output;
	}
	
	/**
	 * utility function to convert a bufferedimage class object into a Mat class object
	 * @param image
	 * @return
	 * @throws IOException
	 */
	public Mat bufferedImage2Mat(BufferedImage image) throws IOException {
	    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	    ImageIO.write(image, "png", byteArrayOutputStream);
	    byteArrayOutputStream.flush();
	    return Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_UNCHANGED);
	}
	
	@Override
	public byte[] getByteArrayRepresentation() {
		shape_byte[0] = (byte)shape_detected;
		return shape_byte;
	}

	@Override
	public double getDistance(LireFeature arg0) {
		if(!(arg0 instanceof ShapeDetector))
			throw new UnsupportedOperationException("Wrong descriptor.");
		
		ShapeDetector tmpFeature = (ShapeDetector)arg0;
        if ((tmpFeature.shape_byte.length!= this.shape_byte.length) || tmpFeature.shape_byte == null)
            throw new UnsupportedOperationException("Shape byte do not match");
		
		double dist = (double)Math.abs(tmpFeature.shape_detected - this.shape_detected);
		//scale to range 0 to 100
		double num_labels = 3; //rectangle,circle,undefined
		
		return (dist * 100)/num_labels;
	}

	@Override
	public String getFeatureName() {
		return "ShapeDetector-Beta";
	}

	@Override
	public String getFieldName() {
		return "ShapeDetector";
	}

	@Override
	public void setByteArrayRepresentation(byte[] in) {
		shape_byte[0] = in[0];
		shape_detected = (int)shape_byte[0];
	}

	@Override
	public void setByteArrayRepresentation(byte[] in, int arg1, int arg2) {
		shape_byte[0] = in[0];
		shape_detected = (int)shape_byte[0];
	}

	@Override
	public double[] getFeatureVector() {
		return SerializationUtils.castToDoubleArray(shape_byte);
	}

	@Override
	public void extract(BufferedImage arg0) {
		Mat img = new Mat();
		try {
			img = bufferedImage2Mat(arg0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Mat proc = this.preprocess(img);
		//showResult(proc);
		
		double max_circle_area = this.detect_circle(proc);
		double max_rect_area = this.detect_rectangle(proc);
		double img_area = proc.cols() * proc.rows();
		double threshold_validation_max = img_area; // 300*300 (max icon size)
		double threshold_validation_min_circle = threshold_validation_max / 3; //x are calibrated parameter
		double threshold_validation_min_rect = threshold_validation_max / 2.5;
		
		if(max_circle_area >= threshold_validation_min_circle && max_rect_area < threshold_validation_min_rect)
			//System.out.println("circle");
			shape_detected = 2;
		else if(max_circle_area < threshold_validation_min_circle && max_rect_area >= threshold_validation_min_rect)
			//System.out.println("rectangle");
			shape_detected = 1;
		else if(max_circle_area >= threshold_validation_min_circle && max_rect_area >= threshold_validation_min_rect) {
			if(max_circle_area > max_rect_area)
				//System.out.println("circle");
				shape_detected = 2;
			else if(max_circle_area < max_rect_area)
				//System.out.println("rectangle");
				shape_detected = 1;
			else
				//System.out.println("undefined");
				shape_detected = 0;
		} else {
			//System.out.println("undefined");
			shape_detected = 0;
		}
		
		
		shape_byte[0] = (byte)shape_detected;
	}

}
