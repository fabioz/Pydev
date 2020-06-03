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
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.debug.model.PyVariableCollection;
import org.python.pydev.debug.model.PyVariableGroup;
import org.python.pydev.debug.model.PyVariablesPreferences;

public class ShowFunctionReferencesActionDelegate extends AbstractShowReferencesActionDelegate {
    @Override
    protected boolean isShowReference() {
        return PyVariablesPreferences.isShowFunctionAndModuleReferences();
    }

    @Override
    protected void setShowReferences(boolean checked) {
        PyVariablesPreferences.setShowFunctionAndModuleReferences(checked);
    }

    @Override
    protected boolean isShowReferenceProperty(String property) {
        return PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_FUNCTION_REFERENCES.equals(property);
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (isShowReference()) {
            return true;
        } else {
            if (element instanceof PyVariableGroup) {
                PyVariableGroup variable = (PyVariableGroup) element;
                try {
                    String name = variable.getName();
                    if (PyVariableCollection.SCOPE_FUNCTION_VARS.equals(name)) {
                        return false;
                    }
                } catch (DebugException e) {
                    // Ignore error, if we get one, don't filter
                }
            }

            return true;
        }
    }

}
