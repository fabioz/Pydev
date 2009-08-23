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
package edu.umd.cs.piccolox.swt;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

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
public class PSWTPath extends PNode {
    
    /** 
     * The property name that identifies a change of this node's path. 
     * In any property change event the new value will be a reference to 
     * this node's path, but old value will always be null.
     */
    public static final String PROPERTY_SHAPE = "shape";

    private static final double BOUNDS_TOLERANCE = 0.01;
    private static final Rectangle2D.Float TEMP_RECTANGLE = new Rectangle2D.Float();
    private static final Ellipse2D.Float TEMP_ELLIPSE = new Ellipse2D.Float();
    private static final PAffineTransform TEMP_TRANSFORM = new PAffineTransform();
    private static final Color DEFAULT_STROKE_PAINT = Color.black;
    private static final BasicStroke BASIC_STROKE = new BasicStroke();
    private static final float PEN_WIDTH = 1f; 
    
    private Paint strokePaint;

    boolean updatingBoundsFromPath;
    Shape origShape;
    Shape shape;    
    
    PAffineTransform internalXForm;
    AffineTransform inverseXForm;
    
    double[] shapePts;    

    public static PSWTPath createRectangle(float x, float y, float width, float height) {
        TEMP_RECTANGLE.setFrame(x, y, width, height);
        PSWTPath result = new PSWTPath(TEMP_RECTANGLE);
        result.setPaint(Color.white);
        return result;
    }
        
    public static PSWTPath createEllipse(float x, float y, float width, float height) {
        TEMP_ELLIPSE.setFrame(x, y, width, height);
        PSWTPath result = new PSWTPath(TEMP_ELLIPSE);
        result.setPaint(Color.white);
        return result;
    }
    
    public static PSWTPath createPolyline(Point2D[] points) {
        PSWTPath result = new PSWTPath();
        result.setPathToPolyline(points);
        result.setPaint(Color.white);
        return result;
    }

    public static PSWTPath createPolyline(float[] xp, float[] yp) {
        PSWTPath result = new PSWTPath();
        result.setPathToPolyline(xp, yp);
        result.setPaint(Color.white);
        return result;
    }
        
    public PSWTPath() {
        strokePaint = DEFAULT_STROKE_PAINT;
    }

    public PSWTPath(Shape aShape) {
        this();
        setShape(aShape);
    }
    
    //****************************************************************
    // Stroke
    //****************************************************************
    
    public Paint getStrokePaint() {
        return strokePaint;
    }

    public void setStrokeColor(Paint aPaint) {
        Paint old = strokePaint;
        strokePaint = aPaint;
        invalidatePaint();
        firePropertyChange(PPath.PROPERTY_CODE_STROKE_PAINT, PPath.PROPERTY_STROKE_PAINT, old, strokePaint);
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
        if (origShape == null) return;
        
        Rectangle2D pathBounds = origShape.getBounds2D();

        if (Math.abs(x-pathBounds.getX())/x < BOUNDS_TOLERANCE &&
            Math.abs(y-pathBounds.getY())/y < BOUNDS_TOLERANCE &&
            Math.abs(width-pathBounds.getWidth())/width < BOUNDS_TOLERANCE &&
            Math.abs(height-pathBounds.getHeight())/height < BOUNDS_TOLERANCE) {
            return;
        }
        
        if (internalXForm == null) {
            internalXForm = new PAffineTransform();
        }
        internalXForm.setToIdentity();
        internalXForm.translate(x, y);
        internalXForm.scale(width / pathBounds.getWidth(), height / pathBounds.getHeight());
        internalXForm.translate(-pathBounds.getX(), -pathBounds.getY());
        
        try {
            inverseXForm = internalXForm.createInverse();
        }
        catch (Exception e) {            
        }
    }

    
    public boolean intersects(Rectangle2D aBounds) {
        if (super.intersects(aBounds)) {
            
            if (internalXForm != null) {
                aBounds = new PBounds(aBounds);         
                internalXForm.inverseTransform(aBounds,aBounds);
            }
            
            if (getPaint() != null && shape.intersects(aBounds)) {
                return true;
            } else if (strokePaint != null) {
                return BASIC_STROKE.createStrokedShape(shape).intersects(aBounds);
            }
        }
        return false;
    }
                        
    public void updateBoundsFromPath() {
        updatingBoundsFromPath = true;
        
        if (origShape == null) {
            resetBounds();
        } else {
            Rectangle2D b = origShape.getBounds2D();
            
            // Note that this pen width code does not really work for SWT since it assumes
            // that the pen width scales - in actuality it does not.  However, the fix would
            // be to have volatile bounds for all shapes which isn't a nice alternative
            super.setBounds(b.getX()-PEN_WIDTH, 
                    b.getY()-PEN_WIDTH, 
                    b.getWidth()+2*PEN_WIDTH, 
                    b.getHeight()+2*PEN_WIDTH);
        }
        updatingBoundsFromPath = false;
    }
    
    //****************************************************************
    // Painting
    //****************************************************************
    
    protected void paint(PPaintContext paintContext) {
        Paint p = getPaint();
        SWTGraphics2D g2 = (SWTGraphics2D)paintContext.getGraphics();
        
        if (internalXForm != null) {
            g2.transform(internalXForm);
        }
        
        if (p != null) {
            g2.setBackground((Color)p);

            double lineWidth = g2.getTransformedLineWidth();
            if (shape instanceof Rectangle2D) {
                g2.fillRect(shapePts[0]+lineWidth/2,shapePts[1]+lineWidth/2,shapePts[2]-lineWidth,shapePts[3]-lineWidth);
            }
            else if (shape instanceof Ellipse2D) {
                g2.fillOval(shapePts[0]+lineWidth/2,shapePts[1]+lineWidth/2,shapePts[2]-lineWidth,shapePts[3]-lineWidth);
            }
            else if (shape instanceof Arc2D) {
                g2.fillArc(shapePts[0]+lineWidth/2,shapePts[1]+lineWidth/2,shapePts[2]-lineWidth,shapePts[3]-lineWidth,shapePts[4],shapePts[5]);
            }
            else if (shape instanceof RoundRectangle2D) {
                g2.fillRoundRect(shapePts[0]+lineWidth/2,shapePts[1]+lineWidth/2,shapePts[2]-lineWidth,shapePts[3]-lineWidth,shapePts[4],shapePts[5]);                
            }
            else {
                g2.fillPolygon(shapePts);    
            }
        }
        
        if (strokePaint != null) {
            g2.setColor((Color)strokePaint);
            
            double lineWidth = g2.getTransformedLineWidth();            
            if (shape instanceof Rectangle2D) {
                g2.drawRect(shapePts[0]+lineWidth/2,shapePts[1]+lineWidth/2,shapePts[2]-lineWidth,shapePts[3]-lineWidth);
            }
            else if (shape instanceof Ellipse2D) {
                g2.drawOval(shapePts[0]+lineWidth/2,shapePts[1]+lineWidth/2,shapePts[2]-lineWidth,shapePts[3]-lineWidth);
            }
            else if (shape instanceof Arc2D) {
                g2.drawArc(shapePts[0]+lineWidth/2,shapePts[1]+lineWidth/2,shapePts[2]-lineWidth,shapePts[3]-lineWidth,shapePts[4],shapePts[5]);
            }
            else if (shape instanceof RoundRectangle2D) {
                g2.drawRoundRect(shapePts[0]+lineWidth/2,shapePts[1]+lineWidth/2,shapePts[2]-lineWidth,shapePts[3]-lineWidth,shapePts[4],shapePts[5]);                
            }
            else {
                // TODO The bounds may be incorrect for polylines at the moment - resulting in graphics turds at some scales 
                g2.drawPolyline(shapePts);    
            }
        }
        
        if (inverseXForm != null) {            
            g2.transform(inverseXForm);
        }
    }    
        
    public void setShape(Shape aShape) {        
        this.shape = cloneShape(aShape);
        this.origShape = shape;
        updateShapePoints(aShape);
        
        firePropertyChange(PPath.PROPERTY_CODE_PATH, PPath.PROPERTY_PATH, null, shape);
        updateBoundsFromPath();
        invalidatePaint();        
    }

    public void updateShapePoints(Shape aShape) {
        if (aShape instanceof Rectangle2D) {
            if (shapePts == null || shapePts.length < 4) {
                shapePts = new double[4];
            }
                
            shapePts[0] = ((Rectangle2D)shape).getX();    
            shapePts[1] = ((Rectangle2D)shape).getY();    
            shapePts[2] = ((Rectangle2D)shape).getWidth();    
            shapePts[3] = ((Rectangle2D)shape).getHeight();                
        }
        else if (aShape instanceof Ellipse2D) {
            if (shapePts == null || shapePts.length < 4) {
                shapePts = new double[4];
            }

            shapePts[0] = ((Ellipse2D)shape).getX();    
            shapePts[1] = ((Ellipse2D)shape).getY();    
            shapePts[2] = ((Ellipse2D)shape).getWidth();    
            shapePts[3] = ((Ellipse2D)shape).getHeight();                            
        }
        else if (aShape instanceof Arc2D) {
            if (shapePts == null || shapePts.length < 6) {
                shapePts = new double[6];
            }
            
            shapePts[0] = ((Arc2D)shape).getX();    
            shapePts[1] = ((Arc2D)shape).getY();    
            shapePts[2] = ((Arc2D)shape).getWidth();    
            shapePts[3] = ((Arc2D)shape).getHeight();
            shapePts[4] = ((Arc2D)shape).getAngleStart();
            shapePts[5] = ((Arc2D)shape).getAngleExtent();
        }
        else if (aShape instanceof RoundRectangle2D) {
            if (shapePts == null || shapePts.length < 6) {
                shapePts = new double[6];
            }

            shapePts[0] = ((RoundRectangle2D)shape).getX();    
            shapePts[1] = ((RoundRectangle2D)shape).getY();    
            shapePts[2] = ((RoundRectangle2D)shape).getWidth();    
            shapePts[3] = ((RoundRectangle2D)shape).getHeight();
            shapePts[4] = ((RoundRectangle2D)shape).getArcWidth();
            shapePts[5] = ((RoundRectangle2D)shape).getArcHeight();            
        }
        else {
            shapePts = SWTShapeManager.shapeToPolyline(shape);            
        }        
    }

    public Shape cloneShape(Shape aShape) {
        if (aShape instanceof Rectangle2D) {
            return new PBounds((Rectangle2D)aShape);
        }            
        else if (aShape instanceof Ellipse2D) {
            Ellipse2D e2 = (Ellipse2D)aShape;
            return new Ellipse2D.Double(e2.getX(),e2.getY(),e2.getWidth(),e2.getHeight());
        }
        else if (aShape instanceof Arc2D) {
            Arc2D a2 = (Arc2D)aShape;
            return new Arc2D.Double(a2.getX(),a2.getY(),a2.getWidth(),a2.getHeight(),a2.getAngleStart(),a2.getAngleExtent(),a2.getArcType());
        }
        else if (aShape instanceof RoundRectangle2D) {
            RoundRectangle2D r2 = (RoundRectangle2D)aShape;
            return new RoundRectangle2D.Double(r2.getX(),r2.getY(),r2.getWidth(),r2.getHeight(),r2.getArcWidth(),r2.getArcHeight());
        }
        else if (aShape instanceof GeneralPath) {
            GeneralPath aPath = new GeneralPath();
            aPath.append(aShape,false);
            return aPath;
            
        } else {
            new Exception().printStackTrace();
            GeneralPath aPath = new GeneralPath();
            aPath.append(aShape,false);
            return aPath;
        }
    }
    
    public void setPathToRectangle(float x, float y, float width, float height) {
        TEMP_RECTANGLE.setFrame(x, y, width, height);
        setShape(TEMP_RECTANGLE);
    }

    public void setPathToEllipse(float x, float y, float width, float height) {
        TEMP_ELLIPSE.setFrame(x, y, width, height);
        setShape(TEMP_ELLIPSE);
    }

    public void setPathToPolyline(Point2D[] points) {
        GeneralPath path = new GeneralPath();
        path.reset();
        path.moveTo((float)points[0].getX(), (float)points[0].getY());
        for (int i = 1; i < points.length; i++) {
            path.lineTo((float)points[i].getX(), (float)points[i].getY());
        }
        setShape(path);        
    }

    public void setPathToPolyline(float[] xp, float[] yp) {
        GeneralPath path = new GeneralPath();
        path.reset();
        path.moveTo(xp[0], yp[0]);
        for (int i = 1; i < xp.length; i++) {
            path.lineTo(xp[i], yp[i]);
        }
        setShape(path);
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

        result.append("path=" + (shape == null ? "null" : shape.toString()));
        result.append(",strokePaint=" + (strokePaint == null ? "null" : strokePaint.toString()));
        result.append(',');
        result.append(super.paramString());

        return result.toString();
    }

    
    /**
     * @author Fabio Zadrozny
     * @return The center of this object (based on its bounds).
     */
    public Point2D getCenter() {
        PBounds bounds2 = getBounds();
        return new Point2D.Double(bounds2.x+(bounds2.width/2.0), bounds2.y+(bounds2.height/2.0)); 
    }    
}
