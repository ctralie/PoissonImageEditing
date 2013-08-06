//A GUI for selecting, resizing, rotating, and flipping images that are
//used in the Poisson Image Editing application
//(c) Chris Tralie, 2012
import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.JOptionPane;
import java.awt.image.*;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

public class ImageSelector extends JFrame implements ActionListener, DocumentListener {
	public String[] imageNames = {"apples.jpg", "BarackObama.jpg", "beach.jpg", "beach2.jpg", "carlton.jpg", "formals.jpg", "GreatWhiteShark.jpg", "harrypottercast.jpg", "johannes.jpg", "LilWayne.jpg", "LionYawning.jpg", "me_headphones.jpg", "me_lookingforward.jpg", "MichaelJackson_Bad.jpg", "NicholasCage.jpg", "oranges.jpg", "PoeField.jpg", "prade.jpg", "wedding.jpg", "willuncle.jpg"};
	public JComboBox imageList;
	public JTextField field;
	public JTextField percentScaleText, degreesRotateText;
	public JButton selectURLButton;
	public JButton selectFileButton;
	public JButton selectImageButton;
	public JButton increaseSizeButton;
	public JButton decreaseSizeButton;
	public JButton rotateRightButton;
	public JButton rotateLeftButton;
	public JButton flipVertButton;
	public JButton flipHorizButton;
	public Display canvas;
	public BufferedImage currentImage = null;
	public BufferedImage originalImage = null;
	//Image resizing and rotating properties
	public double scale = 1.0;
	public double rot = 0.0;
	
	//Have a variable that helps remember the last file that was opened
	public static String lastFilename = "";

    public void readImage(String path, boolean absolute) {
		try {
			URL url;
			if (absolute)
				url = new URL(path);
			else
				url = getClass().getResource(path);
			originalImage = ImageIO.read(url);
			scale = 1.0;
			rot = 0.0;
			updateImage(0);
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.toString(), "ERROR Loading Image", 
				JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}  	
    }

    public ImageSelector() {
    	Container content = getContentPane();
    	content.setLayout(new BorderLayout());
    	
    	JPanel selectionFrame = new JPanel();
    	selectionFrame.setLayout(new GridLayout(5, 1));
    	
    	JLabel label1 = new JLabel("Select Example Image");
    	JLabel label2 = new JLabel("Provide URL");
    	imageList = new JComboBox(imageNames);
    	imageList.setSelectedIndex(-1);
    	imageList.addActionListener(this);
    	
    	//Top Panel (example images)
    	JPanel topPanel = new JPanel();
    	topPanel.setLayout(new BorderLayout());
    	topPanel.add(label1, BorderLayout.WEST);
    	topPanel.add(imageList, BorderLayout.CENTER);
    	selectionFrame.add(topPanel);
    	
    	//URL Panel
    	JPanel URLPanel = new JPanel();
    	URLPanel.setLayout(new BorderLayout());
    	URLPanel.add(label2, BorderLayout.WEST);
    	field = new JTextField();
    	URLPanel.add(field, BorderLayout.CENTER);
    	selectURLButton = new JButton("Load From URL");
    	selectURLButton.addActionListener(this);
    	URLPanel.add(selectURLButton, BorderLayout.EAST);
    	selectionFrame.add(URLPanel);
    	selectFileButton = new JButton("Select File from Your Computer");
    	selectFileButton.addActionListener(this);
    	selectionFrame.add(selectFileButton);
    	
    	//Resize panel    	
    	JPanel resizePanel = new JPanel();
    	resizePanel.setLayout(new BorderLayout());
    	JPanel resizePanel1 = new JPanel();
    	resizePanel1.setLayout(new GridLayout(1, 5));
    	JLabel resizeLabel = new JLabel("Resize Image");
    	resizePanel1.add(resizeLabel);
		decreaseSizeButton = new JButton("-");
		decreaseSizeButton.addActionListener(this);
		resizePanel1.add(decreaseSizeButton);
		increaseSizeButton = new JButton("+");
		increaseSizeButton.addActionListener(this);
		resizePanel1.add(increaseSizeButton);
		percentScaleText = new JTextField();
		percentScaleText.setText("100");
		percentScaleText.getDocument().addDocumentListener(this);
		resizePanel1.add(percentScaleText);
		JLabel percentLabel = new JLabel("%");
		resizePanel1.add(percentLabel);
		resizePanel.add(resizePanel1, BorderLayout.WEST);
    	selectionFrame.add(resizePanel);
    	
    	
    	//Rotate/Flip panel
    	JPanel rotatePanel = new JPanel();
    	rotatePanel.setLayout(new BorderLayout());
    	JPanel rotatePanel1 = new JPanel();
    	rotatePanel1.setLayout(new GridLayout(1, 5));
    	JLabel rotateLabel = new JLabel("Rotate Image");
    	rotatePanel1.add(rotateLabel);
		rotateLeftButton = new JButton("<--");
		rotateLeftButton.addActionListener(this);
		rotatePanel1.add(rotateLeftButton);
		rotateRightButton = new JButton("-->");
		rotateRightButton.addActionListener(this);
		rotatePanel1.add(rotateRightButton);
		degreesRotateText = new JTextField();
		degreesRotateText.setText("0");
		degreesRotateText.getDocument().addDocumentListener(this);
		rotatePanel1.add(degreesRotateText);
		JLabel degreesLabel = new JLabel("Degrees");
		rotatePanel1.add(degreesLabel);
		rotatePanel.add(rotatePanel1, BorderLayout.WEST);
		
		JPanel rotatePanel2 = new JPanel();
		rotatePanel2.setLayout(new GridLayout(1, 3));
		JLabel flipLabel = new JLabel("Flip Image");
		rotatePanel2.add(flipLabel);
		flipHorizButton = new JButton("<");
		flipHorizButton.addActionListener(this);
		rotatePanel2.add(flipHorizButton);
		flipVertButton = new JButton("^");
		flipVertButton.addActionListener(this);
		rotatePanel2.add(flipVertButton);
		rotatePanel.add(rotatePanel2, BorderLayout.EAST);
		
    	selectionFrame.add(rotatePanel);
    	
    	content.add(selectionFrame, BorderLayout.NORTH);
    	canvas = new Display();
    	content.add(canvas, BorderLayout.CENTER);
    	selectImageButton = new JButton("Select Image");
    	selectImageButton.addActionListener(this);
    	content.add(selectImageButton, BorderLayout.SOUTH);
    	
    	setSize(800, 800);
    	show();
    }
    
    public class Display extends JPanel {
    	public void paintComponent(Graphics g) {
    		g.setColor(Color.WHITE);
    		g.fillRect(0, 0, 1000, 1000);
    		if (currentImage != null)
    			g.drawImage(currentImage, 0, 0, this);
    		else {
    			g.setColor(Color.red);
    			g.drawString("Preview Image Here", 200, 200);
    		}
    	}
    }
    
    
    //Helper functions for determining how to resize an image around a rotation
    //(similar to code I wrote for Princeton COS 426 Assignment 1)
    double getMax(double[] a) {
    	double max = a[0];
    	for (int i = 1; i < a.length; i++) {
    		if (a[i] > max)
    			max = a[i];
    	}
    	return max;
    }
    double getMin(double[] a) {
    	double min = a[0];
    	for (int i = 1; i < a.length; i++) {
    		if (a[i] < min)
    			min = a[i];
    	}
    	return min;    	
    }
    public int getNewWidth(double w, double h, double theta) {
    	double cosT = Math.cos(theta), sinT = Math.sin(theta);
    	double[] candidates = {0, w*cosT, w*cosT-h*sinT, -h*sinT};
    	return (int)(getMax(candidates) - getMin(candidates));
    }
    public int getNewHeight(double w, double h, double theta) {
    	double cosT = Math.cos(theta), sinT = Math.sin(theta);
    	double[] candidates = {0, w*sinT, w*sinT+h*cosT, h*cosT};
    	return (int)(getMax(candidates) - getMin(candidates));
    }
    public int getHorizBias(double w, double h, double theta) {
    	double cosT = Math.cos(theta), sinT = Math.sin(theta);
    	double[] candidates = {0, w*cosT, w*cosT-h*sinT, -h*sinT};
    	return (int)getMin(candidates);
    }
    public int getVertBias(double w, double h, double theta) {
    	double cosT = Math.cos(theta), sinT = Math.sin(theta);
    	double[] candidates = {0, w*sinT, w*sinT+h*cosT, h*cosT};
    	return (int)getMin(candidates);
    }    
    
    public void updateImage(int typedText) {
    	if (originalImage == null)
    		return;
    	double w = scale*originalImage.getWidth();
    	double h = scale*originalImage.getHeight();
    	if (w < 4 || h < 4)
    		return;
    	double theta = rot*Math.PI/180;
    	int width = getNewWidth(w, h, theta);
    	int height = getNewHeight(w, h, theta);
    	int dx = -getHorizBias(w, h, theta);
    	int dy = -getVertBias(w, h, theta);
    	BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    	Graphics2D graphics = scaledImage.createGraphics();
    	graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    	graphics.translate(dx, dy);
    	graphics.rotate(theta);
    	graphics.drawImage(originalImage, 0, 0, (int)Math.round(w), (int)Math.round(h), null);
    	graphics.dispose();
    	currentImage = scaledImage;
    	if (typedText != 1)
    		percentScaleText.setText(String.format("%.2f", scale*100));
    	if (typedText != 2)
    		degreesRotateText.setText(String.format("%.2f", rot));
    	canvas.repaint();
    }
    
    public void updateFlip(boolean horizFlip, boolean vertFlip) {
    	int w = originalImage.getWidth(), h = originalImage.getHeight();
    	BufferedImage newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    	Graphics2D graphics = newImage.createGraphics();
    	int dx = 0, dy = 0;
    	if (horizFlip)
    		dx = w;
    	if (vertFlip)
    		dy = h;
    	graphics.translate(dx, dy);
    	graphics.scale(horizFlip?-1:1, vertFlip?-1:1);
    	graphics.drawImage(originalImage, 0, 0, w, h, null);
    	originalImage = newImage;
    	updateImage(0);
    }
    
    public void actionPerformed(ActionEvent evt) {
    	if (evt.getSource() == selectURLButton) {
    		String path = field.getText();
    		readImage(path, true);
    	}
    	else if (evt.getSource() == imageList) {
    		String imageName = "Pictures/" + (String)imageList.getSelectedItem();
			readImage(imageName, false);
    	}
    	else if (evt.getSource() == selectImageButton) {
			dispose();
    	}
    	else if (evt.getSource() == increaseSizeButton) {
    		if (originalImage != null) {
    			scale *= 1.1;
    			updateImage(0);
    		}
    	}
    	else if (evt.getSource() == decreaseSizeButton) {
    		if (originalImage != null) {
    			scale /= 1.1;
    			updateImage(0);
    		}
    	}
    	else if (evt.getSource() == rotateRightButton) {
    		if (originalImage != null) {
    			rot += 10;
    			if (rot > 360)
    				rot -= 360;
    			updateImage(0);
    		}
    	}
    	else if (evt.getSource() == rotateLeftButton) {
    		if (originalImage != null) {
    			rot -= 10;
    			if (rot < 0)
    				rot += 360;
    			updateImage(0);
    		}
    	}
    	else if (evt.getSource() == flipHorizButton) {
    		if (originalImage != null) {
	    		updateFlip(true, false);
    		}
    	}
    	else if (evt.getSource() == flipVertButton) {
    		if (originalImage != null) {
	    		updateFlip(false, true);
    		}
    	}
    	else if (evt.getSource() == selectFileButton) {
    		try {
	    		JFileChooser chooser = new JFileChooser(lastFilename);
				int returnVal = chooser.showOpenDialog(null);
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					lastFilename = chooser.getSelectedFile().getPath();
					originalImage = ImageIO.read(chooser.getSelectedFile());
					scale = 1.0;
					rot = 0.0;
					updateImage(0);
				}
    		}
    		catch (Exception e) {
    			e.printStackTrace();
    		}   		
    	}
    	canvas.repaint();
    }
    public void updateScaleFromText() {
    	if (percentScaleText.getText().length() > 0) {
    		try {
	     		scale = Double.parseDouble(percentScaleText.getText())/100.0;
	    		updateImage(1);
    		}
    		catch (Exception e) { //Number format exception
    			return;
    		}
    	}
    }
    
    public void updateRotateFromText() {
    	if (degreesRotateText.getText().length() > 0) {
    		try {
    			rot = Double.parseDouble(degreesRotateText.getText());
    			updateImage(2);
    		}
    		catch (Exception e) {//Number format exception
    			return;
    		}
    	}    	
    }
    
    public void changedUpdate(DocumentEvent e) {
    	if (e.getDocument() == percentScaleText.getDocument())
    		updateScaleFromText();
    	else if (e.getDocument() == degreesRotateText.getDocument())
    		updateRotateFromText();
    }
    public void removeUpdate(DocumentEvent e) {
    	if (e.getDocument() == percentScaleText.getDocument())
    		updateScaleFromText();
    	else if (e.getDocument() == degreesRotateText.getDocument())
    		updateRotateFromText();
    }
    public void insertUpdate(DocumentEvent e) {
    	if (e.getDocument() == percentScaleText.getDocument())
    		updateScaleFromText();
    	else if (e.getDocument() == degreesRotateText.getDocument())
    		updateRotateFromText();
    }
    
    public static void main(String[] args) {
    	//For debugging
    	ImageSelector selector = new ImageSelector();
		selector.addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent evt){}
		    public void windowClosed(WindowEvent evt){}
		    public void windowClosing(WindowEvent evt){
		    	System.exit(0);
		    }
		    public void windowDeactivated(WindowEvent evt){}
		    public void windowDeiconified(WindowEvent evt) {}
		    public void windowIconified(WindowEvent evt){}
		    public void windowOpened(WindowEvent evt){}	
		});
    }
}