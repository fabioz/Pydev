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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.piccolo.activities.PTransformActivity;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDebug;
import edu.umd.cs.piccolo.util.PDimension;
import edu.umd.cs.piccolo.util.PObjectOutputStream;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.util.PPickPath;
import edu.umd.cs.piccolo.util.PUtil;

/**
 * <b>PCamera</b> represents a viewport onto a list of layer nodes.
 * Each camera maintains a view transform through which it views these
 * layers. Translating and scaling this view transform is how zooming
 * and panning are implemented.
 * <p>
 * Cameras are also the point through which all PInputEvents enter Piccolo. The
 * canvas coordinate system, and the local coordinate system of the topmost camera
 * should always be the same.
 * <p>
 * @see PLayer
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PCamera extends PNode {
	
	/** 
	 * The property name that identifies a change in the set of this camera's
	 * layers (see {@link #getLayer getLayer}, {@link #getLayerCount
	 * getLayerCount}, {@link #getLayersReference getLayersReference}). A
	 * property change event's new value will be a reference to the list of this
	 * nodes layers, but old value will always be null.
	 */
	public static final String PROPERTY_LAYERS = "layers";
    public static final int PROPERTY_CODE_LAYERS = 1 << 11;
	
	/** 
	 * The property name that identifies a change in this camera's view
	 * transform (see {@link #getViewTransform getViewTransform}, {@link
	 * #getViewTransformReference getViewTransformReference}). A property change
	 * event's new value will be a reference to the view transform, but old
	 * value will always be null.
	 */
	public static final String PROPERTY_VIEW_TRANSFORM = "viewTransform";
    public static final int PROPERTY_CODE_VIEW_TRANSFORM = 1 << 12;
	
	public static final int VIEW_CONSTRAINT_NONE = 0;
	public static final int VIEW_CONSTRAINT_ALL = 1;
	public static final int VIEW_CONSTRAINT_CENTER = 2;
			
	private transient PComponent component;
	private transient List layers;
	private PAffineTransform viewTransform;
	private int viewConstraint;
	
	/**
	 * Construct a new camera with no layers and a default white color.
	 */
	public PCamera() {
		super();
		viewTransform = new PAffineTransform();
		layers = new ArrayList();
		viewConstraint = VIEW_CONSTRAINT_NONE;
	}

	/**
	 * Get the canvas associated with this camera. This will return null if
	 * not canvas has been associated, as may be the case for internal cameras.
	 */ 	
	public PComponent getComponent() {
		return component;
	}
	
	/**
	 * Set the canvas associated with this camera. When the camera is repainted
	 * it will request repaints on this canvas.
	 */
	public void setComponent(PComponent aComponent) {
		component = aComponent;
		invalidatePaint();
	}
				
	/**
	 * Repaint this camera, and forward the repaint request to the camera's
	 * canvas if it is not null.
	 */
	public void repaintFrom(PBounds localBounds, PNode descendentOrThis) {
		if (getParent() != null) {
			if (descendentOrThis != this) {
				localToParent(localBounds);
			}
			
			if (component != null) {
				component.repaint(localBounds);
			}
			
			getParent().repaintFrom(localBounds, this);
		}
	}

	private static PBounds TEMP_REPAINT_RECT = new PBounds();
		
	/**
	 * Repaint from one of the cameras layers. The repaint region needs to be
	 * transformed from view to local in this case. Unlike most repaint
	 * methods in piccolo this one must not modify the viewBounds parameter.
	 */
	public void repaintFromLayer(PBounds viewBounds, PNode repaintedLayer) {
		TEMP_REPAINT_RECT.setRect(viewBounds);
		
		viewToLocal(TEMP_REPAINT_RECT);
		if (getBoundsReference().intersects(TEMP_REPAINT_RECT)) {
			PBounds.intersect(TEMP_REPAINT_RECT, getBoundsReference(), TEMP_REPAINT_RECT);
			repaintFrom(TEMP_REPAINT_RECT, repaintedLayer);
		}
	}
	
	//****************************************************************
	// Layers
	//****************************************************************
	
	/**
	 * Return a reference to the list of layers managed by this camera.
	 */
	public List getLayersReference() {
		return layers;
	}
	
	public int getLayerCount() {
		return layers.size();
	}

	public PLayer getLayer(int index) {
		return (PLayer) layers.get(index);
	}
	
	public int indexOfLayer(PLayer layer) {
		return layers.indexOf(layer);
	}

	/**
	 * Add the layer to the end of this camera's list of layers. 
	 * Layers may be viewed by multiple cameras at once.
	 */ 
	public void addLayer(PLayer layer) {
		addLayer(layers.size(), layer);
	}

	/**
	 * Add the layer at the given index in this camera's list of layers. 
	 * Layers may be viewed by multiple cameras at once.
	 */ 
	public void addLayer(int index, PLayer layer) {
		layers.add(index, layer);
		layer.addCamera(this);
		invalidatePaint();
		firePropertyChange(PROPERTY_CODE_LAYERS ,PROPERTY_LAYERS, null, layers); 			
	}
	
	/**
	 * Remove the given layer from the list of layers managed by this
	 * camera.
	 */
	public PLayer removeLayer(PLayer layer) {
		return removeLayer(layers.indexOf(layer));
	}
		
	/**
	 * Remove the layer at the given index from the list of 
	 * layers managed by this camera.
	 */
	public PLayer removeLayer(int index) {
		PLayer layer = (PLayer) layers.remove(index);
		layer.removeCamera(this);
		invalidatePaint();
		firePropertyChange(PROPERTY_CODE_LAYERS, PROPERTY_LAYERS, null, layers); 			
		return layer;
	}
	
	/**
	 * Return the total bounds of all the layers that this camera looks at.
	 */
	public PBounds getUnionOfLayerFullBounds() {
		PBounds result = new PBounds();
		
		int count = getLayerCount();			
		for (int i = 0; i < count; i++) {
			PLayer each = (PLayer) layers.get(i);
			result.add(each.getFullBoundsReference());
		}					
		
		return result;
	}
	
	//****************************************************************
	// Painting Layers
	//****************************************************************
	
	/**
	 * Paint this camera (default background color is white) and then paint
	 * the cameras view through the view transform.
	 */
	protected void paint(PPaintContext paintContext) {
		super.paint(paintContext);
		
		paintContext.pushClip(getBoundsReference());			
		paintContext.pushTransform(viewTransform);

		paintCameraView(paintContext);		
		paintDebugInfo(paintContext);
						
		paintContext.popTransform(viewTransform);
		paintContext.popClip(getBoundsReference());
	}
	
	/**
	 * Paint all the layers that the camera is looking at, this method is
	 * only called when the cameras view transform and clip are applied
	 * to the paintContext.
	 */
	protected void paintCameraView(PPaintContext paintContext) {
		int count = getLayerCount();			
		for (int i = 0; i < count; i++) {
			PLayer each = (PLayer) layers.get(i);
			each.fullPaint(paintContext);
		}			
	}

	protected void paintDebugInfo(PPaintContext paintContext) {
		if (PDebug.debugBounds || PDebug.debugFullBounds) {
			Graphics2D g2 = paintContext.getGraphics();
			paintContext.setRenderQuality(PPaintContext.LOW_QUALITY_RENDERING);
			g2.setStroke(new BasicStroke(0));
			ArrayList nodes = new ArrayList();
			PBounds nodeBounds = new PBounds();
		
			Color boundsColor = Color.red;
			Color fullBoundsColor = new Color(1.0f, 0f, 0f, 0.2f);
		
			for (int i = 0; i < getLayerCount(); i++) {
				getLayer(i).getAllNodes(null, nodes);
			}				
			
			Iterator i = getAllNodes(null, nodes).iterator();
			
			while (i.hasNext()) {
				PNode each = (PNode) i.next();
		
				if (PDebug.debugBounds) {
					g2.setPaint(boundsColor);
					nodeBounds.setRect(each.getBoundsReference());
					
					if (!nodeBounds.isEmpty()) {
						each.localToGlobal(nodeBounds);
						globalToLocal(nodeBounds);
						if (each == this || each.isDescendentOf(this)) {
							localToView(nodeBounds);
						}
						g2.draw(nodeBounds);
					}
				}
		
				if (PDebug.debugFullBounds) {
					g2.setPaint(fullBoundsColor);
					nodeBounds.setRect(each.getFullBoundsReference());

					if (!nodeBounds.isEmpty()) {
						if (each.getParent() != null) {
							each.getParent().localToGlobal(nodeBounds);
						}
						globalToLocal(nodeBounds);		
						if (each == this || each.isDescendentOf(this)) {
							localToView(nodeBounds);
						}	
						g2.fill(nodeBounds);				
					}
				}
			}
		}
	}

	/**
	 * Override fullPaint to push the camera onto the paintContext so that it
	 * can be later be accessed by PPaintContext.getCamera();
	 */
	public void fullPaint(PPaintContext paintContext) {
		paintContext.pushCamera(this);
		super.fullPaint(paintContext);
		paintContext.popCamera(this);		
	}
	
	//****************************************************************
	// Picking
	//****************************************************************

	/**
	 * Generate and return a PPickPath for the point x,y specified in the local
	 * coord system of this camera. Picking is done with a rectangle, halo
	 * specifies how large that rectangle will be.
	 */
	public PPickPath pick(double x, double y, double halo) {		
		PBounds b = new PBounds(new Point2D.Double(x, y), -halo, -halo);
		PPickPath result = new PPickPath(this, b);
		
		fullPick(result);
			
		// make sure this camera is pushed.
		if (result.getNodeStackReference().size() == 0) {
			result.pushNode(this);
			result.pushTransform(getTransformReference(false));
		}
		
		return result;
	}
	
	/**
	 * After the direct children of the camera have been given a chance to be
	 * picked objects viewed by the camera are given a chance to be picked.
	 */
	protected boolean pickAfterChildren(PPickPath pickPath) {
		if (intersects(pickPath.getPickBounds())) { 		
			pickPath.pushTransform(viewTransform);
			
			if (pickCameraView(pickPath)) {
				return true;	
			}
				
			pickPath.popTransform(viewTransform);			
			return true;
		}
		return false;
	}
	
	/**
	 * Pick all the layers that the camera is looking at, this method is
	 * only called when the cameras view transform and clip are applied
	 * to the pickPath.
	 */
	protected boolean pickCameraView(PPickPath pickPath) {
		int count = getLayerCount();
		for (int i = count - 1; i >= 0; i--) {
			PLayer each = (PLayer) layers.get(i);
			if (each.fullPick(pickPath)) {
				return true;
			}
		}
		return false;
	}
	
		
	//****************************************************************
	// View Transform - Methods for accessing the view transform. The
	// view transform is applied before painting and picking the cameras
	// layers. But not before painting or picking its direct children.
	// 
	// Changing the view transform is how zooming and panning are
	// accomplished.
	//****************************************************************

	/**
	 * Return the bounds of this camera in the view coordinate system.
	 */
	public PBounds getViewBounds() {
		return (PBounds) localToView(getBounds());
	}
		 
	/**
	 * Translates and scales the camera's view transform so that the given bounds (in camera
	 * layer's coordinate system)are centered withing the cameras view bounds. Use this method
	 * to point the camera at a given location.
	 */
	public void setViewBounds(Rectangle2D centerBounds) {
		animateViewToCenterBounds(centerBounds, true, 0);
	}

	/**
	 * Return the scale applied by the view transform to the layers
	 * viewed by this camera.
	 */
	public double getViewScale() {
		return viewTransform.getScale();
	}

	/**
	 * Scale the view transform that is applied to the layers
	 * viewed by this camera by the given amount.
	 */
	public void scaleView(double scale) {
		scaleViewAboutPoint(scale, 0, 0);
	}

	/**
	 * Scale the view transform that is applied to the layers
	 * viewed by this camera by the given amount about the given point.
	 */
	public void scaleViewAboutPoint(double scale, double x, double y) {
		viewTransform.scaleAboutPoint(scale, x, y);
		applyViewConstraints();
		invalidatePaint();
		firePropertyChange(PROPERTY_CODE_VIEW_TRANSFORM, PROPERTY_VIEW_TRANSFORM, null, viewTransform);				
	}

	/**
	 * Set the scale of the view transform that is applied to 
	 * the layers viewed by this camera.
	 */
	public void setViewScale(double scale) {
		scaleView(scale / getViewScale());
	}

	/**
	 * Translate the view transform that is applied to the camera's
	 * layers.
	 */
	public void translateView(double dx, double dy) {
		viewTransform.translate(dx, dy);
		applyViewConstraints();
		invalidatePaint();
		firePropertyChange(PROPERTY_CODE_VIEW_TRANSFORM, PROPERTY_VIEW_TRANSFORM, null, viewTransform);				
	}

	/**
	 * Sets the offset of the view transform that is applied 
	 * to the camera's layers.
	 */
	public void setViewOffset(double x, double y) {
		viewTransform.setOffset(x, y);
		applyViewConstraints();
		invalidatePaint();
		firePropertyChange(PROPERTY_CODE_VIEW_TRANSFORM, PROPERTY_VIEW_TRANSFORM, null, viewTransform);				
	}

	/**
	 * Get a copy of the view transform that is applied to the camera's
	 * layers.
	 */
	public PAffineTransform getViewTransform() {
		return (PAffineTransform) viewTransform.clone();
	}

	/**
	 * Get a reference to the view transform that is applied to the camera's
	 * layers.
	 */
	public PAffineTransform getViewTransformReference() {
		return viewTransform;
	}
	
	/**
	 * Set the view transform that is applied to the views layers.
	 */
	public void setViewTransform(AffineTransform aTransform) {
		viewTransform.setTransform(aTransform);
		applyViewConstraints();
		invalidatePaint();
		firePropertyChange(PROPERTY_CODE_VIEW_TRANSFORM, PROPERTY_VIEW_TRANSFORM, null, viewTransform);				
	}

	/**
	 * Animate the camera's view from its current transform when the activity
	 * starts to a new transform that centers the given bounds in the camera
	 * layers coordinate system into the cameras view bounds. If the duration is
	 * 0 then the view will be transformed immediately, and null will be
	 * returned. Else a new PTransformActivity will get returned that is set to
	 * animate the camera's view transform to the new bounds. If shouldScale
	 * is true, then the camera will also scale its view so that the given
	 * bounds fit fully within the cameras view bounds, else the camera will
	 * maintain its original scale.
	 */
	public PTransformActivity animateViewToCenterBounds(Rectangle2D centerBounds, boolean shouldScaleToFit, long duration) {		
		PBounds viewBounds = getViewBounds();
		PDimension delta = viewBounds.deltaRequiredToCenter(centerBounds);
		PAffineTransform newTransform = getViewTransform();
		newTransform.translate(delta.width, delta.height);
		
		if (shouldScaleToFit) {
			double s = Math.min(viewBounds.getWidth() / centerBounds.getWidth(), viewBounds.getHeight() / centerBounds.getHeight());
			if (s != Double.POSITIVE_INFINITY && s != 0) {
				newTransform.scaleAboutPoint(s, centerBounds.getCenterX(), centerBounds.getCenterY());
			}
		}

		return animateViewToTransform(newTransform, duration);
	}

	/**
	 * Pan the camera's view from its current transform when the activity starts
	 * to a new transform so that the view bounds will contain (if possible, intersect 
	 * if not possible) the new bounds in the camera layers coordinate system. 
	 * If the duration is 0 then the view will be transformed immediately, and null 
	 * will be returned. Else a new PTransformActivity will get returned that is set 
	 * to animate the camera's view transform to the new bounds.
	 */
	public PTransformActivity animateViewToPanToBounds(Rectangle2D panToBounds, long duration) {
		PBounds viewBounds = getViewBounds();
		PDimension delta = viewBounds.deltaRequiredToContain(panToBounds);
		
		if (delta.width != 0 || delta.height != 0) {
			if (duration == 0) {
				translateView(-delta.width, -delta.height);
			} else {
				AffineTransform at = getViewTransform();
				at.translate(-delta.width, -delta.height);
				return animateViewToTransform(at, duration);
			}
		}

		return null;
	}

	/**
	 * @deprecated Renamed to animateViewToPanToBounds
	 */
	public PTransformActivity animateViewToIncludeBounds(Rectangle2D includeBounds, long duration) {
		return animateViewToPanToBounds(includeBounds, duration);
	}
	
	/**
	 * Animate the cameras view transform from its current value when the
	 * activity starts to the new destination transform value.
	 */
	public PTransformActivity animateViewToTransform(AffineTransform destination, long duration) {
		if (duration == 0) {
			setViewTransform(destination);
			return null;
		}
		
		PTransformActivity.Target t = new PTransformActivity.Target() {
			public void setTransform(AffineTransform aTransform) {
				PCamera.this.setViewTransform(aTransform);
			}
			public void getSourceMatrix(double[] aSource) {
				PCamera.this.viewTransform.getMatrix(aSource);
			}
		};
		
		PTransformActivity ta = new PTransformActivity(duration, PUtil.DEFAULT_ACTIVITY_STEP_RATE, t, destination);
		
		PRoot r = getRoot();
		if (r != null) {
			r.getActivityScheduler().addActivity(ta);
		}
		
		return ta;
	}

	//****************************************************************
	// View Transform Constraints - Methods for setting and applying
	// constraints to the view transform.
	//****************************************************************
	
	public int getViewConstraint() {
		return viewConstraint;
	}
	public void setViewConstraint(int constraint) {
		viewConstraint = constraint;
		applyViewConstraints();
	}
	
	protected void applyViewConstraints() {
		if (viewConstraint == VIEW_CONSTRAINT_NONE)
			return;

		PBounds viewBounds = getViewBounds();
		PBounds layerBounds = (PBounds) globalToLocal(getUnionOfLayerFullBounds());
		PDimension constraintDelta = null;
					
		switch (viewConstraint) {
			case VIEW_CONSTRAINT_ALL:
				constraintDelta = viewBounds.deltaRequiredToContain(layerBounds);
				break;

			case VIEW_CONSTRAINT_CENTER:
				layerBounds.setRect(layerBounds.getCenterX(), layerBounds.getCenterY(), 0, 0);
				constraintDelta = viewBounds.deltaRequiredToContain(layerBounds);
				break;
		}
		
		viewTransform.translate(-constraintDelta.width, -constraintDelta.height);	
	}

	//****************************************************************
	// Camera View Coord System Conversions - Methods to translate from
	// the camera's local coord system (above the camera's view transform) to the
	// camera view coord system (below the camera's view transform). When
	// converting geometry from one of the canvas's layers you must go
	// through the view transform.
	//****************************************************************

	/**
	 * Convert the point from the camera's view coordinate system to the 
	 * camera's local coordinate system. The given point is modified by this.
	 */
	public Point2D viewToLocal(Point2D viewPoint) {
		return viewTransform.transform(viewPoint, viewPoint);
	}

	/**
	 * Convert the dimension from the camera's view coordinate system to the 
	 * camera's local coordinate system. The given dimension is modified by this.
	 */
	public Dimension2D viewToLocal(Dimension2D viewDimension) {
		return viewTransform.transform(viewDimension, viewDimension);
	}

	/**
	 * Convert the rectangle from the camera's view coordinate system to the 
	 * camera's local coordinate system. The given rectangle is modified by this method.
	 */
	public Rectangle2D viewToLocal(Rectangle2D viewRectangle) {
		return viewTransform.transform(viewRectangle, viewRectangle);
	}

	/**
	 * Convert the point from the camera's local coordinate system to the 
	 * camera's view coordinate system. The given point is modified by this method.
	 */
	public Point2D localToView(Point2D localPoint) {
		try {
			return viewTransform.inverseTransform(localPoint, localPoint);
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Convert the dimension from the camera's local coordinate system to the 
	 * camera's view coordinate system. The given dimension is modified by this method.
	 */
	public Dimension2D localToView(Dimension2D localDimension) {
		return viewTransform.inverseTransform(localDimension, localDimension);
	}

	/**
	 * Convert the rectangle from the camera's local coordinate system to the 
	 * camera's view coordinate system. The given rectangle is modified by this method.
	 */
	public Rectangle2D localToView(Rectangle2D localRectangle) {
		return viewTransform.inverseTransform(localRectangle, localRectangle);
	}
	
	//****************************************************************
	// Serialization - Cameras conditionally serialize their layers.
	// This means that only the layer references that were unconditionally
	// (using writeObject) serialized by someone else will be restored
	// when the camera is unserialized.
	//****************************************************************/
	
	/**
	 * Write this camera and all its children out to the given stream. Note
	 * that the cameras layers are written conditionally, so they will only
	 * get written out if someone else writes them unconditionally.
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
				
		int count = getLayerCount();
		for (int i = 0; i < count; i++) {
			((PObjectOutputStream)out).writeConditionalObject(layers.get(i));			
		}
		
		out.writeObject(Boolean.FALSE); 	
		((PObjectOutputStream)out).writeConditionalObject(component);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		
		layers = new ArrayList();

		while (true) {
			Object each = in.readObject();
			if (each != null) {
				if (each.equals(Boolean.FALSE)) {
					break;
				} else {
					layers.add(each);
				}
			}
		}
				
		component = (PComponent) in.readObject();
	}	
}

