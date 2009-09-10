/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.pages.extractlocal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.messages.Messages;
import org.python.pydev.refactoring.ui.controls.LabeledEdit;
import org.python.pydev.refactoring.ui.pages.listener.IValidationPage;
import org.python.pydev.refactoring.ui.validator.NameValidator;

public class ExtractLocalComposite extends Composite {

    private LabeledEdit variableNameEdit;

    private AbstractScopeNode<?> scopeAdapter;

    private IValidationPage page;

    public ExtractLocalComposite(IValidationPage page, Composite parent, AbstractScopeNode<?> scope) {
        super(parent, SWT.NONE);
        this.page = page;
        this.scopeAdapter = scope;

        createComposite();
    }

    public void createComposite() {
        setLayout(new GridLayout());

        createVariableName(this);

        pack();
    }

    private void createVariableName(Composite control) {
        Composite variableNameComposite = new Composite(control, SWT.NONE);
        FillLayout compositeLayout = new FillLayout(SWT.HORIZONTAL);
        GridData gridData = new GridData();
        gridData.horizontalAlignment = GridData.FILL;
        variableNameComposite.setLayoutData(gridData);
        variableNameComposite.setLayout(compositeLayout);
        variableNameEdit = new LabeledEdit(variableNameComposite, Messages.extractLocalVariableTitle);
    }

    public void registerListeners(final IValidationPage page) {
        variableNameEdit.getEdit().addListener(SWT.Modify, page);
    }

    public String getVariableName() {
        return variableNameEdit.getEdit().getText();
    }

    public boolean validate() {
        NameValidator nameValidator = new NameValidator(scopeAdapter);
        try{
            nameValidator.validateVariableName(getVariableName());
            nameValidator.validateUniqueVariable(getVariableName());
        }catch(Throwable e){
            page.setErrorMessage(e.getMessage());
        }

        return page.getErrorMessage() == null;
    }
}
