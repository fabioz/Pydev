/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.search;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_core.structure.TreeNodeContentProvider;

public abstract class AbstractSearchIndexTreeContentProvider extends TreeNodeContentProvider
        implements ITreeContentProvider, ISearchIndexContentProvider {

    protected TreeNode<Object> root;
    protected Map<Object, TreeNode<?>> elementToTreeNode = new HashMap<>();
    protected TreeViewer viewer;
    protected AbstractTextSearchResult fResult;

    public int groupWith = 0;

    public AbstractSearchIndexTreeContentProvider(TreeViewer viewer) {
        this.viewer = viewer;
    }

    public void setGroupWith(int groupWith) {
        if (this.groupWith == groupWith) {
            return;
        }
        this.groupWith = groupWith;

        // Pretend the input changed (as the whole structure changed).
        this.inputChanged(this.viewer, null, fResult);

        this.clearFilterCaches();

        // And at last, ask for a refresh!
        this.viewer.refresh();
    }

    protected void clearFilterCaches() {
        ViewerFilter[] filters = this.viewer.getFilters();
        if (filters != null) {
            for (ViewerFilter viewerFilter : filters) {
                if (viewerFilter instanceof AbstractSearchResultsViewerFilter) {
                    AbstractSearchResultsViewerFilter filter = (AbstractSearchResultsViewerFilter) viewerFilter;
                    filter.clearCache();
                }
            }
        }
    }

    public int getGroupWith() {
        return groupWith;
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        elementToTreeNode.clear();
        if (newInput instanceof AbstractTextSearchResult) {
            AbstractTextSearchResult abstractTextSearchResult = (AbstractTextSearchResult) newInput;
            this.fResult = abstractTextSearchResult;
            root = new TreeNode<>(null, newInput);
            Object[] elements = abstractTextSearchResult.getElements();
            int elementsLen = elements.length;
            for (int i = 0; i < elementsLen; i++) {
                Object object = elements[i];
                Match[] matches = abstractTextSearchResult.getMatches(object);
                int matchesLen = matches.length;
                for (int j = 0; j < matchesLen; j++) {
                    Match match = matches[j];
                    if (match instanceof ICustomMatch) {
                        ICustomMatch moduleMatch = (ICustomMatch) match;
                        obtainTeeNodeElement(moduleMatch.getLineElement());
                    } else {
                        Log.log("Expecting ICustomMatch. Found:" + match.getClass() + " - " + match);
                    }
                }
            }
        } else {
            this.clear();
        }
    }

    @Override
    public Object[] getElements(Object inputElement) {
        return getChildren(root);
    }

    @Override
    public void elementsChanged(Object[] updatedElements) {
        for (int i = 0; i < updatedElements.length; i++) {
            Object object = updatedElements[i];
            int matchCount;
            if (object instanceof ICustomLineElement) {
                ICustomLineElement iCustomLineElement = (ICustomLineElement) object;
                matchCount = iCustomLineElement.getNumberOfMatches(fResult);
            } else {
                matchCount = fResult.getMatchCount(updatedElements[i]);
            }
            if (matchCount > 0) {
                obtainTeeNodeElement(object);
            } else {
                TreeNode<?> treeNode = this.elementToTreeNode.get(object);
                if (treeNode != null) {
                    Object parent = treeNode.getParent();
                    treeNode.detachFromParent();
                    if (parent instanceof TreeNode<?>) {
                        checkClearParentTree((TreeNode<?>) parent);
                    }
                }
            }
        }
        this.viewer.refresh();
    }

    private void checkClearParentTree(TreeNode<?> treeNode) {
        if (!treeNode.hasChildren()) {
            Object parent = treeNode.getParent();
            treeNode.detachFromParent();
            if (parent instanceof TreeNode<?>) {
                checkClearParentTree((TreeNode<?>) parent);
            }
        }
    }

    @Override
    public void clear() {
        root = new TreeNode<Object>(null, null);
        this.elementToTreeNode.clear();
        this.clearFilterCaches();
        this.viewer.refresh();
    }

    /**
     * Subclasses should override to actually create the structure
     */
    protected abstract TreeNode<?> obtainTeeNodeElement(final Object object);

}
