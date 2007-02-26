package org.python.pydev.refactoring.ui.model.table;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public class SimpleTableItem extends TableItem {

	private String realName;

	public SimpleTableItem(Table parent, String name) {
		super(parent, SWT.None);
		this.realName = name;
		setText(realName);
	}

	public SimpleTableItem(Table parent, String originalName, String text,
			int pos) {
		super(parent, SWT.None, pos);
		this.realName = originalName;
		setText(text);
	}

	public String getOriginalName() {
		return realName;
	}

	@Override
	protected void checkSubclass() {
	}

	public boolean hasNewName() {
		return this.realName.compareTo(getText()) != 0;
	}

}
