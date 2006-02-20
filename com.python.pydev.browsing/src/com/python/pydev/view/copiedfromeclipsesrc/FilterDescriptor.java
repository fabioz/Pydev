/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.python.pydev.view.copiedfromeclipsesrc;

import java.text.Collator;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.IPluginContribution;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.view.filters.AbstractViewerFilter;

/**
 * Represents a custom filter which is provided by the
 * "org.eclipse.jdt.ui.javaElementFilters" extension point.
 * 
 * since 2.0
 */
public class FilterDescriptor implements Comparable, IPluginContribution {

	private AbstractViewerFilter filter;	
	
	private static final String  FilterDescriptor_filterCreationError_message = "Filter Description: createViewerFilter";
	
	/**
	 * Creates a new filter descriptor for the given configuration element.
	 */
	public FilterDescriptor( AbstractViewerFilter filter ) {
		this.filter = filter;
	}

	/**
	 * Creates a new <code>ViewerFilter</code>.
	 * This method is only valid for viewer filters.
	 */
	public ViewerFilter createViewerFilter() {
		if (!isCustomFilter())
			return null;
		
		final ViewerFilter[] result= new ViewerFilter[1]; 
		String message= FilterDescriptor_filterCreationError_message;
		ISafeRunnable code= new SafeRunnable(message) {			
			public void run() throws Exception {
				result[0]= (ViewerFilter)filter;
			}			
		};
		Platform.run(code);
		return result[0];
	}
	
	//---- XML Attribute accessors ---------------------------------------------
	
	/**
	 * Returns the filter's id.
	 * <p>
	 * This attribute is mandatory for custom filters.
	 * The ID for pattern filters is
	 * PATTERN_FILTER_ID_PREFIX plus the pattern itself.
	 * </p>
	 */
	public String getId() {		
		return filter.getId();
	}
	
	/**
	 * Returns the filter's name.
	 * <p>
	 * If the name of a pattern filter is missing
	 * then the pattern is used as its name.
	 * </p>
	 */
	public String getName() {
		String name= filter.getName();
		if (name == null && isPatternFilter())
			name= getPattern();
		return name;
	}

	/**
	 * Returns the filter's pattern.
	 * 
	 * @return the pattern string or <code>null</code> if it's not a pattern filter
	 */
	public String getPattern() {
		return null;
	}

	/**
	 * Returns the filter's viewId.
	 * 
	 * @return the view ID or <code>null</code> if the filter is for all views
	 * @since 3.0
	 */
	public String getTargetId() {
		return filter.getViewerId();		
	}

	/**
	 * Returns the filter's description.
	 * 
	 * @return the description or <code>null</code> if no description is provided
	 */
	public String getDescription() {
		String description= filter.getDescription();
		if (description == null)
			description= ""; //$NON-NLS-1$
		return description;
	}

	/**
	 * @return <code>true</code> if this filter is a custom filter.
	 */
	public boolean isPatternFilter() {
		return getPattern() != null;
	}

	/**
	 * @return <code>true</code> if this filter is a pattern filter.
	 */
	public boolean isCustomFilter() {
		return true;
	}

	/**
	 * Returns <code>true</code> if the filter
	 * is initially enabled.
	 * 
	 * This attribute is optional and defaults to <code>true</code>.
	 */
	public boolean isEnabled() {		
		return filter.isInitiallyEnabled();
	}

	/* 
	 * Implements a method from IComparable 
	 */ 
	public int compareTo(Object o) {
		if (o instanceof FilterDescriptor)
			return Collator.getInstance().compare(getName(), ((FilterDescriptor)o).getName());
		else
			return Integer.MIN_VALUE;
	}
		
	public String getLocalId() {
		return filter.getId();
	}

    public String getPluginId() {
        return PydevPlugin.getPluginID();
    }
}
