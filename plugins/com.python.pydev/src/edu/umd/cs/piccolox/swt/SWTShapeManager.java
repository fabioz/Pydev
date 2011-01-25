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

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import org.eclipse.swt.graphics.Rectangle;

/**
 * @author Lance Good
 */
public class SWTShapeManager {

    static AffineTransform IDENTITY_XFORM = new AffineTransform();
    static Point2D aPoint = new Point2D.Double();
    static ArrayList segList = new ArrayList();
    static double[] pts = new double[8];


    /**
     * Apply the specified transform to the specified rectangle, modifying the rect.
     * @param rect The rectangle to be transformed
     * @param at The transform to use to transform the rectangle
     */
    public static void transform(Rectangle2D rect, AffineTransform at) {
                                // First, transform all 4 corners of the rectangle
        pts[0] = rect.getX();          // top left corner
        pts[1] = rect.getY();
        pts[2] = rect.getX() + rect.getWidth();  // top right corner
        pts[3] = rect.getY();
        pts[4] = rect.getX() + rect.getWidth();  // bottom right corner
        pts[5] = rect.getY() + rect.getHeight();
        pts[6] = rect.getX();          // bottom left corner
        pts[7] = rect.getY() + rect.getHeight();
        at.transform(pts, 0, pts, 0, 4);

                                // Then, find the bounds of those 4 transformed points.
        double minX = pts[0];
        double minY = pts[1];
        double maxX = pts[0];
        double maxY = pts[1];
        int i;
        for (i=1; i<4; i++) {
            if (pts[2*i] < minX) {
                minX = pts[2*i];
            }
            if (pts[2*i+1] < minY) {
                minY = pts[2*i+1];
            }
            if (pts[2*i] > maxX) {
                maxX = pts[2*i];
            }
            if (pts[2*i+1] > maxY) {
                maxY = pts[2*i+1];
            }
        }
        rect.setRect(minX, minY, maxX - minX, maxY - minY);
    }

    public static void awtToSWT(Rectangle2D aRect, Rectangle sRect) {
        sRect.x = (int)(aRect.getX()+0.5);
        sRect.y = (int)(aRect.getY()+0.5);
        sRect.width = (int)(aRect.getWidth()+0.5);
        sRect.height = (int)(aRect.getHeight()+0.5);
    }
    
    public static double[] shapeToPolyline(Shape s) {
        segList.clear();
        aPoint.setLocation(0,0);
        
        PathIterator pi = s.getPathIterator(IDENTITY_XFORM,0.000000001);
        while (!pi.isDone()) {
            int segType = pi.currentSegment(pts);
            switch (segType) {
                case PathIterator.SEG_MOVETO:
                    aPoint.setLocation(pts[0],pts[1]);
                    segList.add(new Point2D.Double(pts[0],pts[1]));
                    break;
                case PathIterator.SEG_LINETO:
                    segList.add(new Point2D.Double(pts[0],pts[1]));                    
                    break;
                case PathIterator.SEG_CLOSE:
                    segList.add(new Point2D.Double(aPoint.getX(),aPoint.getY()));
                    break;
            }
            pi.next();
        }
        
        double[] polyObj = new double[2*segList.size()];
        for(int i=0; i<segList.size(); i++) {
            Point2D p2 = (Point2D)segList.get(i);
            polyObj[2*i] = (int)(p2.getX()+0.5);
            polyObj[2*i+1] = (int)(p2.getY()+0.5);
        }
        
        return polyObj;
    }
    
    public static int[] transform(double[] pts, AffineTransform at) {
        int[] intPts = new int[pts.length];
        for(int i=0; i<pts.length/2; i++) {
            aPoint.setLocation(pts[2*i],pts[2*i+1]);
            at.transform(aPoint,aPoint);
            intPts[2*i] = (int)(aPoint.getX()+0.5);
            intPts[2*i+1] = (int)(aPoint.getY()+0.5);
        }
        return intPts;
    }    
}
