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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * <b>PInputEventFilter</b> is a class that filters input events based on the
 * events modifiers and type. Any PBasicInputEventHandler that is associated 
 * with an event filter will only receive events that pass through the filter. 
 * <P>
 * To be accepted events must contain all the modifiers listed in the andMask,
 * at least one of the modifiers listed in the orMask, and none of the 
 * modifiers listed in the notMask. The event filter also lets you specify specific
 * event types (mousePressed, released, ...) to accept or reject.
 * <P>
 * If the event filter is set to consume, then it will call consume on any event
 * that it successfully accepts.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PInputEventFilter {

	public static int ALL_MODIFIERS_MASK = InputEvent.BUTTON1_MASK |
										   InputEvent.BUTTON2_MASK |
										   InputEvent.BUTTON3_MASK |
										   InputEvent.SHIFT_MASK |
										   InputEvent.CTRL_MASK |
										   InputEvent.ALT_MASK |
										   InputEvent.ALT_GRAPH_MASK |
										   InputEvent.META_MASK;

	private int andMask;
	private int orMask;
	private int notMask;
	private short clickCount = -1;

	private boolean marksAcceptedEventsAsHandled = false;
	
	private boolean acceptsAlreadyHandledEvents = false;
	private boolean acceptsKeyPressed = true;
	private boolean acceptsKeyReleased = true;
	private boolean acceptsKeyTyped = true;

	private boolean acceptsMouseClicked = true;
	private boolean acceptsMouseDragged = true;
	private boolean acceptsMouseEntered = true;
	private boolean acceptsMouseExited = true;
	private boolean acceptsMouseMoved = true;
	private boolean acceptsMousePressed = true;
	private boolean acceptsMouseReleased = true;
	private boolean acceptsMouseWheelRotated = true;
	private boolean acceptsFocusEvents = true;

	public PInputEventFilter() {
		acceptEverything();
	}

	public PInputEventFilter(int aAndMask) {
		this();
		andMask = aAndMask;
	}

	public PInputEventFilter(int aAndMask, int aNotMask) {
		this(aAndMask);
		notMask = aNotMask;
	}

	public boolean acceptsEvent(PInputEvent aEvent, int type) {
		boolean aResult = false;
		int modifiers = 0;
		
		if (!aEvent.isFocusEvent()) {
			modifiers = aEvent.getModifiers();
		}
				
		if ((!aEvent.isHandled() || acceptsAlreadyHandledEvents) &&
			(modifiers == 0 ||					   	// if no modifiers then ignore modifier constraints, ELSE
			(modifiers & andMask) == andMask &&  	// must have all modifiers from the AND mask and
			(modifiers & orMask) != 0 &&		   	// must have at least one modifier from the OR mask and
			(modifiers & notMask) == 0)) {		   	// can't have any modifiers from the NOT mask

			if (aEvent.isMouseEvent() && clickCount != -1 && clickCount != aEvent.getClickCount()) {
				aResult = false;
			} else {
				switch (type) {
					case KeyEvent.KEY_PRESSED:
						aResult = getAcceptsKeyPressed();
						break;

					case KeyEvent.KEY_RELEASED:
						aResult = getAcceptsKeyReleased();
						break;

					case KeyEvent.KEY_TYPED:
						aResult = getAcceptsKeyTyped();
						break;

					case MouseEvent.MOUSE_CLICKED:
						aResult = getAcceptsMouseClicked();
						break;

					case MouseEvent.MOUSE_DRAGGED:
						aResult = getAcceptsMouseDragged();
						break;

					case MouseEvent.MOUSE_ENTERED:
						aResult = getAcceptsMouseEntered();
						break;

					case MouseEvent.MOUSE_EXITED:
						aResult = getAcceptsMouseExited();
						break;

					case MouseEvent.MOUSE_MOVED:
						aResult = getAcceptsMouseMoved();
						break;

					case MouseEvent.MOUSE_PRESSED:
						aResult = getAcceptsMousePressed();
						break;

					case MouseEvent.MOUSE_RELEASED:
						aResult = getAcceptsMouseReleased();
						break;

					case MouseWheelEvent.WHEEL_UNIT_SCROLL:
					case MouseWheelEvent.WHEEL_BLOCK_SCROLL:
						aResult = getAcceptsMouseWheelRotated();
						break;
						
					case FocusEvent.FOCUS_GAINED:
					case FocusEvent.FOCUS_LOST:
						aResult = getAcceptsFocusEvents();
						break;

					default:
						throw new RuntimeException("PInputEvent with bad ID");
				}
			}
		}

		if (aResult && getMarksAcceptedEventsAsHandled()) {
			aEvent.setHandled(true);	 
		}

		return aResult;
	}

	public void acceptAllClickCounts() {
		clickCount = -1;
	}

	public void acceptAllEventTypes() {
		acceptsKeyPressed = true;
		acceptsKeyReleased = true;
		acceptsKeyTyped = true;
		acceptsMouseClicked = true;
		acceptsMouseDragged = true;
		acceptsMouseEntered = true;
		acceptsMouseExited = true;
		acceptsMouseMoved = true;
		acceptsMousePressed = true;
		acceptsMouseReleased = true;
		acceptsMouseWheelRotated = true;
		acceptsFocusEvents = true;
	}

	public void acceptEverything() {
		acceptAllEventTypes();
		setAndMask(0);
		setOrMask(ALL_MODIFIERS_MASK);
		setNotMask(0);
		acceptAllClickCounts();
	}

	public boolean getAcceptsKeyPressed() {
		return acceptsKeyPressed;
	}

	public boolean getAcceptsKeyReleased() {
		return acceptsKeyReleased;
	}

	public boolean getAcceptsKeyTyped() {
		return acceptsKeyTyped;
	}

	public boolean getAcceptsMouseClicked() {
		return acceptsMouseClicked;
	}

	public boolean getAcceptsMouseDragged() {
		return acceptsMouseDragged;
	}

	public boolean getAcceptsMouseEntered() {
		return acceptsMouseEntered;
	}

	public boolean getAcceptsMouseExited() {
		return acceptsMouseExited;
	}

	public boolean getAcceptsMouseMoved() {
		return acceptsMouseMoved;
	}

	public boolean getAcceptsMousePressed() {
		return acceptsMousePressed;
	}

	public boolean getAcceptsMouseReleased() {
		return acceptsMouseReleased;
	}

	public boolean getAcceptsMouseWheelRotated() {
		return acceptsMouseWheelRotated;
	}

	public boolean getAcceptsFocusEvents() {
		return acceptsFocusEvents;
	}

	public boolean getAcceptsAlreadyHandledEvents() {
		return acceptsAlreadyHandledEvents;
	}

	public boolean getMarksAcceptedEventsAsHandled() {
		return marksAcceptedEventsAsHandled;
	}

	public void rejectAllClickCounts() {
		clickCount = Short.MAX_VALUE;
	}

	public void rejectAllEventTypes() {
		acceptsKeyPressed = false;
		acceptsKeyReleased = false;
		acceptsKeyTyped = false;
		acceptsMouseClicked = false;
		acceptsMouseDragged = false;
		acceptsMouseEntered = false;
		acceptsMouseExited = false;
		acceptsMouseMoved = false;
		acceptsMousePressed = false;
		acceptsMouseReleased = false;
		acceptsMouseWheelRotated = false;
		acceptsFocusEvents = false;
	}

	public void setAcceptClickCount(short aClickCount) {
		clickCount = aClickCount;
	}

	public void setAcceptsKeyPressed(boolean aBoolean) {
		acceptsKeyPressed = aBoolean;
	}

	public void setAcceptsKeyReleased(boolean aBoolean) {
		acceptsKeyReleased = aBoolean;
	}

	public void setAcceptsKeyTyped(boolean aBoolean) {
		acceptsKeyTyped = aBoolean;
	}

	public void setAcceptsMouseClicked(boolean aBoolean) {
		acceptsMouseClicked = aBoolean;
	}

	public void setAcceptsMouseDragged(boolean aBoolean) {
		acceptsMouseDragged = aBoolean;
	}

	public void setAcceptsMouseEntered(boolean aBoolean) {
		acceptsMouseEntered = aBoolean;
	}

	public void setAcceptsMouseExited(boolean aBoolean) {
		acceptsMouseExited = aBoolean;
	}

	public void setAcceptsMouseMoved(boolean aBoolean) {
		acceptsMouseMoved = aBoolean;
	}

	public void setAcceptsMousePressed(boolean aBoolean) {
		acceptsMousePressed = aBoolean;
	}

	public void setAcceptsMouseReleased(boolean aBoolean) {
		acceptsMouseReleased = aBoolean;
	}

	public void setAcceptsMouseWheelRotated(boolean aBoolean) {
		acceptsMouseWheelRotated = aBoolean;
	}
	
	public void setAcceptsFocusEvents(boolean aBoolean) {
		acceptsFocusEvents = aBoolean;
	}

	public void setAndMask(int aAndMask) {
		andMask = aAndMask;
	}
	
	public void setAcceptsAlreadyHandledEvents(boolean aBoolean) {
		acceptsAlreadyHandledEvents = aBoolean;
	}

	public void setMarksAcceptedEventsAsHandled(boolean aBoolean) {
		marksAcceptedEventsAsHandled = aBoolean;
	}

	public void setNotMask(int aNotMask) {
		notMask = aNotMask;
	}

	public void setOrMask(int aOrMask) {
		orMask = aOrMask;
	}	
}
