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
package edu.umd.cs.piccolox.util;

import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;

import sun.dc.path.FastPathProducer;
import sun.dc.path.PathConsumer;
import sun.dc.path.PathException;
import sun.dc.pr.PathDasher;
import sun.dc.pr.PathStroker;
import sun.dc.pr.Rasterizer;

import edu.umd.cs.piccolo.util.PAffineTransform;
import edu.umd.cs.piccolo.util.PDebug;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.util.PPickPath;

/**
 * <b>PFixedWidthStroke</b> is the same as java.awt.BasicStroke except that PFixedWidthStroke
 * has a fixed width on the screen so that even when the canvas view is zooming its
 * width stays the same in canvas coordinates. Note that this stroke draws in the inside of 
 * the stroked shape, instead of the normal draw on center behavior.
 * <P>
 * @see edu.umd.cs.piccolo.nodes.PPath
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PFixedWidthStroke implements Stroke, Serializable {
	
	private static PAffineTransform TEMP_TRANSFORM = new PAffineTransform();
	private static GeneralPath TEMP_PATH = new GeneralPath(GeneralPath.WIND_NON_ZERO);
	
	public final static int JOIN_MITER = 0;
	public final static int JOIN_ROUND = 1;
	public final static int JOIN_BEVEL = 2;
	public final static int CAP_BUTT = 0;
	public final static int CAP_ROUND = 1;
	public final static int CAP_SQUARE = 2;

	private float width;
	private int join;
	private int cap;
	private float miterlimit;
	private float dash[];
	private float dash_phase;

	public static final int RasterizerCaps[] = {
		Rasterizer.BUTT, Rasterizer.ROUND, Rasterizer.SQUARE
	};

	public static final int RasterizerCorners[] = {
		Rasterizer.MITER, Rasterizer.ROUND, Rasterizer.BEVEL
	};

	public class FillAdapter implements PathConsumer {
		boolean closed;
		GeneralPath path;

		public FillAdapter() {
			path = TEMP_PATH;
			path.reset();
		}

		public Shape getShape() {
			return path;
		}

		public void beginPath() {}

		public void beginSubpath(float x0, float y0) {
			if (closed) {
				path.closePath();
				closed = false;
			}
			path.moveTo(x0, y0);
		}

		public void appendLine(float x1, float y1) {
			path.lineTo(x1, y1);
		}

		public void appendQuadratic(float xm, float ym, float x1, float y1) {
			path.quadTo(xm, ym, x1, y1);
		}

		public void appendCubic(float xm, float ym,
					float xn, float yn,
					float x1, float y1) {
			path.curveTo(xm, ym, xn, yn, x1, y1);
		}

		public void closedSubpath() {
			closed = true;
		}

		public void endPath() {
			if (closed) {
				path.closePath();
				closed = false;
			}
		}

		public void useProxy(FastPathProducer proxy)
			throws PathException {
			proxy.sendTo(this);
		}

		public long getCPathConsumer() {
			return 0;
		}
		
		public void dispose() {
		}

		public PathConsumer getConsumer() {
			return null;
		}
	}
	
	public PFixedWidthStroke() {
		this(1.0f, CAP_SQUARE, JOIN_MITER, 10.0f, null, 0.0f);
	}
	
	public PFixedWidthStroke(float width) {
		this(width, CAP_SQUARE, JOIN_MITER, 10.0f, null, 0.0f);
	}
	
	public PFixedWidthStroke(float width, int cap, int join) {
		this(width, cap, join, 10.0f, null, 0.0f);
	}
	
	public PFixedWidthStroke(float width, int cap, int join, float miterlimit) {
		this(width, cap, join, miterlimit, null, 0.0f);
	}
	
	public PFixedWidthStroke(float width, int cap, int join, float miterlimit, float dash[], float dash_phase) {
		if (width < 0.0f) {
			throw new IllegalArgumentException("negative width");
		}
		if (cap != CAP_BUTT && cap != CAP_ROUND && cap != CAP_SQUARE) {
			throw new IllegalArgumentException("illegal end cap value");
		}
		if (join == JOIN_MITER) {
			if (miterlimit < 1.0f) {
				throw new IllegalArgumentException("miter limit < 1");
			}
		} else if (join != JOIN_ROUND && join != JOIN_BEVEL) {
			throw new IllegalArgumentException("illegal line join value");
		}
		if (dash != null) {
			if (dash_phase < 0.0f) {
				throw new IllegalArgumentException("negative dash phase");
			}
			boolean allzero = true;
			for (int i = 0; i < dash.length; i++) {
				float d = dash[i];
				if (d > 0.0) {
					allzero = false;
				} else if (d < 0.0) {
					throw new IllegalArgumentException("negative dash length");
				}
			}
			
			if (allzero) {
				throw new IllegalArgumentException("dash lengths all zero");
			}
		}
		this.width	= width;
		this.cap	= cap;
		this.join	= join;
		this.miterlimit = miterlimit;
		if (dash != null) {
			this.dash = (float []) dash.clone();
		}
		this.dash_phase = dash_phase;
	}
	
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Shape createStrokedShape(Shape s) {
		FillAdapter filler = new FillAdapter();
		PathStroker stroker = new PathStroker(filler);
		PathConsumer consumer;

		// Fixed Width Additions, always stroke path inside shape.
		float fixedWidth = 1;
		
		if (PDebug.getProcessingOutput()) {
			if (PPaintContext.CURRENT_PAINT_CONTEXT != null) fixedWidth = width / (float) PPaintContext.CURRENT_PAINT_CONTEXT.getScale();
		} else {
			if (PPickPath.CURRENT_PICK_PATH != null) fixedWidth = width / (float) PPickPath.CURRENT_PICK_PATH.getScale();
		}
		
		Rectangle2D bounds = s.getBounds2D();
		double scale = 1.0;
	
		if (bounds.getWidth() > bounds.getHeight()) {
			if (bounds.getWidth() != 0) {
				scale = (bounds.getWidth()-fixedWidth)/bounds.getWidth();
			}
		} else {
			if (bounds.getHeight() != 0) {
				scale = (bounds.getHeight()-fixedWidth)/bounds.getHeight();
			}
		}
	
		TEMP_TRANSFORM.setToIdentity();
		TEMP_TRANSFORM.scaleAboutPoint(scale, bounds.getCenterX(), bounds.getCenterY());
		stroker.setPenDiameter(fixedWidth);
		PathIterator pi = s.getPathIterator(TEMP_TRANSFORM);
		
		stroker.setPenT4(null);
		stroker.setCaps(RasterizerCaps[cap]);
		stroker.setCorners(RasterizerCorners[join], miterlimit);
		if (dash != null) {
			PathDasher dasher = new PathDasher(stroker);
			dasher.setDash(dash, dash_phase);
			dasher.setDashT4(null);
			consumer = dasher;
		} else {
			consumer = stroker;
		}

		try {
			consumer.beginPath();
			boolean pathClosed = false;
			float mx = 0.0f;
			float my = 0.0f;
			float point[]  = new float[6];

			while (!pi.isDone()) {
				int type = pi.currentSegment(point);
				if (pathClosed == true) {
					pathClosed = false;
					if (type != PathIterator.SEG_MOVETO) {
						// Force current point back to last moveto point
						consumer.beginSubpath(mx, my);
					}
				}
				switch (type) {
					case PathIterator.SEG_MOVETO:
						mx = point[0];
						my = point[1];
						consumer.beginSubpath(point[0], point[1]);
						break;
					case PathIterator.SEG_LINETO:
						consumer.appendLine(point[0], point[1]);
						break;
					case PathIterator.SEG_QUADTO:
						// Quadratic curves take two points
						consumer.appendQuadratic(point[0], point[1],
									 point[2], point[3]);
						break;
					case PathIterator.SEG_CUBICTO:
						// Cubic curves take three points
						consumer.appendCubic(point[0], point[1],
								 point[2], point[3],
								 point[4], point[5]);
						break;
					case PathIterator.SEG_CLOSE:
						consumer.closedSubpath();
						pathClosed = true;
						break;
				}
				pi.next();
			}

			consumer.endPath();
		} catch (PathException e) {
			throw new InternalError("Unable to Stroke shape ("+
						e.getMessage()+")");
		}

		return filler.getShape();
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof PFixedWidthStroke)) {
			return false;
		}

		PFixedWidthStroke bs = (PFixedWidthStroke) obj;
		if (width != bs.width) {
			return false;
		}

		if (join != bs.join) {
			return false;
		}

		if (cap != bs.cap) {
			return false;
		}

		if (miterlimit != bs.miterlimit) {
			return false;
		}

		if (dash != null) {
			if (dash_phase != bs.dash_phase) {
				return false;
			}

			if (!java.util.Arrays.equals(dash, bs.dash)) {
				return false;
			}
		} else if (bs.dash != null) {
			return false;
		}

		return true;
	}
	
	public float[] getDashArray() {
		if (dash == null) {
			return null;
		}

		return (float[]) dash.clone();
	}
	
	public float getDashPhase() {
		return dash_phase;
	}
	
	public int getEndCap() {
		return cap;
	}
	
	public int getLineJoin() {
		return join;
	}
	
	public float getLineWidth() {
		return width;
	}
	
	public float getMiterLimit() {
		return miterlimit;
	}
	
	public int hashCode() {
		int hash = Float.floatToIntBits(width);
		hash = hash * 31 + join;
		hash = hash * 31 + cap;
		hash = hash * 31 + Float.floatToIntBits(miterlimit);
		if (dash != null) {
			hash = hash * 31 + Float.floatToIntBits(dash_phase);
			for (int i = 0; i < dash.length; i++) {
				hash = hash * 31 + Float.floatToIntBits(dash[i]);
			}
		}
		return hash;
	}
}
