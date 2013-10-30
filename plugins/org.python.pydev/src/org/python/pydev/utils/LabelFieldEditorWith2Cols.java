/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.utils;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.python.pydev.shared_ui.field_editors.LabelFieldEditor;

/**
 * Field editor that only shows info in 2 columns.
 * 
 * It must be overridden (getLabelTextCol1()) to return the text for the 1st column (that's because
 * calling super will already request that info, so, passing it in the constructor is not ok).
 *
 * @author Fabio
 */
public abstract class LabelFieldEditorWith2Cols extends LabelFieldEditor {

    private Label labelCol1;

    public LabelFieldEditorWith2Cols(String name, String labelTextCol2, Composite parent) {
        super(name, labelTextCol2, parent);
    }

    /**
     * Returns this field editor's label component.
     * <p>
     * The label is created if it does not already exist
     * </p>
     *
     * @param parent the parent
     * @return the label control
     */
    public Label getLabelControl2(Composite parent) {
        if (labelCol1 == null) {
            labelCol1 = new Label(parent, SWT.LEFT);
            labelCol1.setFont(parent.getFont());
            String text = getLabelText();
            if (text != null) {
                labelCol1.setText(getLabelTextCol1());
            }

            labelCol1.addDisposeListener(new DisposeListener() {
                public void widgetDisposed(DisposeEvent event) {
                    labelCol1 = null;
                }
            });
        } else {
            checkParent(labelCol1, parent);
        }
        return labelCol1;
    }

    /**
     * @return The text to appear in the 1st col.
     * 
     * It needs to be always reimplemented in this class because when super is called, we already need 
     * to have it (so, before making the init we need it).
     */
    public abstract String getLabelTextCol1();

    public int getNumberOfControls() {
        return 2;
    }

    protected void doFillIntoGrid(Composite parent, int numColumns) {
        getLabelControl2(parent);
        getLabelControl(parent);
    }

}
