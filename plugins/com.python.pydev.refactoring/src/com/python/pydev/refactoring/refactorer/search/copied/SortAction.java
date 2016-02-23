/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.search.copied;

import org.eclipse.jface.action.Action;

public class SortAction extends Action {
    private int fSortOrder;
    private FileSearchPage fPage;

    public SortAction(String label, FileSearchPage page, int sortOrder) {
        super(label);
        fPage = page;
        fSortOrder = sortOrder;
    }

    @Override
    public void run() {
        fPage.setSortOrder(fSortOrder);
    }

    public int getSortOrder() {
        return fSortOrder;
    }
}
