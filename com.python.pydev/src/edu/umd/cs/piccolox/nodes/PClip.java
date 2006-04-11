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

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.util.PPickPath;

/**
 * <b>PClip</b> is a simple node that applies a clip before rendering or picking its
 * children. PClip is a subclass of PPath, the clip applies is the GeneralPath wrapped
 * by its super class. See piccolo/examples ClipExample.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean 
 */
public class PClip extends PPath {

	public PBounds computeFullBounds(PBounds dstBounds) {
		if (dstBounds == null) dstBounds = new PBounds();
		dstBounds.reset();
		dstBounds.add(getBoundsReference());
		localToParent(dstBounds);
		return dstBounds;
	}

	public void repaintFrom(PBounds localBounds, PNode childOrThis) {
		if (childOrThis != this) {
			Rectangle2D.intersect(getBoundsReference(), localBounds, localBounds);
			super.repaintFrom(localBounds, childOrThis);
		} else {
			super.repaintFrom(localBounds, childOrThis);
		}
	}

	protected void paint(PPaintContext paintContext) {
		Paint p = getPaint();			
		if (p != null) {
			Graphics2D g2 = paintContext.getGraphics();
			g2.setPaint(p);
			g2.fill(getPathReference());
		}
		paintContext.pushClip(getPathReference());
	}
	
	protected void paintAfterChildren(PPaintContext paintContext) {
		paintContext.popClip(getPathReference());
		if (getStroke() != null && getStrokePaint() != null) {
			Graphics2D g2 = paintContext.getGraphics();
			g2.setPaint(getStrokePaint());
			g2.setStroke(getStroke());
			g2.draw(getPathReference());
		}		
	}
	
	public boolean fullPick(PPickPath pickPath) {
		if (getPickable() && fullIntersects(pickPath.getPickBounds())) {
			pickPath.pushNode(this);
			pickPath.pushTransform(getTransformReference(false));
			
			if (pick(pickPath)) {
				return true;
			}
			
			if (getChildrenPickable() && getPathReference().intersects(pickPath.getPickBounds())) { 		
				int count = getChildrenCount();
				for (int i = count - 1; i >= 0; i--) {
					PNode each = getChild(i);
					if (each.fullPick(pickPath))
						return true;
				}				
			}

			if (pickAfterChildren(pickPath)) {
				return true;
			}

			pickPath.popTransform(getTransformReference(false));
			pickPath.popNode(this);
		}

		return false;
	}	
}
