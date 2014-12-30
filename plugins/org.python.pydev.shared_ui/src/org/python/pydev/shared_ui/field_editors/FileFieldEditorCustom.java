package org.python.pydev.shared_ui.field_editors;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class FileFieldEditorCustom extends FileFieldEditor {

    public FileFieldEditorCustom(String name, String labelText, Composite parent) {
        super(name, labelText, parent);
    }

    @Override
    public void fillIntoGrid(Composite parent, int numColumns) {
        Assert.isTrue(parent.getLayout() instanceof GridLayout);
        doFillIntoGrid(parent, numColumns);
        adjustForNumColumns(numColumns);
    }

    /* (non-Javadoc)
     * Method declared on FieldEditor.
     */
    @Override
    protected void adjustForNumColumns(int numColumns) {
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
