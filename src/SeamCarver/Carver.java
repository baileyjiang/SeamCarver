package SeamCarver;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * Seam Carver Class. Takes in an image and carves seams based on user input.
 * 
 * @author Bailey Jiang
 * @author Alexander Gottlieb
 *
 */
public class Carver {
	public static BufferedImage image;
	private static File file;

	public static void main (String args []) throws InvocationTargetException, InterruptedException {
		//Create a file chooser
		
		//In response to a button click:
		// Used EventQueue since JFileChooser on OSX is buggy.
		EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JFileChooser fc = new JFileChooser();
                fc.showOpenDialog(null);
                file = fc.getSelectedFile();
            }
        });

		try {	
			image = ImageIO.read(file);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String userwidth = JOptionPane.showInputDialog("Input width to cut from new image.");
		int wid = Integer.parseInt(userwidth);
		if(wid > image.getWidth()) {
			JOptionPane.showMessageDialog(null, "Invalid width! Must be less than image width");
			System.exit(0);
		}
		String userheight = JOptionPane.showInputDialog("Input height to cut from new image.");
		int hei = Integer.parseInt(userheight);
		if(hei > image.getHeight()) {
			JOptionPane.showMessageDialog(null, "Invalid height! Must be less than image height");
			System.exit(0);
		}
		
		
		for(int i=0; i<hei; i++){
		carve(image);
		}
		image = rotate(image);
		for(int i=0; i<wid; i++){
		carve(image);
		}
		image = rotate(image);
		
		
		EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JFileChooser fc2 = new JFileChooser();
                fc2.showOpenDialog(null);
                file = fc2.getSelectedFile();
            }
        });
		try {
			ImageIO.write(image, "jpeg", file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
	/**
	 * Cuts out a seam from the image based on the lowest cost.
	 * @param img - Input image
	 */
	public static void carve(BufferedImage img){
		BufferedImage result =  new BufferedImage(img.getWidth(), img.getHeight() - 1, BufferedImage.TYPE_INT_RGB);
		int width = img.getWidth();
		int height = img.getHeight();
		//cost of each pixel
		int[][] cost = new int [width][height];
		//cost of a seam from left to right
		int[][] seamcost = new int[width][height];
		//final seam path
		int[] seampath = new int [width];

		//First and last column edge cases for filling energy array
		for(int j = 0; j < height; j++){
			cost[0][j] += energy(img.getRGB(0, j), img.getRGB(1, j));
			cost[width-1][j] += energy(img.getRGB(width-2, j), img.getRGB(width-1, j));
		}
		//first and last row edge cases for filling energy array
		for(int i = 0; i < width; i++){
			cost[i][0] += energy(img.getRGB(i, 0), img.getRGB(i, 1));
			cost[i][height-1] += energy(img.getRGB(i, height-2), img.getRGB(i, height-1));
		}
		//remaining interior pixel values
		for(int i = 1; i < width-1; i++)
			for(int j = 1; j < height-1; j++){
				cost[i][j] += energy(img.getRGB(i-1, j), img.getRGB(i+1, j));
				cost[i][j] += energy(img.getRGB(i, j-1), img.getRGB(i, j+1));
			}
		//set first column of seamcost array to left most value
		for(int i = 0; i < height; i++){
			seamcost[0][i] = cost[0][i];
		}
		/*
		 * These lines use the Math.min java method to take a starting pixel and find the minimum seam from
		 * that pixel by choosing only the least expensive path. The ending sums can then be compared to find
		 * the least expensive total seam.
		 */
		//remaining interior
		for(int i = 1; i < width; i++){
			for(int j = 1; j < height - 1; j++){
				seamcost[i][j] = cost[i][j] + Math.min(seamcost[i-1][j], Math.min(seamcost[i-1][j-1], seamcost[i-1][j+1]));
			}
			//first and last row edge cases
			seamcost[i][0] = cost[i][0] + Math.min(seamcost[i-1][0], seamcost[i-1][1]);
			seamcost[i][height-1] = cost[i][height-1] + Math.min(seamcost[i-1][height-1], seamcost[i-1][height-2]);
		}
		//find cheapest seam
		int totalseam = 0;
		for (int i =1; i < height; i++){
			if (seamcost[width-1][totalseam] >seamcost[width-1][i]){
				totalseam = i;
			}
		seampath[width-1]= totalseam;
		}
		//pack seampath with cheapest seam
		for(int i = width-2; i >= 0; i--){
			int temp = totalseam;
			if(totalseam < height-1 && seamcost[i][totalseam+1] < seamcost[i][totalseam]){
				temp = totalseam+1;
			}
			if(totalseam > 0 && seamcost[i][totalseam-1] < seamcost[i][temp]){
				temp = totalseam-1;
			}
			totalseam = temp;
			seampath[i] = totalseam;
		}
		//Remove seam from image
		for(int x = 0; x < width; x++){
			for(int y = 0; y < seampath[x]; y++)
				result.setRGB(x, y, img.getRGB(x, y));
			for(int y = seampath[x] + 1; y < height; y++)
				result.setRGB(x, y-1, img.getRGB(x, y));
		}
		image = result;
	}
	/**
	 * Calculates the energy. Taken from Dr. Bargteil's EdgeDetection.java.
	 * @param x - Input RGB
	 * @param y - Input RGB
	 * @return - Returns the energy value.
	 */
	public static int energy (int x, int y) {
		Color color1 = new Color(x);
		Color color2 = new Color(y);
		return Math.abs(color1.getRed()-color2.getRed())+Math.abs(color1.getGreen()-color2.getGreen())+Math.abs(color1.getBlue()-color2.getBlue());
	}
	/**
	 * Rotates the buffered image.
	 * @param img - Input image
	 * @return - Returns a rotated image.
	 */
	public static BufferedImage rotate(BufferedImage img) {
		BufferedImage result =  new BufferedImage(img.getHeight(), img.getWidth(), BufferedImage.TYPE_INT_RGB);
	    for(int i = 0; i < img.getWidth(); i++) {
	    	for(int j = 0; j < img.getHeight(); j++) {
	    		result.setRGB(j, i, img.getRGB(i, j));
	    	}
	    }
	    return result;
	}

}