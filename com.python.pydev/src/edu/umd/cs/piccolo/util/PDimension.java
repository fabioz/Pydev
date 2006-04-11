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

import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.io.Serializable;

/**
 * <b>PDimension</b> this class should be removed once a concrete Dimension2D 
 * that supports doubles is added to java. 
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PDimension extends Dimension2D implements Serializable {

	public double width;
	public double height;

	public PDimension() {
		super();
	}

	public PDimension(Dimension2D aDimension) {
		this(aDimension.getWidth(), aDimension.getHeight());
	}
	
	public PDimension(double aWidth, double aHeight) {
		super();
		width = aWidth;
		height = aHeight;
	}

	public PDimension(Point2D p1, Point2D p2) {
		width = p2.getX() - p1.getX();
		height = p2.getY() - p1.getY();
	}

	public double getHeight() {
		return height;
	}

	public double getWidth() {
		return width;
	}

	public void setSize(double aWidth, double aHeight) {
		width = aWidth;
		height = aHeight;
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();

		result.append(super.toString().replaceAll(".*\\.", ""));
		result.append('[');
		result.append("width=");
		result.append(width);
		result.append(",height=");
		result.append(height);
		result.append(']');

		return result.toString();
	}	
}
