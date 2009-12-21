package org.python.pydev.compare;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.viewers.Viewer;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IViewerCreator;


/**
 * Required when creating a PyMergeViewer from the plugin.xml file.
 */
public class PyContentViewerCreator implements IViewerCreator {

	public Viewer createViewer(Composite parent, CompareConfiguration mp) {
		return new PyMergeViewer(parent, SWT.NULL, mp);
	}
}
