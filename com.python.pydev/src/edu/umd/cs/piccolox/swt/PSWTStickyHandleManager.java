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

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPickPath;

public class PSWTStickyHandleManager extends PNode {
	
	private PNode target;
	private PCamera camera;
	private boolean isResizing;
		
	public PSWTStickyHandleManager(PCamera newCamera, PNode newTarget) {		
		setCameraTarget(newCamera, newTarget);		
		PSWTBoundsHandle.addBoundsHandlesTo(this);
	}

	public void setCameraTarget(PCamera newCamera, PNode newTarget) {
		camera = newCamera;
		camera.addChild(this);
		target = newTarget;
	}
	
	public boolean setBounds(double x, double y, double width, double height) {
		PBounds b = new PBounds(x, y, width, height);
		camera.localToGlobal(b);
		camera.localToView(b);
		target.globalToLocal(b);
		target.setBounds(b);
		return super.setBounds(x, y, width, height);
	}
	
	protected boolean getBoundsVolatile() {
		return true;
	}

	public PBounds getBoundsReference() {
		PBounds targetBounds = target.getFullBounds();
		camera.viewToLocal(targetBounds);
		camera.globalToLocal(targetBounds);
		PBounds bounds = super.getBoundsReference();
		bounds.setRect(targetBounds);
		return super.getBoundsReference();
	}

	public void startResizeBounds() {
		super.startResizeBounds();
		target.startResizeBounds();
		isResizing = true;
	}

	public void endResizeBounds() {
		super.endResizeBounds();
		target.endResizeBounds();
		isResizing = false;
	}

	public boolean pickAfterChildren(PPickPath pickPath) {
		return false;
	}
}
