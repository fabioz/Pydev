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
package edu.umd.cs.piccolox.util;

import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PPickPath;

/**
 * Experimental class for detecting occlusions.
 * 
 * @author Jesse Grosjean
 */
public class POcclusionDetection {

	/**
	 * Traverse from the bottom right of the scene graph (top visible node)
	 * up the tree determining which parent nodes are occluded by their children
	 * nodes. Note that this is only detecting a subset of occlusions (parent, child), 
	 * others such as overlapping siblings or cousins are not detected.
	 */
	public void detectOccusions(PNode n, PBounds parentBounds) {
		detectOcclusions(n, new PPickPath(null, parentBounds));
	}

	public void detectOcclusions(PNode n, PPickPath pickPath) {
		if (n.fullIntersects(pickPath.getPickBounds())) {
			pickPath.pushTransform(n.getTransformReference(false));
		
			int count = n.getChildrenCount();
			for (int i = count - 1; i >= 0; i--) {
				PNode each = (PNode) n.getChild(i);
				if (n.getOccluded()) {
					// if n has been occuded by a previous decendent then
					// this child must also be occuded
					each.setOccluded(true);
				} else {
					// see if child each occludes n
					detectOcclusions(each, pickPath);
				}
			}

			// see if n occudes it's parents		
			if (!n.getOccluded()) {
				if (n.intersects(pickPath.getPickBounds())) {
					if (n.isOpaque(pickPath.getPickBounds())) {
						PNode p = n.getParent();
						while (p != null && !p.getOccluded()) {
							p.setOccluded(true);
						}
					}
				}
			}
	
			pickPath.popTransform(n.getTransformReference(false));
		}				
	}
}
