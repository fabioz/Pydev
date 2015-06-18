/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.search.copied;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

import com.python.pydev.refactoring.refactorer.search.AbstractPythonSearchQuery;
import com.python.pydev.refactoring.refactorer.search.PythonFileSearchResult;

public class FileTreeContentProvider implements ITreeContentProvider, IFileSearchContentProvider {

    private final Object[] EMPTY_ARR = new Object[0];

    private AbstractTextSearchResult fResult;
    private FileSearchPage fPage;
    private AbstractTreeViewer fTreeViewer;
    private Map<Object, Set> fChildrenMap;

    FileTreeContentProvider(FileSearchPage page, AbstractTreeViewer viewer) {
        fPage = page;
        fTreeViewer = viewer;
    }

    public Object[] getElements(Object inputElement) {
        Object[] children = getChildren(inputElement);
        int elementLimit = getElementLimit();
        if (elementLimit != -1 && elementLimit < children.length) {
            Object[] limitedChildren = new Object[elementLimit];
            System.arraycopy(children, 0, limitedChildren, 0, elementLimit);
            return limitedChildren;
        }
        return children;
    }

    private int getElementLimit() {
        try {
            return fPage.getElementLimit().intValue();
        } catch (Throwable e) {
            // not available in eclipse 3.2
            return 0;
        }
    }

    public void dispose() {
        // nothing to do
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (newInput instanceof PythonFileSearchResult) {
            initialize((PythonFileSearchResult) newInput);
        }
    }

    private synchronized void initialize(AbstractTextSearchResult result) {
        fResult = result;
        fChildrenMap = new HashMap<Object, Set>();
        boolean showLineMatches = !((AbstractPythonSearchQuery) fResult.getQuery()).isFileNameSearch();

        if (result != null) {
            Object[] elements = result.getElements();
            for (int i = 0; i < elements.length; i++) {
                if (showLineMatches) {
                    Match[] matches = result.getMatches(elements[i]);
                    for (int j = 0; j < matches.length; j++) {
                        insert(((FileMatch) matches[j]).getLineElement(), false);
                    }
                } else {
                    insert(elements[i], false);
                }
            }
        }
    }

    private void insert(Object child, boolean refreshViewer) {
        Object parent = getParent(child);
        while (parent != null) {
            if (insertChild(parent, child)) {
                if (refreshViewer)
                    fTreeViewer.add(parent, child);
            } else {
                if (refreshViewer)
                    fTreeViewer.refresh(parent);
                return;
            }
            child = parent;
            parent = getParent(child);
        }
        if (insertChild(fResult, child)) {
            if (refreshViewer)
                fTreeViewer.add(fResult, child);
        }
    }

    /**
     * returns true if the child already was a child of parent.
     * 
     * @param parent
     * @param child
     * @return Returns <code>trye</code> if the child was added
     */
    private boolean insertChild(Object parent, Object child) {
        Set children = (Set) fChildrenMap.get(parent);
        if (children == null) {
            children = new HashSet();
            fChildrenMap.put(parent, children);
        }
        return children.add(child);
    }

    private boolean hasChild(Object parent, Object child) {
        Set children = (Set) fChildrenMap.get(parent);
        return children != null && children.contains(child);
    }

    private void remove(Object element, boolean refreshViewer) {
        // precondition here:  fResult.getMatchCount(child) <= 0

        if (hasChildren(element)) {
            if (refreshViewer)
                fTreeViewer.refresh(element);
        } else {
            if (!hasMatches(element)) {
                fChildrenMap.remove(element);
                Object parent = getParent(element);
                if (parent != null) {
                    removeFromSiblings(element, parent);
                    remove(parent, refreshViewer);
                } else {
                    removeFromSiblings(element, fResult);
                    if (refreshViewer)
                        fTreeViewer.refresh();
                }
            } else {
                if (refreshViewer) {
                    fTreeViewer.refresh(element);
                }
            }
        }
    }

    private boolean hasMatches(Object element) {
        if (element instanceof LineElement) {
            LineElement lineElement = (LineElement) element;
            return lineElement.getNumberOfMatches(fResult) > 0;
        }
        return fResult.getMatchCount(element) > 0;
    }

    private void removeFromSiblings(Object element, Object parent) {
        Set siblings = (Set) fChildrenMap.get(parent);
        if (siblings != null) {
            siblings.remove(element);
        }
    }

    public Object[] getChildren(Object parentElement) {
        Set children = (Set) fChildrenMap.get(parentElement);
        if (children == null)
            return EMPTY_ARR;
        return children.toArray();
    }

    public boolean hasChildren(Object element) {
        return getChildren(element).length > 0;
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.search.internal.ui.text.IFileSearchContentProvider#elementsChanged(java.lang.Object[])
     */
    public synchronized void elementsChanged(Object[] updatedElements) {
        for (int i = 0; i < updatedElements.length; i++) {
            if (!(updatedElements[i] instanceof LineElement)) {
                // change events to elements are reported in file search
                if (fResult.getMatchCount(updatedElements[i]) > 0)
                    insert(updatedElements[i], true);
                else
                    remove(updatedElements[i], true);
            } else {
                // change events to line elements are reported in text search
                LineElement lineElement = (LineElement) updatedElements[i];
                int nMatches = lineElement.getNumberOfMatches(fResult);
                if (nMatches > 0) {
                    if (hasChild(lineElement.getParent(), lineElement)) {
                        fTreeViewer.update(new Object[] { lineElement, lineElement.getParent() }, null);
                    } else {
                        insert(lineElement, true);
                    }
                } else {
                    remove(lineElement, true);
                }
            }
        }
    }

    public void clear() {
        initialize(fResult);
        fTreeViewer.refresh();
    }

    public Object getParent(Object element) {
        if (element instanceof IProject)
            return null;
        if (element instanceof IResource) {
            IResource resource = (IResource) element;
            return resource.getParent();
        }
        if (element instanceof LineElement) {
            return ((LineElement) element).getParent();
        }

        if (element instanceof FileMatch) {
            FileMatch match = (FileMatch) element;
            return match.getLineElement();
        }
        return null;
    }
}
