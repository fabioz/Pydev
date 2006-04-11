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
package edu.umd.cs.piccolox.nodes;

import java.awt.*;
import java.awt.geom.*;
import edu.umd.cs.piccolo.*;
import edu.umd.cs.piccolo.util.*;
import edu.umd.cs.piccolox.*;

/**
 * This is a simple node that draws a "3D" rectangle within the bounds of the node.
 * Drawing a 3D rectangle in a zooming environment is a little tricky because
 * if you just use the regular (Java2D) 3D rectangle, the 3D borders get scaled,
 * and that is ugly.  This version always draws the 3D border at fixed 2 pixel width.
 * 
 * @author Ben Bederson
 */
public class P3DRect extends PNode {

	private Color topLeftOuterColor;
	private Color topLeftInnerColor;
	private Color bottomRightInnerColor;
	private Color bottomRightOuterColor;
	private GeneralPath path;
	private Stroke stroke;
	private boolean raised;
	
	public P3DRect() {
		raised = true;
		stroke = new BasicStroke(0);
		path = new GeneralPath();
	}
	
	public P3DRect(Rectangle2D bounds) {
		this(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
	}
	
	public P3DRect(double x, double y, double width, double height) {
		this();
		setBounds(x, y, width, height);
	}
	
	public void setRaised(boolean raised) {
		this.raised = raised;
		setPaint(getPaint());
	}
	
	public boolean getRaised() {
		return raised;
	}
	
	protected void paint(PPaintContext paintContext) {
		Graphics2D g2 = paintContext.getGraphics();
				
		double x = getX();
		double y = getY();
		double width = getWidth();
		double height = getHeight();
		double magX = g2.getTransform().getScaleX();
		double magY = g2.getTransform().getScaleY();
		double dx = (float)(1.0 / magX);
		double dy = (float)(1.0 / magY);
		PBounds bounds = getBounds();
		
		g2.setPaint(getPaint());
		g2.fill(bounds);
		g2.setStroke(stroke);	

		path.reset();
		path.moveTo((float)(x+width), (float)y);
		path.lineTo((float)x, (float)y);
		path.lineTo((float)x, (float)(y+height));
		g2.setPaint(topLeftOuterColor);
		g2.draw(path);

		path.reset();
		path.moveTo((float)(x+width), (float)(y+dy));
		path.lineTo((float)(x+dx), (float)(y+dy));
		path.lineTo((float)(x+dx), (float)(y+height));
		g2.setPaint(topLeftInnerColor);
		g2.draw(path);

		path.reset();
		path.moveTo((float)(x+width), (float)(y));
		path.lineTo((float)(x+width), (float)(y+height));
		path.lineTo((float)(x), (float)(y+height));
		g2.setPaint(bottomRightOuterColor);
		g2.draw(path);

		path.reset();
		path.moveTo((float)(x+width-dx), (float)(y+dy));
		path.lineTo((float)(x+width-dx), (float)(y+height-dy));
		path.lineTo((float)(x), (float)(y+height-dy));
		g2.setPaint(bottomRightInnerColor);
		g2.draw(path);
	}	

	public void setPaint(Paint newPaint) {
		super.setPaint(newPaint);
		
		if (newPaint instanceof Color) {
			Color color = (Color)newPaint;
			
			if (raised) {
				topLeftOuterColor = color.brighter();
				topLeftInnerColor = topLeftOuterColor.brighter();
				bottomRightInnerColor = color.darker();
				bottomRightOuterColor = bottomRightInnerColor.darker();
			} else {
				topLeftOuterColor = color.darker();
				topLeftInnerColor = topLeftOuterColor.darker();
				bottomRightInnerColor = color.brighter();
				bottomRightOuterColor = bottomRightInnerColor.brighter();			
			}
		} else {
			topLeftOuterColor = null;
			topLeftInnerColor = null;
			bottomRightInnerColor = null;
			bottomRightOuterColor = null;
		}
	}

	public static void main(String[] args) {
		new PFrame() {
			public void initialize() {
				getCanvas().setDefaultRenderQuality(PPaintContext.LOW_QUALITY_RENDERING);

				P3DRect rect1 = new P3DRect(50, 50, 100, 100);
				rect1.setPaint(new Color(239, 235, 222));

				P3DRect rect2 = new P3DRect(50, 50, 100, 100);
				rect2.setPaint(new Color(239, 235, 222));
				rect2.translate(110, 0);
				rect2.setRaised(false);
				
				getCanvas().getLayer().addChild(rect1);
				getCanvas().getLayer().addChild(rect2);
			}
		};
	}
}
