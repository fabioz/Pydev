package edu.umd.cs.piccolox;

import javax.swing.JApplet;
import javax.swing.SwingUtilities;

import edu.umd.cs.piccolo.PCanvas;

/**
 * <b>PApplet</b> is meant to be subclassed by applications that just need a PCanvas
 * embedded in a web page.
 *
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PApplet extends JApplet {

	private PCanvas canvas;

	public void init() {
		setBackground(null);

		canvas = createCanvas();
		getContentPane().add(canvas);
		validate();
		canvas.requestFocus();
		beforeInitialize();
		
		// Manipulation of Piccolo's scene graph should be done from Swings
		// event dispatch thread since Piccolo is not thread safe. This code calls
		// initialize() from that thread once the PFrame is initialized, so you are 
		// safe to start working with Piccolo in the initialize() method.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				PApplet.this.initialize();
				repaint();
			}
		});
	}
	
	public PCanvas getCanvas() {
		return canvas;
	}
	
	public PCanvas createCanvas() {
		return new PCanvas();
	}
	
	//****************************************************************
	// Initialize
	//****************************************************************

	/**
	 * This method will be called before the initialize() method and will be
	 * called on the thread that is constructing this object.
	 */
	public void beforeInitialize() {
	}

	/**
	 * Subclasses should override this method and add their 
	 * Piccolo initialization code there. This method will be called on the
	 * swing event dispatch thread. Note that the constructors of PFrame
	 * subclasses may not be complete when this method is called. If you need to
	 * initailize some things in your class before this method is called place
	 * that code in beforeInitialize();
	 */
	public void initialize() {
	}	
}