/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.pages.listener;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.python.pydev.refactoring.ui.core.LabeledEdit;
import org.python.pydev.refactoring.ui.pages.core.SimpleTableItem;

public class FunctionSignatureListener implements Listener {

    private static final String METHODDEF = "def ";

    private static final String OPENBRACKET = "(";

    private static final String CLOSEBRACKET = ")";

    private Table argumentTable;

    private Label signatureLabel;

    private LabeledEdit functionNameEdit;

    private IValidationPage page;

    public FunctionSignatureListener(IValidationPage page, Label signature, LabeledEdit functionNameEdit,
            Table argumentTable) {
        this.page = page;
        this.signatureLabel = signature;
        this.functionNameEdit = functionNameEdit;
        this.argumentTable = argumentTable;
    }

    private void updateSignature() {
        if (functionNameEdit.getEdit().getText().length() == 0) {
            return;
        }

        StringBuilder signature = new StringBuilder();

        signature.append(METHODDEF);
        signature.append(this.functionNameEdit.getEdit().getText());
        signature.append(OPENBRACKET);

        initArguments(signature);
        signature.append(CLOSEBRACKET);

        signatureLabel.setText(signature.toString());
    }

    private void initArguments(StringBuilder signature) {
        if (this.argumentTable != null) {
            List<TableItem> items = Arrays.asList(argumentTable.getItems());
            Iterator<TableItem> iter = items.iterator();
            while (iter.hasNext()) {
                TableItem item = iter.next();
                if (item instanceof SimpleTableItem) {
                    signature.append(item.getText());
                    if (iter.hasNext()) {
                        signature.append(", ");
                    }
                }
            }
        }
    }

    @Override
    public void handleEvent(Event event) {
        if (page.isPageComplete()) {
            updateSignature();
        }
    }
}
