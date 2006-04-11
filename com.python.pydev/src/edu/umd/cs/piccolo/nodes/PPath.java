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
package edu.umd.cs.piccolo.nodes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.util.PUtil;

/**
 * <b>PPath</b> is a wrapper around a java.awt.geom.GeneralPath. The
 * setBounds method works by scaling the path to fit into the specified
 * bounds. This normally works well, but if the specified base bounds
 * get too small then it is impossible to expand the path shape again since
 * all its numbers have tended to zero, so application code may need to take
 * this into consideration. 
 * <P>
 * One option that applications have is to call <code>startResizeBounds</code> before
 * starting an interaction that may make the bounds very small, and calling 
 * <code>endResizeBounds</code> when this interaction is finished. When this is done
 * PPath will use a copy of the original path to do the resizing so the numbers
 * in the path wont loose resolution.
 * <P>
 * This class also provides methods for constructing common shapes using a 
 * general path.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PPath extends PNode {
	
	/** 
	 * The property name that identifies a change of this node's stroke paint
	 * (see {@link #getStrokePaint getStrokePaint}). Both old and new value will
	 * be set correctly to Paint objects in any property change event.
	 */
	public static final String PROPERTY_STROKE_PAINT = "strokePaint";
    public static final int PROPERTY_CODE_STROKE_PAINT = 1 << 16;

	/** 
	 * The property name that identifies a change of this node's stroke (see
	 * {@link #getStroke getStroke}). Both old and new value will be set
	 * correctly to Stroke objects in any property change event.
	 */
	public static final String PROPERTY_STROKE = "stroke";
    public static final int PROPERTY_CODE_STROKE = 1 << 17;

	/** 
	 * The property name that identifies a change of this node's path (see
	 * {@link #getPathReference getPathReference}).  In any property change
	 * event the new value will be a reference to this node's path,  but old
	 * value will always be null.
	 */
	public static final String PROPERTY_PATH = "path";
    public static final int PROPERTY_CODE_PATH = 1 << 18;
	
	private static final Rectangle2D.Float TEMP_RECTANGLE = new Rectangle2D.Float();
	private static final Ellipse2D.Float TEMP_ELLIPSE = new Ellipse2D.Float();
	private static final PAffineTransform TEMP_TRANSFORM = new PAffineTransform();
	private static final BasicStroke DEFAULT_STROKE = new BasicStroke(1.0f);
	private static final Color DEFAULT_STROKE_PAINT = Color.black;
	
	private transient GeneralPath path;
	private transient GeneralPath resizePath;
	private transient Stroke stroke;
	private transient boolean updatingBoundsFromPath;
	private Paint strokePaint;

	public static PPath createRectangle(float x, float y, float width, float height) {
		TEMP_RECTANGLE.setFrame(x, y, width, height);
		PPath result = new PPath(TEMP_RECTANGLE);
		result.setPaint(Color.white);
		return result;
	}
		
	public static PPath createEllipse(float x, float y, float width, float height) {
		TEMP_ELLIPSE.setFrame(x, y, width, height);
		PPath result = new PPath(TEMP_ELLIPSE);
		result.setPaint(Color.white);
		return result;
	}
	
	public static PPath createLine(float x1, float y1, float x2, float y2) {
		PPath result = new PPath();
		result.moveTo(x1, y1);
		result.lineTo(x2, y2);
		result.setPaint(Color.white);
		return result;
	}
	
	public static PPath createPolyline(Point2D[] points) {
		PPath result = new PPath();
		result.setPathToPolyline(points);
		result.setPaint(Color.white);
		return result;
	}

	public static PPath createPolyline(float[] xp, float[] yp) {
		PPath result = new PPath();
		result.setPathToPolyline(xp, yp);
		result.setPaint(Color.white);
		return result;
	}
		
	public PPath() {
		strokePaint = DEFAULT_STROKE_PAINT;
		stroke = DEFAULT_STROKE;
		path = new GeneralPath();
	}

	public PPath(Shape aShape) {
		this(aShape, DEFAULT_STROKE);
	}

	/**
	 * Construct this path with the given shape and stroke.
	 * This method may be used to optimize the creation of a large number of
	 * PPaths. Normally PPaths have a default stroke of width one, but when a
	 * path has a non null stroke it takes significantly longer to compute its
	 * bounds. This method allows you to override that default stroke before the
	 * bounds are ever calculated, so if you pass in a null stroke here you
	 * won't ever have to pay that bounds calculation price if you don't need
	 * to.
	 */
	public PPath(Shape aShape, Stroke aStroke) {
		this();
		stroke = aStroke;
		if (aShape != null) append(aShape, false);
	}
	
	//****************************************************************
	// Stroke
	//****************************************************************
	
	public Paint getStrokePaint() {
		return strokePaint;
	}

	public void setStrokePaint(Paint aPaint) {
		Paint old = strokePaint;
		strokePaint = aPaint;
		invalidatePaint();
		firePropertyChange(PROPERTY_CODE_STROKE_PAINT ,PROPERTY_STROKE_PAINT, old, strokePaint);
	}
	
	public Stroke getStroke() {
		return stroke;
	}

	public void setStroke(Stroke aStroke) {
		Stroke old = stroke;
		stroke = aStroke;
		updateBoundsFromPath();
		invalidatePaint();
		firePropertyChange(PROPERTY_CODE_STROKE ,PROPERTY_STROKE, old, stroke);
	}
		
	//****************************************************************
	// Bounds
	//****************************************************************
		
	public void startResizeBounds() {
		resizePath = new GeneralPath(path);
	}

	public void endResizeBounds() {
		resizePath = null;
	}
				
	/**
	 * Set the bounds of this path. This method works by scaling the path 
	 * to fit into the specified bounds. This normally works well, but if 
	 * the specified base bounds get too small then it is impossible to 
	 * expand the path shape again since all its numbers have tended to zero, 
	 * so application code may need to take this into consideration.
	 */
	protected void internalUpdateBounds(double x, double y, double width, double height) {
		if (updatingBoundsFromPath) return;
		if (path == null) return;
		
		if (resizePath != null) {
			path.reset();
			path.append(resizePath, false);
		}

		Rectangle2D pathBounds = path.getBounds2D();
		Rectangle2D pathStrokeBounds = getPathBoundsWithStroke();
		double strokeOutset = Math.max(pathStrokeBounds.getWidth() - pathBounds.getWidth(), 
										pathStrokeBounds.getHeight() - pathBounds.getHeight());
		
		x += strokeOutset / 2;
		y += strokeOutset / 2;
		width -= strokeOutset;
		height -= strokeOutset;
		
		double scaleX = (width == 0 || pathBounds.getWidth() == 0) ? 1 : width / pathBounds.getWidth();
		double scaleY = (height == 0 || pathBounds.getHeight() == 0) ? 1 : height / pathBounds.getHeight();
		
		TEMP_TRANSFORM.setToIdentity();
		TEMP_TRANSFORM.translate(x, y);
		TEMP_TRANSFORM.scale(scaleX, scaleY);
		TEMP_TRANSFORM.translate(-pathBounds.getX(), -pathBounds.getY());
		
		path.transform(TEMP_TRANSFORM);
	}
	
	public boolean intersects(Rectangle2D aBounds) {
		if (super.intersects(aBounds)) {
			if (getPaint() != null && path.intersects(aBounds)) {
				return true;
			} else if (stroke != null && strokePaint != null) {
				return stroke.createStrokedShape(path).intersects(aBounds);
			}
		}
		return false;
	}
			
	public Rectangle2D getPathBoundsWithStroke() {
		if (stroke != null) {
			return stroke.createStrokedShape(path).getBounds2D();
		} else {
			return path.getBounds2D();
		}
	}
			
	public void updateBoundsFromPath() {
		updatingBoundsFromPath = true;
		if (path == null) {
			resetBounds();
		} else {
			Rectangle2D b = getPathBoundsWithStroke();
			setBounds(b.getX(), b.getY(), b.getWidth(), b.getHeight());
		}
		updatingBoundsFromPath = false;
	}
	
	//****************************************************************
	// Painting
	//****************************************************************
	
	protected void paint(PPaintContext paintContext) {
		Paint p = getPaint();
		Graphics2D g2 = paintContext.getGraphics();
		
		if (p != null) {
			g2.setPaint(p);
			g2.fill(path);
		}

		if (stroke != null && strokePaint != null) {
			g2.setPaint(strokePaint);
			g2.setStroke(stroke);
			g2.draw(path);
		}		
	}	
		
	//****************************************************************
	// Path Support set java.awt.GeneralPath documentation for more
	// information on using these methods.
	//****************************************************************

	public GeneralPath getPathReference() {
		return path;
	}
	
	public void moveTo(float x, float y) {
		path.moveTo(x, y);
		firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
		updateBoundsFromPath();
		invalidatePaint();
	}
	
	public void lineTo(float x, float y) {
		path.lineTo(x, y);
		firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
		updateBoundsFromPath();
		invalidatePaint();
	}

	public void quadTo(float x1, float y1, float x2, float y2) {
		path.quadTo(x1, y1, x2, y2);
		firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
		updateBoundsFromPath();
		invalidatePaint();
	}

	public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
		path.curveTo(x1, y1, x2, y2, x3, y3);
		firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
		updateBoundsFromPath();
		invalidatePaint();
	}
	
	public void append(Shape aShape, boolean connect) {
		path.append(aShape, connect);
		firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
		updateBoundsFromPath();
		invalidatePaint();
	}
	
	public void setPathTo(Shape aShape) {
		path.reset();
		append(aShape, false);
	}

	public void setPathToRectangle(float x, float y, float width, float height) {
		TEMP_RECTANGLE.setFrame(x, y, width, height);
		setPathTo(TEMP_RECTANGLE);
	}

	public void setPathToEllipse(float x, float y, float width, float height) {
		TEMP_ELLIPSE.setFrame(x, y, width, height);
		setPathTo(TEMP_ELLIPSE);
	}

	public void setPathToPolyline(Point2D[] points) {
		path.reset();
		path.moveTo((float)points[0].getX(), (float)points[0].getY());
		for (int i = 1; i < points.length; i++) {
			path.lineTo((float)points[i].getX(), (float)points[i].getY());
		}
		firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
		updateBoundsFromPath();
		invalidatePaint();		
	}

	public void setPathToPolyline(float[] xp, float[] yp) {
		path.reset();
		path.moveTo(xp[0], yp[0]);
		for (int i = 1; i < xp.length; i++) {
			path.lineTo(xp[i], yp[i]);
		}
		firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
		updateBoundsFromPath();
		invalidatePaint();
	}
	
	public void closePath() {
		path.closePath();
		firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
		updateBoundsFromPath();
		invalidatePaint();
	}
	
	public void reset() {
		path.reset();
		firePropertyChange(PROPERTY_CODE_PATH, PROPERTY_PATH, null, path);
		updateBoundsFromPath();
		invalidatePaint();
	}
	
	//****************************************************************
	// Serialization
	//****************************************************************
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		PUtil.writeStroke(stroke, out); 	   
		PUtil.writePath(path, out); 	   
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();		
		stroke = PUtil.readStroke(in);
		path = PUtil.readPath(in);		
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

		result.append("path=" + (path == null ? "null" : path.toString()));
		result.append(",stroke=" + (stroke == null ? "null" : stroke.toString()));
		result.append(",strokePaint=" + (strokePaint == null ? "null" : strokePaint.toString()));
		result.append(',');
		result.append(super.paramString());

		return result.toString();
	}	
}
