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
package edu.umd.cs.piccolox.handles;

import java.awt.Color;
import java.awt.Shape;
import java.awt.event.InputEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PDragSequenceEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventFilter;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDimension;
import edu.umd.cs.piccolox.util.PLocator;
import edu.umd.cs.piccolox.util.PNodeLocator;

/**
 * <b>PHandle</b> is used to modify some aspect of Piccolo when it
 * is dragged. Each handle has a PLocator that it uses to automatically position
 * itself. See PBoundsHandle for an example of a handle that resizes the bounds
 * of another node.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PHandle extends PPath {

	public static float DEFAULT_HANDLE_SIZE = 8;
	public static Shape DEFAULT_HANDLE_SHAPE = new Ellipse2D.Float(0f, 0f, DEFAULT_HANDLE_SIZE, DEFAULT_HANDLE_SIZE);
	public static Color DEFAULT_COLOR = Color.white;
   
	private static PAffineTransform TEMP_TRANSFORM = new PAffineTransform();
	
	private PLocator locator;
	private transient PDragSequenceEventHandler handleDragger;

	/**
	 * Construct a new handle that will use the given locator
	 * to locate itself on its parent node.
	 */
	public PHandle(PLocator aLocator) {
		super(DEFAULT_HANDLE_SHAPE);
		locator = aLocator;
		setPaint(DEFAULT_COLOR);
		installHandleEventHandlers();
	}

	protected void installHandleEventHandlers() {
		handleDragger = new PDragSequenceEventHandler() {
			protected void startDrag(PInputEvent event) {
				super.startDrag(event);
				startHandleDrag(event.getPositionRelativeTo(PHandle.this), event);
			}
			protected void drag(PInputEvent event) {
				super.drag(event);
				PDimension aDelta = event.getDeltaRelativeTo(PHandle.this); 	
				if (aDelta.getWidth() != 0 || aDelta.getHeight() != 0) {
					dragHandle(aDelta, event);
				}
			}
			protected void endDrag(PInputEvent event) {
				super.endDrag(event);
				endHandleDrag(event.getPositionRelativeTo(PHandle.this), event);
			}
		};

		addPropertyChangeListener(PNode.PROPERTY_TRANSFORM, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				relocateHandle();
			}
		}); 	
		
		handleDragger.setEventFilter(new PInputEventFilter(InputEvent.BUTTON1_MASK));
		handleDragger.getEventFilter().setMarksAcceptedEventsAsHandled(true);
		handleDragger.getEventFilter().setAcceptsMouseEntered(false);
		handleDragger.getEventFilter().setAcceptsMouseExited(false);
		handleDragger.getEventFilter().setAcceptsMouseMoved(false); 	// no need for moved events for handle interaction, 
																		// so reject them so we don't consume them
		addInputEventListener(handleDragger);
	}
	
	/**
	 * Return the event handler that is responsible for the drag handle
	 * interaction.
	 */
	public PDragSequenceEventHandler getHandleDraggerHandler() {
		return handleDragger;
	}	

	/**
	 * Get the locator that this handle uses to position itself on its
	 * parent node.
	 */
	public PLocator getLocator() {
		return locator;
	}
	
	/**
	 * Set the locator that this handle uses to position itself on its
	 * parent node.
	 */
	public void setLocator(PLocator aLocator) {
		locator = aLocator;
		invalidatePaint();
		relocateHandle();
	}
	
	//****************************************************************
	// Handle Dragging - These are the methods the subclasses should
	// normally override to give a handle unique behavior.
	//****************************************************************
	
	/**
	 * Override this method to get notified when the handle starts to get dragged.
	 */
	public void startHandleDrag(Point2D aLocalPoint, PInputEvent aEvent) {
	}
	
	/**
	 * Override this method to get notified as the handle is dragged.
	 */
	public void dragHandle(PDimension aLocalDimension, PInputEvent aEvent) {
	}
	
	/**
	 * Override this method to get notified when the handle stops getting dragged.
	 */
	public void endHandleDrag(Point2D aLocalPoint, PInputEvent aEvent) {
	}
	
	//****************************************************************
	// Layout - When a handle's parent's layout changes the handle
	// invalidates its own layout and then repositions itself on its
	// parents bounds using its locator to determine that new
	// position.
	//****************************************************************
	
	public void setParent(PNode newParent) {
		super.setParent(newParent);
		relocateHandle();
	}
	
	public void parentBoundsChanged() {
		relocateHandle();
	}
		
	/**
	 * Force this handle to relocate itself using its locator.
	 */
	public void relocateHandle() {
		if (locator != null) {
			PBounds b = getBoundsReference();
			Point2D aPoint = locator.locatePoint(null);
			
			if (locator instanceof PNodeLocator) {
				PNode located = ((PNodeLocator)locator).getNode();
				PNode parent = getParent();
				
				located.localToGlobal(aPoint);
				globalToLocal(aPoint);
				
				if (parent != located && parent instanceof PCamera) {
					((PCamera)parent).viewToLocal(aPoint);
				}
			}
			
			double newCenterX = aPoint.getX();
			double newCenterY = aPoint.getY();

			if (newCenterX != b.getCenterX() ||
				newCenterY != b.getCenterY()) {
				
				centerBoundsOnPoint(newCenterX, newCenterY);
			}
		}
	}
		
	//****************************************************************
	// Serialization
	//****************************************************************
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		installHandleEventHandlers();
	}
}