/* 
 * Copyright (C) 2002-@year@ by University of Maryland, College Park, MD 20742, USA 
 * All rights reserved. 
 * 
 * Piccolo was written at the Human-Computer Interaction Laboratory 
 * www.cs.umd.edu/hcil by Jesse Grosjean under the supervision of Ben Bederson. 
 * The Piccolo website is www.cs.umd.edu/hcil/piccolo 
 */
package edu.umd.cs.piccolox.swing;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneLayout;
import javax.swing.border.Border;

import edu.umd.cs.piccolo.util.PBounds;

/**
 * A subclass of ScrollPaneLayout that looks at the Viewport for sizing
 * information rather than View.  Also queries the Viewport for sizing
 * information after each decision about scrollbar visiblity
 *
 * @author Lance Good
 */
public class PScrollPaneLayout extends ScrollPaneLayout {

	/** 
	 * MODIFIED FROM javax.swing.ScrollPaneLayout.layoutContainer
	 *
	 * This is largely the same as ScrollPaneLayout.layoutContainer but
	 * obtains the preferred view size from the viewport rather than directly
	 * from the view so the viewport can get the preferred size from the
	 * PScrollDirector
	 * @param parent the Container to lay out
	 */
	public void layoutContainer(Container parent) {
		/* Sync the (now obsolete) policy fields with the
		 * JScrollPane.
		 */
		JScrollPane scrollPane = (JScrollPane) parent;
		vsbPolicy = scrollPane.getVerticalScrollBarPolicy();
		hsbPolicy = scrollPane.getHorizontalScrollBarPolicy();

		Rectangle availR = scrollPane.getBounds();
		availR.x = availR.y = 0;

		Insets insets = parent.getInsets();
		availR.x = insets.left;
		availR.y = insets.top;
		availR.width -= insets.left + insets.right;
		availR.height -= insets.top + insets.bottom;

		/* Get the scrollPane's orientation.
		 */
		boolean leftToRight = scrollPane.getComponentOrientation().isLeftToRight();

		/* If there's a visible column header remove the space it 
		 * needs from the top of availR.  The column header is treated 
		 * as if it were fixed height, arbitrary width.
		 */

		Rectangle colHeadR = new Rectangle(0, availR.y, 0, 0);

		if ((colHead != null) && (colHead.isVisible())) {
			int colHeadHeight = colHead.getPreferredSize().height;
			colHeadR.height = colHeadHeight;
			availR.y += colHeadHeight;
			availR.height -= colHeadHeight;
		}

		/* If there's a visible row header remove the space it needs
		 * from the left or right of availR.  The row header is treated 
		 * as if it were fixed width, arbitrary height.
		 */

		Rectangle rowHeadR = new Rectangle(0, 0, 0, 0);

		if ((rowHead != null) && (rowHead.isVisible())) {
			int rowHeadWidth = rowHead.getPreferredSize().width;
			rowHeadR.width = rowHeadWidth;
			availR.width -= rowHeadWidth;
			if (leftToRight) {
				rowHeadR.x = availR.x;
				availR.x += rowHeadWidth;
			}
			else {
				rowHeadR.x = availR.x + availR.width;
			}
		}

		/* If there's a JScrollPane.viewportBorder, remove the
		 * space it occupies for availR.
		 */

		Border viewportBorder = scrollPane.getViewportBorder();
		Insets vpbInsets;
		if (viewportBorder != null) {
			vpbInsets = viewportBorder.getBorderInsets(parent);
			availR.x += vpbInsets.left;
			availR.y += vpbInsets.top;
			availR.width -= vpbInsets.left + vpbInsets.right;
			availR.height -= vpbInsets.top + vpbInsets.bottom;
		}
		else {
			vpbInsets = new Insets(0, 0, 0, 0);
		}

		/* At this point availR is the space available for the viewport
		 * and scrollbars. rowHeadR is correct except for its height and y
			 * and colHeadR is correct except for its width and x.	Once we're 
		 * through computing the dimensions  of these three parts we can 
		 * go back and set the dimensions of rowHeadR.height, rowHeadR.y,
			 * colHeadR.width, colHeadR.x and the bounds for the corners.
		 * 
			 * We'll decide about putting up scrollbars by comparing the 
			 * viewport views preferred size with the viewports extent
		 * size (generally just its size).	Using the preferredSize is
		 * reasonable because layout proceeds top down - so we expect
		 * the viewport to be layed out next.  And we assume that the
		 * viewports layout manager will give the view it's preferred
		 * size.  
		 */

		Dimension extentSize = (viewport != null) ? viewport.toViewCoordinates(availR.getSize()) : new Dimension(0, 0);

		PBounds cameraBounds = new PBounds(0, 0, extentSize.getWidth(), extentSize.getHeight());

		// LEG: Modification to ask the viewport for the view size rather
		// than asking the view directly
		Dimension viewPrefSize = (viewport != null) ? ((PViewport) viewport).getViewSize(cameraBounds) : new Dimension(0, 0);

		/* If there's a vertical scrollbar and we need one, allocate
		 * space for it (we'll make it visible later). A vertical 
		 * scrollbar is considered to be fixed width, arbitrary height.
		 */

		Rectangle vsbR = new Rectangle(0, availR.y - vpbInsets.top, 0, 0);

		boolean vsbNeeded;
		if (vsbPolicy == VERTICAL_SCROLLBAR_ALWAYS) {
			vsbNeeded = true;
		}
		else if (vsbPolicy == VERTICAL_SCROLLBAR_NEVER) {
			vsbNeeded = false;
		}
		else { // vsbPolicy == VERTICAL_SCROLLBAR_AS_NEEDED

			vsbNeeded = (viewPrefSize.height > extentSize.height);
		}

		if ((vsb != null) && vsbNeeded) {
			adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
			extentSize = viewport.toViewCoordinates(availR.getSize());

			// LEG: Modification because the view's preferred size needs to
			// be recomputed because the extent may have changed
			cameraBounds.setRect(0, 0, extentSize.getWidth(), extentSize.getHeight());
			viewPrefSize = ((PViewport) viewport).getViewSize(cameraBounds);
		}

		/* If there's a horizontal scrollbar and we need one, allocate
		 * space for it (we'll make it visible later). A horizontal 
		 * scrollbar is considered to be fixed height, arbitrary width.
		 */

		Rectangle hsbR = new Rectangle(availR.x - vpbInsets.left, 0, 0, 0);
		boolean hsbNeeded;
		if (hsbPolicy == HORIZONTAL_SCROLLBAR_ALWAYS) {
			hsbNeeded = true;
		}
		else if (hsbPolicy == HORIZONTAL_SCROLLBAR_NEVER) {
			hsbNeeded = false;
		}
		else { // hsbPolicy == HORIZONTAL_SCROLLBAR_AS_NEEDED
			hsbNeeded = (viewPrefSize.width > extentSize.width);
		}

		if ((hsb != null) && hsbNeeded) {
			adjustForHSB(true, availR, hsbR, vpbInsets);

			/* If we added the horizontal scrollbar then we've implicitly 
			 * reduced	the vertical space available to the viewport. 
			 * As a consequence we may have to add the vertical scrollbar, 
			 * if that hasn't been done so already.  Ofcourse we
			 * don't bother with any of this if the vsbPolicy is NEVER. 	 
			 */

			if ((vsb != null) && !vsbNeeded && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {

				extentSize = viewport.toViewCoordinates(availR.getSize());

				// LEG: Modification because the view's preferred size needs to
				// be recomputed because the extent may have changed
				cameraBounds.setRect(0, 0, extentSize.getWidth(), extentSize.getHeight());
				viewPrefSize = ((PViewport) viewport).getViewSize(cameraBounds);

				vsbNeeded = viewPrefSize.height > extentSize.height;

				if (vsbNeeded) {
					adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
				}
			}
		}

		/* Set the size of the viewport first, and then recheck the Scrollable
		 * methods. Some components base their return values for the Scrollable
		 * methods on the size of the Viewport, so that if we don't
		 * ask after resetting the bounds we may have gotten the wrong
		 * answer.
		 */

		if (viewport != null) {
			viewport.setBounds(availR);
		}

		/* We now have the final size of the viewport: availR.
		 * Now fixup the header and scrollbar widths/heights.
		 */
		vsbR.height = availR.height + vpbInsets.top + vpbInsets.bottom;
		hsbR.width = availR.width + vpbInsets.left + vpbInsets.right;
		rowHeadR.height = availR.height + vpbInsets.top + vpbInsets.bottom;
		rowHeadR.y = availR.y - vpbInsets.top;
		colHeadR.width = availR.width + vpbInsets.left + vpbInsets.right;
		colHeadR.x = availR.x - vpbInsets.left;

		/* Set the bounds of the remaining components.	The scrollbars
		 * are made invisible if they're not needed.
		 */

		if (rowHead != null) {
			rowHead.setBounds(rowHeadR);
		}

		if (colHead != null) {
			colHead.setBounds(colHeadR);
		}

		if (vsb != null) {
			if (vsbNeeded) {
				vsb.setVisible(true);
				vsb.setBounds(vsbR);
			}
			else {
				vsb.setVisible(false);
			}
		}

		if (hsb != null) {
			if (hsbNeeded) {
				hsb.setVisible(true);
				hsb.setBounds(hsbR);
			}
			else {
				hsb.setVisible(false);
			}
		}

		if (lowerLeft != null) {
			lowerLeft.setBounds(leftToRight ? rowHeadR.x : vsbR.x, hsbR.y, leftToRight ? rowHeadR.width : vsbR.width, hsbR.height);
		}

		if (lowerRight != null) {
			lowerRight.setBounds(leftToRight ? vsbR.x : rowHeadR.x, hsbR.y, leftToRight ? vsbR.width : rowHeadR.width, hsbR.height);
		}

		if (upperLeft != null) {
			upperLeft.setBounds(leftToRight ? rowHeadR.x : vsbR.x, colHeadR.y, leftToRight ? rowHeadR.width : vsbR.width, colHeadR.height);
		}

		if (upperRight != null) {
			upperRight.setBounds(leftToRight ? vsbR.x : rowHeadR.x, colHeadR.y, leftToRight ? vsbR.width : rowHeadR.width, colHeadR.height);
		}
	}

	/**
	 * Copied FROM javax.swing.ScrollPaneLayout.adjustForVSB
	 *
	 * This method is called from ScrollPaneLayout.layoutContainer and is
	 * private in ScrollPaneLayout so it was copied here
	 */
	protected void adjustForVSB(boolean wantsVSB, Rectangle available, Rectangle vsbR, Insets vpbInsets, boolean leftToRight) {
		int vsbWidth = vsb.getPreferredSize().width;
		if (wantsVSB) {
			available.width -= vsbWidth;
			vsbR.width = vsbWidth;

			if (leftToRight) {
				vsbR.x = available.x + available.width + vpbInsets.right;
			}
			else {
				vsbR.x = available.x - vpbInsets.left;
				available.x += vsbWidth;
			}
		}
		else {
			available.width += vsbWidth;
		}
	}

	/**
	 * Copied FROM javax.swing.ScrollPaneLayout.adjustForHSB
	 *
	 * This method is called from ScrollPaneLayout.layoutContainer and is
	 * private in ScrollPaneLayout so it was copied here
	 */
	protected void adjustForHSB(boolean wantsHSB, Rectangle available, Rectangle hsbR, Insets vpbInsets) {
		int hsbHeight = hsb.getPreferredSize().height;
		if (wantsHSB) {
			available.height -= hsbHeight;
			hsbR.y = available.y + available.height + vpbInsets.bottom;
			hsbR.height = hsbHeight;
		}
		else {
			available.height += hsbHeight;
		}
	}

	/**
	 * The UI resource version of PScrollPaneLayout.  It isn't clear why
	 * Swing does this in ScrollPaneLayout but we'll do it here too just
	 * to be safe.
	 */
	public static class UIResource extends PScrollPaneLayout implements javax.swing.plaf.UIResource {
	}
}
