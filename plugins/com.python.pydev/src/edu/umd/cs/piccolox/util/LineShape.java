package edu.umd.cs.piccolox.util;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class LineShape implements Shape, MutablePoints
{
    private MutablePoints points;
    private Rectangle2D bounds = new Rectangle2D.Double(); 

    public LineShape(MutablePoints points) {
        setPoints(points);
    }
    
    public void setPoints(MutablePoints points) {
        if (points == null) {
            points = new XYArray();
        }
        this.points = points;
    }

    // from Points
    
    public int getPointCount() {
        return points.getPointCount();
    }

    public double getX(int i) { return points.getX(i); }
    public double getY(int i) { return points.getY(i); }

    public Point2D getPoint(int i, Point2D dst) {
        return points.getPoint(i, dst);
    }

    public Rectangle2D getBounds(Rectangle2D dst) {
        points.getBounds(dst);
        return dst;
    }

    // from MutablePoints

    public void updateBounds() {
        bounds.setRect(0.0d, 0.0d, 0.0d, 0.0d);
        points.getBounds(bounds);
    }
    
    public void setPoint(int i, double x, double y) {
        points.setPoint(i, x, y);
        updateBounds();
    }

    public void addPoint(int pos, double x, double y) {
        points.addPoint(pos, x, y);
        updateBounds();
    }

    public void removePoints(int pos, int num) {
        points.removePoints(pos, num);
        updateBounds();
    }

    public void transformPoints(AffineTransform trans) {
        XYArray newPoints = new XYArray(points.getPointCount());
        newPoints.appendPoints(points);
        newPoints.transformPoints(trans);
        points = newPoints;
    }
    
    //
    
    public Rectangle getBounds() {
        return new Rectangle((int)bounds.getX(), (int)bounds.getY(), (int)bounds.getWidth(), (int)bounds.getHeight());
    }

    public Rectangle2D getBounds2D() {
        return bounds;
    }

    public static boolean contains(double x, double y, double x1, double y1, double x2, double y2, 
                                   boolean min, boolean max,
                                   double d)
    {
        double dx = x2 - x1, dy = y2 - y1;
        double dx2 = dx * dx, dy2 = dy * dy;
        double p;
        if (dx != 0) {
            p = (((x - x1) / dx) + ((dy * (y - y1)) / dx2)) / (1 + (dy2 / dx2));
        }
        else if (dy != 0) {
            p = (((y - y1) / dy) + ((dx * (x - x1)) / dy2)) / (1 + (dx2 / dy2));
        }
        else {
            return false;
        }
        if (max && p > 1.0) {
            return false;
        }
        else if (min && p < 0.0) {
            return false;
        }
        dx = (p * dx) + x1 - x; dy = (p * dy) + y1 - y;
        double len = dx * dx + dy * dy;
        return (len < d);
    }

    public boolean contains(double x, double y, double d) {
        double x1, y1, x2, y2;
        if (points.getPointCount() == 0) {
            return false;
        }
        x2 = points.getX(0);
        y2 = points.getX(0);
        for (int i = 0; i < points.getPointCount(); i++) {
            x1 = x2;
            y1 = y2;
            x2 = points.getX(i);
            y2 = points.getX(i);
            if (contains(x, y, x1, y1, x2, y2, false, false, d)) {
                return true;
            }
        }
        return false;
    }
    public boolean contains(double x, double y) {
        return contains(x, y, 2.0d);
    }

    public boolean contains(Point2D p) {
        return contains(p.getX(), p.getY());
    }

    public static boolean intersects(double x1, double y1, double x2, double y2,
                                     double x3, double y3, double x4, double y4,
                                     boolean min1, boolean max1,
                                     boolean min2, boolean max2)
    {
        double dx1 = x2 - x1, dy1 = y2 - y1, dx2 = x4 - x3, dy2 = y4 - y3;
        double d, p2, p1;

        if (dy1 != 0.0) {
            d = dx1 / dy1;
            p2 = (x3 - x1 + (d * (y1 - y3))) / ((d * dy2) - dx2);
            p1 = (dy2 * p2 + y3 - y1) / dy1;
        }
        else if (dy2 != 0.0) {
            d = dx2 / dy2;
            p1 = (x1 - x3 + (d * (y3 - y1))) / ((d * dy1) - dx1);
            p2 = (dy1 * p1 + y1 - y3) / dy2;
        }
        else if (dx1 != 0.0) {
            d = dy1 / dx1;
            p2 = (y3 - y1 + (d * (x1 - x3))) / ((d * dx2) - dy2);
            p1 = (dx2 * p2 + x3 - x1) / dx1;
        }
        else if (dx2 != 0.0) {
            d = dy2 / dx2;
            p1 = (y1 - y3 + (d * (x3 - x1))) / ((d * dx1) - dy1);
            p2 = (dx1 * p1 + x1 - x3) / dx2;
        }
        else {
            return false;
        }
        return (((! min1) || (p1 >= 0.0)) && ((! max1) || (p1 <= 1.0)) &&
                ((! min2) || (p2 >= 0.0)) && ((! max2) || (p2 <= 1.0)));
    }

    public boolean intersects(double x, double y, double w, double h) {
        double x1, y1, x2, y2;
        if (points.getPointCount() == 0) {
            return false;
        }
        x2 = points.getX(0);
        y2 = points.getX(0);
        for (int i = 0; i < points.getPointCount(); i++) {
            x1 = x2;
            y1 = y2;
            x2 = points.getX(i);
            y2 = points.getX(i);
            if (intersects(x,     y,     x + w, y,     x1, y1, x2, y2, true, true, true, true) ||
                intersects(x + w, y,     x + w, y + h, x1, y1, x2, y2, true, true, true, true) ||
                intersects(x + w, y + h, x,     y + h, x1, y1, x2, y2, true, true, true, true) ||
                intersects(x,     y + h, x,     y,     x1, y1, x2, y2, true, true, true, true)) {
                return true;
            }
        }
        return false;
    }

    public boolean intersects(Rectangle2D r) {
        return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public boolean contains(double x, double y, double w, double h) {
        return contains(x, y) && contains(x + w, y) && contains(x, y + h) && contains(x + w, y + h);
    }

    public boolean contains(Rectangle2D r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    //
    
    //
    
    public PathIterator getPathIterator(AffineTransform at) {
        return new LinePathIterator(points, at);
    }

    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return new LinePathIterator(points, at);
    }
    
    private static class LinePathIterator implements PathIterator {

        private Points points;
        private AffineTransform trans;
        private int i = 0;
        
        public LinePathIterator(Points points, AffineTransform trans) {
            this.points = points;
            this.trans = trans;
        }

        public int getWindingRule() {
            return GeneralPath.WIND_EVEN_ODD;
        }

        public boolean isDone() {
            return i >= points.getPointCount();
        }

        public void next() {
            i++;
        }

        private Point2D tempPoint = new Point2D.Double();
        
        private void currentSegment() {
            tempPoint.setLocation(points.getX(i), points.getY(i));
            if (trans != null) {
                trans.transform(tempPoint, tempPoint);
            }
        }

        public int currentSegment(float[] coords) {
            currentSegment();
            coords[0] = (float)tempPoint.getX();
            coords[1] = (float)tempPoint.getY();
            return (i == 0 ? PathIterator.SEG_MOVETO : PathIterator.SEG_LINETO);
        }

        public int currentSegment(double[] coords) {
            currentSegment();
            coords[0] = tempPoint.getX();
            coords[1] = tempPoint.getY();
            return (i == 0 ? PathIterator.SEG_MOVETO : PathIterator.SEG_LINETO);
        }
    }
}
