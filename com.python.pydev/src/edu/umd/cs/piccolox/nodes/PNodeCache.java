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
import java.awt.Image;
import java.awt.geom.Dimension2D;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDimension;
import edu.umd.cs.piccolo.util.PPaintContext;
import edu.umd.cs.piccolo.util.PPickPath;

/**
 * <b>PNodeCache</b> caches a visual representation of it's children 
 * into an image and uses this cached image for painting instead of
 * painting it's children directly. This  is intended to be used in 
 * two ways.
 * <P>
 * First it can be used as a simple optimization technique. If a node 
 * has many descendents it may be faster to paint the cached image 
 * representation instead of painting each node.
 * <P>
 * Second PNodeCache provides a place where "image" effects such as
 * blurring and drop shadows can be added to the Piccolo scene graph.
 * This can be done by overriding the method createImageCache and
 * returing an image with the desired effect applied.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PNodeCache extends PNode {
	
	private transient Image imageCache;
	private boolean validatingCache;
	
	/**
	 * Override this method to customize the image cache creation process. For
	 * example if you want to create a shadow effect you would do that here. Fill
	 * in the cacheOffsetRef if needed to make your image cache line up with the
	 * nodes children.
	 */
	public Image createImageCache(Dimension2D cacheOffsetRef) {
		return toImage();		
	}
	
	public Image getImageCache() {
		if (imageCache == null) {			
			PDimension cacheOffsetRef = new PDimension();
			validatingCache = true;
			resetBounds();
			imageCache = createImageCache(cacheOffsetRef);
			PBounds b = getFullBoundsReference();
			setBounds(b.getX() + cacheOffsetRef.getWidth(),
					  b.getY() + cacheOffsetRef.getHeight(),
					  imageCache.getWidth(null), 
					  imageCache.getHeight(null));
			validatingCache = false;
		}
		return imageCache;
	}
	
	public void invalidateCache() {
		imageCache = null;
	}	
	
	public void invalidatePaint() {
		if (!validatingCache) {
			super.invalidatePaint();
		}
	}
	
	public void repaintFrom(PBounds localBounds, PNode childOrThis) {
		if (!validatingCache) {
			super.repaintFrom(localBounds, childOrThis);
			invalidateCache();
		}
	}	
	
	public void fullPaint(PPaintContext paintContext) {		
		if (validatingCache) {
			super.fullPaint(paintContext);
		} else {
			Graphics2D g2 = paintContext.getGraphics();
			g2.drawImage(getImageCache(), (int) getX(), (int) getY(), null);
		}
	}
	
	protected boolean pickAfterChildren(PPickPath pickPath) {
		return false;
	}
}
