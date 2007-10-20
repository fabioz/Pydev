/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.validator;

import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ui.model.table.SimpleTableItem;
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
		NameValidator validator = new NameValidator(this.scope);
		TableItem[] items = table.getItems();
		for (TableItem item : items) {
			if (item instanceof SimpleTableItem) {
				SimpleTableItem variableItem = (SimpleTableItem) item;
				if (variableItem.hasNewName()) {
					try {
						validator.validateVariableName(item.getText());
						validator.validateUniqueVariable(item.getText());
					} catch (Throwable e) {
						page.setErrorMessage(e.getMessage());
					}
				}

			}
		}

	}

	public void validate() {
		validateArguments();
		hasUniqueArguments();
	}
}
