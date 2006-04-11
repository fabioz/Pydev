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
package edu.umd.cs.piccolo.event;

import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.geom.Point2D;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDimension;

/**
 * <b>PPanEventHandler</b> provides event handlers for basic panning
 * of the canvas view with the left mouse.	The interaction is that
 * clicking and dragging the mouse translates the view so that
 * the point on the surface stays under the mouse.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PPanEventHandler extends PDragSequenceEventHandler {
	
	private boolean autopan;
	private double minAutopanSpeed = 250;
	private double maxAutopanSpeed = 750;
	
	public PPanEventHandler() {
		super();
		setEventFilter(new PInputEventFilter(InputEvent.BUTTON1_MASK));
		setAutopan(true);
	}

	protected void drag(PInputEvent e) {
		super.drag(e);
		pan(e);
	}
	
	protected void pan(PInputEvent e) {
		PCamera c = e.getCamera();
		Point2D l = e.getPosition();
		
		if (c.getViewBounds().contains(l)) {
			PDimension d = e.getDelta();
			c.translateView(d.getWidth(), d.getHeight());
		}
	}
		
	//****************************************************************
	// Auto Pan
	//****************************************************************

	public void setAutopan(boolean autopan) {
		this.autopan = autopan;
	}

	public boolean getAutopan() {
		return autopan;
	}
	
	/**
	 * Set the minAutoPan speed in pixels per second.
	 * @param minAutopanSpeed
	 */
	public void setMinAutopanSpeed(double minAutopanSpeed) {
		this.minAutopanSpeed = minAutopanSpeed;
	}
	
	/**
	 * Set the maxAutoPan speed in pixes per second.
	 * @param maxAutopanSpeed
	 */
	public void setMaxAutopanSpeed(double maxAutopanSpeed) {
		this.maxAutopanSpeed = maxAutopanSpeed;
	}
	
	/**
	 * Do auto panning	even when the mouse is not moving.
	 */
	protected void dragActivityStep(PInputEvent aEvent) {
		if (!autopan) return;
		
		PCamera c = aEvent.getCamera();
		PBounds b = c.getBoundsReference();
		Point2D l = aEvent.getPositionRelativeTo(c);
		int outcode = b.outcode(l);
		PDimension delta = new PDimension();
		
		if ((outcode & Rectangle.OUT_TOP) != 0) {
			delta.height = validatePanningSpeed(-1.0 - (0.5 * Math.abs(l.getY() - b.getY())));
		} else if ((outcode & Rectangle.OUT_BOTTOM) != 0) {
			delta.height = validatePanningSpeed(1.0 + (0.5 * Math.abs(l.getY() - (b.getY() + b.getHeight()))));
		}
		
		if ((outcode & Rectangle.OUT_RIGHT) != 0) {
			delta.width = validatePanningSpeed(1.0 + (0.5 * Math.abs(l.getX() - (b.getX() + b.getWidth()))));
		} else if ((outcode & Rectangle.OUT_LEFT) != 0) {
			delta.width = validatePanningSpeed(-1.0 - (0.5 * Math.abs(l.getX() - b.getX())));
		}
		
		c.localToView(delta);		
		
		if (delta.width != 0 || delta.height != 0) {
			c.translateView(delta.width, delta.height);
		}
	}
	
	protected double validatePanningSpeed(double delta) {
		double minDelta = minAutopanSpeed / (1000 / getDragActivity().getStepRate());
		double maxDelta = maxAutopanSpeed / (1000 / getDragActivity().getStepRate());
		
		boolean deltaNegative = delta < 0;
		delta = Math.abs(delta);
		if (delta < minDelta) delta = minDelta;
		if (delta > maxDelta) delta = maxDelta;
		if (deltaNegative) delta = -delta;
		return delta;
	}
	
	//****************************************************************
	// Debugging - methods for debugging
	//****************************************************************

	/**
	 * Returns a string representing the state of this node. This method is
	 * intended to be used only for debugging purposes, and the content and
	 * format of the returned string may vary between implementations. The
	 * returned string may be empty but may not be <code>null</code>.
	 *
	 * @return  a string representation of this node's state
	 */
	protected String paramString() {
		StringBuffer result = new StringBuffer();

		result.append("minAutopanSpeed=" + minAutopanSpeed);
		result.append(",maxAutopanSpeed=" + maxAutopanSpeed);
		if (autopan) result.append(",autopan");
		result.append(',');
		result.append(super.paramString());

		return result.toString();
	}	
}
