/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
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
import org.python.pydev.refactoring.ui.controls.LabeledEdit;
import org.python.pydev.refactoring.ui.model.table.SimpleTableItem;

public class FunctionSignatureListener implements Listener {

    private final String METHODDEF = "def ";

    private final String OPENBRACKET = "(";

    private final String CLOSEBRACKET = ")";

    private Table argumentTable;

    private Label signatureLabel;

    private LabeledEdit functionNameEdit;

    private IValidationPage page;

    public FunctionSignatureListener(IValidationPage page, Label signature, LabeledEdit functionNameEdit, Table argumentTable) {
        this.page = page;
        this.signatureLabel = signature;
        this.functionNameEdit = functionNameEdit;
        this.argumentTable = argumentTable;
    }

    private void updateSignature() {
        if(functionNameEdit.getEdit().getText().length() == 0){
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
        if(this.argumentTable != null){
            List<TableItem> items = Arrays.asList(argumentTable.getItems());
            Iterator<TableItem> iter = items.iterator();
            while(iter.hasNext()){
                TableItem item = iter.next();
                if(item instanceof SimpleTableItem){
                    signature.append(item.getText());
                    if(iter.hasNext())
                        signature.append(", ");
                }
            }
        }
    }

    public void handleEvent(Event event) {
        if(page.isPageComplete()){
            updateSignature();
        }
    }
}
