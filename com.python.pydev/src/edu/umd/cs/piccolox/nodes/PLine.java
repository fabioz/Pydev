package edu.umd.cs.piccolox.nodes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.util.PUtil;
import edu.umd.cs.piccolox.util.LineShape;

/** 
 * <b>PLine</b> a class for drawing multisegment lines. 
 * Submitted by Hallvard Traetteberg.
 */
public class PLine extends PNode {	
	
	private static final PAffineTransform TEMP_TRANSFORM = new PAffineTransform();
	private static final BasicStroke DEFAULT_STROKE = new BasicStroke(1.0f);
	private static final Color DEFAULT_STROKE_PAINT = Color.black;
	
	private transient LineShape line;
	private transient Stroke stroke;
	private Paint strokePaint;

	public PLine(LineShape line) {
		strokePaint = DEFAULT_STROKE_PAINT;
		stroke = DEFAULT_STROKE;
        if (line == null) {
            line = new LineShape(null);
        }
		this.line = line;
	}

    public PLine() {
        this(null);
    }

	public PLine(LineShape line, Stroke aStroke) {
		this(line);
		stroke = aStroke;
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
		firePropertyChange(PPath.PROPERTY_CODE_STROKE_PAINT, PPath.PROPERTY_STROKE_PAINT, old, strokePaint);
	}
	
	public Stroke getStroke() {
		return stroke;
	}

	public void setStroke(Stroke aStroke) {
		Stroke old = stroke;
		stroke = aStroke;
		updateBoundsFromLine();
		invalidatePaint();
		firePropertyChange(PPath.PROPERTY_CODE_STROKE, PPath.PROPERTY_STROKE, old, stroke);
	}
		
	//****************************************************************
	// Bounds
	//****************************************************************

	public boolean setBounds(double x, double y, double width, double height) {
		if (line == null || !super.setBounds(x, y, width, height)) {
			return false;
		}

		Rectangle2D lineBounds = line.getBounds2D();
		Rectangle2D lineStrokeBounds = getLineBoundsWithStroke();
		double strokeOutset = Math.max(lineStrokeBounds.getWidth() - lineBounds.getWidth(), 
                                       lineStrokeBounds.getHeight() - lineBounds.getHeight());
		
		x += strokeOutset / 2;
		y += strokeOutset / 2;
		width -= strokeOutset;
		height -= strokeOutset;
		
		TEMP_TRANSFORM.setToIdentity();
		TEMP_TRANSFORM.translate(x, y);
		TEMP_TRANSFORM.scale(width / lineBounds.getWidth(), height / lineBounds.getHeight());
		TEMP_TRANSFORM.translate(-lineBounds.getX(), -lineBounds.getY());		
        line.transformPoints(TEMP_TRANSFORM);
		
		return true;
	}

	public boolean intersects(Rectangle2D aBounds) {
		if (super.intersects(aBounds)) {
			if (line.intersects(aBounds)) {
				return true;
			} else if (stroke != null && strokePaint != null) {
				return stroke.createStrokedShape(line).intersects(aBounds);
			}
		}
		return false;
	}
			
	public Rectangle2D getLineBoundsWithStroke() {
		if (stroke != null) {
			return stroke.createStrokedShape(line).getBounds2D();
		} else {
			return line.getBounds2D();
		}
	}
			
	public void updateBoundsFromLine() {
		if (line.getPointCount() == 0) {
			resetBounds();
		} else {
			Rectangle2D b = getLineBoundsWithStroke();
			super.setBounds(b.getX(), b.getY(), b.getWidth(), b.getHeight());
		}
	}
	
	//****************************************************************
	// Painting
	//****************************************************************
	
	protected void paint(PPaintContext paintContext) {
		Graphics2D g2 = paintContext.getGraphics();
		
		if (stroke != null && strokePaint != null) {
			g2.setPaint(strokePaint);
			g2.setStroke(stroke);
			g2.draw(line);
		}		
	}	

	public LineShape getLineReference() {
		return line;
	}

    public int getPointCount() {
        return line.getPointCount();
    }

    public Point2D getPoint(int i, Point2D dst) {
        if (dst == null) {
            dst = new Point2D.Double();
        }
        return line.getPoint(i, dst);
    }

    protected void lineChanged() {
        firePropertyChange(PPath.PROPERTY_CODE_PATH, PPath.PROPERTY_PATH, null, line);
        updateBoundsFromLine();
        invalidatePaint();
    }
    
	public void setPoint(int i, double x, double y) {
        line.setPoint(i, x, y);
        lineChanged();
	}

    public void addPoint(int i, double x, double y) {
        line.addPoint(i, x, y);
        lineChanged();
    }

    public void removePoints(int i, int n) {
        line.removePoints(i, n);
        lineChanged();
    }

	public void removeAllPoints() {
        line.removePoints(0, line.getPointCount());
        lineChanged();
	}
	
	//****************************************************************
	// Serialization
	//****************************************************************
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		PUtil.writeStroke(stroke, out); 	   
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();		
		stroke = PUtil.readStroke(in);
	}
}
