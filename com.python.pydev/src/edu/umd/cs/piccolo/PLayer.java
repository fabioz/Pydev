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
package edu.umd.cs.piccolo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PObjectOutputStream;

/**
 * <b>PLayer</b> is a node that can be viewed directly by multiple camera nodes.
 * Generally child nodes are added to a layer to give the viewing cameras 
 * something to look at.
 * <P>
 * A single layer node may be viewed through multiple cameras with each
 * camera using its own view transform. This means that any node (since layers can have
 * children) may be visible through multiple cameras at the same time.
 * <p>
 * @see PCamera
 * @see edu.umd.cs.piccolo.event.PInputEvent
 * @see edu.umd.cs.piccolo.util.PPickPath
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PLayer extends PNode {
	
	/** 
	 * The property name that identifies a change in the set of this layer's
	 * cameras (see {@link #getCamera getCamera}, {@link #getCameraCount
	 * getCameraCount}, {@link #getCamerasReference getCamerasReference}). In
	 * any property change event the new value will be a reference to the list
	 * of cameras, but old value will always be null.
	 */
	public static final String PROPERTY_CAMERAS = "cameras";
    public static final int PROPERTY_CODE_CAMERAS = 1 << 13;

	private transient List cameras;

	public PLayer() {
		super();
		cameras = new ArrayList();
	}
	
	//****************************************************************
	// Cameras - Maintain the list of cameras that are viewing this
	// layer.
	//****************************************************************
	
	/**
	 * Get the list of cameras viewing this layer.
	 */
	public List getCamerasReference() {
		return cameras;
	}

	/**
	 * Get the number of cameras viewing this layer.
	 */ 
	public int getCameraCount() {
		if (cameras == null) {
			return 0;
		}
		return cameras.size();
	}
	
	/**
	 * Get the camera in this layer's camera list at the specified index.
	 */
	public PCamera getCamera(int index) {
		return (PCamera) cameras.get(index);
	}
	
	/**
	 * Add a camera to this layer's camera list. This method it called automatically
	 * when a layer is added to a camera.
	 */
	public void addCamera(PCamera camera) {
		addCamera(cameras.size(), camera);
	}
	
	/**
	 * Add a camera to this layer's camera list at the specified index. This 
	 * method it called automatically when a layer is added to a camera.
	 */
	public void addCamera(int index, PCamera camera) {
		cameras.add(index, camera);
		invalidatePaint();
		firePropertyChange(PROPERTY_CODE_CAMERAS, PROPERTY_CAMERAS, null, cameras);
	}

	/**
	 * Remove the camera from this layer's camera list.
	 */ 
	public PCamera removeCamera(PCamera camera) {
		return removeCamera(cameras.indexOf(camera));
	}
	
	/**
	 * Remove the camera at the given index from this layer's camera list.
	 */ 
	public PCamera removeCamera(int index) {
		PCamera result = (PCamera) cameras.remove(index);
		invalidatePaint();
		firePropertyChange(PROPERTY_CODE_CAMERAS, PROPERTY_CAMERAS, null, cameras);
		return result;
	}
		
	//****************************************************************
	// Camera Repaint Notifications - Layer nodes must forward their
	// repaints to each camera that is viewing them so that the camera
	// views will also get repainted.
	//****************************************************************
		
	/**
	 * Override repaints and forward them to the cameras that are
	 * viewing this layer.
	 */
	public void repaintFrom(PBounds localBounds, PNode childOrThis) {						
		if (childOrThis != this) {
			localToParent(localBounds);
		}
			
		notifyCameras(localBounds);
		
		if (getParent() != null) {
			getParent().repaintFrom(localBounds, childOrThis);
		}
	}	
	
	protected void notifyCameras(PBounds parentBounds) {
		int count = getCameraCount();
		for (int i = 0; i < count; i++) {
			PCamera each = (PCamera) cameras.get(i);
			each.repaintFromLayer(parentBounds, this);
		}
	}
	
	//****************************************************************
	// Serialization - Layers conditionally serialize their cameras.
	// This means that only the camera references that were unconditionally
	// (using writeObject) serialized by someone else will be restored
	// when the layer is unserialized.
	//****************************************************************
	
	/**
	 * Write this layer and all its children out to the given stream. Note
	 * that the layer writes out any cameras that are viewing it conditionally, so they will only
	 * get written out if someone else writes them unconditionally.
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		
		int count = getCameraCount();
		for (int i = 0; i < count; i++) {
			((PObjectOutputStream)out).writeConditionalObject(cameras.get(i));			
		}
		
		out.writeObject(Boolean.FALSE);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		
		cameras = new ArrayList();
		
		while (true) {
			Object each = in.readObject();
			if (each != null) {
				if (each.equals(Boolean.FALSE)) {
					break;
				} else {
					cameras.add(each);
				}
			}
		}
	}	
}
