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
package edu.umd.cs.piccolo;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.Timer;

import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.event.PPanEventHandler;
import edu.umd.cs.piccolo.event.PZoomEventHandler;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDebug;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.util.PStack;
import edu.umd.cs.piccolo.util.PUtil;

/**
 * <b>PCanvas</b> is a simple Swing component that can be used to embed
 * Piccolo into a Java Swing application. Canvases view the Piccolo scene graph
 * through a camera. The canvas manages screen updates coming from this camera,
 * and forwards swing mouse and keyboard events to the camera.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PCanvas extends JComponent implements PComponent {

	public static final String INTERATING_CHANGED_NOTIFICATION = "INTERATING_CHANGED_NOTIFICATION";
	
	public static PCanvas CURRENT_ZCANVAS = null;

	private PCamera camera;
	private PStack cursorStack;
	private int interacting;
	private int defaultRenderQuality;
	private int animatingRenderQuality;
	private int interactingRenderQuality;
	private PPanEventHandler panEventHandler;
	private PZoomEventHandler zoomEventHandler;
	private boolean paintingImmediately;
	private boolean animatingOnLastPaint;
	private MouseListener mouseListener;
	private KeyListener keyListener;
	private MouseWheelListener mouseWheelListener;
	private MouseMotionListener mouseMotionListener;
	
	/**
	 * Construct a canvas with the basic scene graph consisting of a
	 * root, camera, and layer. Event handlers for zooming and panning
	 * are automatically installed.
	 */
	public PCanvas() {
		CURRENT_ZCANVAS = this;
		cursorStack = new PStack();
		setCamera(createDefaultCamera());
		installInputSources();		
		setDefaultRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING);
		setAnimatingRenderQuality(PPaintContext.LOW_QUALITY_RENDERING);
		setInteractingRenderQuality(PPaintContext.LOW_QUALITY_RENDERING);
		setPanEventHandler(new PPanEventHandler());
		setZoomEventHandler(new PZoomEventHandler());
		setBackground(Color.WHITE); 	
	}
		
	protected PCamera createDefaultCamera() {
	    return PUtil.createBasicScenegraph();
	}

	//****************************************************************
	// Basic - Methods for accessing common piccolo nodes.
	//****************************************************************

   /**
	 * Get the pan event handler associated with this canvas. This event handler
	 * is set up to get events from the camera associated with this canvas by 
	 * default.
	 */
	public PPanEventHandler getPanEventHandler() {
		return panEventHandler;
	}
   
	/**
	 * Set the pan event handler associated with this canvas.
	 * @param handler the new zoom event handler
	 */
	public void setPanEventHandler(PPanEventHandler handler) {
		if(panEventHandler != null) {
			removeInputEventListener(panEventHandler);
		}
		
		panEventHandler = handler;
		
		if(panEventHandler != null) {
			addInputEventListener(panEventHandler);
		}
	}
	
	/**
	 * Get the zoom event handler associated with this canvas. This event handler
	 * is set up to get events from the camera associated with this canvas by 
	 * default.
	 */
	public PZoomEventHandler getZoomEventHandler() {
		return zoomEventHandler;
	}

	/**
	 * Set the zoom event handler associated with this canvas.
	 * @param handler the new zoom event handler
	 */
	public void setZoomEventHandler(PZoomEventHandler handler) {
		if(zoomEventHandler != null) {
			removeInputEventListener(zoomEventHandler);
		}
		
		zoomEventHandler = handler;
		
		if(zoomEventHandler != null) {
			addInputEventListener(zoomEventHandler);
		}
	}
	
	/**
	 * Return the camera associated with this canvas. All input events from this canvas
	 * go through this camera. And this is the camera that paints this canvas.
	 */
	public PCamera getCamera() {
		return camera;
	}
	
	/**
	 * Set the camera associated with this canvas. All input events from this canvas
	 * go through this camera. And this is the camera that paints this canvas.
	 */
	public void setCamera(PCamera newCamera) {
		if (camera != null) {
			camera.setComponent(null);
		}
		
		camera = newCamera;
		
		if (camera != null) {
			camera.setComponent(this);
			camera.setBounds(getBounds());
		}
	}

	/**
	 * Return root for this canvas.
	 */
	public PRoot getRoot() {
		return camera.getRoot();
	}
		
	/**
	 * Return layer for this canvas.
	 */
	public PLayer getLayer() {
		return camera.getLayer(0);
	}

	/**
	 * Add an input listener to the camera associated with this canvas.
	 */ 	
	public void addInputEventListener(PInputEventListener listener) {
		getCamera().addInputEventListener(listener);
	}
	
	/**
	 * Remove an input listener to the camera associated with this canvas.
	 */ 	
	public void removeInputEventListener(PInputEventListener listener) {
		getCamera().removeInputEventListener(listener);
	}
	
	//****************************************************************
	// Painting
	//****************************************************************

	/**
	 * Return true if this canvas has been marked as interacting. If so
	 * the canvas will normally render at a lower quality that is faster.
	 */
	public boolean getInteracting() {
		return interacting > 0;
	}
	
	/**
	 * Return true if any activities that respond with true to the method
	 * isAnimating were run in the last PRoot.processInputs() loop. This
	 * values is used by this canvas to determine the render quality
	 * to use for the next paint.
	 */
	public boolean getAnimating() {
		return getRoot().getActivityScheduler().getAnimating();
	}

	/**
	 * Set if this canvas is interacting. If so the canvas will normally 
	 * render at a lower quality that is faster. Also repaints the canvas if the
	 * render quality should change.
	 */
	public void setInteracting(boolean isInteracting) {
		boolean wasInteracting = getInteracting();
		
		if (isInteracting) {
			interacting++;
		} else {
			interacting--;
		}
				
		if (!getInteracting()) { // determine next render quality and repaint if it's greater then the old
								 // interacting render quality.
			int nextRenderQuality = defaultRenderQuality;
			if (getAnimating()) nextRenderQuality = animatingRenderQuality;
			if (nextRenderQuality > interactingRenderQuality) {
				repaint();
			}
		}
		
		isInteracting = getInteracting();
		
		if (wasInteracting != isInteracting) {
			firePropertyChange(INTERATING_CHANGED_NOTIFICATION, wasInteracting, isInteracting);
		}
	}

	/**
	 * Set the render quality that should be used when rendering this canvas
	 * when it is not interacting or animating. The default value is
	 * PPaintContext. HIGH_QUALITY_RENDERING.
	 * 
	 * @param requestedQuality supports PPaintContext.HIGH_QUALITY_RENDERING or PPaintContext.LOW_QUALITY_RENDERING
	 */
	public void setDefaultRenderQuality(int requestedQuality) {
		defaultRenderQuality = requestedQuality;
		repaint();
	}

	/**
	 * Set the render quality that should be used when rendering this canvas
	 * when it is animating. The default value is PPaintContext.LOW_QUALITY_RENDERING.
	 * 
	 * @param requestedQuality supports PPaintContext.HIGH_QUALITY_RENDERING or PPaintContext.LOW_QUALITY_RENDERING
	 */
	public void setAnimatingRenderQuality(int requestedQuality) {
		animatingRenderQuality = requestedQuality;
		if (getAnimating()) repaint();
	}

	/**
	 * Set the render quality that should be used when rendering this canvas
	 * when it is interacting. The default value is PPaintContext.LOW_QUALITY_RENDERING.
	 * 
	 * @param requestedQuality supports PPaintContext.HIGH_QUALITY_RENDERING or PPaintContext.LOW_QUALITY_RENDERING
	 */
	public void setInteractingRenderQuality(int requestedQuality) {
		interactingRenderQuality = requestedQuality;
		if (getInteracting()) repaint();
	}
		
	/**
	 * Set the canvas cursor, and remember the previous cursor on the
	 * cursor stack.
	 */ 
	public void pushCursor(Cursor cursor) {
		cursorStack.push(getCursor());
		setCursor(cursor);
	}
	
	/**
	 * Pop the cursor on top of the cursorStack and set it as the 
	 * canvas cursor.
	 */ 
	public void popCursor() {
		setCursor((Cursor)cursorStack.pop());
	}	
	
	//****************************************************************
	// Code to manage connection to Swing. There appears to be a bug in
	// swing where it will occasionally send to many mouse pressed or mouse
	// released events. Below we attempt to filter out those cases before
	// they get delivered to the Piccolo framework.
	//****************************************************************	
	
	private boolean isButton1Pressed;
	private boolean isButton2Pressed;
	private boolean isButton3Pressed;	
	
	/**
	 * Overrride setEnabled to install/remove canvas input sources as needed.
	 */
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		
		if (isEnabled()) {
			installInputSources();
		} else {
			removeInputSources();
		}
	}
	
	/**
	 * This method installs mouse and key listeners on the canvas that forward
	 * those events to piccolo.
	 */
	protected void installInputSources() {
		if (mouseListener == null) {
			mouseListener = new MouseListener() {
				public void mouseClicked(MouseEvent e) {
					sendInputEventToInputManager(e, MouseEvent.MOUSE_CLICKED);
				}
	
				public void mouseEntered(MouseEvent e) {
					MouseEvent simulated = null;
	
					if ((e.getModifiersEx() & (InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK)) != 0) {
						simulated = new MouseEvent((Component)e.getSource(),MouseEvent.MOUSE_DRAGGED,e.getWhen(),e.getModifiers(),e.getX(),e.getY(),e.getClickCount(),e.isPopupTrigger(),e.getButton());
					} else {
						simulated = new MouseEvent((Component)e.getSource(),MouseEvent.MOUSE_MOVED,e.getWhen(),e.getModifiers(),e.getX(),e.getY(),e.getClickCount(),e.isPopupTrigger(),e.getButton());
					}
	
					sendInputEventToInputManager(e, MouseEvent.MOUSE_ENTERED);
					sendInputEventToInputManager(simulated, simulated.getID());
				}
				
				public void mouseExited(MouseEvent e) {
					MouseEvent simulated = null;
					
					if ((e.getModifiersEx() & (InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK)) != 0) {
						simulated = new MouseEvent((Component)e.getSource(),MouseEvent.MOUSE_DRAGGED,e.getWhen(),e.getModifiers(),e.getX(),e.getY(),e.getClickCount(),e.isPopupTrigger(),e.getButton());
					} else {
						simulated = new MouseEvent((Component)e.getSource(),MouseEvent.MOUSE_MOVED,e.getWhen(),e.getModifiers(),e.getX(),e.getY(),e.getClickCount(),e.isPopupTrigger(),e.getButton());
					}
	
					sendInputEventToInputManager(simulated, simulated.getID());
					sendInputEventToInputManager(e, MouseEvent.MOUSE_EXITED);
				}
	
				public void mousePressed(MouseEvent e) {
					requestFocus();
					
					boolean shouldBalanceEvent = false;
	
					if (e.getButton() == MouseEvent.NOBUTTON) {
						if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK) {
							e = new MouseEvent((Component)e.getSource(),MouseEvent.MOUSE_PRESSED,e.getWhen(),e.getModifiers(),e.getX(),e.getY(),e.getClickCount(),e.isPopupTrigger(),MouseEvent.BUTTON1);
						}
						else if ((e.getModifiers() & MouseEvent.BUTTON2_MASK) == MouseEvent.BUTTON2_MASK) {
							e = new MouseEvent((Component)e.getSource(),MouseEvent.MOUSE_PRESSED,e.getWhen(),e.getModifiers(),e.getX(),e.getY(),e.getClickCount(),e.isPopupTrigger(),MouseEvent.BUTTON2);
						}
						else if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK) {
							e = new MouseEvent((Component)e.getSource(),MouseEvent.MOUSE_PRESSED,e.getWhen(),e.getModifiers(),e.getX(),e.getY(),e.getClickCount(),e.isPopupTrigger(),MouseEvent.BUTTON3);
						}					
					}
	
					switch (e.getButton()) {
						case MouseEvent.BUTTON1:
							if (isButton1Pressed) {
								shouldBalanceEvent = true;
							}
							isButton1Pressed = true;
							break;
							
						case MouseEvent.BUTTON2:
							if (isButton2Pressed) {
								shouldBalanceEvent = true;
							}
							isButton2Pressed = true;
							break;
							
						case MouseEvent.BUTTON3:
							if (isButton3Pressed) {
								shouldBalanceEvent = true;
							}
							isButton3Pressed = true;
							break;						
					}
					
					if (shouldBalanceEvent) {
						MouseEvent balanceEvent = new MouseEvent((Component)e.getSource(),MouseEvent.MOUSE_RELEASED,e.getWhen(),e.getModifiers(),e.getX(),e.getY(),e.getClickCount(),e.isPopupTrigger(),e.getButton());
						sendInputEventToInputManager(balanceEvent, MouseEvent.MOUSE_RELEASED);
					}
					
					sendInputEventToInputManager(e, MouseEvent.MOUSE_PRESSED);
				}
						
				public void mouseReleased(MouseEvent e) {
					boolean shouldBalanceEvent = false;
	
					if (e.getButton() == MouseEvent.NOBUTTON) {
						if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) == MouseEvent.BUTTON1_MASK) {
							e = new MouseEvent((Component)e.getSource(),MouseEvent.MOUSE_RELEASED,e.getWhen(),e.getModifiers(),e.getX(),e.getY(),e.getClickCount(),e.isPopupTrigger(),MouseEvent.BUTTON1);
						}
						else if ((e.getModifiers() & MouseEvent.BUTTON2_MASK) == MouseEvent.BUTTON2_MASK) {
							e = new MouseEvent((Component)e.getSource(),MouseEvent.MOUSE_RELEASED,e.getWhen(),e.getModifiers(),e.getX(),e.getY(),e.getClickCount(),e.isPopupTrigger(),MouseEvent.BUTTON2);
						}
						else if ((e.getModifiers() & MouseEvent.BUTTON3_MASK) == MouseEvent.BUTTON3_MASK) {
							e = new MouseEvent((Component)e.getSource(),MouseEvent.MOUSE_RELEASED,e.getWhen(),e.getModifiers(),e.getX(),e.getY(),e.getClickCount(),e.isPopupTrigger(),MouseEvent.BUTTON3);
						}					
					}
	
					switch (e.getButton()) {
						case MouseEvent.BUTTON1:
							if (!isButton1Pressed) {
								shouldBalanceEvent = true;
							}
							isButton1Pressed = false;
							break;
							
						case MouseEvent.BUTTON2:
							if (!isButton2Pressed) {
								shouldBalanceEvent = true;
							}
							isButton2Pressed = false;
							break;
							
						case MouseEvent.BUTTON3:
							if (!isButton3Pressed) {
								shouldBalanceEvent = true;
							}
							isButton3Pressed = false;
							break;						
					}
					
					if (shouldBalanceEvent) {
						MouseEvent balanceEvent = new MouseEvent((Component)e.getSource(),MouseEvent.MOUSE_PRESSED,e.getWhen(),e.getModifiers(),e.getX(),e.getY(),e.getClickCount(),e.isPopupTrigger(),e.getButton());
						sendInputEventToInputManager(balanceEvent, MouseEvent.MOUSE_PRESSED);
					}
	
					sendInputEventToInputManager(e, MouseEvent.MOUSE_RELEASED);
				}
			};
			addMouseListener(mouseListener);
		}

		if (mouseMotionListener == null) {
			mouseMotionListener = new MouseMotionListener() {
				public void mouseDragged(MouseEvent e) {
					sendInputEventToInputManager(e, MouseEvent.MOUSE_DRAGGED);
				}
				public void mouseMoved(MouseEvent e) {
					sendInputEventToInputManager(e, MouseEvent.MOUSE_MOVED);
				}
			};
			addMouseMotionListener(mouseMotionListener);
		}

		if (mouseWheelListener == null) {
			mouseWheelListener = new MouseWheelListener() {
				public void mouseWheelMoved(MouseWheelEvent e) {
					sendInputEventToInputManager(e, e.getScrollType());
					if (!e.isConsumed() && getParent() != null) {
						getParent().dispatchEvent(e);
					}
				}
			};
			addMouseWheelListener(mouseWheelListener);
		}

		if (keyListener == null) {
			keyListener = new KeyListener() {
				public void keyPressed(KeyEvent e) {
					sendInputEventToInputManager(e, KeyEvent.KEY_PRESSED);
				}
				public void keyReleased(KeyEvent e) {
					sendInputEventToInputManager(e, KeyEvent.KEY_RELEASED);
				}
				public void keyTyped(KeyEvent e) {
					sendInputEventToInputManager(e, KeyEvent.KEY_TYPED);
				}
			};
			addKeyListener(keyListener);
		}
	}

	/**
	 * This method removes mouse and key listeners on the canvas that forward
	 * those events to piccolo.
	 */
	protected void removeInputSources() {
		if (mouseListener != null) removeMouseListener(mouseListener);
		if (mouseMotionListener != null) removeMouseMotionListener(mouseMotionListener);
		if (mouseWheelListener != null) removeMouseWheelListener(mouseWheelListener);
		if (keyListener != null) removeKeyListener(keyListener);

		mouseListener = null;
		mouseMotionListener = null;
		mouseWheelListener = null;
		keyListener = null;
	}
	
	protected void sendInputEventToInputManager(InputEvent e, int type) {
		getRoot().getDefaultInputManager().processEventFromCamera(e, type, getCamera());
	}
	
	public void setBounds(int x, int y, final int w, final int h) {
		camera.setBounds(camera.getX(), camera.getY(), w, h);
		super.setBounds(x, y, w, h);
	}	
	
	public void repaint(PBounds bounds) {
		PDebug.processRepaint();
		
		bounds.expandNearestIntegerDimensions();
		bounds.inset(-1, -1);
		
		repaint((int)bounds.x,
				(int)bounds.y,
				(int)bounds.width,
				(int)bounds.height);
	}

	public void paintComponent(Graphics g) {
		PDebug.startProcessingOutput();

		Graphics2D g2 = (Graphics2D) g.create();		
	    g2.setColor(getBackground());
	    g2.fillRect(0, 0, getWidth(), getHeight());
		
		// create new paint context and set render quality to lowest common 
		// denominator render quality.
		PPaintContext paintContext = new PPaintContext(g2);
		if (getInteracting() || getAnimating()) {
			if (interactingRenderQuality < animatingRenderQuality) {
				paintContext.setRenderQuality(interactingRenderQuality);
			} else {
				paintContext.setRenderQuality(animatingRenderQuality);
			}
		} else {
			paintContext.setRenderQuality(defaultRenderQuality);
		}
		
		// paint piccolo
		camera.fullPaint(paintContext);
		
		// if switched state from animating to not animating invalidate the entire
		// screen so that it will be drawn with the default instead of animating 
		// render quality.
		if (!getAnimating() && animatingOnLastPaint) {
			repaint();
		}
		animatingOnLastPaint = getAnimating();
		
		PDebug.endProcessingOutput(g2);				
	}		
	
	public void paintImmediately() {
		if (paintingImmediately) {
			return;
		}
		
		paintingImmediately = true;
		RepaintManager.currentManager(this).paintDirtyRegions();
		paintingImmediately = false;
	}	

	public Timer createTimer(int delay, ActionListener listener) {
		return new Timer(delay,listener);
	}		
}