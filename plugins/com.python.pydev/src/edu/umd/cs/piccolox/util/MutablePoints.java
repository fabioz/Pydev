package edu.umd.cs.piccolox.util;

import java.awt.geom.AffineTransform;

public interface MutablePoints extends Points
{
    public void setPoint(int i, double x, double y);
    public void addPoint(int pos, double x, double y);
    public void removePoints(int pos, int num);
    public void transformPoints(AffineTransform t);
}
