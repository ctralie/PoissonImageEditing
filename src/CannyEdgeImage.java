//A Canny Edge Image extractor used for texture flattening in the Poisson
//Image Editor
//(c) Chris Tralie 2012
import java.util.ArrayList;
import java.awt.image.*;

public class CannyEdgeImage {
	public static final double CANNYSIGMA = 2;

    //This method does 2D convolution in the spatial domain (no FFT)
    //XKernel is a kernel of width "XWidth," meaning there's XWidth samples to the left
    //and to the right of the origin
    //YKernel is a kernel of width "YWidth"
    public static double[][] spatialConv2(double[] XKernel, int XWidth, double[] YKernel, int YWidth, double[][] im) {
    	int W = im.length, H = im[0].length;
        double[][] im1 = new double[W][H];
        
        //First convolve in the x direction 
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                im1[x][y] = 0.0;
                for (int k = 0; k < XWidth*2+1; k++) {
                    int dx = x + k - XWidth;
                    //If out of bounds, hold the color on the boundary constant
                    //This will ensure that edges don't get triggered improperly
                    if (dx < 0)    dx = 0;
                    if (dx >= W) dx = W-1;
                    im1[x][y] += im[dx][y]*XKernel[XWidth*2-k];
                }
            }
        }
        double[][] ret = new double[W][H];
        //Now convolve in the y direction
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
            	ret[x][y] = 0.0;
                for (int k = 0; k < YWidth*2+1; k++) {
                    int dy = y + k - YWidth;
                    if (dy < 0) dy = 0;
                    if (dy >= H) dy = H-1;
                    ret[x][y] += im1[x][dy]*YKernel[YWidth*2-k];
                }
            }
        }
        return ret;
    }

    static boolean inBounds(int x, int y, int W, int H) {
        if (x < 0 || x >= W || y < 0 || y >= H)
            return false;
        return true;
    }

    //Helper function for getHysteresis
    public static void Connect(double[][] SuppressedImage, double[][] EdgeOrient, boolean[][] ret,
                 boolean[][] visited, int x, int y, int depth, double T_l) {
        int[] NDir = {1, 0, -1, 0,    1, 1, -1, -1,    0, 1, 0, -1,    -1, 1, 1, -1};
        int W = SuppressedImage.length, H = SuppressedImage[0].length;
        if (!inBounds(x, y, W, H))
            return;        
        if (depth > 450) {
            visited[x][y] = false;
            return;
        }
        if (visited[x][y])
            return;
        visited[x][y] = true;
        //First check to make sure this pixel is strong enough
        if (SuppressedImage[x][y] < T_l)
            return;
        //If it is strong enough, set it to "1" in the output image
        //since it's passed the threshold test
        ret[x][y] = true;

        //Move along the gradient to check further pixels in the 
        //connected chain
        double LevelSetAngle = EdgeOrient[x][y]+90;
        int AngleIndex = (int)Math.floor(LevelSetAngle/45.0+0.5);
        AngleIndex = (AngleIndex + 4)%4;
        int x1 = x + NDir[AngleIndex*4], y1 = y + NDir[AngleIndex*4+1];
        int x2 = x + NDir[AngleIndex*4+2], y2 = y + NDir[AngleIndex*4+3];
        Connect(SuppressedImage, EdgeOrient, ret, visited, x1, y1, depth+1, T_l);
        Connect(SuppressedImage, EdgeOrient, ret, visited, x2, y2, depth+1, T_l);
    }

    public static boolean[][] getHysteresisImage(double[][] SuppressedImage, double[][] EdgeOrient) {
    	int W = SuppressedImage.length, H = SuppressedImage[0].length;
    	boolean[][] ret = new boolean[W][H];
        //Determine thresholds automatically
        double avg = 0.0;
        for (int x = 0; x < W; x++) {
        	for (int y = 0; y < H; y++) {
        		avg += SuppressedImage[x][y];
        	}
        }
        avg /= (double)(W*H);
        int numbelow = 0, numabove = 0;
        double T_l = 0.0, T_h = 0.0;
        for (int x = 0; x < W; x++) {
        	for (int y = 0; y < H; y++) {
	            if (SuppressedImage[x][y] < avg) {
	                numbelow++;
	                T_l += SuppressedImage[x][y];
	            }
	            else {
	                numabove++;
	                T_h += SuppressedImage[x][y];
	            }
        	}
        }
        T_h /= 100*(numabove+1);
        T_l /= 100*(numbelow+1);
        double Th1 = T_h;
        numabove = 0;
        T_h = 0;
        for (int x = 0; x < W; x++) {
        	for (int y = 0; y < H; y++) {
	            if (SuppressedImage[x][y] > Th1) {
	                numabove++;
	                T_h += SuppressedImage[x][y];
	            }
        	}
        }
        T_h /= (double)(numabove+1);

        boolean[][] visited = new boolean[W][H];
        for (int x = 0; x < W; x++) {
        	for (int y = 0; y < H; y++) {
        		visited[x][y] = false;
        	}
        }
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                if (SuppressedImage[x][y] > T_h)
                    Connect(SuppressedImage, EdgeOrient, ret, visited, x, y, 0, T_l);
            }
        }
    	return ret;
    }

    //Get the thinned edge image by using gradient information
    public static double[][] getNonmaxSuppression(double[][] MagGrad, double[][] EdgeOrient) {
		int[] NDir = {1, 0, -1, 0,    1, 1, -1, -1,    0, 1, 0, -1,    -1, 1, 1, -1};
		int W = MagGrad.length, H = MagGrad[0].length;
		double[][] SuppressedImage = new double[W][H];
        for (int x = 1; x < W-1; x++) {
            for (int y = 1; y < H-1; y++) {
                int index = x*H+y;
                //If the edge strength F(x,y) is smaller than at least one of 
                //its neighbors along D*, set I(x,y) = 0, else set I(x,y) = F(x,y).
                //(0, 45, 90, 135, 180, 225, 270, 315, 360)
                //(0, 1,  2,   3,   0,   1,   2,   3,   0)
                int AngleIndex = (int)Math.floor(EdgeOrient[x][y]/45.0+0.5);
                AngleIndex = (AngleIndex + 4) % 4;
                int x1 = x + NDir[AngleIndex*4], y1 = y + NDir[AngleIndex*4+1];
                int x2 = x + NDir[AngleIndex*4+2], y2 = y + NDir[AngleIndex*4+3];
                double N1 = MagGrad[x1][y1];//Neighbor 1
                double N2 = MagGrad[x2][y2];//Neighbor 2
                double P = MagGrad[x][y];
                if ((N1 > P) || (N2 > P))
                    SuppressedImage[x][y] = 0;
                else
                    SuppressedImage[x][y] = P;
            }
        }
        return SuppressedImage;
    }

	//Return a mask that represents whether or not a pixel is on an edge
    public static boolean[][] getCannyEdgeImage(BufferedImage selectedImage) {
    	int W = selectedImage.getWidth(), H = selectedImage.getHeight();
    	double[][] image = new double[W][H];
       	double[][] Fx;
        double[][] Fy;
        double[][] MagGrad = new double[W][H];
		double[][] EdgeOrient = new double[W][H];
        
        //Initialize the Gaussian kernels
        int gaussWidth = (int)Math.floor(CANNYSIGMA*3+0.5);
        double[] gaussf = new double[2*gaussWidth+1];
        double[] xgaussf = new double[2*gaussWidth+1];
        double sumGauss = 0.0;
        for (int i = 0; i < 2*gaussWidth+1; i++) {
            double t = i - gaussWidth;
            gaussf[i] = Math.exp(-t*t / (2*CANNYSIGMA*CANNYSIGMA));
            xgaussf[i] = -t*gaussf[i];
            sumGauss += gaussf[i];
        }
        if (sumGauss > 0) {
            for (int i = 0; i < 2*gaussWidth+1; i++)
                gaussf[i] /= sumGauss;
        }
        //Now find a proper normalization scale for the gaussian gradient, such 
        //that a region with slope 1 will come out to have slope 1
        sumGauss = 0.0;
        for (int i = 0; i < 2*gaussWidth+1; i++)
            sumGauss += (1+i)*xgaussf[i];
        sumGauss = Math.abs(sumGauss);
        if (sumGauss > 0) {
            for (int i = 0; i < 2*gaussWidth+1; i++)
                xgaussf[i] /= sumGauss;
        }
        
        //Initialize grayscale image
        for (int x = 0; x < W; x++) {
        	for (int y = 0; y < H; y++) {
				int RGB = selectedImage.getRGB(x, y);
				int R = (RGB & 0xFF0000) >> 16;
				int G = (RGB & 0xFF00) >> 8;
				int B = RGB & 0xFF;
				image[x][y] = 0.2989*R + 0.587*G + 0.1140*B;
        	}
        }
        
        //Compute the filtered partial derivatives
        Fx = spatialConv2(xgaussf, gaussWidth, gaussf, gaussWidth, image);
        Fy = spatialConv2(gaussf, gaussWidth, xgaussf, gaussWidth, image);
        //Calculate the magnitude and direction of the gradient
        //at every pixel
        for (int x = 0; x < W; x++) {
        	for (int y = 0; y < H; y++) {
            	MagGrad[x][y] = Math.sqrt(Fx[x][y]*Fx[x][y] + Fy[x][y]*Fy[x][y]);
            	EdgeOrient[x][y] = 180.0/Math.PI*Math.atan2(Fy[x][y], Fx[x][y]);
        	}
        }
        double[][] SuppressedImage = getNonmaxSuppression(MagGrad, EdgeOrient);
        boolean[][] canny = getHysteresisImage(SuppressedImage, EdgeOrient);
    	boolean[][] ret = new boolean[canny.length][canny[0].length];
    	//Thicken the canny edge image a little bit
    	for (int i = 0; i < canny.length; i++) {
    		for (int j = 0; j < canny[i].length; j++) {
    			for (int k = -2; k <= 2; k++) {
    				for (int n = -2; n <= 2; n++) {
    					int dx = i + k, dy = j + n;
    					if (dx < 0)	dx = 0;
    					if (dy < 0) dy = 0;
    					if (dx >= canny.length) dx = canny.length - 1;
    					if (dy >= canny[i].length) dy = canny[i].length - 1;
    					if (canny[dx][dy])
    						ret[i][j] = true;
    				}
    			}
    		}
    	}
    	return ret;
    }
    
}