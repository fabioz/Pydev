/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 29, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.actions;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.navigator.CommonViewer;
import org.eclipse.ui.navigator.ILinkHelper;
import org.eclipse.ui.part.FileEditorInput;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.navigator.InterpreterInfoTreeNodeRoot;
import org.python.pydev.navigator.PythonpathTreeNode;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.shared_core.structure.TreeNode;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class PythonLinkHelper implements ILinkHelper {

    private WeakReference<CommonViewer> commonViewer;

    public void setCommonViewer(CommonViewer commonViewer) {
        this.commonViewer = new WeakReference<CommonViewer>(commonViewer);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.navigator.ILinkHelper#findSelection(org.eclipse.ui.IEditorInput)
     */
    public IStructuredSelection findSelection(IEditorInput anInput) {
        if (anInput instanceof IFileEditorInput) {
            return new StructuredSelection(((IFileEditorInput) anInput).getFile());
        }

        if (anInput instanceof IAdaptable) {
            //handles org.eclipse.compare.CompareEditorInput without a specific reference to it
            Object adapter = anInput.getAdapter(IFile.class);
            if (adapter != null) {
                return new StructuredSelection(adapter);
            }
        }

        return StructuredSelection.EMPTY;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.navigator.ILinkHelper#activateEditor(org.eclipse.ui.IWorkbenchPage, org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void activateEditor(IWorkbenchPage aPage, IStructuredSelection aSelection) {
        if (aSelection == null || aSelection.isEmpty()) {
            return;
        }

        Object firstElement = aSelection.getFirstElement();

        //if it is a python element, let's first get the actual object for finding the editor
        if (firstElement instanceof IWrappedResource) {
            IWrappedResource resource = (IWrappedResource) firstElement;
            firstElement = resource.getActualObject();
        }

        //and now, if it is really a file...
        if (firstElement instanceof IFile) {

            //ok, let's check if the active editor is already the selection, because although the findEditor(editorInput) method
            //may return an editor for the correct file, we may have multiple editors for the same file, and if the current
            //editor is already correct, we don't want to change it
            //@see bug: https://sourceforge.net/tracker/?func=detail&atid=577329&aid=2037682&group_id=85796
            IEditorPart activeEditor = aPage.getActiveEditor();
            if (activeEditor != null) {
                IEditorInput editorInput = activeEditor.getEditorInput();
                IFile currFile = (IFile) editorInput.getAdapter(IFile.class);
                if (currFile != null && currFile.equals(firstElement)) {
                    return; //the current editor is already the active editor.
                }
            }

            //if we got here, the active editor is not a match, so, let's find one and show it.
            IEditorPart editor = null;
            IEditorInput fileInput = new FileEditorInput((IFile) firstElement);
            if ((editor = aPage.findEditor(fileInput)) != null) {
                aPage.bringToTop(editor);
            }
        }

    }

    /**
     * Here we'll try to make a show -> in for an external file. The idea is that we'll traverse the 
     * available InterpreterInfoTreeNodeRoot's found until we find a match.
     * 
     * Some things need to be taken care of thought:
     * 
     * 1. The same interpreter may appear multiple times, so, if we pass one interpreter once, we should not try to
     * traverse any other place where the same interpreter is configured.
     * 
     * 2. As an interpreter may appear multiple times, we have to use some heuristic in order to decide which one will
     * be searched first. This is done through:
     * 
     *  - looking for the current selection (i.e.: try to get the file in the same project in an existing selection)
     *  - looking at opened editors (i.e.: try to get the file in the same project of an existing editor)
     *  
     * if that fails, we should go through what's visible in the package explorer and if that still fails, maybe
     * show an error to the user.
     * 
     * 3. We may need to look into zip files too.
     */
    public IStructuredSelection findExternalFileSelection(File f) {
        if (this.commonViewer == null) {
            return null;
        }
        CommonViewer commonViewer = this.commonViewer.get();
        if (commonViewer == null) {
            return null;
        }

        ISelection treeSelection = commonViewer.getSelection();

        Set<IInterpreterInfo> infosSearched = new HashSet<IInterpreterInfo>();

        IContentProvider contentProvider = commonViewer.getContentProvider();
        ITreeContentProvider treeContentProvider;
        if (contentProvider instanceof ITreeContentProvider) {
            treeContentProvider = (ITreeContentProvider) contentProvider;
        } else {
            Log.log("On tryToRevealExternalFile, the common viewer content provider is not an ITreeContentProvider. Found: "
                    + contentProvider);
            return null;
        }

        //Step 1: look into a selection
        if (treeSelection instanceof IStructuredSelection && !treeSelection.isEmpty()) {
            IStructuredSelection structuredSelection = (IStructuredSelection) treeSelection;
            Iterator it = structuredSelection.iterator();
            while (it.hasNext()) {
                Object next = it.next();
                IStructuredSelection sel = findExternalFileSelectionGivenTreeSelection(f, commonViewer,
                        treeContentProvider, infosSearched, next);
                if (sel != null && !sel.isEmpty()) {
                    return sel;
                }
            }
        }
        //Step 2: look into what's expanded in the package explorer
        Object[] expandedElements = commonViewer.getVisibleExpandedElements();
        for (Object expandedElement : expandedElements) {
            IStructuredSelection sel = findExternalFileSelectionGivenTreeSelection(f, commonViewer,
                    treeContentProvider, infosSearched, expandedElement);
            if (sel != null && !sel.isEmpty()) {
                return sel;
            }

        }

        //Step 3: look into existing editors
        Set<IFile> openFiles = PyAction.getOpenFiles();
        for (IFile iFile : openFiles) {
            IStructuredSelection sel = findExternalFileSelectionGivenTreeSelection(f, commonViewer,
                    treeContentProvider, infosSearched, iFile);
            if (sel != null && !sel.isEmpty()) {
                return sel;
            }
        }

        //Step 4: look into what's available in the package explorer
        Object input = commonViewer.getInput();
        for (Object child : treeContentProvider.getChildren(input)) {
            IStructuredSelection sel = findExternalFileSelectionGivenTreeSelection(f, commonViewer,
                    treeContentProvider, infosSearched, child);
            if (sel != null && !sel.isEmpty()) {
                return sel;
            }
        }

        //If all failed, just return null!
        return null;
    }

    private IStructuredSelection findExternalFileSelectionGivenTreeSelection(File f, CommonViewer commonViewer,
            ITreeContentProvider treeContentProvider, Set<IInterpreterInfo> infosSearched, final Object next) {

        if (next instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) next;
            IResource resource = (IResource) adaptable.getAdapter(IResource.class);
            if (resource != null) {
                IProject project = resource.getProject();
                if (project != null) {
                    Object[] children = treeContentProvider.getChildren(project);
                    for (Object object : children) {
                        if (object instanceof InterpreterInfoTreeNodeRoot) {
                            IStructuredSelection sel = findMatchInTreeNodeRoot(f, commonViewer,
                                    (InterpreterInfoTreeNodeRoot) object, infosSearched);
                            if (sel != null) {
                                return sel;
                            }
                        }
                    }
                    return null;
                }
            }
            //Keep on going to try to find a parent that'll adapt to IResource...

        }

        if (next instanceof TreeNode) {
            TreeNode treeNode = (TreeNode) next;
            while (true) {
                if (treeNode instanceof InterpreterInfoTreeNodeRoot) {
                    IStructuredSelection sel = findMatchInTreeNodeRoot(f, commonViewer,
                            (InterpreterInfoTreeNodeRoot) treeNode, infosSearched);
                    if (sel != null) {
                        return sel;
                    }
                    return null;
                }
                if (treeNode instanceof PythonpathTreeNode) {
                    PythonpathTreeNode pythonpathTreeNode = (PythonpathTreeNode) treeNode;
                    if (f.equals(pythonpathTreeNode.file)) {
                        return new StructuredSelection(treeNode);
                    }
                }
                Object parent = treeNode.getParent();
                if (parent instanceof TreeNode) {
                    treeNode = (TreeNode) parent;
                } else {
                    break;
                }
            }
            //Couldn't find a proper InterpreterInfoTreeNodeRoot already having a TreeNode? Let's log it, as a TreeNode
            //should always map to an InterpreterInfoTreeNodeRoot.
            Log.log("Couldn't find a proper InterpreterInfoTreeNodeRoot already having TreeNode: " + next);
            return null;
        }

        //Some unexpected type... let's get its parent until we find one expected (or just end up trying if we get to the root).
        Object parent = next;
        int i = 200;
        //just playing safe to make sure we won't get into a recursion (the tree should never group up to 200 levels,
        //so, this is likely a problem in the content provider).
        while (i > 0) {
            i--;
            if (i == 0) {
                Log.log("Found a recursion for the element: " + next
                        + " when searching parents. Please report this a a bug!");
            }
            if (parent == null || parent instanceof IWorkspaceRoot || parent instanceof IWorkingSet) {
                break;
            }
            if (parent instanceof TreeNode && parent != next) {
                return findExternalFileSelectionGivenTreeSelection(f, commonViewer, treeContentProvider, infosSearched,
                        parent);
            } else if (parent instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable) parent;
                IResource resource = (IResource) adaptable.getAdapter(IResource.class);
                if (resource != null) {
                    IProject project = resource.getProject();
                    if (project != null && project != next) {
                        return findExternalFileSelectionGivenTreeSelection(f, commonViewer, treeContentProvider,
                                infosSearched, project);
                    }
                }
            }
            parent = treeContentProvider.getParent(parent);
        }

        return null;
    }

    /**
     * Tries to find a match for the element in the given root passed. If found returns true.
     * 
     * @param infosSearched: a memo to know which infos were already searched to prevent searching many times in
     * the same place.
     */
    private IStructuredSelection findMatchInTreeNodeRoot(File element, CommonViewer commonViewer,
            InterpreterInfoTreeNodeRoot treeNodeRoot, Set<IInterpreterInfo> infosSearched) {
        if (infosSearched.contains(treeNodeRoot.interpreterInfo)) {
            return null;
        }
        infosSearched.add(treeNodeRoot.interpreterInfo);

        List<TreeNode> nodesOrderedForFileSearch = treeNodeRoot.getNodesOrderedForFileSearch();
        for (TreeNode node : nodesOrderedForFileSearch) {
            PythonpathTreeNode match = findMatch(node, element);
            if (match != null) {
                return new StructuredSelection(match);
            }
        }
        return null;
    }

    /**
     * Recursively iterates a tree node structure from parent -> children to find a match for the given element.
     * The match is returned if found (null is returned if not found).
     */
    private PythonpathTreeNode findMatch(TreeNode treeNode, Object element) {
        if (treeNode instanceof PythonpathTreeNode) {
            PythonpathTreeNode pythonpathTreeNode = (PythonpathTreeNode) treeNode;
            if (element.equals(pythonpathTreeNode.file)) {
                return pythonpathTreeNode;
            }
        }
        List<TreeNode> children = treeNode.getChildren();
        for (TreeNode object : children) {
            PythonpathTreeNode m = findMatch(object, element);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

}
