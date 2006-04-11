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

import java.awt.Color;
import java.awt.Paint;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PDragEventHandler;
import edu.umd.cs.piccolo.nodes.PPath;

/**
 * <b>PLens</b> is a simple default lens implementation for Piccolo. See piccolo/examples
 * LensExample for one possible use of this lens. Lens's are often application
 * specific, it may be easiest to study this code, and then implement your own custom
 * lens using the general principles illustrated here.
 * <P>
 * The basic design here is to add a PCamera as the child of a Pnode (the lens node). The camera is
 * the viewing part of the lens, and the node is the title bar that can be used to
 * move the lens around. Users of this lens will probably want to set up some lens
 * specific event handler and attach it to the camera. 
 * <P>
 * A lens also needs a layer that it will look at (it should not be the same as the layer
 * that it's added to because then it will draw itself in a recursive loop. Last of all 
 * the PLens will need to be added to the PCanvas layer (so that it can be seen 
 * by the main camera).
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PLens extends PNode {

	public static double LENS_DRAGBAR_HEIGHT = 20;
	public static Paint DEFAULT_DRAGBAR_PAINT = Color.DARK_GRAY;
	public static Paint DEFAULT_LENS_PAINT = Color.LIGHT_GRAY;
		
	private PPath dragBar;
	private PCamera camera;
	private PDragEventHandler lensDragger;
	
	public PLens() {
		dragBar = PPath.createRectangle(0, 0, 100, 100);  // Drag bar gets resized to fit the available space, so any rectangle will do here
		dragBar.setPaint(DEFAULT_DRAGBAR_PAINT);
		dragBar.setPickable(false);  // This forces drag events to percolate up to PLens object
		addChild(dragBar);
		
		camera = new PCamera();
		camera.setPaint(DEFAULT_LENS_PAINT);
		addChild(camera);
		
		// create an event handler to drag the lens around. Note that this event
		// handler consumes events in case another conflicting event handler has been
		// installed higher up in the heirarchy.
		lensDragger = new PDragEventHandler();
		lensDragger.getEventFilter().setMarksAcceptedEventsAsHandled(true);
		addInputEventListener(lensDragger);

		// When this PLens is dragged around adjust the cameras view transform. 	
		addPropertyChangeListener(PNode.PROPERTY_TRANSFORM, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent evt) {
				camera.setViewTransform(getInverseTransform());
			}
		}); 	
	}
	
	public PLens(PLayer layer) {
		this();
		addLayer(0, layer);
	}
	
	public PCamera getCamera() {
		return camera;
	}
	
	public PPath getDragBar() {
		return dragBar;
	}

	public PDragEventHandler getLensDraggerHandler() {
		return lensDragger;
	}
		
	public void addLayer(int index, PLayer layer) {
		camera.addLayer(index, layer);
	}
	
	public void removeLayer(PLayer layer) {
		camera.removeLayer(layer);
	}
	
	// when the lens is resized this method gives us a chance to layout the lenses
	// camera child appropriately.
	protected void layoutChildren() {
		dragBar.setPathToRectangle((float)getX(), (float)getY(), (float)getWidth(), (float)LENS_DRAGBAR_HEIGHT);
		camera.setBounds(getX(), getY() + LENS_DRAGBAR_HEIGHT, getWidth(), getHeight() - LENS_DRAGBAR_HEIGHT);
	}
}
