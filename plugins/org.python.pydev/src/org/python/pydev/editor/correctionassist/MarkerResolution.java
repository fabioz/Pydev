/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 22, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IMarkerResolution;

/**
 * @author Fabio Zadrozny
 */
public class MarkerResolution implements IMarkerResolution {

    private String label;

    /**
     * @param string
     */
    public MarkerResolution(String string) {

        this.label = string;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IMarkerResolution#getLabel()
     */
    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void run(IMarker marker) {
        MessageDialog.openInformation(null, "QuickFix Demo", "This quick-fix is not yet implemented");
    }
}
