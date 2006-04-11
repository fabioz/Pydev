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

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PRoot;

/**
 * <b>PUtil</b> util methods for the Piccolo framework.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PUtil {
	
	public static Iterator NULL_ITERATOR = Collections.EMPTY_LIST.iterator();
	public static Enumeration NULL_ENUMERATION = new Enumeration() {
        public boolean hasMoreElements() { return false; }
        public Object nextElement() { return null; }
	};
	public static long DEFAULT_ACTIVITY_STEP_RATE = 20;
	public static int ACTIVITY_SCHEDULER_FRAME_DELAY = 10;
		
	public static OutputStream NULL_OUTPUT_STREAM = new OutputStream() {
		public void close() { }
		public void flush() { }
		public void write(byte[] b) { }
		public void write(byte[] b, int off, int len) { }
		public void write(int b) { }
	};	
	
	public static PCamera createBasicScenegraph() {
		PRoot r = new PRoot();
		PLayer l = new PLayer();
		PCamera c = new PCamera();
		
		r.addChild(c); 
		r.addChild(l); 
		c.addLayer(l);
		
		return c;		
	}

	public static void writeStroke(Stroke aStroke, ObjectOutputStream out) throws IOException {
		if (aStroke instanceof Serializable) {
			out.writeBoolean(true);
			out.writeBoolean(true);
			out.writeObject(aStroke);
		} else if (aStroke instanceof BasicStroke) {
			out.writeBoolean(true);
			out.writeBoolean(false);
			BasicStroke s = (BasicStroke) aStroke;
			
			float[] dash = s.getDashArray();
			
			if (dash == null) {
				out.write(0);
			} else {
				out.write(dash.length);
				for (int i = 0; i < dash.length; i++) {
					out.writeFloat(dash[i]);
				}
			}
						
			out.writeFloat(s.getLineWidth());
			out.writeInt(s.getEndCap());
			out.writeInt(s.getLineJoin());
			out.writeFloat(s.getMiterLimit());			
			out.writeFloat(s.getDashPhase());
		} else {
			out.writeBoolean(false);
		}
	}
	
	public static Stroke readStroke(ObjectInputStream in) throws IOException, ClassNotFoundException {
		boolean wroteStroke = in.readBoolean();
		if (wroteStroke) {
			boolean serializedStroke = in.readBoolean();
			if (serializedStroke) {
				return (Stroke) in.readObject();
			} else {
				float[] dash = null;
				int dashLength = in.read();
				
				if (dashLength != 0) {
					dash = new float[dashLength];
					for (int i = 0; i < dashLength; i++) {
						dash[i] = in.readFloat();
					}
				}
				
				float lineWidth = in.readFloat();
				int endCap = in.readInt();
				int lineJoin = in.readInt();
				float miterLimit = in.readFloat();
				float dashPhase = in.readFloat();
				
				return new BasicStroke(lineWidth, endCap, lineJoin, miterLimit, dash, dashPhase);
			}
		} else {
			return null;
		}
	}
	
	private static final int PATH_IS_DONE = -1;

	public static GeneralPath readPath(ObjectInputStream in) throws IOException, ClassNotFoundException {
		GeneralPath path = new GeneralPath();
	
		while(true) {
			int segType = in.readInt();
		
			switch(segType) {
				case PathIterator.SEG_MOVETO:
					path.moveTo(in.readFloat(), in.readFloat());
					break;
				
				case PathIterator.SEG_LINETO:
					path.lineTo(in.readFloat(), in.readFloat());
					break;
				
				case PathIterator.SEG_QUADTO:
					path.quadTo(in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat());
					break;
				
				case PathIterator.SEG_CUBICTO:
					path.curveTo(in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat());
					break;
				
				case PathIterator.SEG_CLOSE:
					path.closePath();
					break;
				
				case PATH_IS_DONE:
					return path;

				default:
					throw new IOException();
			}
		}
	}
	
	public static void writePath(GeneralPath path, ObjectOutputStream out) throws IOException {		
		PathIterator i = path.getPathIterator(null);
		float[] data = new float[6];
	
		while(!i.isDone()) {
			switch(i.currentSegment(data)) {
				case PathIterator.SEG_MOVETO:
					out.writeInt(PathIterator.SEG_MOVETO);
					out.writeFloat(data[0]);
					out.writeFloat(data[1]);
					break;
	
				case PathIterator.SEG_LINETO:
					out.writeInt(PathIterator.SEG_LINETO);
					out.writeFloat(data[0]);
					out.writeFloat(data[1]);
					break;
	
				case PathIterator.SEG_QUADTO:
					out.writeInt(PathIterator.SEG_QUADTO);
					out.writeFloat(data[0]);
					out.writeFloat(data[1]);
					out.writeFloat(data[2]);
					out.writeFloat(data[3]);
					break;
						
				case PathIterator.SEG_CUBICTO:
					out.writeInt(PathIterator.SEG_CUBICTO);
					out.writeFloat(data[0]);
					out.writeFloat(data[1]);
					out.writeFloat(data[2]);
					out.writeFloat(data[3]);
					out.writeFloat(data[4]);
					out.writeFloat(data[5]);				
					break;
						
				case PathIterator.SEG_CLOSE :
					out.writeInt(PathIterator.SEG_CLOSE);					
					break;
						
				default :
					throw new IOException();
			}
	
			i.next();
		}
			
		out.writeInt(PATH_IS_DONE);
	}
}
