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
package edu.umd.cs.piccolo.nodes;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPaintContext;

/**
 * <b>PImage</b> is a wrapper around a java.awt.Image. If this node is copied or
 * serialized that image will be converted into a BufferedImage if it is not
 * already one.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PImage extends PNode {
	
	/** 
	 * The property name that identifies a change of this node's image (see
	 * {@link #getImage getImage}). Both old and new value will be set correctly
	 * to Image objects in any property change event.
	 */	
	public static final String PROPERTY_IMAGE = "image";
    public static final int PROPERTY_CODE_IMAGE = 1 << 15;

	private transient Image image;

	public PImage() {
		super();
	}
	
	/**
	 * Construct a new PImage wrapping the given java.awt.Image.
	 */
	public PImage(Image newImage) {
		this();
		setImage(newImage);
	}

	/**
	 * Construct a new PImage by loading the given fileName and wrapping the
	 * resulting java.awt.Image.
	 */
	public PImage(String fileName) {
		this(Toolkit.getDefaultToolkit().getImage(fileName));
	}
	
	/**
	 * Construct a new PImage by loading the given url and wrapping the
	 * resulting java.awt.Image.  If the url is <code>null</code>, create an
	 * empty PImage; this behaviour is useful when fetching resources that may
	 * be missing.
	 */
	public PImage(java.net.URL url) {
		this();
		if (url != null) setImage(Toolkit.getDefaultToolkit().getImage(url));
	}	
	
	/**
	 * Returns the image that is shown by this node.
	 * @return the image that is shown by this node
	 */ 
	public Image getImage() {
		return image;
	}

	/**
	 * Set the image that is wrapped by this PImage node. This method will also
	 * load the image using a MediaTracker before returning.
	 */
	public void setImage(String fileName) {
		setImage(Toolkit.getDefaultToolkit().getImage(fileName));
	}

	/**
	 * Set the image that is wrapped by this PImage node. This method will also
	 * load the image using a MediaTracker before returning.
	 */
	public void setImage(Image newImage) {
		Image old = image;
		
		if (newImage instanceof BufferedImage) {
			image = newImage;
		} else { // else make sure the image is loaded
			ImageIcon imageLoader = new ImageIcon(newImage);
			switch (imageLoader.getImageLoadStatus()) {
				case MediaTracker.LOADING:
					System.err.println("media tracker still loading image after requested to wait until finished");

				case MediaTracker.COMPLETE:
					image = imageLoader.getImage();
					break;

				case MediaTracker.ABORTED:
					System.err.println("media tracker aborted image load");
					image = null;
					break;

				case MediaTracker.ERRORED:
					System.err.println("media tracker errored image load");
					image = null;
					break;
			}
		}
		
		if (image != null) {					
			setBounds(0, 0, getImage().getWidth(null), getImage().getHeight(null));
			invalidatePaint();
		} else {
			image = null;
		}
		
		firePropertyChange(PROPERTY_CODE_IMAGE, PROPERTY_IMAGE, old, image);
	}

	protected void paint(PPaintContext paintContext) {
		if (getImage() != null) {
			double iw = image.getWidth(null);
			double ih = image.getHeight(null);
			PBounds b = getBoundsReference();
			Graphics2D g2 = paintContext.getGraphics();

			if (b.x != 0 || b.y != 0 || b.width != iw || b.height != ih) {
				g2.translate(b.x, b.y);
				g2.scale(b.width / iw, b.height / ih);
				g2.drawImage(image, 0, 0, null);
				g2.scale(iw / b.width, ih / b.height);
				g2.translate(-b.x, -b.y);
			} else {
				g2.drawImage(image, 0, 0, null);
			}
		}
	}
	
	//****************************************************************
	// Serialization
	//****************************************************************
	
	/**
	 * The java.awt.Image wrapped by this PImage is converted into a BufferedImage
	 * when serialized.
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		BufferedImage bufferedImage = toBufferedImage(image, false);
		if (bufferedImage != null) ImageIO.write(bufferedImage, "png", out);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		image = ImageIO.read(in);
	}
	
	//****************************************************************
	// Util
	//****************************************************************
	
	/**
	 * If alwaysCreateCopy is false then if the image is already a buffered
	 * image it will not be copied and instead the original image will just be
	 * returned.
	 */
	public static BufferedImage toBufferedImage(Image image, boolean alwaysCreateCopy) {
		if (image == null) return null;
		
		if (!alwaysCreateCopy && image instanceof BufferedImage) {
			return (BufferedImage) image;
		} else {
			GraphicsConfiguration graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
			BufferedImage result = graphicsConfiguration.createCompatibleImage(image.getWidth(null), image.getHeight(null));
			Graphics2D g2 = result.createGraphics();
			g2.drawImage(image, 0, 0, null);
			g2.dispose();
			return result;
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
