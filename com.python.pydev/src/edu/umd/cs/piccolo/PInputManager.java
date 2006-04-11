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

import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventListener;
import edu.umd.cs.piccolo.util.PPickPath;

/**
 * <b>PInputManager</b> is responsible for dispatching PInputEvents
 * to node's event listeners. Events are dispatched from PRoot's processInputs
 * method.
 * <P>
 * @see edu.umd.cs.piccolo.event.PInputEvent
 * @see PRoot
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PInputManager extends PBasicInputEventHandler implements PRoot.InputSource {

	private Point2D lastCanvasPosition;
	private Point2D currentCanvasPosition;

	private InputEvent nextInput;
	private int nextType;
	private PCamera nextInputSource;

	private PPickPath mouseFocus;
	private PPickPath previousMouseFocus;
	private PPickPath mouseOver;
	private PPickPath previousMouseOver;
	private PInputEventListener keyboardFocus;

	private int pressedCount;
	
	public PInputManager() {
		super();
		lastCanvasPosition = new Point2D.Double();
		currentCanvasPosition = new Point2D.Double();
	}
		
	//****************************************************************
	// Basic
	//****************************************************************
	 
	/**
	 * Return the node that currently has the keyboard focus. This node
	 * receives the key events.
	 */
	public PInputEventListener getKeyboardFocus() {
		return keyboardFocus;
	}
	
	/**
	 * Set the node that should recive key events.
	 */	
	public void setKeyboardFocus(PInputEventListener eventHandler) {		
		PInputEvent focusEvent = new PInputEvent(this, null);
		
		if (keyboardFocus != null) {
			dispatchEventToListener(focusEvent, FocusEvent.FOCUS_LOST, keyboardFocus);
		}
		
		keyboardFocus = eventHandler;
		
		if (keyboardFocus != null) {
			dispatchEventToListener(focusEvent, FocusEvent.FOCUS_GAINED, keyboardFocus);
		}
	}
	
	/**
	 * Return the node that currently has the mouse focus. This will return
	 * the node that received the current mouse pressed event, or null if the
	 * mouse is not pressed. The mouse focus gets mouse dragged events even
	 * what the mouse is not over the mouse focus.
	 */
	public PPickPath getMouseFocus() {
		return mouseFocus;
	}
	
	public void setMouseFocus(PPickPath path) {
		previousMouseFocus = mouseFocus;
		mouseFocus = path;
	}

	/**
	 * Return the node the the mouse is currently over.
	 */  
	public PPickPath getMouseOver() {
		return mouseOver;
	}
	
	public void setMouseOver(PPickPath path) {
		mouseOver = path;
	}
		
	public Point2D getLastCanvasPosition() {
		return lastCanvasPosition;
	}	

	public Point2D getCurrentCanvasPosition() {
		return currentCanvasPosition;
	}	
	
	//****************************************************************
	// Event Handling - Methods for handling events
	// 
	// The dispatch manager updates the focus nodes based on the
	// incoming events, and dispatches those events to the appropriate
	// focus nodes.
	//****************************************************************
	
	public void keyPressed(PInputEvent event) {
		dispatchEventToListener(event, KeyEvent.KEY_PRESSED, keyboardFocus);
	}
	
	public void keyReleased(PInputEvent event) {
		dispatchEventToListener(event, KeyEvent.KEY_RELEASED, keyboardFocus);
	}
	
	public void keyTyped(PInputEvent event) {
		dispatchEventToListener(event, KeyEvent.KEY_TYPED, keyboardFocus);
	}
	
	public void mouseClicked(PInputEvent event) {
		dispatchEventToListener(event, MouseEvent.MOUSE_CLICKED, previousMouseFocus);
	}
	
	public void mouseWheelRotated(PInputEvent event) {
		setMouseFocus(getMouseOver());
		dispatchEventToListener(event, MouseWheelEvent.WHEEL_UNIT_SCROLL, mouseOver);
	}
	
	public void mouseWheelRotatedByBlock(PInputEvent event) {
		setMouseFocus(getMouseOver());
		dispatchEventToListener(event, MouseWheelEvent.WHEEL_BLOCK_SCROLL, mouseOver);
	}
	
	public void mouseDragged(PInputEvent event) {
		checkForMouseEnteredAndExited(event);
		dispatchEventToListener(event, MouseEvent.MOUSE_DRAGGED, mouseFocus);
	}
	
	public void mouseEntered(PInputEvent event) {
		dispatchEventToListener(event, MouseEvent.MOUSE_ENTERED, mouseOver);
	}
	
	public void mouseExited(PInputEvent event) {
		dispatchEventToListener(event, MouseEvent.MOUSE_EXITED, previousMouseOver);
	}
	
	public void mouseMoved(PInputEvent event) {
		checkForMouseEnteredAndExited(event);
		dispatchEventToListener(event, MouseEvent.MOUSE_MOVED, mouseOver);
	}
	
	public void mousePressed(PInputEvent event) {		
		if (pressedCount == 0) {
			setMouseFocus(getMouseOver());
		}
		pressedCount++;
		dispatchEventToListener(event, MouseEvent.MOUSE_PRESSED, mouseFocus);
		if (pressedCount < 1 || pressedCount > 3) System.err.println("invalid pressedCount on mouse pressed: " + pressedCount);
	}
	
	public void mouseReleased(PInputEvent event) {
		pressedCount--;
		checkForMouseEnteredAndExited(event);		
		dispatchEventToListener(event, MouseEvent.MOUSE_RELEASED, mouseFocus);
		if (pressedCount == 0) {
			setMouseFocus(null);
		}
		if (pressedCount < 0 || pressedCount > 2) System.err.println("invalid pressedCount on mouse released: " + pressedCount);
	}
	
	protected void checkForMouseEnteredAndExited(PInputEvent event) {		
		PNode c = (mouseOver != null) ? mouseOver.getPickedNode() : null; 
		PNode p = (previousMouseOver != null) ? previousMouseOver.getPickedNode() : null;
		
		if (c != p) {
			dispatchEventToListener(event, MouseEvent.MOUSE_EXITED, previousMouseOver);
			dispatchEventToListener(event, MouseEvent.MOUSE_ENTERED, mouseOver);
			previousMouseOver = mouseOver;
		}
	}
		
	//****************************************************************
	// Event Dispatch.
	//****************************************************************

	public void processInput() {
		if (nextInput == null) return;

		PInputEvent e = new PInputEvent(this, nextInput);
		
		Point2D newCurrentCanvasPosition = null;
		Point2D newLastCanvasPosition = null;
		
		if (e.isMouseEvent()) {
			if (e.isMouseEnteredOrMouseExited()) {
				PPickPath aPickPath = nextInputSource.pick(((MouseEvent)nextInput).getX(), ((MouseEvent)nextInput).getY(), 1);
				setMouseOver(aPickPath);
				previousMouseOver = aPickPath;
				newCurrentCanvasPosition = (Point2D) currentCanvasPosition.clone();
				newLastCanvasPosition = (Point2D) lastCanvasPosition.clone();
			} else {
				lastCanvasPosition.setLocation(currentCanvasPosition);
				currentCanvasPosition.setLocation(((MouseEvent)nextInput).getX(), ((MouseEvent)nextInput).getY());						
				PPickPath aPickPath = nextInputSource.pick(currentCanvasPosition.getX(), currentCanvasPosition.getY(), 1);
				setMouseOver(aPickPath);			
			}
		}
				
		nextInput = null;
		nextInputSource = null;
		
		this.processEvent(e, nextType);
		
		if (newCurrentCanvasPosition != null && newLastCanvasPosition != null) {
			currentCanvasPosition.setLocation(newCurrentCanvasPosition);
			lastCanvasPosition.setLocation(newLastCanvasPosition);
		}
	}

	public void processEventFromCamera(InputEvent event, int type, PCamera camera) {
		// queue input
		nextInput = event;
		nextType = type;
		nextInputSource = camera;
		
		// tell root to process queued inputs
		camera.getRoot().processInputs();
	}
	
	private void dispatchEventToListener(PInputEvent event, int type, PInputEventListener listener) {
		if (listener != null) {
			// clear the handled bit since the same event object is used to send multiple events such as
			// mouseEntered/mouseExited and mouseMove.
			event.setHandled(false); 
			listener.processEvent(event, type);
		}		
	}
}

