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
package edu.umd.cs.piccolox.event;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PDragSequenceEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolo.util.PDimension;
import edu.umd.cs.piccolo.util.PNodeFilter;
import edu.umd.cs.piccolox.handles.PBoundsHandle;

/**
 * <code>PSelectionEventHandler</code> provides standard interaction for selection.  Clicking
 * selects the object under the cursor.  Shift-clicking allows multiple objects to be
 * selected.  Dragging offers marquee selection.  Pressing the delete key deletes
 * the selection by default.
 * @version 1.0
 * @author Ben Bederson
 */ 
public class PSelectionEventHandler extends PDragSequenceEventHandler {

	public static final String SELECTION_CHANGED_NOTIFICATION = "SELECTION_CHANGED_NOTIFICATION";

	final static int DASH_WIDTH = 5;
	final static int NUM_STROKES = 10;
	
	private HashMap selection = null; 		// The current selection
	private List selectableParents = null;  // List of nodes whose children can be selected
	private PPath marquee = null;
	private PNode marqueeParent = null; 	 // Node that marquee is added to as a child
	private Point2D presspt = null;
	private Point2D canvasPressPt = null;
	private float strokeNum = 0;
	private Stroke[] strokes = null;
	private HashMap allItems = null;		// Used within drag handler temporarily
	private ArrayList unselectList = null;	// Used within drag handler temporarily
	private HashMap marqueeMap = null;
	private PNode pressNode = null; 		// Node pressed on (or null if none)
	private boolean deleteKeyActive = true; // True if DELETE key should delete selection
	private Paint marqueePaint;
	private float marqueePaintTransparency = 1.0f;
	
	/**
	 * Creates a selection event handler.
	 * @param marqueeParent The node to which the event handler dynamically adds a marquee
	 * (temporarily) to represent the area being selected.
	 * @param selectableParent The node whose children will be selected
	 * by this event handler.
	 */
	public PSelectionEventHandler(PNode marqueeParent, PNode selectableParent) {
		this.marqueeParent = marqueeParent;
		this.selectableParents = new ArrayList();
		this.selectableParents.add(selectableParent);
		init();
	}

	/**
	 * Creates a selection event handler.
	 * @param marqueeParent The node to which the event handler dynamically adds a marquee
	 * (temporarily) to represent the area being selected.
	 * @param selectableParents A list of nodes whose children will be selected
	 * by this event handler.
	 */
	public PSelectionEventHandler(PNode marqueeParent, List selectableParents) {
		this.marqueeParent = marqueeParent;
		this.selectableParents = selectableParents;
		init();
	}

	protected void init() {
		float[] dash = { DASH_WIDTH, DASH_WIDTH };
		strokes = new Stroke[NUM_STROKES];
		for (int i = 0; i < NUM_STROKES; i++) {
			strokes[i] = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, dash, i);
		}
		
		selection = new HashMap();
		allItems = new HashMap();
		unselectList = new ArrayList();
		marqueeMap = new HashMap();
	}

	///////////////////////////////////////////////////////
	// Public static methods for manipulating the selection
	///////////////////////////////////////////////////////
		
	public void select(Collection items) {
		boolean changes = false;
		Iterator itemIt = items.iterator();
		while (itemIt.hasNext()) {
			PNode node = (PNode)itemIt.next();
			changes |= internalSelect(node);
		}
		if (changes) {
			postSelectionChanged();
		}
	}

	public void select(Map items) {
		select( items.keySet() );
	}

	private boolean internalSelect( PNode node ) {
		if (isSelected(node)) {
			return false;
		}

		selection.put(node, Boolean.TRUE);
		decorateSelectedNode(node);
		return true;
	}

	private void postSelectionChanged()
	{
		PNotificationCenter.defaultCenter().postNotification(SELECTION_CHANGED_NOTIFICATION, this);
	}

	public void select(PNode node) {
		if (internalSelect(node)) {
			postSelectionChanged();
		}
	}

	public void decorateSelectedNode(PNode node) {
		PBoundsHandle.addBoundsHandlesTo(node);
	}
		
	public void unselect(Collection items) {
		boolean changes = false;
		Iterator itemIt = items.iterator();
		while (itemIt.hasNext()) {
			PNode node = (PNode)itemIt.next();
			changes |= internalUnselect(node);
		}
		if (changes) {
			postSelectionChanged();
		}
	}

	private boolean internalUnselect( PNode node ) {
		if (!isSelected(node)) {
			return false;
		}

		undecorateSelectedNode(node);
		selection.remove(node);
		return true;
	}

	public void unselect(PNode node) {
		if( internalUnselect(node) ) {
			postSelectionChanged();
		}
	}

	public void undecorateSelectedNode(PNode node) {
		PBoundsHandle.removeBoundsHandlesFrom(node);
	}

	public void unselectAll() {
		//  Because unselect() removes from selection, we need to
		//  take a copy of it first so it isn't changed while we're iterating
		ArrayList sel = new ArrayList(selection.keySet());
		unselect( sel );
	}

	public boolean isSelected(PNode node) {
		if ((node != null) && (selection.containsKey(node))) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns a copy of the currently selected nodes.
	 */
	public Collection getSelection() {
		ArrayList sel = new ArrayList(selection.keySet());
		return sel;
	}

	/**
	 * Gets a reference to the currently selected nodes.  You should not modify or store
	 * this collection.
	 */
	public Collection getSelectionReference()
	{
		return Collections.unmodifiableCollection( selection.keySet() );
	}

	/**
	 * Determine if the specified node is selectable (i.e., if it is a child
	 * of the one the list of selectable parents.
	 */
	protected boolean isSelectable(PNode node) {
		boolean selectable = false;

		Iterator parentsIt = selectableParents.iterator();
		while (parentsIt.hasNext()) {
			PNode parent = (PNode)parentsIt.next();
			if (parent.getChildrenReference().contains(node)) {
				selectable = true;
				break;
			}
			else if (parent instanceof PCamera) {
				for(int i=0; i<((PCamera)parent).getLayerCount(); i++) {
					PLayer layer = ((PCamera)parent).getLayer(i);	
					if (layer.getChildrenReference().contains(node)) {
						selectable = true;
						break;	
					}
				}
			}
		}
		
		return selectable;
	}

	//////////////////////////////////////////////////////
	// Methods for modifying the set of selectable parents
	//////////////////////////////////////////////////////

	public void addSelectableParent(PNode node) {
		selectableParents.add(node);	
	}
	
	public void removeSelectableParent(PNode node) {		
		selectableParents.remove(node); 
	}
	
	public void setSelectableParent(PNode node) {
		selectableParents.clear();
		selectableParents.add(node);	
	}
	
	public void setSelectableParents(Collection c) {
		selectableParents.clear();
		selectableParents.addAll(c);	
	}

	public Collection getSelectableParents() {
		return new ArrayList(selectableParents);
	}

	////////////////////////////////////////////////////////
	// The overridden methods from PDragSequenceEventHandler
	////////////////////////////////////////////////////////
	
	protected void startDrag(PInputEvent e) {
		super.startDrag(e);

		initializeSelection(e); 			

		if (isMarqueeSelection(e)) {
			initializeMarquee(e);

			if (!isOptionSelection(e)) {
				startMarqueeSelection(e);
			}
			else {
				startOptionMarqueeSelection(e);
			}
		}
		else {					
			if (!isOptionSelection(e)) {
				startStandardSelection(e);
			} else {
				startStandardOptionSelection(e);
			}
		}
	}

	protected void drag(PInputEvent e) {
		super.drag(e);

		if (isMarqueeSelection(e)) {
			updateMarquee(e);	

			if (!isOptionSelection(e)) {
				computeMarqueeSelection(e);
			}
			else {
				computeOptionMarqueeSelection(e);
			}
		} else {
			dragStandardSelection(e);
		}
	}

	protected void endDrag(PInputEvent e) {
		super.endDrag(e);

		if (isMarqueeSelection(e)) {
			endMarqueeSelection(e); 
		}
		else {
			endStandardSelection(e);
		}		
	}

	////////////////////////////
	// Additional methods
	////////////////////////////

	public boolean isOptionSelection(PInputEvent pie) {
		return pie.isShiftDown();	
	}

	protected boolean isMarqueeSelection(PInputEvent pie) {
		return (pressNode == null); 
	}

	protected void initializeSelection(PInputEvent pie) {
		canvasPressPt = pie.getCanvasPosition();
		presspt = pie.getPosition();
		pressNode = pie.getPath().getPickedNode();
		if (pressNode instanceof PCamera) {
			pressNode = null;
		}		
	}

	protected void initializeMarquee(PInputEvent e) {
		marquee = PPath.createRectangle((float)presspt.getX(), (float)presspt.getY(), 0, 0);
		marquee.setPaint(marqueePaint);
		marquee.setTransparency(marqueePaintTransparency);
		marquee.setStrokePaint(Color.black);
		marquee.setStroke(strokes[0]);
		marqueeParent.addChild(marquee);			

		marqueeMap.clear();
	}

	protected void startOptionMarqueeSelection(PInputEvent e) { 
	}

	protected void startMarqueeSelection(PInputEvent e) {
		unselectAll();
	}
	
	protected void startStandardSelection(PInputEvent pie) {
							// Option indicator not down - clear selection, and start fresh
		if (!isSelected(pressNode)) {
			unselectAll();
			
			if (isSelectable(pressNode)) {
				select(pressNode);
			}
		}		
	}

	protected void startStandardOptionSelection(PInputEvent pie) {
							// Option indicator is down, toggle selection
		if (isSelectable(pressNode)) {
			if (isSelected(pressNode)) {
				unselect(pressNode);
			} else {
				select(pressNode);
			}
		}		
	}

	protected void updateMarquee(PInputEvent pie) {
		PBounds b = new PBounds();

		if (marqueeParent instanceof PCamera) {
			b.add(canvasPressPt);
			b.add(pie.getCanvasPosition());
		}
		else {
			b.add(presspt);
			b.add(pie.getPosition());
		}

		marquee.globalToLocal(b);
		marquee.setPathToRectangle((float) b.x, (float) b.y, (float) b.width, (float) b.height);				
		b.reset();
		b.add(presspt);
		b.add(pie.getPosition());

		allItems.clear();
		PNodeFilter filter = createNodeFilter(b);
		Iterator parentsIt = selectableParents.iterator();
		while (parentsIt.hasNext()) {
			PNode parent = (PNode) parentsIt.next();
			
			Collection items;
			if (parent instanceof PCamera) {
				items = new ArrayList();
				for(int i=0; i<((PCamera)parent).getLayerCount(); i++) {
					((PCamera)parent).getLayer(i).getAllNodes(filter,items);	
				}
			}
			else {
				items = parent.getAllNodes(filter, null);
			}
			
			Iterator itemsIt = items.iterator();
			while (itemsIt.hasNext()) {
				allItems.put(itemsIt.next(), Boolean.TRUE);
			}
		}
	}

	protected void computeMarqueeSelection(PInputEvent pie) {
		unselectList.clear();
		// Make just the items in the list selected
		// Do this efficiently by first unselecting things not in the list
		Iterator selectionEn = selection.keySet().iterator();
		while (selectionEn.hasNext()) {
			PNode node = (PNode) selectionEn.next();
			if (!allItems.containsKey(node)) {
				unselectList.add(node);
			}
		}
		unselect(unselectList);
		
		// Then select the rest
		selectionEn = allItems.keySet().iterator();
		while (selectionEn.hasNext()) {
			PNode node = (PNode) selectionEn.next();
			if (!selection.containsKey(node) && !marqueeMap.containsKey(node) && isSelectable(node)) {
				marqueeMap.put(node,Boolean.TRUE);
			}
			else if (!isSelectable(node)) {
				selectionEn.remove();
			}
		}
		
		select(allItems);		
	}

	protected void computeOptionMarqueeSelection(PInputEvent pie) {
		unselectList.clear();
		Iterator selectionEn = selection.keySet().iterator();
		while (selectionEn.hasNext()) {
			PNode node = (PNode) selectionEn.next();
			if (!allItems.containsKey(node) && marqueeMap.containsKey(node)) {
				marqueeMap.remove(node);
				unselectList.add(node);
			}
		}
		unselect(unselectList);
		

		// Then select the rest
		selectionEn = allItems.keySet().iterator();
		while (selectionEn.hasNext()) {
			PNode node = (PNode) selectionEn.next();
			if (!selection.containsKey(node) && !marqueeMap.containsKey(node) && isSelectable(node)) {
				marqueeMap.put(node,Boolean.TRUE);
			}
			else if (!isSelectable(node)) {
				selectionEn.remove();
			}
		}

		select(allItems);	
	}

	protected PNodeFilter createNodeFilter(PBounds bounds) {
		return new BoundsFilter(bounds);	
	}

	protected PBounds getMarqueeBounds() {
		if (marquee != null) {
			return marquee.getBounds();
		}	
		return new PBounds();
	}

	protected void dragStandardSelection(PInputEvent e) {
		// There was a press node, so drag selection
	    PDimension d = e.getCanvasDelta();
	    e.getTopCamera().localToView(d);

	    PDimension gDist = new PDimension();
		Iterator selectionEn = getSelection().iterator();
		while (selectionEn.hasNext()) {
			PNode node = (PNode) selectionEn.next();

			gDist.setSize(d);
			node.getParent().globalToLocal(gDist);
			node.offset(gDist.getWidth(), gDist.getHeight());
		}		
	}

	protected void endMarqueeSelection(PInputEvent e) {
		// Remove marquee
		marquee.removeFromParent();
		marquee = null; 		
	}

	protected void endStandardSelection(PInputEvent e) {
		pressNode = null;		
	}

	/**
	 * This gets called continuously during the drag, and is used to animate the marquee
	 */
	protected void dragActivityStep(PInputEvent aEvent) {
		if (marquee != null) {
			float origStrokeNum = strokeNum;
			strokeNum = (strokeNum + 0.5f) % NUM_STROKES;	// Increment by partial steps to slow down animation
			if ((int)strokeNum != (int)origStrokeNum) {
				marquee.setStroke(strokes[(int)strokeNum]);
			}
		}
	}

	/**
	 * Delete selection when delete key is pressed (if enabled)
	 */
	public void keyPressed(PInputEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_DELETE:
				if (deleteKeyActive) {
					Iterator selectionEn = selection.keySet().iterator();
					while (selectionEn.hasNext()) {
						PNode node = (PNode) selectionEn.next();
						node.removeFromParent();
					}
					selection.clear();
				}
		}
	}

	public boolean getSupportDeleteKey() {
		return deleteKeyActive;
	}

	public boolean isDeleteKeyActive() {
		return deleteKeyActive;
	}

	/**
	 * Specifies if the DELETE key should delete the selection
	 */
	public void setDeleteKeyActive(boolean deleteKeyActive) {
		this.deleteKeyActive = deleteKeyActive;
	}

	//////////////////////
	// Inner classes
	//////////////////////

	protected class BoundsFilter implements PNodeFilter {
		PBounds localBounds = new PBounds();
		PBounds bounds;
		
		protected BoundsFilter(PBounds bounds) {
			this.bounds = bounds;
		}

		public boolean accept(PNode node) {
			localBounds.setRect(bounds);
			node.globalToLocal(localBounds);
			
			boolean boundsIntersects = node.intersects(localBounds);
			boolean isMarquee = (node == marquee);
			return (node.getPickable() && boundsIntersects && !isMarquee && !selectableParents.contains(node) && !isCameraLayer(node));
		}

		public boolean acceptChildrenOf(PNode node) {
			return selectableParents.contains(node) || isCameraLayer(node);
		}
		
		public boolean isCameraLayer(PNode node) {
			if (node instanceof PLayer) {
				for(Iterator i=selectableParents.iterator(); i.hasNext();) {
					PNode parent = (PNode)i.next();
					if (parent instanceof PCamera) {
						if (((PCamera)parent).indexOfLayer((PLayer)node) != -1) {
							return true;	
						}
					}
				}	
			}
			return false;
		}
	}

	/**
	 * Indicates the color used to paint the marquee.
	 * @return the paint for interior of the marquee
	 */
	public Paint getMarqueePaint() {
		return marqueePaint;
	}

	/**
	 * Sets the color used to paint the marquee.
	 * @param paint the paint color
	 */
	public void setMarqueePaint(Paint paint) {
		this.marqueePaint = paint;
	}

	/**
	 * Indicates the transparency level for the interior of the marquee.
	 * @return Returns the marquee paint transparency, zero to one
	 */
	public float getMarqueePaintTransparency() {
		return marqueePaintTransparency;
	}

	/**
	 * Sets the transparency level for the interior of the marquee.
	 * @param marqueePaintTransparency The marquee paint transparency to set.
	 */
	public void setMarqueePaintTransparency(float marqueePaintTransparency) {
		this.marqueePaintTransparency = marqueePaintTransparency;
	}
}