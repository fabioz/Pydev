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

import java.awt.geom.Rectangle2D;

import javax.swing.SwingConstants;

import edu.umd.cs.piccolo.PNode;

/**
 * <b>PBoundsLocator</b> is a locator that locates points on the 
 * bounds of a node.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PBoundsLocator extends PNodeLocator {

	private int side;
	
	public static PBoundsLocator createEastLocator(PNode node) {
		return new PBoundsLocator(node, SwingConstants.EAST);
	}

	public static PBoundsLocator createNorthEastLocator(PNode node) {
		return new PBoundsLocator(node, SwingConstants.NORTH_EAST);
	}

	public static PBoundsLocator createNorthWestLocator(PNode node) {
		return new PBoundsLocator(node, SwingConstants.NORTH_WEST);
	}

	public static PBoundsLocator createNorthLocator(PNode node) {
		return new PBoundsLocator(node, SwingConstants.NORTH);
	}

	public static PBoundsLocator createSouthLocator(PNode node) {
		return new PBoundsLocator(node, SwingConstants.SOUTH);
	}

	public static PBoundsLocator createWestLocator(PNode node) {
		return new PBoundsLocator(node, SwingConstants.WEST);
	}

	public static PBoundsLocator createSouthWestLocator(PNode node) {
		return new PBoundsLocator(node, SwingConstants.SOUTH_WEST);
	}

	public static PBoundsLocator createSouthEastLocator(PNode node) {
		return new PBoundsLocator(node, SwingConstants.SOUTH_EAST);
	}

	public PBoundsLocator(PNode node, int aSide) {
		super(node);
		side = aSide;
	}

	public int getSide() {
		return side;
	}
	
	public void setSide(int side) {
		this.side = side;
	}
	
	public double locateX() {
		Rectangle2D aBounds = node.getBoundsReference();

		switch (side) {
			case SwingConstants.NORTH_WEST :
			case SwingConstants.SOUTH_WEST :
			case SwingConstants.WEST :
				return aBounds.getX();

			case SwingConstants.NORTH_EAST :
			case SwingConstants.SOUTH_EAST :
			case SwingConstants.EAST :
				return aBounds.getX() + aBounds.getWidth();

			case SwingConstants.NORTH :
			case SwingConstants.SOUTH :
				return aBounds.getX() + (aBounds.getWidth() / 2);
		}
		return -1;
	}

	public double locateY() {
		Rectangle2D aBounds = node.getBoundsReference();

		switch (side) {
			case SwingConstants.EAST :
			case SwingConstants.WEST :
				return aBounds.getY() + (aBounds.getHeight() / 2);

			case SwingConstants.SOUTH :
			case SwingConstants.SOUTH_WEST :
			case SwingConstants.SOUTH_EAST :
				return aBounds.getY() + aBounds.getHeight();

			case SwingConstants.NORTH_WEST :
			case SwingConstants.NORTH_EAST :
			case SwingConstants.NORTH :
				return aBounds.getY();
		}
		return -1;
	}	
}
