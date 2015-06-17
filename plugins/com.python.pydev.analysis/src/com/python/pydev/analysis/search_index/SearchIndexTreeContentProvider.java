/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.search_index;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_core.structure.TreeNodeContentProvider;
import org.python.pydev.shared_ui.search.ICustomLineElement;
import org.python.pydev.shared_ui.search.ICustomMatch;
import org.python.pydev.shared_ui.search.ISearchIndexContentProvider;

/**
 * This is a content provider that creates a separate structure based on TreeNodes
 * so that we can have better control on how to show things.
 */
public class SearchIndexTreeContentProvider extends TreeNodeContentProvider
        implements ITreeContentProvider, ISearchIndexContentProvider {

    private TreeNode<Object> root;
    private Map<Object, TreeNode<?>> elementToTreeNode = new HashMap<>();
    private TreeViewer viewer;
    private AbstractTextSearchResult fResult;

    public int groupWith = 0;

    public void setGroupWith(int groupWith) {
        if (this.groupWith == groupWith) {
            return;
        }
        this.groupWith = groupWith;

        // Pretend the input changed (as the whole structure changed).
        this.inputChanged(this.viewer, null, fResult);

        // And at last, ask for a refresh!
        this.viewer.refresh();
    }

    public int getGroupWith() {
        return groupWith;
    }

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

    @SuppressWarnings("rawtypes")
    private TreeNode obtainTeeNodeElement(final Object object) {
        if (object instanceof TreeNode) {
            return (TreeNode) object;
        }

        TreeNode treeNode = elementToTreeNode.get(object);
        if (treeNode != null) {
            return treeNode;

        }

        TreeNode ret = null;
        if (object instanceof ModuleLineElement) {
            ModuleLineElement moduleLineElement = (ModuleLineElement) object;
            TreeNode parentNode;

            if ((this.groupWith & GROUP_WITH_MODULES) != 0) {
                parentNode = obtainTeeNodeElement(new CustomModule(moduleLineElement));
                ret = new TreeNode<>(parentNode, object);

            } else if ((this.groupWith & GROUP_WITH_FOLDERS) != 0) {
                IResource parent = moduleLineElement.getParent();
                parentNode = obtainTeeNodeElement(parent);
                ret = new TreeNode<>(parentNode, object);

            } else if ((this.groupWith & GROUP_WITH_PROJECT) != 0) {
                parentNode = obtainTeeNodeElement(moduleLineElement.getProject());
                ret = new TreeNode<>(parentNode, object);

            } else {
                // No grouping at all (flat)
                ret = new TreeNode<>(root, object);

            }

        } else if (object instanceof CustomModule) {
            if ((this.groupWith & GROUP_WITH_FOLDERS) != 0) {
                CustomModule package1 = (CustomModule) object;
                TreeNode parentNode = obtainTeeNodeElement(package1.resource.getParent());
                ret = new TreeNode<>(parentNode, object);

            } else if ((this.groupWith & GROUP_WITH_PROJECT) != 0) {
                CustomModule package1 = (CustomModule) object;
                TreeNode parentNode = obtainTeeNodeElement(package1.project);
                ret = new TreeNode<>(parentNode, object);

            } else {
                // Already at root
                ret = new TreeNode<>(root, object);

            }

        } else if (object instanceof IProject) {
            // Projects are always beneath root
            ret = new TreeNode<>(root, object);

        } else if (object instanceof IResource) {
            if ((this.groupWith & GROUP_WITH_FOLDERS) != 0) {
                // If we got a resource use its parent
                IResource resource = (IResource) object;
                IContainer parent = resource.getParent();
                if (parent instanceof IProject) {
                    if ((this.groupWith & GROUP_WITH_PROJECT) != 0) {
                        TreeNode parentNode = obtainTeeNodeElement(parent);
                        ret = new TreeNode<>(parentNode, object);
                    } else {
                        // Already at root
                        ret = new TreeNode<>(root, object);
                    }
                } else {
                    TreeNode parentNode = obtainTeeNodeElement(parent);
                    ret = new TreeNode<>(parentNode, object);
                }

            } else {
                // Already at root
                ret = new TreeNode<>(root, object);
            }
        }

        if (ret == null) {
            Log.log("Unhandled: " + object + " group by: " + this.groupWith);
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
