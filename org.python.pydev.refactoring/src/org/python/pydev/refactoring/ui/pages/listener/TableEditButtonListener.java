package org.python.pydev.refactoring.ui.pages.listener;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public class TableEditButtonListener implements Listener {
	private final Table table;

	private final Button upButton;

	private final Button downButton;

	private final Button editButton;

	public TableEditButtonListener(Table table, Button upButton,
			Button downButton, Button editButton) {
		this.table = table;
		this.upButton = upButton;
		this.downButton = downButton;
		this.editButton = editButton;
	}

	private void update() {
		resetState();

		TableItem[] selection = table.getSelection();
		if (selection == null || selection.length != 1) {
			return;
		} else
			editButton.setEnabled(true);

		if (table.getSelectionIndex() == 0) {
			upButton.setEnabled(false);
		}
		if (table.getSelectionIndex() == table.getItemCount() - 1) {
			downButton.setEnabled(false);
		}

	}

	private void resetState() {
		downButton.setEnabled(false);
		upButton.setEnabled(false);
		editButton.setEnabled(false);
	}

	public void handleEvent(Event event) {
		update();		
	}
}
