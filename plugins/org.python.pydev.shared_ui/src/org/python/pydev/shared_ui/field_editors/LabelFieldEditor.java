/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.shared_ui.field_editors;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.widgets.Composite;

public class LabelFieldEditor extends FieldEditor {
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