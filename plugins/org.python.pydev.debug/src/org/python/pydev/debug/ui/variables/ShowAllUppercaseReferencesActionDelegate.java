package org.python.pydev.debug.ui.variables;

import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.debug.model.PyVariable;
import org.python.pydev.debug.model.PyVariablesPreferences;

/**
 * All Uppercase in this context means variables with no lower case. So
 * ABC
 * ABC_DEF
 * ABC1
 * are all all uppercase
 */
public class ShowAllUppercaseReferencesActionDelegate extends AbstractShowReferencesActionDelegate {
    @Override
    protected boolean isShowReference() {
        return PyVariablesPreferences.isShowAllUppercaseReferences();
    }

    @Override
    protected void setShowReferences(boolean checked) {
        PyVariablesPreferences.setShowAllUppercaseReferences(checked);
    }

    @Override
    protected boolean isShowReferenceProperty(String property) {
        return PyVariablesPreferences.DEBUG_UI_VARIABLES_SHOW_ALLUPPERCASE_REFERENCES.equals(property);
    }

    @Override
    protected boolean select(Viewer viewer, Object parentElement, PyVariable variable, String variableName) {

        if (variableName != null) {
            for (int i = 0; i < variableName.length(); i++) {
                if (Character.isLowerCase(variableName.charAt(i))) {
                    return true;
                }
            }
            // no lower case letters, filter out
            return false;
        }
        return true;
    }
}
