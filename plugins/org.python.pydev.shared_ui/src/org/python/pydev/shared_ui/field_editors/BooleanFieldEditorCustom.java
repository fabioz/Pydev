package org.python.pydev.shared_ui.field_editors;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class BooleanFieldEditorCustom extends BooleanFieldEditor {

    private Button checkBox;

    public BooleanFieldEditorCustom(String name, String labelText, int style, Composite parent) {
        super(name, labelText, style, parent);
        this.checkBox = getCheckBox(parent);
    }

    public BooleanFieldEditorCustom(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
        this.checkBox = getCheckBox(parent);
    }

    public Button getCheckBox() {
        return checkBox;
    }

    public Button getCheckBox(Composite parent) {
        return getChangeControl(parent);
    }

    public void setTooltip(Composite parent, String tooltip) {
        getChangeControl(parent).setToolTipText(tooltip);
    }

}
