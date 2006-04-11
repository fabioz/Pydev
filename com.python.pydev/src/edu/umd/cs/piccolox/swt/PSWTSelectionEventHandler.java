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

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.eclipse.swt.SWT;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolox.event.PSelectionEventHandler;

/**
 * Modified to use SWT paths instead of normal paths 
 * @version 1.0
 * @author Lance Good
 */ 
public class PSWTSelectionEventHandler extends PSelectionEventHandler {

    PSWTPath marquee;
    PNode marqueeParent;
    Point2D pressPt;
    Point2D canvasPressPt;
    
    /**
	 * Creates a selection event handler.
	 * @param marqueeParent The node to which the event handler dynamically adds a marquee
	 * (temporarily) to represent the area being selected.
	 * @param selectableParent The node whose children will be selected
	 * by this event handler.
	 */
	public PSWTSelectionEventHandler(PNode marqueeParent, PNode selectableParent) {
	    super(new PNode(),selectableParent);
	    this.marqueeParent = marqueeParent;
	}

	/**
	 * Creates a selection event handler.
	 * @param marqueeParent The node to which the event handler dynamically adds a marquee
	 * (temporarily) to represent the area being selected.
	 * @param selectableParents A list of nodes whose children will be selected
	 * by this event handler.
	 */
	public PSWTSelectionEventHandler(PNode marqueeParent, List selectableParents) {
	    super(new PNode(),selectableParents);
	    this.marqueeParent = marqueeParent;
	}

	public void decorateSelectedNode(PNode node) {	    
		PSWTBoundsHandle.addBoundsHandlesTo(node);
	}

	public void undecorateSelectedNode(PNode node) {
		PSWTBoundsHandle.removeBoundsHandlesFrom(node);
	}

	protected void initializeSelection(PInputEvent pie) {
	    super.initializeSelection(pie);
	    pressPt = pie.getPosition();
		canvasPressPt = pie.getCanvasPosition();
	}
	
	protected void initializeMarquee(PInputEvent e) {
	    super.initializeMarquee(e);
	    
	    marquee = new PSWTPath(new Rectangle2D.Float((float)pressPt.getX(), (float)pressPt.getY(), 0, 0)) {
            protected void paint(PPaintContext paintContext) {
                SWTGraphics2D s2g = (SWTGraphics2D)paintContext.getGraphics();
                s2g.gc.setLineStyle(SWT.LINE_DASH);
                super.paint(paintContext);
                s2g.gc.setLineStyle(SWT.LINE_SOLID);
            }
	    };
		marquee.setStrokeColor(Color.black);
		marquee.setPaint(null);
		marqueeParent.addChild(marquee);
	}

	protected void updateMarquee(PInputEvent pie) {
		super.updateMarquee(pie);
	    
	    PBounds b = new PBounds();

		if (marqueeParent instanceof PCamera) {
			b.add(canvasPressPt);
			b.add(pie.getCanvasPosition());
		}
		else {
			b.add(pressPt);
			b.add(pie.getPosition());
		}

		marquee.setPathToRectangle((float) b.x, (float) b.y, (float) b.width, (float) b.height);				
		b.reset();
		b.add(pressPt);
		b.add(pie.getPosition());
	}

	protected PBounds getMarqueeBounds() {
		if (marquee != null) {
			return marquee.getBounds();
		}	
		return new PBounds();
	}

	protected void endMarqueeSelection(PInputEvent e) {
	    super.endMarqueeSelection(e);
	    
	    // Remove marquee
		marquee.removeFromParent();
		marquee = null; 				
	}

	/**
	 * This gets called continuously during the drag, and is used to animate the marquee
	 */
	protected void dragActivityStep(PInputEvent aEvent) {
	}
}