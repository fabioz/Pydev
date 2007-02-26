package org.python.pydev.refactoring.ui.model;

import org.eclipse.jface.viewers.StructuredSelection;

public class OffsetStrategyModel extends StructuredSelection {

	private int key;

	private String description;

	public OffsetStrategyModel(int key, String description) {
		this.key = key;
		this.description = description;
	}

	@Override
	public String toString() {
		return description;
	}

	public int getStrategy() {
		return key;
	}

}
