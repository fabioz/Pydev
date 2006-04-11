package edu.umd.cs.piccolox.util;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class XYArray implements MutablePoints
{
    // the coordinates are stored as alternating x and y pairs

    private double[] points = null;

    // the number of valid x, y pairs,
    // i.e. not the length of the points array

    private int numPoints = 0;

    public int getPointCount() { return numPoints;}

    // normalize an index, negative counts from end

    private int i(int i) { 
        if (i < 0) {
            i = numPoints + i;
        }
        if (i >= numPoints) {
            throw new IllegalArgumentException("The point index " + i + " is not below " + numPoints);
        }
        return i;
    }

    // various get and set methods

    // from Points

    public double getX(int i)
    {
        i = i(i);
        return points[i*2];
    }
    public double getY(int i)
    {
        i = i(i);
        return points[i*2 + 1];
    }
    public Point2D getPoint(int i, Point2D dst) {
        i = i(i);
        dst.setLocation(points[i*2], points[i*2 + 1]);
        return dst;
    }

    public void setX(int i, double x)
    {
        i = i(i);
        points[i*2] = x;
    }
    public void setY(int i, double y)
    {
        i = i(i);
        points[i*2 + 1] = y;
    }

    public void setPoint(int i, double x, double y)
    {
        i = i(i);
        points[i*2]     = x;
        points[i*2 + 1] = y;
    }

    public void setPoint(int i, Point2D pt)
    {
        setPoint(i, pt.getX(), pt.getY());
    }

    public void transformPoints(AffineTransform t)
    {
        t.transform(points, 0, points, 0, numPoints);
    }

    public Rectangle2D getBounds(Rectangle2D dst)
    {
        int i = 0;
        if (dst.isEmpty() && getPointCount() > 0) {
            dst.setRect(getX(i), getY(i), 1.0d, 1.0d);
            i++;
        }
        while (i < getPointCount()) {
            dst.add(getX(i), getY(i));
            i++;
        }
        return dst;
    }

    //

    // initialization of points array

    public static double[] initPoints(double[] points, int n, double[] old)
    {
        if (points == null || n * 2 > points.length) {
            points = new double[n * 2];
        }
        if (old != null && points != old) {
            System.arraycopy(old, 0, points, 0, Math.min(old.length, n * 2));
        }
        return(points);
    }

    private void initPoints(double[] points, int n)
    {
        this.points = initPoints(points, n, this.points);
        numPoints = (points != null ? points.length / 2 : 0);
    }

    // constructors

    public XYArray(double[] points) {
        initPoints(points, points.length / 2);
    }
    public XYArray(int n) {
        initPoints(null, n); 
    }
    public XYArray() {
        this(0);
    }

    // adding points to points array

    public void addPoints(int pos, Points pts, int start, int end)
    {
        if (end < 0) {
            end = pts.getPointCount() + end + 1;
        }
        int n = numPoints + end - start;
        points = initPoints(points, n, points);
        int pos1 = pos * 2, pos2 = (pos + end - start) * 2, len = (numPoints - pos) * 2;
        System.arraycopy(points, pos1, points, pos2, len);
        numPoints = n;
        if (pts == null) {
            return;
        }
        for (int count = 0; start < end; count++, start++) {
            setPoint(pos + count, pts.getX(start), pts.getY(start));
        }
    }
    public void addPoints(int pos, Points pts) {
        addPoints(pos, pts, 0, pts.getPointCount());
    }

    public void appendPoints(Points pts)
    { addPoints(numPoints, pts);}

    public static XYArray copyPoints(Points pts)
    {
        XYArray newList = new XYArray(pts.getPointCount());
        newList.appendPoints(pts);
        return newList;
    }

    // from MutablePoints

    public void addPoint(int pos, double x, double y)
    {
        addPoints(pos, null, 0, 1);
        setPoint(pos, x, y);
    }

    public void addPoint(int pos, Point2D pt)
    {
        addPoint(pos, pt.getX(), pt.getY());
    }

    public void removePoints(int pos, int num)
    {
        num = Math.min(num, numPoints - pos);
        if (num <= 0)
            return;
        System.arraycopy(points, (pos + num) * 2, points, pos * 2, (numPoints - (pos + num)) * 2);
        numPoints -= num;
    }
    public void removeAllPoints()
    { removePoints(0, numPoints);}

    //

    public Object clone()
    {
       XYArray ps = null;
       try { ps = (XYArray)(super.clone());}
       catch (CloneNotSupportedException e) {}
       if (ps != null) {
           ps.points = initPoints(ps.points, numPoints, points);
           ps.numPoints = numPoints;
       }
       return(ps);
    }
}
