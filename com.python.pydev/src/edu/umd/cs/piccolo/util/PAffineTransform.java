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
package edu.umd.cs.piccolo.util;

import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * <b>PAffineTransform</b> is a subclass of AffineTransform that has been extended
 * with convenience methods.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PAffineTransform extends AffineTransform {

	private static double[] PTS1 = new double[8];
	private static double[] PTS2 = new double[8];

	public PAffineTransform() {
		super();
	}

	public PAffineTransform(double[] flatmatrix) {
		super(flatmatrix);
	}

	public PAffineTransform(float[] flatmatrix) {
		super(flatmatrix);
	}

	public PAffineTransform(double m00, double m10, double m01, double m11, double m02, double m12) {
		super(m00, m10, m01, m11, m02, m12);
	}

	public PAffineTransform(float m00, float m10, float m01, float m11, float m02, float m12) {
		super(m00, m10, m01, m11, m02, m12);
	}

	public PAffineTransform(AffineTransform tx) {
		super(tx);
	}

	public void scaleAboutPoint(double scale, double x, double y) {
		translate(x, y);
		scale(scale, scale);
		translate(-x, -y);
	}

	public double getScale() {
		PTS1[0] = 0;//x1
		PTS1[1] = 0;//y1
		PTS1[2] = 1;//x2
		PTS1[3] = 0;//y2
		transform(PTS1, 0, PTS2, 0, 2);
		return Point2D.distance(PTS2[0], PTS2[1], PTS2[2], PTS2[3]);
	}
	
	public void setScale(double scale) {
		if (scale == 0) throw new RuntimeException("Can't set scale to 0");
		scaleAboutPoint(scale / getScale(), 0, 0);
	}

	public void setOffset(double tx, double ty) {
		setTransform(getScaleX(), getShearY(), getShearX(), getScaleY(), tx, ty);
	}

	/**
	 * Returns the rotation applied to this affine transform in radians. The
	 * value returned will be between 0 and 2pi.
	 * 
	 * @return rotation in radians
	 */
	public double getRotation() {				
		PTS1[0] = 0;//x1
		PTS1[1] = 0;//y1
		PTS1[2] = 1;//x2
		PTS1[3] = 0;//y2

		transform(PTS1, 0, PTS2, 0, 2);

		double dy = Math.abs(PTS2[3] - PTS2[1]);
		double l = Point2D.distance(PTS2[0], PTS2[1], PTS2[2], PTS2[3]);
		double rotation = Math.asin(dy / l);		
		
		// correct for quadrant
		if (PTS2[3] - PTS2[1] > 0) {
			if (PTS2[2] - PTS2[0] < 0) {
				rotation = Math.PI - rotation;
			}
		} else {
			if (PTS2[2] - PTS2[0] > 0) {
				rotation = 2 * Math.PI - rotation;
			} else {
				rotation = rotation + Math.PI;
			}
		}

		return rotation;
	}

	/**
	 * Set rotation in radians.
	 */
	public void setRotation(double theta) {
		rotate(theta - getRotation());
	}

	public Dimension2D transform(Dimension2D dimSrc, Dimension2D dimDst) {
		if (dimDst == null) {
			dimDst = (Dimension2D) dimSrc.clone();
		}

		PTS1[0] = dimSrc.getWidth();
		PTS1[1] = dimSrc.getHeight();
		deltaTransform(PTS1, 0, PTS2, 0, 1);
		dimDst.setSize(PTS2[0], PTS2[1]);
		return dimDst;
	}

	public Dimension2D inverseTransform(Dimension2D dimSrc, Dimension2D dimDst) {
		if (dimDst == null) {
			dimDst = (Dimension2D) dimSrc.clone();
		}

		double width = dimSrc.getWidth();
		double height = dimSrc.getHeight();
		double m00 = getScaleX();
		double m11 = getScaleY();
		double m01 = getShearX();
		double m10 = getShearY();
		double det = m00 * m11 - m01 * m10;
		
		try {
			if (Math.abs(det) <= Double.MIN_VALUE) {
				throw new NoninvertibleTransformException("Determinant is "+ det);
			}
			dimDst.setSize((width * m11 - height * m01) / det, (height * m00 - width * m10) / det);
		} catch (NoninvertibleTransformException e) {
			e.printStackTrace();
		}

		return dimDst;
	}
								 
	public Rectangle2D transform(Rectangle2D rectSrc, Rectangle2D rectDst) {
		if (rectDst == null) {
			rectDst = (Rectangle2D) rectSrc.clone();
		}
		
		if (rectSrc.isEmpty()) {
			rectDst.setRect(rectSrc);
			if (rectDst instanceof PBounds) {
				((PBounds)rectDst).reset();
			}
			return rectDst;
		}

		double scale;

		switch (getType()) {
			case AffineTransform.TYPE_IDENTITY:
				if (rectSrc != rectDst)
					rectDst.setRect(rectSrc);
				break;

			case AffineTransform.TYPE_TRANSLATION:
				rectDst.setRect(rectSrc.getX() + getTranslateX(), 
								rectSrc.getY() + getTranslateY(), 
								rectSrc.getWidth(), 
								rectSrc.getHeight());
				break;

			case AffineTransform.TYPE_UNIFORM_SCALE:
				scale = getScaleX();
				rectDst.setRect(rectSrc.getX() * scale, 
								rectSrc.getY() * scale, 
								rectSrc.getWidth() * scale, 
								rectSrc.getHeight() * scale);
				break;
				
			case AffineTransform.TYPE_TRANSLATION | AffineTransform.TYPE_UNIFORM_SCALE:
				scale = getScaleX();
				rectDst.setRect((rectSrc.getX() * scale) + getTranslateX(), 
								(rectSrc.getY() * scale) + getTranslateY(), 
								rectSrc.getWidth() * scale, 
								rectSrc.getHeight() * scale);
				break;

			default :
				double[] pts = rectToArray(rectSrc);
				transform(pts, 0, pts, 0, 4);
				rectFromArray(rectDst, pts);				
				break;
		}
		
		
		return rectDst;
	}
			
	public Rectangle2D inverseTransform(Rectangle2D rectSrc, Rectangle2D rectDst) {
		if (rectDst == null) {
			rectDst = (Rectangle2D) rectSrc.clone();
		}
		
		if (rectSrc.isEmpty()) {
			rectDst.setRect(rectSrc);
			if (rectDst instanceof PBounds) {
				((PBounds)rectDst).reset();
			}
			return rectDst; 		
		}
		
		double scale;

		switch (getType()) {
			case AffineTransform.TYPE_IDENTITY:
				if (rectSrc != rectDst)
					rectDst.setRect(rectSrc);
				break;

			case AffineTransform.TYPE_TRANSLATION:
				rectDst.setRect(rectSrc.getX() - getTranslateX(), 
								rectSrc.getY() - getTranslateY(), 
								rectSrc.getWidth(), 
								rectSrc.getHeight());
				break;

			case AffineTransform.TYPE_UNIFORM_SCALE:
				scale = 1 / getScaleX();
				rectDst.setRect(rectSrc.getX() * scale, 
								rectSrc.getY() * scale, 
								rectSrc.getWidth() * scale, 
								rectSrc.getHeight() * scale);
				break;
				
			case AffineTransform.TYPE_TRANSLATION | AffineTransform.TYPE_UNIFORM_SCALE:
				scale = 1 / getScaleX();
				rectDst.setRect((rectSrc.getX() - getTranslateX()) * scale, 
								(rectSrc.getY() - getTranslateY()) * scale, 
								rectSrc.getWidth() * scale, 
								rectSrc.getHeight() * scale);
				break;
				
			default :
				double[] pts = rectToArray(rectSrc);
				try {
					inverseTransform(pts, 0, pts, 0, 4);
				} catch (NoninvertibleTransformException e) {
					e.printStackTrace();
				}		
				rectFromArray(rectDst, pts);
				break;
		}
			
		return rectDst;
	}
	
	private static double[] rectToArray(Rectangle2D aRectangle) {
		PTS1[0] = aRectangle.getX();
		PTS1[1] = aRectangle.getY();
		PTS1[2] = PTS1[0] + aRectangle.getWidth();
		PTS1[3] = PTS1[1];
		PTS1[4] = PTS1[0] + aRectangle.getWidth();
		PTS1[5] = PTS1[1] + aRectangle.getHeight();
		PTS1[6] = PTS1[0];
		PTS1[7] = PTS1[1] + aRectangle.getHeight();
		return PTS1;
	}

	private static void rectFromArray(Rectangle2D aRectangle, double[] pts) {
		double minX = pts[0];
		double minY = pts[1];
		double maxX = pts[0];
		double maxY = pts[1];

		double x;
		double y;

		for (int i = 1; i < 4; i++) {
			x = pts[2 * i];
			y = pts[(2 * i) + 1];
			
			if (x < minX) {
				minX = x;
			}
			if (y < minY) {
				minY = y;
			}
			if (x > maxX) {
				maxX = x;
			}
			if (y > maxY) {
				maxY = y;
			}
		}
		aRectangle.setRect(minX, minY, maxX - minX, maxY - minY);
	}	
}
