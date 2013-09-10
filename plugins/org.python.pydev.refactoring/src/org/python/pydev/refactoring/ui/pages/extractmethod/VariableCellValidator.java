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

package org.python.pydev.refactoring.ui.pages.extractmethod;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.core.validator.NameValidator;
import org.python.pydev.refactoring.ui.pages.core.SimpleTableItem;
import org.python.pydev.refactoring.ui.pages.listener.IValidationPage;

public class VariableCellValidator {

    private Table table;

    private IValidationPage page;

    private AbstractScopeNode<?> scope;

    public VariableCellValidator(IValidationPage page, Table table, AbstractScopeNode<?> scope) {
        assert (page != null);
        assert (table != null);
        assert (scope != null);
        this.page = page;
        this.table = table;
        this.scope = scope;
    }

    private boolean hasUniqueArguments() {
        TableItem[] items = table.getItems();

        for (TableItem outer : items) {
            for (TableItem inner : items) {
                if (outer != inner) {
                    if (outer.getText().equals(inner.getText())) {
                        page.setErrorMessage("Variable name " + outer.getText() + " was already used");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void validateArguments() {
        RefactoringStatus status = new RefactoringStatus();
        NameValidator validator = new NameValidator(status, this.scope);

        TableItem[] items = table.getItems();
        for (TableItem item : items) {
            if (item instanceof SimpleTableItem) {
                SimpleTableItem variableItem = (SimpleTableItem) item;
                if (variableItem.hasNewName()) {
                    validator.validateVariableName(item.getText());
                    validator.validateUniqueVariable(item.getText());
                }
            }
        }

        if (status.hasError()) {
            page.setErrorMessage(status.getMessageMatchingSeverity(RefactoringStatus.WARNING));
        }
    }

    public void validate() {
        validateArguments();
        hasUniqueArguments();
    }
}
