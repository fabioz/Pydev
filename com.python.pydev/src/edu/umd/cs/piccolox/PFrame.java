/*
 * Copyright (c) 2002-@year@, University of Maryland
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of the University of Maryland nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Piccolo was written at the Human-Computer Interaction Laboratory www.cs.umd.edu/hcil by Jesse Grosjean
 * under the supervision of Ben Bederson. The Piccolo website is www.cs.umd.edu/hcil/piccolo.
 */
package edu.umd.cs.piccolox;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.Iterator;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import edu.umd.cs.piccolo.PCanvas;

/**
 * <b>PFrame</b> is meant to be subclassed by applications that just need a PCanvas in a JFrame.
 * It also includes full screen mode functionality when run in JDK 1.4. These
 * subclasses should override the initialize method and start adding their own
 * code there. Look in the examples package to see lots of uses of PFrame.
 *
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PFrame extends JFrame {

	private PCanvas canvas;
	private GraphicsDevice graphicsDevice;
	private DisplayMode originalDisplayMode;
	private EventListener escapeFullScreenModeListener;

	public PFrame() {
		this("", false, null);
	}

	public PFrame(String title, boolean fullScreenMode, PCanvas aCanvas) {
		this(title, GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice(), fullScreenMode, aCanvas);
	}

	public PFrame(String title, GraphicsDevice aDevice, final boolean fullScreenMode, final PCanvas aCanvas) {
		super(title, aDevice.getDefaultConfiguration());
		
		graphicsDevice = aDevice;
		
		try {
			originalDisplayMode = graphicsDevice.getDisplayMode();		 
		} catch (InternalError e) {
			e.printStackTrace();
		}
		
		setBounds(getDefaultFrameBounds());
		setBackground(null);
		
		try {
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		} catch (SecurityException e) {} // expected from applets
		
		if (aCanvas == null) {
			canvas = new PCanvas();
		} else {
			canvas = aCanvas;
		}
						
		getContentPane().add(canvas);
		validate(); 	
		setFullScreenMode(fullScreenMode);
		canvas.requestFocus();
		beforeInitialize();

		// Manipulation of Piccolo's scene graph should be done from Swings
		// event dispatch thread since Piccolo is not thread safe. This code calls
		// initialize() from that thread once the PFrame is initialized, so you are 
		// safe to start working with Piccolo in the initialize() method.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				PFrame.this.initialize();
				repaint();
			}
		});
	}

	public PCanvas getCanvas() {
		return canvas;
	}
	
	public Rectangle getDefaultFrameBounds() {
		return new Rectangle(100, 100, 400, 400);
	}		
	
	//****************************************************************
	// Full Screen Display Mode
	//****************************************************************

	public boolean isFullScreenMode() {
		return graphicsDevice.getFullScreenWindow() != null;
	}
	
	public void setFullScreenMode(boolean fullScreenMode) {
		if (fullScreenMode) {
			addEscapeFullScreenModeListener();
			
			if (isDisplayable()) {
				dispose();
			}
			
			setUndecorated(true);
			setResizable(false);
			graphicsDevice.setFullScreenWindow(this);			 
			
			if (graphicsDevice.isDisplayChangeSupported()) {
				chooseBestDisplayMode(graphicsDevice);
			}		 
			validate();
		} else {
			removeEscapeFullScreenModeListener();
			
			if (isDisplayable()) {
				dispose();
			}
			
			setUndecorated(false);
			setResizable(true);
			graphicsDevice.setFullScreenWindow(null);					 
			validate();
			setVisible(true);
		}		
	}
	
	protected void chooseBestDisplayMode(GraphicsDevice device) {
		DisplayMode best = getBestDisplayMode(device);
		if (best != null) {
			device.setDisplayMode(best);
		}
	}
	
	protected DisplayMode getBestDisplayMode(GraphicsDevice device) {
		Iterator itr = getPreferredDisplayModes(device).iterator();
		while (itr.hasNext()) {
			DisplayMode each = (DisplayMode) itr.next();
			DisplayMode[] modes = device.getDisplayModes();
			for (int i = 0; i < modes.length; i++) {
				if (modes[i].getWidth() == each.getWidth() && 
					modes[i].getHeight() == each.getHeight() && 
					modes[i].getBitDepth() == each.getBitDepth()) {
						return each;
				}
			}			
		}
		
		return null;
	}
	
	/**
	 * By default return the current display mode. Subclasses may override this method
	 * to return other modes in the collection.
	 */
	protected Collection getPreferredDisplayModes(GraphicsDevice device) {
		ArrayList result = new ArrayList();
		
		result.add(device.getDisplayMode());
		/*result.add(new DisplayMode(640, 480, 32, 0));
		result.add(new DisplayMode(640, 480, 16, 0));
		result.add(new DisplayMode(640, 480, 8, 0));*/
		
		return result;
	}

	/**
	 * This method adds a key listener that will take this PFrame out of full
	 * screen mode when the escape key is pressed. This is called for you
	 * automatically when the frame enters full screen mode.
	 */
	public void addEscapeFullScreenModeListener() {
		removeEscapeFullScreenModeListener();
		escapeFullScreenModeListener = new KeyAdapter() {
			public void keyPressed(KeyEvent aEvent) {
				if (aEvent.getKeyCode() == KeyEvent.VK_ESCAPE) {
					setFullScreenMode(false);
				}
			}
		};	
		canvas.addKeyListener((KeyListener)escapeFullScreenModeListener);
	}
	
	/**
	 * This method removes the escape full screen mode key listener. It will be
	 * called for you automatically when full screen mode exits, but the method
	 * has been made public for applications that wish to use other methods for
	 * exiting full screen mode.
	 */
	public void removeEscapeFullScreenModeListener() {
		if (escapeFullScreenModeListener != null) {
			canvas.removeKeyListener((KeyListener)escapeFullScreenModeListener);
			escapeFullScreenModeListener = null;
		}
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

	public static void main(String[] argv) {
		new PFrame();
	}	
}
