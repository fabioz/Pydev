/* 
 * Copyright (C) 2002-@year@ by University of Maryland, College Park, MD 20742, USA 
 * All rights reserved. 
 * 
 * Piccolo was written at the Human-Computer Interaction Laboratory 
 * www.cs.umd.edu/hcil by Jesse Grosjean under the supervision of Ben Bederson. 
 * The Piccolo website is www.cs.umd.edu/hcil/piccolo 
 */
package edu.umd.cs.piccolox.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.plaf.ScrollPaneUI;

/**
 * A simple extension to a standard scroll pane that uses the jazz version
 * of the viewport by default.	Also uses the jazz version of ScrollPaneLayout
 *
 * @author Lance Good
 */
public class PScrollPane extends JScrollPane {

	// A reusable null action
	protected PNullAction nullAction = null;

	// Are key actions disabled on this component?
	protected boolean disableKeyActions = false;

	/**
	 * Pass on the constructor info to the super
	 */
	public PScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
		super(view, vsbPolicy, hsbPolicy);

		// Set the layout and sync it with the scroll pane
		PScrollPaneLayout layout = new PScrollPaneLayout.UIResource();
		setLayout(layout);
		layout.syncWithScrollPane(this);
	}

	/**
	 * Pass on the constructor info to the super
	 */
	public PScrollPane(Component view) {
		this(view, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	/**
	 * Pass on the constructor info to the super
	 */
	public PScrollPane(int vsbPolicy, int hsbPolicy) {
		this(null, vsbPolicy, hsbPolicy);
	}

	/**
	 * Pass on the constructor info to the super
	 */
	public PScrollPane() {
		this(null, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	/**
	 * Disable or enable key actions on this PScrollPane
	 * @param disable true disables key actions, false enables key actions
	 */
	public void setKeyActionsDisabled(boolean disable) {
		if (disable && this.disableKeyActions != disable) {
			this.disableKeyActions = disable;
			disableKeyActions();
		}
		else if (!disable && this.disableKeyActions != disable) {
			this.disableKeyActions = disable;
			installCustomKeyActions();
		}
	}

	/**
	 * Sets the UI
	 */
	public void setUI(ScrollPaneUI ui) {
		super.setUI(ui);

		if (!disableKeyActions) {
			installCustomKeyActions();
		}
		else {
			disableKeyActions();
		}
	}

	/**
	 * Install custom key actions (in place of the Swing defaults) to
	 * correctly scroll the view
	 */
	protected void installCustomKeyActions() {
		ActionMap map = getActionMap();

		map.put("scrollUp", new PScrollAction("scrollUp", SwingConstants.VERTICAL, -1, true));
		map.put("scrollDown", new PScrollAction("scrollDown", SwingConstants.VERTICAL, 1, true));
		map.put("scrollLeft", new PScrollAction("scrollLeft", SwingConstants.HORIZONTAL, -1, true));

		map.put("scrollRight", new PScrollAction("ScrollRight", SwingConstants.HORIZONTAL, 1, true));
		map.put("unitScrollRight", new PScrollAction("UnitScrollRight", SwingConstants.HORIZONTAL, 1, false));
		map.put("unitScrollLeft", new PScrollAction("UnitScrollLeft", SwingConstants.HORIZONTAL, -1, false));
		map.put("unitScrollUp", new PScrollAction("UnitScrollUp", SwingConstants.VERTICAL, -1, false));
		map.put("unitScrollDown", new PScrollAction("UnitScrollDown", SwingConstants.VERTICAL, 1, false));

		map.put("scrollEnd", new PScrollEndAction("ScrollEnd"));
		map.put("scrollHome", new PScrollHomeAction("ScrollHome"));
	}

	/**
	 * Disables key actions on this PScrollPane
	 */
	protected void disableKeyActions() {
		ActionMap map = getActionMap();

		if (nullAction == null) {
			nullAction = new PNullAction();
		}

		map.put("scrollUp", nullAction);
		map.put("scrollDown", nullAction);
		map.put("scrollLeft", nullAction);
		map.put("scrollRight", nullAction);
		map.put("unitScrollRight", nullAction);
		map.put("unitScrollLeft", nullAction);
		map.put("unitScrollUp", nullAction);
		map.put("unitScrollDown", nullAction);
		map.put("scrollEnd", nullAction);
		map.put("scrollHome", nullAction);
	}

	/**
	 * Overridden to create the Jazz viewport
	 * @return The jazz version of the viewport
	 */
	protected JViewport createViewport() {
		return new PViewport();
	}

	/**
	 * Action to scroll left/right/up/down.
	 * Modified from javax.swing.plaf.basic.BasicScrollPaneUI.ScrollAction
	 *
	 * Gets the view parameters (position and size) from the Viewport
	 * rather than directly from the view - also only performs its actions
	 * when the relevant scrollbar is visible
	 */
	protected static class PScrollAction extends AbstractAction {
		/** Direction to scroll. */
		protected int orientation;
		/** 1 indicates scroll down, -1 up. */
		protected int direction;
		/** True indicates a block scroll, otherwise a unit scroll. */
		private boolean block;

		protected PScrollAction(String name, int orientation, int direction, boolean block) {
			super(name);
			this.orientation = orientation;
			this.direction = direction;
			this.block = block;
		}

		public void actionPerformed(ActionEvent e) {
			JScrollPane scrollpane = (JScrollPane) e.getSource();
			// LEG: Modification to only perform these actions if the relevant
			// scrollbar is actually showing
			if ((orientation == SwingConstants.VERTICAL && scrollpane.getVerticalScrollBar().isShowing())
				|| (orientation == SwingConstants.HORIZONTAL && scrollpane.getHorizontalScrollBar().isShowing())) {

				JViewport vp = scrollpane.getViewport();
				Component view;
				if (vp != null && (view = vp.getView()) != null) {
					Rectangle visRect = vp.getViewRect();
					// LEG: Modification to query the viewport for the
					// view size rather than going directly to the view
					Dimension vSize = vp.getViewSize();
					int amount;

					if (view instanceof Scrollable) {
						if (block) {
							amount = ((Scrollable) view).getScrollableBlockIncrement(visRect, orientation, direction);
						}
						else {
							amount = ((Scrollable) view).getScrollableUnitIncrement(visRect, orientation, direction);
						}
					}
					else {
						if (block) {
							if (orientation == SwingConstants.VERTICAL) {
								amount = visRect.height;
							}
							else {
								amount = visRect.width;
							}
						}
						else {
							amount = 10;
						}
					}
					if (orientation == SwingConstants.VERTICAL) {
						visRect.y += (amount * direction);
						if ((visRect.y + visRect.height) > vSize.height) {
							visRect.y = Math.max(0, vSize.height - visRect.height);
						}
						else if (visRect.y < 0) {
							visRect.y = 0;
						}
					}
					else {
						visRect.x += (amount * direction);
						if ((visRect.x + visRect.width) > vSize.width) {
							visRect.x = Math.max(0, vSize.width - visRect.width);
						}
						else if (visRect.x < 0) {
							visRect.x = 0;
						}
					}
					vp.setViewPosition(visRect.getLocation());
				}
			}
		}
	}

	/**
	 * Action to scroll to x,y location of 0,0.
	 * Modified from javax.swing.plaf.basic.BasicScrollPaneUI.ScrollEndAction
	 *
	 * Only performs the event if a scrollbar is visible
	 */
	private static class PScrollHomeAction extends AbstractAction {
		protected PScrollHomeAction(String name) {
			super(name);
		}

		public void actionPerformed(ActionEvent e) {
			JScrollPane scrollpane = (JScrollPane) e.getSource();
			// LEG: Modification to only perform these actions if one of the
			// scrollbars is actually showing
			if (scrollpane.getVerticalScrollBar().isShowing() || scrollpane.getHorizontalScrollBar().isShowing()) {
				JViewport vp = scrollpane.getViewport();
				if (vp != null && vp.getView() != null) {
					vp.setViewPosition(new Point(0, 0));
				}
			}
		}
	}

	/**
	 * Action to scroll to last visible location.
	 * Modified from javax.swing.plaf.basic.BasicScrollPaneUI.ScrollEndAction
	 *
	 * Gets the view size from the viewport rather than directly from the view
	 * - also only performs the event if a scrollbar is visible
	 */
	protected static class PScrollEndAction extends AbstractAction {
		protected PScrollEndAction(String name) {
			super(name);
		}

		public void actionPerformed(ActionEvent e) {
			JScrollPane scrollpane = (JScrollPane) e.getSource();
			// LEG: Modification to only perform these actions if one of the
			// scrollbars is actually showing
			if (scrollpane.getVerticalScrollBar().isShowing() || scrollpane.getHorizontalScrollBar().isShowing()) {

				JViewport vp = scrollpane.getViewport();
				if (vp != null && vp.getView() != null) {

					Rectangle visRect = vp.getViewRect();
					// LEG: Modification to query the viewport for the
					// view size rather than going directly to the view
					Dimension size = vp.getViewSize();
					vp.setViewPosition(new Point(size.width - visRect.width, size.height - visRect.height));
				}
			}
		}
	}

	/**
	 * An action to do nothing - put into an action map to keep it
	 * from looking to its parent
	 */
	protected static class PNullAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
		}
	}
}
