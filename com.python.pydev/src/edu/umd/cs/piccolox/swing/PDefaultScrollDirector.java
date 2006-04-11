/* 
 * Copyright (C) 2002-@year@ by University of Maryland, College Park, MD 20742, USA 
 * All rights reserved. 
 * 
 * Piccolo was written at the Human-Computer Interaction Laboratory 
 * www.cs.umd.edu/hcil by Jesse Grosjean under the supervision of Ben Bederson. 
 * The Piccolo website is www.cs.umd.edu/hcil/piccolo 
 */
package edu.umd.cs.piccolox.swing;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.JScrollPane;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.PRoot;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * The default scroll director implementation. This default implementation
 * follows the widely accepted model of scrolling - namely the scrollbars
 * control the movement of the window over the document rather than the movement
 * of the document under the window.
 *
 * @author Lance Good
 */
public class PDefaultScrollDirector implements PScrollDirector, PropertyChangeListener {

	/**
	 * The viewport that signals this scroll director
	 */
	protected PViewport viewPort;

	/**
	 * The scrollpane that contains the viewport
	 */
	protected PScrollPane scrollPane;

	/**
	 * The canvas that this class directs
	 */
	protected PCanvas view;

	/**
	 * The canvas' camera
	 */
	protected PCamera camera;

	/**
	 * The canvas' root
	 */
	protected PRoot root;

	/**
	 * Flag to indicate when scrolling is currently in progress
	 */
	protected boolean scrollInProgress = false;

	/**
	 * The default constructor
	 */
	public PDefaultScrollDirector() {
	}

	/**
	 * Installs the scroll director and adds the appropriate listeners
	 * @param viewPort The viewport on which this director directs
	 * @param view The ZCanvas that the viewport looks at
	 */
	public void install(PViewport viewPort, final PCanvas view) {
		this.scrollPane = (PScrollPane) viewPort.getParent();
		this.viewPort = viewPort;
		this.view = view;

		if (view != null) {
			this.camera = view.getCamera();
			this.root = view.getRoot();
		}

		if (camera != null) {
			camera.addPropertyChangeListener(this);
		}
		if (root != null) {
			root.addPropertyChangeListener(this);				
		}

		if (scrollPane != null) {
			scrollPane.revalidate();
		}		
	}

	/**
	 * Uninstall the scroll director from the viewport
	 */
	public void unInstall() {
		viewPort = null;
		view = null;

		if (camera != null) {
			camera.removePropertyChangeListener(this);			
		}
		if (root != null) {
			root.removePropertyChangeListener(this);
		}

		camera = null;
		root = null;
	}

	/**
	 * Get the View position given the specified camera bounds
	 * @param viewBounds The bounds for which the view position will be computed
	 * @return The view position
	 */
	public Point getViewPosition(Rectangle2D viewBounds) {
		Point pos = new Point();
		if (camera != null) {
			// First we compute the union of all the layers
			PBounds layerBounds = new PBounds();
			List layers = camera.getLayersReference();
			for(Iterator i=layers.iterator(); i.hasNext();) {
				PLayer layer = (PLayer)i.next();
				layerBounds.add(layer.getFullBoundsReference());
			}

			// Then we put the bounds into camera coordinates and
			// union the camera bounds
			camera.viewToLocal(layerBounds);
			layerBounds.add(viewBounds);

			pos.setLocation((int) (viewBounds.getX() - layerBounds.getX() + 0.5), (int) (viewBounds.getY() - layerBounds.getY() + 0.5));
		}

		return pos;
	}

	/**
	 * Get the size of the view based on the specified camera bounds
	 * @param viewBounds The view bounds for which the view size will be computed
	 * @return The view size
	 */
	public Dimension getViewSize(Rectangle2D viewBounds) {
		Dimension size = new Dimension();
		if (camera != null) {
			// First we compute the union of all the layers
			PBounds bounds = new PBounds();
			List layers = camera.getLayersReference();
			for(Iterator i=layers.iterator(); i.hasNext();) {
				PLayer layer = (PLayer)i.next();
				bounds.add(layer.getFullBoundsReference());
			}

			// Then we put the bounds into camera coordinates and
			// union the camera bounds		
			if (!bounds.isEmpty()) {
				camera.viewToLocal(bounds);
			}
			bounds.add(viewBounds);

			size.setSize((int) (bounds.getWidth() + 0.5), (int) (bounds.getHeight() + 0.5));
		}

		return size;
	}

	/**
	 * Set the view position in a manner consistent with standardized scrolling
	 * @param x The new x position
	 * @param y The new y position
	 */
	public void setViewPosition(double x, double y) {
		if (camera != null) {
			// If a scroll is in progress - we ignore new scrolls -
			// if we didn't, since the scrollbars depend on the camera location
			// we can end up with an infinite loop
			if (!scrollInProgress) {
				scrollInProgress = true;

				// Get the union of all the layers' bounds
				PBounds layerBounds = new PBounds();
				List layers = camera.getLayersReference();
				for(Iterator i=layers.iterator(); i.hasNext();) {
					PLayer layer = (PLayer)i.next();
					layerBounds.add(layer.getFullBoundsReference());
				}
	
				PAffineTransform at = camera.getViewTransform();
				at.transform(layerBounds,layerBounds);

				// Union the camera bounds
				PBounds viewBounds = camera.getBoundsReference();
				layerBounds.add(viewBounds);

				// Now find the new view position in view coordinates
				Point2D newPoint = new Point2D.Double(layerBounds.getX() + x, layerBounds.getY() + y);

				// Now transform the new view position into global coords
				camera.localToView(newPoint);

				// Compute the new matrix values to put the camera at the
				// correct location
				double newX = - (at.getScaleX() * newPoint.getX() + at.getShearX() * newPoint.getY());
				double newY = - (at.getShearY() * newPoint.getX() + at.getScaleY() * newPoint.getY());

				at.setTransform(at.getScaleX(), at.getShearY(), at.getShearX(), at.getScaleY(), newX, newY);

				// Now actually set the camera's transform
				camera.setViewTransform(at);
				scrollInProgress = false;
			}
		}
	}

	/**
	 * Invoked when the camera's view changes, or the bounds of the root or camera changes
	 */
	public void propertyChange(PropertyChangeEvent pce) {
		boolean isRelevantViewEvent = (PCamera.PROPERTY_VIEW_TRANSFORM == pce.getPropertyName());
		boolean isRelevantBoundsEvent = (PNode.PROPERTY_BOUNDS == pce.getPropertyName() || PNode.PROPERTY_FULL_BOUNDS == pce.getPropertyName()) && (pce.getSource() == camera || pce.getSource() == view.getRoot());		
		if (isRelevantViewEvent || isRelevantBoundsEvent) {
			if (shouldRevalidateScrollPane()) {
				scrollPane.revalidate();	
			}
			else {
				viewPort.fireStateChanged();	
			}
		}		
	}

	/**
	 * Should the ScrollPane be revalidated. This occurs when either the
	 * scrollbars are showing and should be remove or are not showing and should
	 * be added.
	 * 
	 * @return Whether the scroll pane should be revalidated
	 */
	public boolean shouldRevalidateScrollPane() {
		if (camera != null) {
			if (scrollPane.getHorizontalScrollBarPolicy() != JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
					&& scrollPane.getVerticalScrollBarPolicy() != JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED) {
				return false;
			}
			
			// Get the union of all the layers' bounds
			PBounds layerBounds = new PBounds();
			List layers = camera.getLayersReference();
			for(Iterator i=layers.iterator(); i.hasNext();) {
				PLayer layer = (PLayer)i.next();
				layerBounds.add(layer.getFullBoundsReference());
			}

			// Put into camera coordinates
			camera.viewToLocal(layerBounds);

			// And union with the camera bounds
			PBounds cameraBounds = camera.getBoundsReference();
			layerBounds.add(cameraBounds);

			// Truncate these to ints before comparing since
			// that's what the ScrollPane uses 
			int layerWidth = (int) (layerBounds.getWidth() + 0.5);
			int layerHeight = (int) (layerBounds.getHeight() + 0.5);
			int cameraWidth = (int) (cameraBounds.getWidth() + 0.5);
			int cameraHeight = (int) (cameraBounds.getHeight() + 0.5);

			if ((scrollPane.getHorizontalScrollBar().isShowing() && layerWidth <= cameraWidth)
				|| (!scrollPane.getHorizontalScrollBar().isShowing() && layerWidth > cameraWidth)
				|| (scrollPane.getVerticalScrollBar().isShowing() && layerHeight <= cameraHeight)
				|| (!scrollPane.getVerticalScrollBar().isShowing() && layerHeight > cameraHeight)) {
				return true;
			}
		}
		return false;
	}
}
