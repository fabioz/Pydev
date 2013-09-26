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

import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.debug.model.PyVariable;
import org.python.pydev.debug.model.PyVariablesPreferences;

public class ShowCapitalizedReferencesActionDelegate extends AbstractShowReferencesActionDelegate {
    @Override
    protected boolean isShowReference() {
        return PyVariablesPreferences.isShowCapitalizedReferences();
    }

    @Override
    protected void setShowReferences(boolean checked) {
        PyVariablesPreferences.setShowCapitalizedReferences(checked);
    }

    @Override
    protected boolean isShowReferenceProperty(String property) {
        return PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_CAPITALIZED_REFERENCES.equals(property);
    }

    @Override
    protected boolean select(Viewer viewer, Object parentElement, PyVariable variable, String variableName) {
        if (variableName != null && variableName.length() >= 1 && Character.isUpperCase(variableName.charAt(0))) {
            return false;
        }
        return true;
    }
}
