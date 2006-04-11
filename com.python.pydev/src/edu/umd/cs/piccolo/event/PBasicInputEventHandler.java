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
package edu.umd.cs.piccolo.event;

import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * <b>PBasicInputEventHandler</b> is the standard class in Piccolo that
 * is used to register for mouse and keyboard events on a PNode. Note the
 * events that you get depends on the node that you have registered with. For
 * example you will only get mouse moved events when the mouse is over the node
 * that you have registered with, not when the mouse is over some other node.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PBasicInputEventHandler implements PInputEventListener {

	private PInputEventFilter eventFilter;

	public PBasicInputEventHandler() {
		super();
		eventFilter = new PInputEventFilter();		
	}

	public void processEvent(PInputEvent event, int type) {
		if (!acceptsEvent(event, type)) return;

		switch (type) {
			case KeyEvent.KEY_PRESSED:
				keyPressed(event);
				break;

			case KeyEvent.KEY_RELEASED:
				keyReleased(event);
				break;

			case KeyEvent.KEY_TYPED:
				keyTyped(event);
				break;

			case MouseEvent.MOUSE_CLICKED:
				mouseClicked(event);
				break;

			case MouseEvent.MOUSE_DRAGGED:
				mouseDragged(event);
				break;

			case MouseEvent.MOUSE_ENTERED:
				mouseEntered(event);
				break;

			case MouseEvent.MOUSE_EXITED:
				mouseExited(event);
				break;

			case MouseEvent.MOUSE_MOVED:
				mouseMoved(event);
				break;

			case MouseEvent.MOUSE_PRESSED:
				mousePressed(event);
				break;

			case MouseEvent.MOUSE_RELEASED:
				mouseReleased(event);
				break;

			case MouseWheelEvent.WHEEL_UNIT_SCROLL:
				mouseWheelRotated(event);
				break;
			
			case MouseWheelEvent.WHEEL_BLOCK_SCROLL:
				mouseWheelRotatedByBlock(event);
				break;
				
			case FocusEvent.FOCUS_GAINED:
				keyboardFocusGained(event);
				break;
				
			case FocusEvent.FOCUS_LOST:
				keyboardFocusLost(event);
				break;
							
			default:
				throw new RuntimeException("Bad Event Type");
		}
	}
	
	//****************************************************************
	// Event Filter - All this event listener can be associated with a event
	// filter. The filter accepts and rejects events based on their modifier
	// flags and type. If the filter is null (the 
	// default case) then it accepts all events.
	//****************************************************************

	public boolean acceptsEvent(PInputEvent event, int type) {
		return eventFilter.acceptsEvent(event, type);
	}

	public PInputEventFilter getEventFilter() {
		return eventFilter;
	}

	public void setEventFilter(PInputEventFilter newEventFilter) {
		eventFilter = newEventFilter;
	}

	//****************************************************************
	// Events - Methods for handling events sent to the event listener.
	//****************************************************************
	 
	public void keyPressed(PInputEvent event) {
	}

	public void keyReleased(PInputEvent event) {
	}

	public void keyTyped(PInputEvent event) {
	}

	public void mouseClicked(PInputEvent event) {
	}

	public void mousePressed(PInputEvent event) {
	}

	public void mouseDragged(PInputEvent event) {
	}

	public void mouseEntered(PInputEvent event) {
	}

	public void mouseExited(PInputEvent event) {
	}

	public void mouseMoved(PInputEvent event) {
	}

	public void mouseReleased(PInputEvent event) {
	}
	
	public void mouseWheelRotated(PInputEvent event) {
	}
	
	public void mouseWheelRotatedByBlock(PInputEvent event) {
	}
	
	public void keyboardFocusGained(PInputEvent event) {
	}
	
	public void keyboardFocusLost(PInputEvent event) {
	}
	
	//****************************************************************
	// Debugging - methods for debugging
	//****************************************************************

	/**
	 * Returns a string representation of this object for debugging purposes.
	 */
	public String toString() {
		String result = super.toString().replaceAll(".*\\.", "");
		return result + "[" + paramString() + "]";
	}

	/**
	 * Returns a string representing the state of this node. This method is
	 * intended to be used only for debugging purposes, and the content and
	 * format of the returned string may vary between implementations. The
	 * returned string may be empty but may not be <code>null</code>.
	 *
	 * @return  a string representation of this node's state
	 */
	protected String paramString() {
		StringBuffer result = new StringBuffer();
		result.append("eventFilter=" + eventFilter == null ? "null" : eventFilter.toString());
		return result.toString();
	}	
}
