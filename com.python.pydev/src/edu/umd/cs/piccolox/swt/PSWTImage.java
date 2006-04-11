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

import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.nodes.PImage;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * <b>PImage</b> is a wrapper around a java.awt.Image. If this node is
 * copied or serialized that image will be converted into a BufferedImage if
 * it is not already one.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PSWTImage extends PNode {
	
	private transient PSWTCanvas canvas;

	private transient Image image;

	public PSWTImage(PSWTCanvas canvas) {
		super();
		
		this.canvas = canvas;
		canvas.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent de) {
				if (image != null) {
					image.dispose();
				}	
			}
		});
	}
	
	public PSWTImage(PSWTCanvas canvas, Image newImage) {
		this(canvas);
		setImage(newImage);
	}

	public PSWTImage(PSWTCanvas canvas, String fileName) {
		this(canvas);
		setImage(fileName);	
	}
	
	/**
	 * Returns the image that is shown by this node.
	 * @return the image that is shown by this node
	 */ 
	public Image getImage() {
		return image;
	}

	/**
	 * Set the image that is wrapped by this PImage node. This method will also load
	 * the image using a MediaTracker before returning. And if the this PImage is
	 * accelerated that I'm will be copied into an accelerated image if needed. Note
	 * that this may cause undesired results with images that have transparent regions,
	 * for those cases you may want to set the PImage to be not accelerated.
	 */
	public void setImage(String fileName) {
		setImage(new Image(canvas.getDisplay(),fileName));
	}

	/**
	 * Set the image that is wrapped by this PImage node. This method will also load
	 * the image using a MediaTracker before returning. And if the this PImage is
	 * accelerated that I'm will be copied into an accelerated image if needed. Note
	 * that this may cause undesired results with images that have transparent regions,
	 * for those cases you may want to set the PImage to be not accelerated.
	 */
	public void setImage(Image newImage) {
		Image old = image;
		image = newImage;
				
		if (image != null) {								
			Rectangle bounds = getImage().getBounds();
			setBounds(0, 0, bounds.width, bounds.height);
			invalidatePaint();
		} else {
			image = null;
		}
		
		firePropertyChange(PImage.PROPERTY_CODE_IMAGE, PImage.PROPERTY_IMAGE, old, image);
	}

	protected void paint(PPaintContext paintContext) {
		if (getImage() != null) {
			Rectangle r = image.getBounds();
			double iw = r.width;
			double ih = r.height;
			PBounds b = getBoundsReference();
			SWTGraphics2D g2 = (SWTGraphics2D)paintContext.getGraphics();

			if (b.x != 0 || b.y != 0 || b.width != iw || b.height != ih) {
				g2.translate(b.x, b.y);
				g2.scale(b.width / iw, b.height / ih);
				g2.drawImage(image, 0, 0);
				g2.scale(iw / b.width, ih / b.height);
				g2.translate(-b.x, -b.y);
			} else {
				g2.drawImage(image, 0, 0);
			}
		}
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

		result.append("image=" + (image == null ? "null" : image.toString()));
		
		result.append(',');
		result.append(super.paramString());

		return result.toString();
	}
}
