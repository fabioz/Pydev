/* 
 * Copyright (C) 2002-@year@ by University of Maryland, College Park, MD 20742, USA 
 * All rights reserved. 
 * 
 * Piccolo was written at the Human-Computer Interaction Laboratory 
 * www.cs.umd.edu/hcil by Jesse Grosjean under the supervision of Ben Bederson. 
 * The Piccolo website is www.cs.umd.edu/hcil/piccolo 
 */
package edu.umd.cs.piccolox.swing;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * A subclass of JViewport that talks to the scroll director to negotiate
 * the view positions and sizes.
 *
 * @author Lance Good
 */
public class PViewport extends JViewport {

	/**
	 * Controls what happens when scrolling occurs
	 */
	PScrollDirector scrollDirector;

	/**
	 * Pass constructor info to super
	 */
	public PViewport() {
		super();

		setScrollDirector(createScrollDirector());
	}

	/**
	 * Subclassers can override this to install a different
	 * layout manager (or <code>null</code>) in the constructor.  Returns
	 * a new <code>ViewportLayout</code> object.
	 *
	 * @return a <code>LayoutManager</code>
	 */
	protected LayoutManager createLayoutManager() {
		return new PViewportLayout();
	}

	/**
	 * Subclassers can override this to install a different scroll director
	 * in the constructor.	Returns a new <code>PScrollDirector</code> object.
	 * @return a <code>PScrollDirector
	 */
	protected PScrollDirector createScrollDirector() {
		return new PDefaultScrollDirector();
	}

	/**
	 * Set the scroll director on this viewport
	 * @param scrollDirector The new scroll director
	 */
	public void setScrollDirector(PScrollDirector scrollDirector) {
		if (this.scrollDirector != null) {
			this.scrollDirector.unInstall();
		}
		this.scrollDirector = scrollDirector;
		if (scrollDirector != null) {
			this.scrollDirector.install(this, (PCanvas) getView());
		}
	}

	/**
	 * @return The scroll director on this viewport
	 */
	public PScrollDirector getScrollDirector() {
		return scrollDirector;
	}

	/**
	 * Overridden to throw an exception if the view is not a ZCanvas
	 * @param view The new view - it better be a ZCanvas!
	 */
	public void setView(Component view) {
		if (!(view instanceof PCanvas)) {
			throw new UnsupportedOperationException("PViewport only supports ZCanvas");
		}

		super.setView(view);

		if (scrollDirector != null) {
			scrollDirector.install(this, (PCanvas) view);
		}
	}

	/**
	 * Sets the view coordinates that appear in the upper left
	 * hand corner of the viewport, does nothing if there's no view.
	 *
	 * @param p  a <code>Point</code> object giving the upper left coordinates
	 */
	public void setViewPosition(Point p) {
		if (getView() == null) {
			return;
		}

		double oldX = 0, oldY = 0, x = p.x, y = p.y;

		Point2D vp = getViewPosition();
		if (vp != null) {
			oldX = vp.getX();
			oldY = vp.getY();
		}

		/**
		 * Send the scroll director the exact view position and let it
		 * interpret it as needed
		 */
		double newX = x;
		double newY = y;

		if ((oldX != newX) || (oldY != newY)) {
			scrollUnderway = true;

			scrollDirector.setViewPosition(newX, newY);

			fireStateChanged();
		}
	}

	/**
	 * Gets the view position from the scroll director based on the current
	 * extent size
	 * @return The new view position
	 */
	public Point getViewPosition() {
		if (scrollDirector != null) {
			Dimension extent = getExtentSize();
			return scrollDirector.getViewPosition(new PBounds(0, 0, extent.getWidth(), extent.getHeight()));
		}
		else {
			return null;
		}
	}

	/**
	 * Gets the view size from the scroll director based on the current
	 * extent size
	 * @return The new view size
	 */
	public Dimension getViewSize() {
		Dimension extent = getExtentSize();
		return scrollDirector.getViewSize(new PBounds(0, 0, extent.getWidth(), extent.getHeight()));
	}

	/**
	 * Gets the view size from the scroll director based on the specified
	 * extent size
	 * @param r The extent size from which the view is computed
	 * @return The new view size
	 */
	public Dimension getViewSize(Rectangle2D r) {
		return scrollDirector.getViewSize(r);
	}

	/**
	 * Notifies all <code>ChangeListeners</code> when the views
	 * size, position, or the viewports extent size has changed.
	 */
	public void fireStateChanged() {
		super.fireStateChanged();
	}

	/**
	 * A simple layout manager to give the ZCanvas the same size as the
	 * Viewport
	 */
	public static class PViewportLayout extends ViewportLayout {
		/**
		 * Called when the specified container needs to be laid out.
		 *
		 * @param parent  the container to lay out
		 */
		public void layoutContainer(Container parent) {
			JViewport vp = (JViewport) parent;
			Component view = vp.getView();

			if (view == null) {
				return;
			}

			Dimension extentSize = vp.getSize();

			vp.setViewSize(extentSize);
		}

	}
}
