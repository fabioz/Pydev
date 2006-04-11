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

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.umd.cs.piccolo.PCamera;

/**
* <b>PPaintContext</b> is used by piccolo nodes to paint themselves on the screen.
 * PPaintContext wraps a Graphics2D to implement painting.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PPaintContext {

	public static final int LOW_QUALITY_RENDERING = 0;
	public static final int HIGH_QUALITY_RENDERING = 1;
	
	public static FontRenderContext RENDER_QUALITY_LOW_FRC = new FontRenderContext(null, false, true);
	public static FontRenderContext RENDER_QUALITY_HIGH_FRC = new FontRenderContext(null, true, true);	
	public static PPaintContext CURRENT_PAINT_CONTEXT;
	
	private static double[] PTS = new double[4];
		
	private Graphics2D graphics;
	protected PStack compositeStack;
	protected PStack clipStack;
	protected PStack localClipStack;
	protected PStack cameraStack;
	protected PStack transformStack;
	protected int renderQuality;
		
	public PPaintContext(Graphics2D aGraphics) {
		super();
		graphics = aGraphics;
		compositeStack = new PStack();
		clipStack = new PStack();
		localClipStack = new PStack();
		cameraStack = new PStack();
		transformStack = new PStack();
		renderQuality = HIGH_QUALITY_RENDERING;
		
		Shape clip = aGraphics.getClip();
		if (clip == null) {
			clip = new PBounds(
				-Integer.MAX_VALUE / 2, 
				-Integer.MAX_VALUE / 2, 
				Integer.MAX_VALUE,
				Integer.MAX_VALUE);
			aGraphics.setClip(clip);
		}
		
		localClipStack.push(clip.getBounds2D());
		
		CURRENT_PAINT_CONTEXT = this;
	}
	
	public Graphics2D getGraphics() {
		return graphics;
	}

	//****************************************************************
	// Context Attributes.
	//****************************************************************
	
	public Rectangle2D getLocalClip() {
		return (Rectangle2D) localClipStack.peek();
	}
	
	public double getScale() {
		PTS[0] = 0;//x1
		PTS[1] = 0;//y1
		PTS[2] = 1;//x2
		PTS[3] = 0;//y2
		graphics.getTransform().transform(PTS, 0, PTS, 0, 2);
		return Point2D.distance(PTS[0], PTS[1], PTS[2], PTS[3]);
	}
	
	//****************************************************************
	// Context Attribute Stacks. attributes that can be pushed and
	// popped.
	//****************************************************************

	public void pushCamera(PCamera aCamera) {
		cameraStack.push(aCamera);
	}
	
	public void popCamera(PCamera aCamera) {
		cameraStack.pop();
	}

	public PCamera getCamera() {
		return (PCamera) cameraStack.peek();
	}
	
	public void pushClip(Shape aClip) {
		Shape currentClip = graphics.getClip();
		clipStack.push(currentClip);
		graphics.clip(aClip);
		Rectangle2D newLocalClip = aClip.getBounds2D();
		Rectangle2D.intersect(getLocalClip(), newLocalClip, newLocalClip);		
		localClipStack.push(newLocalClip);
	}

	public void popClip(Shape aClip) {
		Shape newClip = (Shape) clipStack.pop();
		graphics.setClip(newClip);
		localClipStack.pop();
	}

	public void pushTransparency(float transparency) {
		if (transparency == 1) {
			return;
		}
		Composite current = graphics.getComposite();
		float currentAlaph = 1.0f;
		compositeStack.push(current);

		if (current instanceof AlphaComposite) {
			currentAlaph = ((AlphaComposite)current).getAlpha();
		}
		AlphaComposite newComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, currentAlaph * transparency);
		graphics.setComposite(newComposite);
	}

	public void popTransparency(float transparency) {
		if (transparency == 1) {
			return;
		}
		Composite c = (Composite) compositeStack.pop();
		graphics.setComposite(c);
	}

	public void pushTransform(PAffineTransform aTransform) {
		if (aTransform == null) return;
		Rectangle2D newLocalClip = (Rectangle2D) getLocalClip().clone();
		aTransform.inverseTransform(newLocalClip, newLocalClip);
		transformStack.push(graphics.getTransform());
		localClipStack.push(newLocalClip);
		graphics.transform(aTransform);
	}

	public void popTransform(PAffineTransform aTransform) {
		if (aTransform == null) return;
		graphics.setTransform((AffineTransform)transformStack.pop());
		localClipStack.pop();
	}

	//****************************************************************
	// Render Quality.
	//****************************************************************/
	
	/**
	 * Return the render quality used by this paint context.
	 */
	public int getRenderQuality() {
		return renderQuality;
	}
	
	/**
	 * Set the rendering hints for this paint context. The render quality is most
	 * often set by the rendering PCanvas. Use PCanvas.setRenderQuality() and
	 * PCanvas.setInteractingRenderQuality() to set these values.
	 * 
	 * @param requestedQuality supports PPaintContext.HIGH_QUALITY_RENDERING or PPaintContext.LOW_QUALITY_RENDERING
	 */
	public void setRenderQuality(int requestedQuality) {
		renderQuality = requestedQuality;
		
		switch (renderQuality) {
			case HIGH_QUALITY_RENDERING:
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON); 			
				break;

			case LOW_QUALITY_RENDERING:
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
				graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
				graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
				graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
				break;				
		}
	}	
}
