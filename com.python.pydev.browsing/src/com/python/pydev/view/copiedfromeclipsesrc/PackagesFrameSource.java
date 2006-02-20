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

import org.eclipse.ui.views.framelist.TreeFrame;
import org.eclipse.ui.views.framelist.TreeViewerFrameSource;

import com.python.pydev.browsing.view.PydevPackageExplorer;

public class PackagesFrameSource extends TreeViewerFrameSource   {
	private PydevPackageExplorer fPackagesExplorer;
	
	public PackagesFrameSource(PydevPackageExplorer explorer) {
		super(explorer.getViewer());
		fPackagesExplorer= explorer;
	}

	protected TreeFrame createFrame(Object input) {
		TreeFrame frame = super.createFrame(input);
		frame.setName(fPackagesExplorer.getFrameName(input));
		frame.setToolTipText(fPackagesExplorer.getToolTipText(input));
		return frame;
	}

	/**
	 * Also updates the title of the packages explorer
	 */
	protected void frameChanged(TreeFrame frame) {
		super.frameChanged(frame);
		fPackagesExplorer.updateTitle();
	}

}
