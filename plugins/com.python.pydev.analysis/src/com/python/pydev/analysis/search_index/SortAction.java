/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.python.pydev.analysis.search_index;

import org.eclipse.jface.action.Action;

/**
 * Based on org.eclipse.search.internal.ui.text.SortAction
 */
public class SortAction extends Action {
    private int fSortOrder;
    private SearchIndexResultPage fPage;

    public SortAction(String label, SearchIndexResultPage page, int sortOrder) {
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
