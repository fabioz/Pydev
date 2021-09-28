package org.python.pydev.shared_ui.field_editors;

import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class CustomStringFieldEditor extends StringFieldEditor {

    public CustomStringFieldEditor(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
    }

    public void setVisible(boolean visible, Composite parent) {
        Label labelControl = getLabelControl(parent);
        Text textControl = getTextControl(parent);
        labelControl.setVisible(visible);
        textControl.setVisible(visible);

        Object layoutData = labelControl.getLayoutData();
        if (layoutData instanceof GridData) {
            ((GridData) layoutData).exclude = !visible;
        }
        layoutData = textControl.getLayoutData();
        if (layoutData instanceof GridData) {
            ((GridData) layoutData).exclude = !visible;
        }

    }

}
