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
 */package edu.umd.cs.piccolox.util;

import edu.umd.cs.piccolo.PNode;

/**
 * <b>PNodeLocator</b> provides an abstraction for locating points on a node.
 * Points are located in the local corrdinate system of the node. The default
 * behavior is to locate the center point of the nodes bounds. The node where
 * the point is located is stored internal to this locator (as an instance
 * varriable). If you want to use the same locator to locate center points on
 * many different nodes you will need to call setNode() before asking for each
 * location.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PNodeLocator extends PLocator {
	
	protected PNode node;

	public PNodeLocator(PNode node) {
		setNode(node);
	}

	public PNode getNode() {
		return node;
	}
	
	public void setNode(PNode node) {
		this.node = node;
	}

	public double locateX() {
		return node.getBoundsReference().getCenterX();
	}

	public double locateY() {
		return node.getBoundsReference().getCenterY();
	}
}
