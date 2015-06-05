/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.search_index;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_core.structure.TreeNodeContentProvider;

import com.python.pydev.analysis.search.ICustomLineElement;

/**
 * This is a content provider that creates a separate structure based on TreeNodes
 * so that we can have better control on how to show things.
 */
public class SearchIndexTreeContentProvider extends TreeNodeContentProvider
        implements ITreeContentProvider, ISearchIndexContentProvider {

    private TreeNode<Object> root;
    private Map<Object, TreeNode> elementToTreeNode = new HashMap<>();
    private TreeViewer viewer;
    private AbstractTextSearchResult fResult;

    public SearchIndexTreeContentProvider(SearchIndexResultPage searchIndexResultPage, TreeViewer viewer) {
        this.viewer = viewer;
    }

    @Override
    public void dispose() {

    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        elementToTreeNode.clear();
        if (newInput instanceof AbstractTextSearchResult) {
            AbstractTextSearchResult abstractTextSearchResult = (AbstractTextSearchResult) newInput;
            this.fResult = abstractTextSearchResult;
            root = new TreeNode<>(null, newInput);
            Object[] elements = abstractTextSearchResult.getElements();
            for (int i = 0; i < elements.length; i++) {
                Object object = elements[i];
                obtainTeeNodeElement(object);
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
                TreeNode treeNode = this.elementToTreeNode.get(object);
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

    @SuppressWarnings("rawtypes")
    private TreeNode obtainTeeNodeElement(final Object object) {
        if (object instanceof TreeNode) {
            return (TreeNode) object;
        }

        TreeNode treeNode = elementToTreeNode.get(object);
        TreeNode ret;
        if (treeNode != null) {
            return treeNode;

        } else if (object instanceof ModuleLineElement) {
            ModuleLineElement moduleLineElement = (ModuleLineElement) object;
            TreeNode parentNode = obtainTeeNodeElement(new CustomModule(moduleLineElement));
            ret = new TreeNode<>(parentNode, object);

        } else if (object instanceof CustomModule) {
            CustomModule package1 = (CustomModule) object;
            TreeNode parentNode = obtainTeeNodeElement(package1.project);
            ret = new TreeNode<>(parentNode, object);

        } else if (object instanceof IProject) {
            return root;
            //As we can filter later on based on package names, let's not include the project for now.
            //ret = new TreeNode<>(root, object);

        } else {
            Log.log("Unhandled: " + object);
            return null;
        }

        elementToTreeNode.put(object, ret);

        return ret;
    }

    @Override
    public void clear() {
        root = new TreeNode<Object>(null, null);
        this.elementToTreeNode.clear();
        this.viewer.refresh();
    }

}
