/*
 * Created on Oct 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.utils;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;


public class LabelFieldEditor extends FieldEditor {
    private Label label;

    public LabelFieldEditor(String name, String labelText, Composite parent) {
        init(name, labelText);
        createControl(parent);
    }

    protected void adjustForNumColumns(int numColumns) {
    }

    protected void doFillIntoGrid(Composite parent, int numColumns) {
        getLabelControl(parent);
    }



    protected void doLoad() {
    }

    protected void doLoadDefault() {
    }

    protected void doStore() {
    }

    public int getNumberOfControls() {
        return 1;
    }
}