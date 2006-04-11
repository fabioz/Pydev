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

import java.awt.event.InputEvent;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PDimension;

/**
 * <b>PDragEventHandler</b> is a simple event handler for dragging a
 * node on the canvas.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PDragEventHandler extends PDragSequenceEventHandler {

	private PNode draggedNode;
	private boolean moveToFrontOnPress = false;
	
	public PDragEventHandler() {
		super();
		setEventFilter(new PInputEventFilter(InputEvent.BUTTON1_MASK));
	}
	
	protected PNode getDraggedNode() {
		return draggedNode;
	}
	
	protected void setDraggedNode(PNode draggedNode) {
		this.draggedNode = draggedNode;
	}
		
	protected boolean shouldStartDragInteraction(PInputEvent event) {
		if (super.shouldStartDragInteraction(event)) {
			return event.getPickedNode() != event.getTopCamera();
		}
		return false;
	}

	protected void startDrag(PInputEvent event) {
		super.startDrag(event); 	
		draggedNode = event.getPickedNode();
		if (moveToFrontOnPress) {
			draggedNode.moveToFront();
		}
	}

	protected void drag(PInputEvent event) {
		super.drag(event);
		PDimension d = event.getDeltaRelativeTo(draggedNode);		
		draggedNode.localToParent(d);
		draggedNode.offset(d.getWidth(), d.getHeight());
	}

	protected void endDrag(PInputEvent event) {
		super.endDrag(event);
		draggedNode = null;
	}	

	public boolean getMoveToFrontOnPress() {
		return moveToFrontOnPress;
	}

	public void setMoveToFrontOnPress(boolean moveToFrontOnPress) {
		this.moveToFrontOnPress = moveToFrontOnPress;
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

		result.append("draggedNode=" + draggedNode == null ? "null" : draggedNode.toString());
		if (moveToFrontOnPress) result.append(",moveToFrontOnPress");
		result.append(',');
		result.append(super.paramString());

		return result.toString();
	}	
}
