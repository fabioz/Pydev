/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.search.copied;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.search.ui.text.AbstractTextSearchResult;

import com.python.pydev.refactoring.refactorer.search.PythonFileSearchResult;

public class FileTableContentProvider implements IStructuredContentProvider, IFileSearchContentProvider {

    private final Object[] EMPTY_ARR = new Object[0];

    private FileSearchPage fPage;
    private AbstractTextSearchResult fResult;

    public FileTableContentProvider(FileSearchPage page) {
        fPage = page;
    }

    @Override
    public void dispose() {
        // nothing to do
    }

    @Override
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof PythonFileSearchResult) {
            int elementLimit = getElementLimit();
            Object[] elements = ((PythonFileSearchResult) inputElement).getElements();
            if (elementLimit != -1 && elements.length > elementLimit) {
                Object[] shownElements = new Object[elementLimit];
                System.arraycopy(elements, 0, shownElements, 0, elementLimit);
                return shownElements;
            }
            return elements;
        }
        return EMPTY_ARR;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (newInput instanceof PythonFileSearchResult) {
            fResult = (PythonFileSearchResult) newInput;
        }
    }

    @Override
    public void elementsChanged(Object[] updatedElements) {
        TableViewer viewer = getViewer();
        int elementLimit = getElementLimit();
        boolean tableLimited = elementLimit != -1;
        for (int i = 0; i < updatedElements.length; i++) {
            if (fResult.getMatchCount(updatedElements[i]) > 0) {
                if (viewer.testFindItem(updatedElements[i]) != null)
                    viewer.update(updatedElements[i], null);
                else {
                    if (!tableLimited || viewer.getTable().getItemCount() < elementLimit)
                        viewer.add(updatedElements[i]);
                }
            } else
                viewer.remove(updatedElements[i]);
        }
    }

    private int getElementLimit() {
        try {
            return fPage.getElementLimit().intValue();
        } catch (Throwable e) {
            //ignore (not available in eclipse 3.2)
            return 0;
        }
    }

    private TableViewer getViewer() {
        return (TableViewer) fPage.getViewer();
    }

    @Override
    public void clear() {
        getViewer().refresh();
    }
}
