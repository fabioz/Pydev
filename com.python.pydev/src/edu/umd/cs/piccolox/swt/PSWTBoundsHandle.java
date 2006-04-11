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

import java.awt.Cursor;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.SwingConstants;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDimension;
import edu.umd.cs.piccolo.util.PPickPath;
import edu.umd.cs.piccolox.util.PBoundsLocator;

/**
 * <b>PBoundsHandle</b> a handle for resizing the bounds of another node. If a
 * bounds handle is dragged such that the other node's width or height becomes
 * negative then the each drag handle's locator assciated with that
 * other node is "flipped" so that they are attached to and dragging a different
 * corner of the nodes bounds.
 * <P>
 * @version 1.0
 * @author Jesse Grosjean
 */
public class PSWTBoundsHandle extends PSWTHandle {
	
	private PBasicInputEventHandler handleCursorHandler;
	
	public static void addBoundsHandlesTo(PNode aNode) {
		aNode.addChild(new PSWTBoundsHandle(PBoundsLocator.createEastLocator(aNode))); 
		aNode.addChild(new PSWTBoundsHandle(PBoundsLocator.createWestLocator(aNode))); 
		aNode.addChild(new PSWTBoundsHandle(PBoundsLocator.createNorthLocator(aNode))); 
		aNode.addChild(new PSWTBoundsHandle(PBoundsLocator.createSouthLocator(aNode)));
		aNode.addChild(new PSWTBoundsHandle(PBoundsLocator.createNorthEastLocator(aNode))); 
		aNode.addChild(new PSWTBoundsHandle(PBoundsLocator.createNorthWestLocator(aNode))); 
		aNode.addChild(new PSWTBoundsHandle(PBoundsLocator.createSouthEastLocator(aNode))); 
		aNode.addChild(new PSWTBoundsHandle(PBoundsLocator.createSouthWestLocator(aNode))); 	
	}

	public static void addStickyBoundsHandlesTo(PNode aNode, PCamera camera) {
		camera.addChild(new PSWTBoundsHandle(PBoundsLocator.createEastLocator(aNode)));
		camera.addChild(new PSWTBoundsHandle(PBoundsLocator.createWestLocator(aNode)));
		camera.addChild(new PSWTBoundsHandle(PBoundsLocator.createNorthLocator(aNode)));
		camera.addChild(new PSWTBoundsHandle(PBoundsLocator.createSouthLocator(aNode)));
		camera.addChild(new PSWTBoundsHandle(PBoundsLocator.createNorthEastLocator(aNode)));
		camera.addChild(new PSWTBoundsHandle(PBoundsLocator.createNorthWestLocator(aNode)));
		camera.addChild(new PSWTBoundsHandle(PBoundsLocator.createSouthEastLocator(aNode)));
		camera.addChild(new PSWTBoundsHandle(PBoundsLocator.createSouthWestLocator(aNode)));
	}
	
	public static void removeBoundsHandlesFrom(PNode aNode) {
		ArrayList handles = new ArrayList();

		Iterator i = aNode.getChildrenIterator();
		while (i.hasNext()) {
			PNode each = (PNode) i.next();
			if (each instanceof PSWTBoundsHandle) {
				handles.add(each);
			}
		}
		aNode.removeChildren(handles);		
	}
	
	public PSWTBoundsHandle(PBoundsLocator aLocator) {
		super(aLocator);
	}
	
	protected void installHandleEventHandlers() {
		super.installHandleEventHandlers();
		handleCursorHandler = new PBasicInputEventHandler() {
			boolean cursorPushed = false;			
			public void mouseEntered(PInputEvent aEvent) {
				if (!cursorPushed) {
					aEvent.pushCursor(getCursorFor(((PBoundsLocator)getLocator()).getSide()));
					cursorPushed = true;
				}
			}
			public void mouseExited(PInputEvent aEvent) {
				PPickPath focus = aEvent.getInputManager().getMouseFocus();
				if (cursorPushed) {
					if (focus == null || focus.getPickedNode() != PSWTBoundsHandle.this) {
						aEvent.popCursor();
						cursorPushed = false;
					}
				}
			}
			public void mouseReleased(PInputEvent event) {
				if (cursorPushed) {
					event.popCursor();
					cursorPushed = false;
				}
			}
		};
		addInputEventListener(handleCursorHandler);
	}	

	/**
	 * Return the event handler that is responsible for setting the mouse
	 * cursor when it enters/exits this handle.
	 */
	public PBasicInputEventHandler getHandleCursorEventHandler() {
		return handleCursorHandler;
	}
	
	public void startHandleDrag(Point2D aLocalPoint, PInputEvent aEvent) {
		PBoundsLocator l = (PBoundsLocator) getLocator();
		l.getNode().startResizeBounds();
	}
	
	public void dragHandle(PDimension aLocalDimension, PInputEvent aEvent) {
	    PBoundsLocator l = (PBoundsLocator) getLocator();
				
		PNode n = l.getNode();
		PBounds b = n.getBounds();

		PNode parent = getParent();
		if (parent != n && parent instanceof PCamera) {
			((PCamera)parent).localToView(aLocalDimension);
		}

		localToGlobal(aLocalDimension);
		n.globalToLocal(aLocalDimension);
		
		double dx = aLocalDimension.getWidth();
		double dy = aLocalDimension.getHeight();
			
		switch (l.getSide()) {
			case SwingConstants.NORTH:
				b.setRect(b.x, b.y + dy, b.width, b.height - dy);
				break;
			
			case SwingConstants.SOUTH:
				b.setRect(b.x, b.y, b.width, b.height + dy);
				break;
			
			case SwingConstants.EAST:
				b.setRect(b.x, b.y, b.width + dx, b.height);
				break;
			
			case SwingConstants.WEST:
				b.setRect(b.x + dx, b.y, b.width - dx, b.height);
				break;
			
			case SwingConstants.NORTH_WEST:
				b.setRect(b.x + dx, b.y + dy, b.width - dx, b.height - dy);
				break;
			
			case SwingConstants.SOUTH_WEST:
				b.setRect(b.x + dx, b.y, b.width - dx, b.height + dy);
				break;
			
			case SwingConstants.NORTH_EAST:
				b.setRect(b.x, b.y + dy, b.width + dx, b.height - dy);
				break;
			
			case SwingConstants.SOUTH_EAST:
				b.setRect(b.x, b.y, b.width + dx, b.height + dy);
				break;
		}
		
		boolean flipX = false;
		boolean flipY = false;
		
		if (b.width < 0) {
			flipX = true;
			b.width = -b.width;
			b.x -= b.width;
		}
		
		if (b.height < 0) {
			flipY = true;
			b.height = -b.height;
			b.y -= b.height;
		}
		
		if (flipX || flipY) {
			flipSiblingBoundsHandles(flipX, flipY);
		}
		
		n.setBounds(b);
	}
	
	public void endHandleDrag(Point2D aLocalPoint, PInputEvent aEvent) {
		PBoundsLocator l = (PBoundsLocator) getLocator();
		l.getNode().endResizeBounds();
	}	
	
	public void flipSiblingBoundsHandles(boolean flipX, boolean flipY) {
		Iterator i = getParent().getChildrenIterator();
		while (i.hasNext()) {
			Object each = i.next();
			if (each instanceof PSWTBoundsHandle) {
				((PSWTBoundsHandle)each).flipHandleIfNeeded(flipX, flipY);
			}
		}
	}
	
	public void flipHandleIfNeeded(boolean flipX, boolean flipY) {		
		PBoundsLocator l = (PBoundsLocator) getLocator();	
		
		if (flipX || flipY) {		
			switch (l.getSide()) {
				case SwingConstants.NORTH: {
					if (flipY) {
						l.setSide(SwingConstants.SOUTH);
					}					
					break;
				}
				
				case SwingConstants.SOUTH: {
					if (flipY) {
						l.setSide(SwingConstants.NORTH);
					}					
					break;
				}
				
				case SwingConstants.EAST: {
					if (flipX) {
						l.setSide(SwingConstants.WEST);
					}					
					break;
				}
				
				case SwingConstants.WEST: {
					if (flipX) {
						l.setSide(SwingConstants.EAST);
					}					
					break;
				}
								
				case SwingConstants.NORTH_WEST: {
					if (flipX && flipY) {
						l.setSide(SwingConstants.SOUTH_EAST);
					} else if (flipX) {
						l.setSide(SwingConstants.NORTH_EAST);
					} else if (flipY) {
						l.setSide(SwingConstants.SOUTH_WEST);
					}
					
					break;
				}
				
				case SwingConstants.SOUTH_WEST: {
					if (flipX && flipY) {
						l.setSide(SwingConstants.NORTH_EAST);
					} else if (flipX) {
						l.setSide(SwingConstants.SOUTH_EAST);
					} else if (flipY) {
						l.setSide(SwingConstants.NORTH_WEST);
					}
					break;
				}
				
				case SwingConstants.NORTH_EAST: {
					if (flipX && flipY) {
						l.setSide(SwingConstants.SOUTH_WEST);
					} else if (flipX) {
						l.setSide(SwingConstants.NORTH_WEST);
					} else if (flipY) {
						l.setSide(SwingConstants.SOUTH_EAST);
					}
					break;
				}
				
				case SwingConstants.SOUTH_EAST: {
					if (flipX && flipY) {
						l.setSide(SwingConstants.NORTH_WEST);
					} else if (flipX) {
						l.setSide(SwingConstants.SOUTH_WEST);
					} else if (flipY) {
						l.setSide(SwingConstants.NORTH_EAST);
					}
					break;
				}
			}
		}
		
		// reset locator to update layout
		setLocator(l);
	}
	
	public Cursor getCursorFor(int side) {
		switch (side) {
			case SwingConstants.NORTH:
				return new Cursor(Cursor.N_RESIZE_CURSOR);

			case SwingConstants.SOUTH:
				return new Cursor(Cursor.S_RESIZE_CURSOR);
				
			case SwingConstants.EAST:
				return new Cursor(Cursor.E_RESIZE_CURSOR);
				
			case SwingConstants.WEST:
				return new Cursor(Cursor.W_RESIZE_CURSOR);
				
			case SwingConstants.NORTH_WEST:
				return new Cursor(Cursor.NW_RESIZE_CURSOR);
				
			case SwingConstants.SOUTH_WEST:
				return new Cursor(Cursor.SW_RESIZE_CURSOR);
				
			case SwingConstants.NORTH_EAST:
				return new Cursor(Cursor.NE_RESIZE_CURSOR);
				
			case SwingConstants.SOUTH_EAST:
				return new Cursor(Cursor.SE_RESIZE_CURSOR);
		}
		return null;
	}
}
