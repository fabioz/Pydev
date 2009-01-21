package com.python.pydev.refactoring.refactorer.search.copied;

import org.eclipse.jface.action.Action;

public class SortAction extends Action {
	private int fSortOrder;
	private FileSearchPage fPage;
	
	public SortAction(String label, FileSearchPage page, int sortOrder) {
		super(label);
		fPage= page;
		fSortOrder= sortOrder;
	}

	public void run() {
		fPage.setSortOrder(fSortOrder);
	}

	public int getSortOrder() {
		return fSortOrder;
	}
}
