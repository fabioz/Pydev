package org.python.pydev.shared_ui.field_editors;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class BooleanFieldEditorCustom extends BooleanFieldEditor {

    public BooleanFieldEditorCustom(String name, String labelText, int style, Composite parent) {
        super(name, labelText, style, parent);
    }

    public Button getCheckBox(Composite parent) {
        return getChangeControl(parent);
    }

}
