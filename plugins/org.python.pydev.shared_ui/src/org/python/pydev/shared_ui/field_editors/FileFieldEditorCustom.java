package org.python.pydev.shared_ui.field_editors;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class FileFieldEditorCustom extends FileFieldEditor {

    private int numColumnsTarget;
    private Composite parent;

    public FileFieldEditorCustom(String name, String labelText, Composite parent, int numColumnsTarget) {
        super(name, labelText, parent);
        this.numColumnsTarget = numColumnsTarget;
        this.parent = parent;
    }

    public FileFieldEditorCustom(String name, String labelText, Composite parent) {
        this(name, labelText, parent, -1);
    }

    @Override
    public void fillIntoGrid(Composite parent, int numColumns) {
        Assert.isTrue(parent.getLayout() instanceof GridLayout);
        doFillIntoGrid(parent, numColumns);
        adjustForNumColumns(numColumns);
    }

    @Override
    public int getNumberOfControls() {
        if (numColumnsTarget != -1) {
            return numColumnsTarget;
        }
        return super.getNumberOfControls();
    }

    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    @Override
    protected void adjustForNumColumns(int numColumns) {
        if (numColumnsTarget != -1) {
            if (numColumnsTarget == 1) {
                Label labelControl = getLabelControl();
                setLayoutHorizontalSpan(labelControl, numColumns);
                setLayoutHorizontalSpan(getTextControl(), numColumns);
                setLayoutHorizontalSpan(getChangeControl(parent), numColumns);
            } else {
                throw new AssertionError("Currently only handles numColumnsTarget == 1 or -1.");
            }
        } else {
            if (numColumns == 2) {
                // Label will take 2 cols and text/button the other 2
                Label labelControl = getLabelControl();
                GridData layoutData = (GridData) labelControl.getLayoutData();
                if (layoutData == null) {
                    layoutData = new GridData();
                    labelControl.setLayoutData(layoutData);
                }
                layoutData.horizontalSpan = 2;
                ((GridData) getTextControl().getLayoutData()).horizontalSpan = 1;

            } else if (numColumns == 1) {
                // 1 column each.
                Label labelControl = getLabelControl();
                GridData layoutData = (GridData) labelControl.getLayoutData();
                if (layoutData == null) {
                    layoutData = new GridData();
                    labelControl.setLayoutData(layoutData);
                }
                layoutData.horizontalSpan = 1;
                ((GridData) getTextControl().getLayoutData()).horizontalSpan = 1;

            } else {
                ((GridData) getTextControl().getLayoutData()).horizontalSpan = numColumns - 2;
            }
        }
    }

    private void setLayoutHorizontalSpan(Control control, int numColumns) {
        GridData layoutData = (GridData) control.getLayoutData();
        if (layoutData == null) {
            layoutData = new GridData();
            control.setLayoutData(layoutData);
        }
        layoutData.horizontalSpan = numColumns;

    }

    public void setVisible(boolean visible) {
        Label labelControl = getLabelControl();
        setVisible(labelControl, visible);
        setVisible(getTextControl(), visible);
        setVisible(getChangeControl(parent), visible);
    }

    private void setVisible(Control control, boolean visible) {
        control.setVisible(visible);
        Object layoutData = control.getLayoutData();
        if (layoutData instanceof GridData) {
            ((GridData) layoutData).exclude = !visible;
        }
    }
}
