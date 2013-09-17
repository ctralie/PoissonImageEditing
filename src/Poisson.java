//Purpose: To provide a front end GUI for cutting and pasting images
//onto each other in the Poisson Image Editing Application
//(c) Chris Tralie, 2012

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;
import java.awt.image.*;

public class Poisson extends JApplet implements ActionListener, WindowListener,
										MouseListener, MouseMotionListener {
	//Display parameters
	public static final int Width = 1200;
	public static final int Height = 800;

	//Program state
	public static final int NOTHING = 0;
	public static final int SELECTING = 1;
	public static final int DRAGGING = 2;
	public static final int BLENDING = 3;
	
	//Menu Options
	public static final String SELECT_REGION = "Select Region";
	public static final String BLEND_SELECTION = "Blend Selection";
	public static final String FLATTEN_SELECTION = "Flatten Selection";
	public static final String SELECT_IMAGE1 = "Select Left Image";
	public static final String SELECT_IMAGE2 = "Select Right Image";
	public static final String STOP = "STOP";
	
	//GUI Widgets
	public Display canvas;
	public JMenuBar menu;
	JMenu fileMenu;
	public BufferedImage image;
	
	//Variables for selected image
	public int[][] mask;//A 2D array that represents a selected region
	//It encodes the enclosed region and the border of that region
	public ArrayList<Coord> selectionBorder;
	public ArrayList<Coord> selectionArea;
	public BufferedImage selectedImage;
	int xMin, xMax, yMin, yMax;//Bounding box of selected area
	
	//GUI State Variables
	public int state;
	public boolean dragValid;
	public int lastX, lastY;
	public int dx, dy;
	public boolean selectingLeft;
	public boolean doneAnything = false;
	
	//Matrix solver
	public MatrixSolver solver;
	public Thread blendingThread;
	public JProgressBar progressBar;
	
	//-2 for uninvolved pixels
	//-1 for border pixels
	//Index number for area pixels
	//This function also moves everything over
	void updateMask() {
		//Clip the motion to the display window
		if (xMin + dx < 1)
			dx = 1 - xMin;
		if (xMax + dx > Width - 1)
			dx = Width - 1 - xMax;
		if (yMin + dy < 1)
			dy = 1 - yMin;
		if (yMax + dy > Height - 1)
			dy = Height - 1 - yMax;
		//Now update the mask
		for (int x = 0; x < Width; x++) {
			for (int y = 0; y < Height; y++)
				mask[x][y] = -2;
		}
		for (int i = 0; i < selectionBorder.size(); i++) {
			int x = selectionBorder.get(i).x + dx;
			int y = selectionBorder.get(i).y + dy;
			selectionBorder.get(i).x = x;
			selectionBorder.get(i).y = y;
			mask[x][y] = -1;
		}
		for (int i = 0; i < selectionArea.size(); i++) {
			int x = selectionArea.get(i).x + dx;
			int y = selectionArea.get(i).y + dy;
			selectionArea.get(i).x = x;
			selectionArea.get(i).y = y;
			mask[x][y] = i;
		}
		xMin += dx; xMax += dx;
		yMin += dy; yMax += dy;
		dx = 0;
		dy = 0;
	}
	
	public void init() {
		mask = new int[Width][Height];
		for (int x = 0; x < Width; x++) {
			for (int y = 0; y < Height; y++)
				mask[x][y] = 0;
		}
		selectionArea = new ArrayList<Coord>();
		selectionBorder = new ArrayList<Coord>();
		
		Container content = getContentPane();
		content.setLayout(null);
		
		menu = new JMenuBar();
		fileMenu = new JMenu("File");
		fileMenu.addActionListener(this);
		fileMenu.add(SELECT_IMAGE1).addActionListener(this);
		fileMenu.add(SELECT_IMAGE2).addActionListener(this);
		fileMenu.add(SELECT_REGION).addActionListener(this);
		fileMenu.add(BLEND_SELECTION).addActionListener(this);
		fileMenu.add(FLATTEN_SELECTION).addActionListener(this);
		//fileMenu.add(STOP).addActionListener(this);
		menu.add(fileMenu);
		
		menu.setBounds(0, 0, Width, 20);
		content.add(menu);
		
		canvas = new Display();
		canvas.setSize(Width, Height);
		canvas.addMouseMotionListener(this);
		canvas.addMouseListener(this);
		canvas.setBounds(0, 50, Width, 50+Height);
		content.add(canvas);
		
		progressBar = new JProgressBar(SwingConstants.HORIZONTAL, 0, 100);
		progressBar.setBounds(0, 20, Width, 50);
		progressBar.setStringPainted(true);
		progressBar.setValue(0);
		content.add(progressBar);
		
		state = NOTHING;
		image = new BufferedImage(Width, Height, BufferedImage.TYPE_INT_RGB);
		Graphics g = image.getGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, Width, Height);
		selectedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		dx = 0;
		dy = 0;
	}

	public class Display extends JPanel {
		public void paintComponent(Graphics g) {
			g.drawImage(image, 0, 0, this);
			g.setColor(Color.RED);
			/*for (int x = 0; x < Width; x++) {
				for (int y = 0; y < Height; y++) {
					if (mask[x][y] == -1) {
						g.drawLine(x, y, x, y);
					}
				}
			}*/
			if (state == SELECTING || state == DRAGGING) {
				for (int i = 0; i < selectionBorder.size(); i++) {
					int x = selectionBorder.get(i).x + dx;
					int y = selectionBorder.get(i).y + dy;
					g.drawLine(x, y, x, y);
				}
			}
			g.setColor(Color.BLUE);
			for (int i = 0; i < selectionArea.size(); i++) {
				int x = selectionArea.get(i).x;
				int y = selectionArea.get(i).y;
				g.drawLine(x, y, x, y);
			}
			g.drawImage(selectedImage, xMin+dx, yMin+dy, this);
			//g.setColor(Color.GREEN);
			//g.drawRect(xMin, yMin, xMax-xMin, yMax-yMin);
			if (!doneAnything) {
				//Give directions
				g.setColor(Color.RED);
				g.drawString("Welcome!  Please click on the \"File\" menu to get started.  A typical run is as follows:", 200, 200);
				g.drawString("1) File -> Select left image", 200, 220);
				g.drawString("2) A GUI Pops up where you can select an image and manipulate it.  Click \"Select Image\" at the bottom when you're done", 200, 240);
				g.drawString("3) Do the same for File -> Select right image", 200, 260);
				g.drawString("4) Now that both images are there click File -> Select Region", 200, 280);
				g.drawString("drag select an area of the image you would like to cut out", 200, 300);
				g.drawString("5) Drag your selected region to another part of the image where you would like to blend it", 200, 320);
				g.drawString("6) Click File -> Blend Selection and wait for it to finish", 200, 340);
				g.drawString("7) Enjoy!  And post the results online or send them to me if they're interesting", 200, 360);
				g.drawString("(c) Chris Tralie (chris.tralie@gmail.com), 2012", 200, 380);
			}
		}
	}
	
	public void actionPerformed(ActionEvent evt) {
		doneAnything = true;
		String str = evt.getActionCommand();
		if (state == BLENDING)
			return;
		if (str.equals(SELECT_REGION)) {
			//Clear previous selection
			selectionBorder.clear();
			selectionArea.clear();
			state = SELECTING;
		}
		else if (str.equals(BLEND_SELECTION)) {
			state = BLENDING;
			updateMask();
			solver = new MatrixSolver(mask, selectionArea, image, selectedImage,
									  xMin, yMin, Width, Height, false);
			IterationBlender blender = new IterationBlender();
			blendingThread = new Thread(blender);
			blendingThread.start();
		}
		else if (str.equals(FLATTEN_SELECTION)) {
			state = BLENDING;
			updateMask();
			solver = new MatrixSolver(mask, selectionArea, image, selectedImage,
									  xMin, yMin, Width, Height, true);
			IterationBlender blender = new IterationBlender();
			blendingThread = new Thread(blender);
			blendingThread.start();
		}
		else if (str.equals(SELECT_IMAGE1)) {
			ImageSelector selector = new ImageSelector();
			selector.addWindowListener(this);
			selectingLeft = true;
		}
		else if (str.equals(SELECT_IMAGE2)) {
			ImageSelector selector = new ImageSelector();
			selector.addWindowListener(this);
			selectingLeft = false;
		}
		else if (str.equals(STOP)) {
			if (state == BLENDING) {
				blendingThread.stop();
				state = NOTHING;
				finalizeBlending();
			}
		}
		canvas.repaint();
	}
	
	public void mouseMoved(MouseEvent evt) {
		lastX = evt.getX();
		lastY = evt.getY();
	}

	public void mouseDragged(MouseEvent evt) {
		int x = evt.getX();
		int y = evt.getY();
		if (state == SELECTING) {
			selectionBorder.add(new Coord(x, y));
		}
		else if (state == DRAGGING) {
			//Make sure the user is dragging within the bounds of the selection
			if (!dragValid) {
				if (mask[x][y] >= 0) {
					dragValid = true;
				}
			}
			if (dragValid) {
				dx += (x-lastX);
				dy += (y-lastY);
			}
		}
		lastX = x;
		lastY = y;
		canvas.repaint();
	}
	
	void fillOutside(int paramx, int paramy) {
		ArrayList<Coord> stack = new ArrayList<Coord>();
		stack.add(new Coord(paramx, paramy));
		while (stack.size() > 0) {
			Coord c = stack.remove(stack.size()-1);
			int x = c.x, y = c.y;
			if (x < 0 || x >= Width || y < 0 || y >= Height)
				continue;
			if (mask[x][y] == -1) //Stop at border pixels
				continue;
			if (mask[x][y] == 0) //Don't repeat nodes that have already been visited
				continue;
			mask[x][y] = 0;
			stack.add(new Coord(x-1, y));
			stack.add(new Coord(x+1, y));
			stack.add(new Coord(x, y-1));
			stack.add(new Coord(x, y+1));
		}
	}
	
	public void getSelectionArea() {
		selectionArea.clear();
		updateMask();
		//Find bounding box of selected region
		xMin = Width;
		xMax = 0;
		yMin = Height;
		yMax = 0;
		for (int i = 0; i < selectionBorder.size(); i++) {
			int x = selectionBorder.get(i).x;
			int y = selectionBorder.get(i).y;
			if (x < xMin)
				xMin = x;
			if (x > xMax)
				xMax = x;
			if (y < yMin)
				yMin = y;
			if (y > yMax)
				yMax = y;
		}
		int selWidth = xMax - xMin;
		int selHeight = yMax - yMin;
		selectedImage = new BufferedImage(selWidth, selHeight, BufferedImage.TYPE_INT_ARGB);
		//Find a pixel outside of the bounding box, which is guaranteed
		//to be outside of the selection
		boolean found = false;
		for (int x = 0; x < Width && !found; x++) {
			for (int y = 0; y < Height && !found; y++) {
				if ((x < xMin || x > xMax) && (y < yMin || y > yMax)) {
					found = true;
					fillOutside(x, y);
				}
			}
		}
		//Pixels in selection area have mask value of -2, outside have mask value of 0
		for (int x = 0; x < Width; x++) {
			for (int y = 0; y < Height; y++) {
				if (x - xMin >= 0 && y - yMin >= 0 && x - xMin < selWidth && y - yMin < selHeight)
					selectedImage.setRGB(x-xMin, y-yMin, image.getRGB(x,y)&0x00FFFFFF);
				if (mask[x][y] == 0) {
					mask[x][y] = -2;
				}
				else if (mask[x][y] != -1) {
					mask[x][y] = selectionArea.size();//Make mask index of this coord
					selectionArea.add(new Coord(x, y));
					int color = (255 << 24) | image.getRGB(x, y);
					if (x - xMin >= 0 && y - yMin >= 0)
						selectedImage.setRGB(x-xMin, y-yMin, color);
				}
			}
		}
		updateMask();
	}
	
	public void mouseReleased(MouseEvent evt) {
		//Fill in pixels in between and connect the first to the last
		int N = selectionBorder.size();		
		if (N == 0 || (state != SELECTING && state != DRAGGING))
			return;
			
		if (state == SELECTING) {
			for (int n = 0; n < N; n++) {
				int startx = selectionBorder.get(n).x;
				int starty = selectionBorder.get(n).y;
				int totalDX = selectionBorder.get((n+1)%N).x - startx;
				int totalDY = selectionBorder.get((n+1)%N).y - starty;
				int numAdded = Math.abs(totalDX) + Math.abs(totalDY);
				for (int t = 0; t < numAdded; t++) {
					double frac = (double)t / (double)numAdded;
					int x = (int)Math.round(frac*totalDX) + startx;
					int y = (int)Math.round(frac*totalDY) + starty;
					selectionBorder.add(new Coord(x, y));
				}
			}
			/*selection.clear();
			for (int x = 0; x < Width; x++) {
				for (int y = 0; y < Height; y++) {
					if (mask[x][y])
						selection.add(new Coord(x, y));
				}
			}*/
			updateMask();
			getSelectionArea();
			state = DRAGGING;
			dragValid = false;
			dx = 0;
			dy = 0;
		}
		else if (state == DRAGGING) {
			dragValid = false;
			updateMask();
		}
		canvas.repaint();
	}
	
	public void nextIteration() {
		for (int i = 0; i < 100; i++)
			solver.nextIteration();
		synchronized(selectedImage) {
			solver.updateImage(selectedImage);
		}
		canvas.repaint();	
	}
	
	public void finalizeBlending() {
		Graphics g = image.getGraphics();
		g.drawImage(selectedImage, xMin, yMin, null);
		selectedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		selectionBorder.clear();
		selectionArea.clear();
		state = NOTHING;
	}
	
	class IterationBlender implements Runnable {
		public void run() {
			int iteration = 0;
			double error;
			double Norm = 1.0;
			progressBar.setValue(0);
			do {
				error = solver.getError();
				if (iteration == 1)
					Norm = Math.log(error);
				if (iteration >= 1) {
					double progress = 1.0 - Math.log(error) / Norm;
					progressBar.setValue((int)(progress*100));
					progressBar.repaint();
				}
				iteration++;
				nextIteration();
			}
			while (error > 1.0 && state == BLENDING);
			finalizeBlending();
		}
	}
	
	public void mouseClicked(MouseEvent evt){}
	public void mouseEntered(MouseEvent evt){}
	public void mouseExited(MouseEvent evt){}
	public void mousePressed(MouseEvent evt) {}
	
    public void windowActivated(WindowEvent evt){}
    public void windowClosed(WindowEvent evt){
    	ImageSelector selector = (ImageSelector)evt.getSource();
    	Graphics g = image.getGraphics();
    	int startX = Width/2;
    	if (selectingLeft)
			startX = 0;
		g.setColor(Color.WHITE);
		g.fillRect(startX, 0, startX + Width/2, Height);
		g.drawImage(selector.currentImage, startX, 0, this);
		canvas.repaint();
    }
    public void windowClosing(WindowEvent evt){}
    public void windowDeactivated(WindowEvent evt){}
    public void windowDeiconified(WindowEvent evt) {}
    public void windowIconified(WindowEvent evt){}
    public void windowOpened(WindowEvent evt){}

	public static void main(String[] args) {
		new Poisson();
	}

}
