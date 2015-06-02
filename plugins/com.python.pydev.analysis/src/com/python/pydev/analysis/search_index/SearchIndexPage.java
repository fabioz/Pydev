/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.search_index;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * This is still a work in progress!!!
 */
public class SearchIndexPage extends DialogPage implements ISearchPage {

    private Combo fPattern;
    private ISearchPageContainer fContainer;

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setFont(parent.getFont());
        GridLayout layout = new GridLayout(2, false);
        composite.setLayout(layout);

        // Info text
        Label label = new Label(composite, SWT.LEAD);
        label.setText("Text");
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
        label.setFont(composite.getFont());

        fPattern = new Combo(composite, SWT.SINGLE | SWT.BORDER);
        fPattern.setFont(parent.getFont());
        GridData data = new GridData(GridData.FILL, GridData.FILL, true, false, 1, 2);
        data.widthHint = convertWidthInCharsToPixels(50);
        fPattern.setLayoutData(data);

        setControl(composite);
        Dialog.applyDialogFont(composite);
    }

    @Override
    public boolean performAction() {
        NewSearchUI.runQueryInBackground(new SearchIndexQuery(fPattern.getText()));
        return true;
    }

    @Override
    public void setContainer(ISearchPageContainer container) {
        fContainer = container;
    }

}
