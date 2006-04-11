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
package edu.umd.cs.piccolo.util;

import java.awt.geom.Dimension2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

import javax.swing.event.EventListenerList;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventListener;

/**
 * <b>PPickPath</b> represents a ordered list of nodes that have been picked. 
 * The topmost ancestor node is the first node in the list (and should be a camera), 
 * the bottommost child node is at the end of the list. It is this bottom node that 
 * is given first chance to handle events, and that any active event handlers usually
 * manipulate.
 * <p>
 * Note that because of layers (which can be picked by multiple camera's) the ordered
 * list of nodes in a pick path do not all share a parent child relationship with the
 * nodes in the list next to them. This means that the normal localToGlobal methods don't
 * work when trying to transform geometry up and down the pick path, instead you should
 * use the pick paths canvasToLocal methods to get the mouse event points into your local
 * coord system.
 * <p>
 * Note that PInputEvent wraps most of the useful PPickPath methods, so often you
 * can use a PInputEvent directly instead of having to access its pick path.
 * <p>
 * @see edu.umd.cs.piccolo.event.PInputEvent
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PPickPath implements PInputEventListener {
	
	public static PPickPath CURRENT_PICK_PATH;

	private static double[] PTS = new double[4];
	
	private PStack nodeStack;
	private PStack transformStack;
	private PStack pickBoundsStack;
	private PCamera topCamera;
	private PCamera bottomCamera;
	private HashMap excludedNodes;
	
	public PPickPath(PCamera aCamera, PBounds aScreenPickBounds) {
		super();
		pickBoundsStack = new PStack();
		topCamera = aCamera;
		nodeStack = new PStack();
		transformStack = new PStack();
		pickBoundsStack.push(aScreenPickBounds);
		
		CURRENT_PICK_PATH = this;
	}	
	
	public PBounds getPickBounds() {
		return (PBounds) pickBoundsStack.peek();
	}
	
	public boolean acceptsNode(PNode node) {
		if (excludedNodes != null) {
			return !excludedNodes.containsKey(node);
		}
		return true;
	}
	
	//****************************************************************
	// Picked Nodes
	//****************************************************************
	
	public void pushNode(PNode aNode) {
		nodeStack.push(aNode);
	}

	public void popNode(PNode aNode) {
		nodeStack.pop();
	}
		
	/**
	 * Get the bottom node on the pick path node stack. That is the last node to
	 * be picked.
	 */
	public PNode getPickedNode() {
		return (PNode) nodeStack.peek();
	}

	//****************************************************************
	// Iterating over picked nodes.
	//****************************************************************

	/**
	 * Return the next node that will be picked after the current picked node.
	 * For instance of you have two overlaping children nodes then the topmost
	 * child will always be picked first, use this method to find the covered child.
	 * Return null when no more nodes will be picked.
	 */
	public PNode nextPickedNode() {
		PNode picked = getPickedNode();
		
		if (picked == topCamera) return null;		
		if (excludedNodes == null) excludedNodes = new HashMap();
		
		// exclude current picked node
		excludedNodes.put(picked, picked);
		
		Object screenPickBounds = pickBoundsStack.get(0);
		
		// reset path state
		pickBoundsStack = new PStack();
		nodeStack = new PStack();
		transformStack = new PStack();
		pickBoundsStack = new PStack();
		
		pickBoundsStack.push(screenPickBounds);

		// pick again
		topCamera.fullPick(this);
		
		// make sure top camera is pushed.
		if (getNodeStackReference().size() == 0) {
			pushNode(topCamera);
			pushTransform(topCamera.getTransformReference(false));
		}

		return getPickedNode();
	}
	
	/**
	 * Get the top camera on the pick path. This is the camera that originated the
	 * pick action.
	 */
	public PCamera getTopCamera() {
		return topCamera;
	}

	/**
	 * Get the bottom camera on the pick path. This may be different then the top
	 * camera if internal cameras are in use.
	 */ 
	public PCamera getBottomCamera() {
		if (bottomCamera == null) {
			for (int i = nodeStack.size() - 1; i >= 0; i--) {
				PNode each = (PNode) nodeStack.get(i);
				if (each instanceof PCamera) {
					bottomCamera = (PCamera) each;
					return bottomCamera;
				}				
			}			
		}
		return bottomCamera;
	}
	
	public PStack getNodeStackReference() {
		return nodeStack;
	}
	
	//****************************************************************
	// Path Transform 
	//****************************************************************

	public double getScale() {
		PTS[0] = 0;//x1
		PTS[1] = 0;//y1
		PTS[2] = 1;//x2
		PTS[3] = 0;//y2
		
		int count = transformStack.size();
		for (int i = 0; i < count; i++) {
			PAffineTransform each = ((PTuple)transformStack.get(i)).transform;
			if (each != null)
				each.transform(PTS, 0, PTS, 0, 2);
		}
		
		return Point2D.distance(PTS[0], PTS[1], PTS[2], PTS[3]);		
	}

	public void pushTransform(PAffineTransform aTransform) {
		transformStack.push(new PTuple(getPickedNode(), aTransform));
		if (aTransform != null) {
			Rectangle2D newPickBounds = (Rectangle2D) getPickBounds().clone();
			aTransform.inverseTransform(newPickBounds, newPickBounds);
			pickBoundsStack.push(newPickBounds);
		}
	}

	public void popTransform(PAffineTransform aTransform) {
		transformStack.pop();
		if (aTransform != null) {
			pickBoundsStack.pop();
		}
	}

	public PAffineTransform getPathTransformTo(PNode nodeOnPath) {
		PAffineTransform aTransform = new PAffineTransform();
		
		int count = transformStack.size();
		for (int i = 0; i < count; i++) {
			PTuple each = (PTuple) transformStack.get(i);
			if (each.transform != null) aTransform.concatenate(each.transform);
			if (nodeOnPath == each.node) {
				return aTransform;
			}
		}
		
		throw new RuntimeException("Node could not be found on pick path");
	}
	
	//****************************************************************
	// Process Events - Give each node in the pick path, starting at 
	// the bottom most one, a chance to handle the event.
	//****************************************************************
	
	public void processEvent(PInputEvent aEvent, int type) {
		aEvent.setPath(this);
		
		for (int i = nodeStack.size() - 1; i >= 0; i--) {
			PNode each = (PNode) nodeStack.get(i);

			EventListenerList list = each.getListenerList();
			
			if (list != null) {
				Object[] listeners = list.getListeners(PInputEventListener.class);
				
				for (int j = 0; j < listeners.length; j++) {
					PInputEventListener listener = (PInputEventListener) listeners[j];
					listener.processEvent(aEvent, type);					
				}
			}			
		}			
	}
		
	//****************************************************************
	// Transforming Geometry - Methods to transform geometry through
	// this path. 
	// <p>
	// Note that this is different that just using the
	// PNode.localToGlobal (an other coord system transform methods). 
	// The PNode coord system transform methods always go directly up
	// through their parents. The PPickPath coord system transform
	// methods go up through the list of picked nodes instead. And since
	// cameras can pick their layers in addition to their children these
	// two paths may be different.
	//****************************************************************
	
	/**
	 * Convert the given point from the canvas coordinates, down through
	 * the pick path (and through any camera view transforms applied to the
	 * path) to the local coordinates of the given node.
	 */
	public Point2D canvasToLocal(Point2D canvasPoint, PNode nodeOnPath) {
		try {
			return getPathTransformTo(nodeOnPath).inverseTransform(canvasPoint, canvasPoint);		
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Convert the given dimension from the canvas coordinates, down through
	 * the pick path (and through any camera view transforms applied to the
	 * path) to the local coordinates of the given node.
	 */
	public Dimension2D canvasToLocal(Dimension2D canvasDimension, PNode nodeOnPath) {
		return getPathTransformTo(nodeOnPath).inverseTransform(canvasDimension, canvasDimension);
	}

	/**
	 * Convert the given rectangle from the canvas coordinates, down through
	 * the pick path (and through any camera view transforms applied to the
	 * path) to the local coordinates of the given node.
	 */
	public Rectangle2D canvasToLocal(Rectangle2D canvasRectangle, PNode nodeOnPath) {
		return getPathTransformTo(nodeOnPath).inverseTransform(canvasRectangle, canvasRectangle);
	}	
	
	/**
	 * Used to associated nodes with their transforms on the transform stack.
	 */
	private static class PTuple {
		public PNode node;
		public PAffineTransform transform;
		
		public PTuple(PNode n, PAffineTransform t) {
			node = n;
			transform = t;
		}
	}
}
