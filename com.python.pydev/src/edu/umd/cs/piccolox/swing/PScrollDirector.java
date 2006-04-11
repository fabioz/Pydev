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

import edu.umd.cs.piccolo.PCanvas;

/**
 * The interface an application can implement to control scrolling in a
 * PScrollPane->PViewport->ZCanvas component hierarchy.
 * @see PDefaultScrollDirector
 * @author Lance Good
 */
public interface PScrollDirector {

	/**
	 * Installs the scroll director
	 * @param viewport The viewport on which this director directs
	 * @param view The ZCanvas that the viewport looks at
	 */
	public void install(PViewport viewport, PCanvas view);

	/**
	 * Uninstall the scroll director
	 */
	public void unInstall();

	/**
	 * Get the View position given the specified camera bounds
	 * @param viewBounds The bounds for which the view position will be computed
	 * @return The view position
	 */
	public Point getViewPosition(Rectangle2D viewBounds);


	/**
	 * Set the view position
	 * @param x The new x position
	 * @param y The new y position
	 */
	public void setViewPosition(double x, double y);

	/**
	 * Get the size of the view based on the specified camera bounds
	 * @param viewBounds The view bounds for which the view size will be computed
	 * @return The view size
	 */
	public Dimension getViewSize(Rectangle2D viewBounds);
}
