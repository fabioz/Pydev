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

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.util.PUtil;

/**
 * <b>PDragSequenceEventHandler</b> is designed to support mouse pressed, dragged, and
 * released interaction sequences. Support is also provided for running a continuous
 * activity during the drag sequence. 
 * <P>
 * PDragSequenceEventHandler should be subclassed by a concrete event handler
 * that implements a particular interaction. See PPanEventHandler, PZoomEventHandler,
 * and PDragEventHandler for examples.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public abstract class PDragSequenceEventHandler extends PBasicInputEventHandler {

	private double minDragStartDistance = 0;
	private transient boolean isDragging = false;
	private transient Point2D mousePressedCanvasPoint;
	private transient PActivity dragActivity;
	private transient PInputEvent dragEvent;
	private transient int sequenceInitiatedButton = MouseEvent.NOBUTTON;

	public PDragSequenceEventHandler() {
	}

	//****************************************************************
	// Basics
	//****************************************************************
	 
	public boolean isDragging() {
		return isDragging;
	}

	public void setIsDragging(boolean isDragging) {
		this.isDragging = isDragging;
	}
	
	public double getMinDragStartDistance() {
		return minDragStartDistance;
	}

	/**
	 * Set the minimum distance that the mouse should be dragged (in screen coords)
	 * before a new drag sequence is initiate.
	 */
	public void setMinDragStartDistance(double minDistance) {
		minDragStartDistance = minDistance;
	}

	/**
	 * Return the point in canvas coordinates where the mouse was last
	 * pressed.
	 */
	public Point2D getMousePressedCanvasPoint() {
		if (mousePressedCanvasPoint == null) {
			mousePressedCanvasPoint = new Point2D.Double();
		}
		return mousePressedCanvasPoint;
	}
	
	//****************************************************************
	// Dragging - Methods to indicate the stages of the drag sequence.
	//****************************************************************
	
	/**
	 * Subclasses should override this method to get notified of the start of
	 * a new drag sequence. Note that that overriding methods must still
	 * call super.startDrag() for correct behavior.
	 */
	protected void startDrag(PInputEvent e) {
		dragEvent = e;
		startDragActivity(e);
		setIsDragging(true);
		e.getComponent().setInteracting(true);
	}

	/**
	 * Subclasses should override this method to get notified of the drag
	 * events in a drag sequence. Note that that overriding methods must still
	 * call super.startDrag() for correct behavior.
	 */
	protected void drag(PInputEvent e) {
		dragEvent = e;
	}

	/**
	 * Subclasses should override this method to get notified of the end event
	 * in a drag sequence. Note that that overriding methods must still
	 * call super.startDrag() for correct behavior.
	 */
	protected void endDrag(PInputEvent e) {
		stopDragActivity(e);
		dragEvent = null;
		e.getComponent().setInteracting(false);
		setIsDragging(false);
	}

	protected boolean shouldStartDragInteraction(PInputEvent e) {
		return getMousePressedCanvasPoint().distance(e.getCanvasPosition()) >= getMinDragStartDistance();
	}
	
	//****************************************************************
	// Drag Activity - Used for scheduling an activity during a drag
	// sequence. For example zooming and auto panning are implemented
	// using this.
	//****************************************************************
	
	protected PActivity getDragActivity() {
		return dragActivity;
	}
	
	protected void startDragActivity(PInputEvent aEvent) {
		dragActivity = new PActivity(-1, PUtil.DEFAULT_ACTIVITY_STEP_RATE);
		dragActivity.setDelegate(new PActivity.PActivityDelegate() {
			public void activityStarted(PActivity activity) {
				dragActivityFirstStep(dragEvent);				
			}
			public void activityStepped(PActivity activity) {
				dragActivityStep(dragEvent);				
			}
			public void activityFinished(PActivity activity) {
				dragActivityFinalStep(dragEvent);				
			}
		});
		
		aEvent.getCamera().getRoot().addActivity(dragActivity);
	}
	
	protected void stopDragActivity(PInputEvent aEvent) {
		dragActivity.terminate();
		dragActivity = null;
	}
	
	/**
	 * Override this method to get notified when the drag activity
	 * starts stepping.
	 */
	protected void dragActivityFirstStep(PInputEvent aEvent) {
	}

	/**
	 * During a drag sequence an activity is scheduled that runs continuously
	 * while the drag sequence is active. This can be used to support some 
	 * additional behavior that is not driven directly by mouse events. For
	 * example PZoomEventHandler uses it for zooming and PPanEventHandler uses
	 * it for auto panning.
	 */
	protected void dragActivityStep(PInputEvent aEvent) {
	}

	/**
	 * Override this method to get notified when the drag activity
	 * stops stepping.
	 */
	protected void dragActivityFinalStep(PInputEvent aEvent) {
	}

	//****************************************************************
	// Events - subclasses should not override these methods, instead
	// override the appropriate drag method.
	//****************************************************************
	 
	public void mousePressed(PInputEvent e) {
		super.mousePressed(e);
		
		if (sequenceInitiatedButton == MouseEvent.NOBUTTON) {
			sequenceInitiatedButton = e.getButton();
		} else {
			return;
		}
		
		getMousePressedCanvasPoint().setLocation(e.getCanvasPosition());
		if (!isDragging()) {
			if (shouldStartDragInteraction(e)) {
				startDrag(e);
			}
		}
	}
	
	public void mouseDragged(PInputEvent e) {
		super.mouseDragged(e);

		if (sequenceInitiatedButton != MouseEvent.NOBUTTON) {
			if (!isDragging()) {
				if (shouldStartDragInteraction(e)) {
					startDrag(e);
				}
				return;
			}
			drag(e);
		}
	}
	
	public void mouseReleased(PInputEvent e) {
		super.mouseReleased(e);
		if (sequenceInitiatedButton == e.getButton()) {
			if (isDragging()) endDrag(e);
			sequenceInitiatedButton = MouseEvent.NOBUTTON;
		}
	}
	
	//****************************************************************
	// Debugging - methods for debugging
	//****************************************************************

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
		
		result.append("minDragStartDistance=" + minDragStartDistance);
		result.append(",mousePressedCanvasPoint=" + (mousePressedCanvasPoint == null ? "null" : mousePressedCanvasPoint.toString()));
		result.append(",sequenceInitiatedButton=" + sequenceInitiatedButton);
		if (isDragging) result.append(",dragging");
		result.append(',');
		result.append(super.paramString());
		
		return result.toString();
	}
}
