/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.core;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class LabeledEdit extends Composite {

    private Text edit;

    public LabeledEdit(Composite parent, String caption, String text) {
        super(parent, SWT.None);

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        setLayout(layout);

        createLabel(caption);

        createEdit(text);
    }

    private void createEdit(String text) {
        edit = new Text(this, SWT.BORDER | SWT.SINGLE);
        edit.setText(text);
        edit.selectAll();

        GridData textData = new GridData(GridData.FILL_HORIZONTAL);
        textData.grabExcessHorizontalSpace = true;
        edit.setLayoutData(textData);
    }

    private void createLabel(String caption) {
        Label label = new Label(this, SWT.NONE);
        label.setText(caption);
        label.setLayoutData(new GridData());
    }

    public LabeledEdit(Composite parent, String labelName) {
        this(parent, labelName, "");
    }

    public Text getEdit() {
        return edit;
    }

}
