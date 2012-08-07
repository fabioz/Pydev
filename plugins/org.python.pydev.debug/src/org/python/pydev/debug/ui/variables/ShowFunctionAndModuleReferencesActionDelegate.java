package org.python.pydev.debug.ui.variables;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.debug.model.PyVariable;
import org.python.pydev.debug.model.PyVariablesPreferences;

public class ShowFunctionAndModuleReferencesActionDelegate extends AbstractShowReferencesActionDelegate {
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
        return PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_FUNCTION_AND_MODULE_REFERENCES.equals(property);
    }

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (isShowReference()) {
            return true;
        } else {
            if (element instanceof PyVariable) {
                PyVariable variable = (PyVariable) element;
                try {
                    String typeName = variable.getReferenceTypeName();
                    if ("module".equals(typeName) || "function".equals(typeName)) {
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
