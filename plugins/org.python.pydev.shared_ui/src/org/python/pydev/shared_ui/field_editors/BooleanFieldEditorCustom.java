package org.python.pydev.shared_ui.field_editors;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class BooleanFieldEditorCustom extends BooleanFieldEditor {

    private Button checkBox;
    private int style;

    public BooleanFieldEditorCustom(String name, String labelText, int style, Composite parent) {
        super(name, labelText, style, parent);
        this.checkBox = getCheckBox(parent);
        this.style = style;
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

    public void setVisible(boolean visible, Composite parent) {
        if (style == SEPARATE_LABEL) {
            Label control = getLabelControl(parent);
            control.setVisible(visible);
            GridData layoutData = (GridData) control.getLayoutData();
            if (layoutData != null) {
                layoutData.exclude = !visible;
            }
        }
        Button control = getChangeControl(parent);
        control.setVisible(visible);
        Object layoutData = control.getLayoutData();
        if (layoutData instanceof GridData) {
            ((GridData) layoutData).exclude = !visible;
        }

    }

}
