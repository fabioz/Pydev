package edu.umd.cs.piccolox.util;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public interface Points
{
    public int getPointCount();

    public double getX(int i);
    public double getY(int i);
    public Point2D getPoint(int i, Point2D dst);

    public Rectangle2D getBounds(Rectangle2D dst);
}
