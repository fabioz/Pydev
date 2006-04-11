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
package edu.umd.cs.piccolox.event;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.activities.PActivity;
import edu.umd.cs.piccolo.activities.PTransformActivity;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.event.PInputEventFilter;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDimension;

/**
 * <b>PNavigationEventHandler</b> implements simple focus based navigation. Uses 
 * mouse button one or the arrow keys to set a new focus. Animates the canvas
 * view to keep the focus node on the screen and at 100 percent scale with minimal 
 * view movement.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PNavigationEventHandler extends PBasicInputEventHandler {

	public static final int NORTH = 0;
	public static final int SOUTH = 1;
	public static final int EAST = 2;
	public static final int WEST = 3;
	public static final int IN = 4;
	public static final int OUT = 5;
	
	private static Hashtable NODE_TO_GLOBAL_NODE_CENTER_MAPPING = new Hashtable();
	
	private PNode focusNode;
	private PActivity navigationActivity;
	
	public PNavigationEventHandler() {
		super();
		setEventFilter(new PInputEventFilter(InputEvent.BUTTON1_MASK)); 	
	}

	//****************************************************************
	// Focus Change Events.
	//****************************************************************
	 
	public void keyPressed(PInputEvent e) {
		PNode oldLocation = focusNode;
		
		switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
				moveFocusLeft(e);
				break;

			case KeyEvent.VK_RIGHT:
				moveFocusRight(e);
				break;

			case KeyEvent.VK_UP:
			case KeyEvent.VK_PAGE_UP:
				if (e.isAltDown()) {
					moveFocusOut(e);
				} else {
					moveFocusUp(e);
				}
				break;

			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_PAGE_DOWN:
				if (e.isAltDown()) {
					moveFocusIn(e);
				} else {
					moveFocusDown(e);
				}
				break;
		}

		if (focusNode != null && oldLocation != focusNode) {
			directCameraViewToFocus(e.getCamera(), focusNode, 500);
		}		
	}
	
	public void mousePressed(PInputEvent aEvent) {
		moveFocusToMouseOver(aEvent);
		
		if (focusNode != null) {
			directCameraViewToFocus(aEvent.getCamera(), focusNode, 500);
			aEvent.getInputManager().setKeyboardFocus(aEvent.getPath());		
		}
	}
	
	//****************************************************************
	// Focus Movement - Moves the focus the specified direction. Left,
	// right, up, down mean move the focus to the closest sibling of the 
	// current focus node that exists in that direction. Move in means
	// move the focus to a child of the current focus, move out means
	// move the focus to the parent of the current focus.
	//****************************************************************
	
	public void moveFocusDown(PInputEvent e) {
		PNode n = getNeighborInDirection(SOUTH);

		if (n != null) {
			focusNode = n;
		}
	}
	
	public void moveFocusIn(PInputEvent e) {
		PNode n = getNeighborInDirection(IN);

		if (n != null) {
			focusNode = n;
		}		
	}
	
	public void moveFocusLeft(PInputEvent e) {
		PNode n = getNeighborInDirection(WEST);

		if (n != null) {
			focusNode = n;
		}
	}
	
	public void moveFocusOut(PInputEvent e) {
		PNode n = getNeighborInDirection(OUT);

		if (n != null) {
			focusNode = n;
		}		
	}
	
	public void moveFocusRight(PInputEvent e) {
		PNode n = getNeighborInDirection(EAST);

		if (n != null) {
			focusNode = n;
		}		
	}
		
	public void moveFocusUp(PInputEvent e) {
		PNode n = getNeighborInDirection(NORTH);

		if (n != null) {
			focusNode = n;
		}
	}

	public void moveFocusToMouseOver(PInputEvent e) {
		PNode focus = e.getPickedNode();
		if (!(focus instanceof PCamera)) {
			focusNode = focus;
		}
	}
	
	public PNode getNeighborInDirection(int aDirection) {
		if (focusNode == null) return null;

		NODE_TO_GLOBAL_NODE_CENTER_MAPPING.clear();

		Point2D highlightCenter = focusNode.getGlobalFullBounds().getCenter2D();
		NODE_TO_GLOBAL_NODE_CENTER_MAPPING.put(focusNode, highlightCenter);

		List l = getNeighbors();
		sortNodesByDistanceFromPoint(l, highlightCenter);

		Iterator i = l.iterator();
		while (i.hasNext()) {
			PNode each = (PNode) i.next();
			if (nodeIsNeighborInDirection(each, aDirection)) {
				return each;
			}
		}

		return null;
	}
	
	public List getNeighbors() {
		ArrayList result = new ArrayList();
		
		if (focusNode == null) return result;
		if (focusNode.getParent() == null) return result;

		PNode focusParent = focusNode.getParent();

		Iterator i = focusParent.getChildrenIterator();

		while (i.hasNext()) {
			PNode each = (PNode) i.next();
			if (each != focusNode && each.getPickable()) {
				result.add(each);
			}
		}

		result.add(focusParent);

		i = focusNode.getChildrenIterator();
		while (i.hasNext()) {
			result.add(i.next());
		}

		return result;
	}

	public boolean nodeIsNeighborInDirection(PNode aNode, int aDirection) {
		switch (aDirection) {
			case IN: {
				return aNode.isDescendentOf(focusNode);
			}

			case OUT: {
				return aNode.isAncestorOf(focusNode);
			}
			
			default: {
				if (aNode.isAncestorOf(focusNode) || aNode.isDescendentOf(focusNode)) {
					return false;
				}
			}
		}

		Point2D highlightCenter = (Point2D) NODE_TO_GLOBAL_NODE_CENTER_MAPPING.get(focusNode);
		Point2D nodeCenter = (Point2D) NODE_TO_GLOBAL_NODE_CENTER_MAPPING.get(aNode);

		double ytest1 = nodeCenter.getX() - highlightCenter.getX() + highlightCenter.getY();
		double ytest2 = -nodeCenter.getX() + highlightCenter.getX() + highlightCenter.getY();

		switch (aDirection) {
			case NORTH: {
				if (nodeCenter.getY() < highlightCenter.getY()) {
					if (nodeCenter.getY() < ytest1 && nodeCenter.getY() < ytest2) {
						return true;
					}
				}
				break;
			}

			case EAST: {
				if (nodeCenter.getX() > highlightCenter.getX()) {
					if (nodeCenter.getY() < ytest1 && nodeCenter.getY() > ytest2) {
						return true;
					}
				}
				break;
			}

			case SOUTH: {
				if (nodeCenter.getY() > highlightCenter.getY()) {
					if (nodeCenter.getY() > ytest1 && nodeCenter.getY() > ytest2) {
						return true;
					}
				}
				break;
			}
			case WEST: {
				if (nodeCenter.getX() < highlightCenter.getX()) {
					if (nodeCenter.getY() > ytest1 && nodeCenter.getY() < ytest2) {
						return true;
					}
				}
				break;
			}
		}
		return false;
	}
	
	public void sortNodesByDistanceFromPoint(List aNodesList, final Point2D aPoint) {
		Collections.sort(aNodesList, new Comparator() {
			public int compare(Object o1, Object o2) {
				PNode each1 = (PNode) o1;
				PNode each2 = (PNode) o2;
				Point2D each1Center = each1.getGlobalFullBounds().getCenter2D();
				Point2D each2Center = each2.getGlobalFullBounds().getCenter2D();

				NODE_TO_GLOBAL_NODE_CENTER_MAPPING.put(each1, each1Center);
				NODE_TO_GLOBAL_NODE_CENTER_MAPPING.put(each2, each2Center);

				double distance1 = aPoint.distance(each1Center);
				double distance2 = aPoint.distance(each2Center);

				if (distance1 < distance2) {
					return -1;
				} else if (distance1 == distance2) {
					return 0;
				} else {
					return 1;
				}
			}
		});
	}
	
	//****************************************************************
	// Canvas Movement - The canvas view is updated so that the current
	// focus remains visible on the screen at 100 percent scale.
	//****************************************************************

	protected PActivity animateCameraViewTransformTo(final PCamera aCamera, AffineTransform aTransform, int duration) {
		boolean wasOldAnimation = false;
		
		// first stop any old animations.
		if (navigationActivity != null) {
			navigationActivity.terminate();
			wasOldAnimation = true;
		}
			
		if (duration == 0) {
			aCamera.setViewTransform(aTransform);
			return null;
		}

		AffineTransform source = aCamera.getViewTransformReference();

		if (!source.equals(aTransform)) {
			navigationActivity = aCamera.animateViewToTransform(aTransform, duration);
			((PTransformActivity)navigationActivity).setSlowInSlowOut(!wasOldAnimation);
			return navigationActivity;			
		}
		
		return null;
	}

	public PActivity directCameraViewToFocus(PCamera aCamera, PNode aFocusNode, int duration) {
		AffineTransform originalViewTransform = aCamera.getViewTransform();

		// Scale the canvas to include
		PDimension d = new PDimension(1, 0);
		focusNode.globalToLocal(d);
		
		double scaleFactor = d.getWidth() / aCamera.getViewScale();
		Point2D scalePoint = focusNode.getGlobalFullBounds().getCenter2D();
		if (scaleFactor != 1) {
			aCamera.scaleViewAboutPoint(scaleFactor, scalePoint.getX(), scalePoint.getY());
		}
		
		// Pan the canvas to include the view bounds with minimal canvas
		// movement.
		aCamera.animateViewToPanToBounds(focusNode.getGlobalFullBounds(), 0);

		// Get rid of any white space. The canvas may be panned and
		// zoomed in to do this. But make sure not stay constrained by max
		// magnification.
		//fillViewWhiteSpace(aCamera);

		AffineTransform resultingTransform = aCamera.getViewTransform();
		aCamera.setViewTransform(originalViewTransform);

		// Animate the canvas so that it ends up with the given
		// view transform.
		return animateCameraViewTransformTo(aCamera, resultingTransform, duration);
	}

	protected void fillViewWhiteSpace(PCamera aCamera) {
		PBounds rootBounds = aCamera.getRoot().getFullBoundsReference();
		PBounds viewBounds = aCamera.getViewBounds();

		if (!rootBounds.contains(aCamera.getViewBounds())) {
			aCamera.animateViewToPanToBounds(rootBounds, 0);
			aCamera.animateViewToPanToBounds(focusNode.getGlobalFullBounds(), 0);

			// center content.
			double dx = 0;
			double dy = 0;
			viewBounds = aCamera.getViewBounds();

			if (viewBounds.getWidth() > rootBounds.getWidth()) {   // then center along x axis.
				double boundsCenterX = rootBounds.getMinX() + (rootBounds.getWidth() / 2);
				double viewBoundsCenterX = viewBounds.getMinX() + (viewBounds.getWidth() / 2);
				dx = viewBoundsCenterX - boundsCenterX;
			}

			if (viewBounds.getHeight() > rootBounds.getHeight()) { // then center along y axis.
				double boundsCenterY = rootBounds.getMinY() + (rootBounds.getHeight() / 2);
				double viewBoundsCenterY = viewBounds.getMinY() + (viewBounds.getHeight() / 2);
				dy = viewBoundsCenterY - boundsCenterY;
			}
			aCamera.translateView(dx, dy);
		}
	}
}
