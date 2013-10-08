/******************************************************************************
* Copyright (C) 2012  Jonah Graham
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.debug.ui.variables;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.debug.model.PyVariable;
import org.python.pydev.debug.model.PyVariableCollection;
import org.python.pydev.debug.model.PyVariablesPreferences;

public class ShowPrivateReferencesActionDelegate extends AbstractShowReferencesActionDelegate {
    @Override
    protected boolean isShowReference() {
        return PyVariablesPreferences.isShowPrivateReferences();
    }

    @Override
    protected void setShowReferences(boolean checked) {
        PyVariablesPreferences.setShowPrivateReferences(checked);
    }

    @Override
    protected boolean isShowReferenceProperty(String property) {
        return PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_PRIVATE_REFERENCES.equals(property);
    }

    @Override
    protected boolean select(Viewer viewer, Object parentElement, PyVariable variable, String variableName) {
        if (variableName != null && variableName.startsWith("_")) {
            // although we want to exclude _ names, don't do that on self and cls 
            if (parentElement instanceof TreePath) {
                TreePath path = (TreePath) parentElement;
                if (path.getSegmentCount() == 1) {
                    Object segment = path.getFirstSegment();
                    if (segment instanceof PyVariableCollection) {
                        PyVariableCollection varCol = (PyVariableCollection) segment;
                        try {
                            String varColName = varCol.getName();
                            if ("self".equals(varColName) || "cls".equals(varColName)) {
                                // Keep this, it is private to "us" so the user probably want to see it
                                return true;
                            }
                        } catch (DebugException e) {
                            // If we aren't able to get a name, play it safe and don't
                            // hide it from the user
                            return true;
                        }
                    }
                }
            }
            // The variable isn't part of "self" or "cls", filter it out
            return false;
        }
        return true;
    }
}
