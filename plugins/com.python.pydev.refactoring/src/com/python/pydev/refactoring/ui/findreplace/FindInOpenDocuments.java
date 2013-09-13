/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.ui.findreplace;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.TextSearchQueryProvider;
import org.eclipse.search.ui.text.TextSearchQueryProvider.TextSearchInput;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_ui.EditorUtils;


/**
 * Helper to make a search in the currently opened documents.
 */
public class FindInOpenDocuments {

    /**
     * Here, all the editors available will be gotten and searched (if possible).
     * 
     * Note that editors that are not in the workspace may not be searched (it should be possible
     * to do, but one may have to reimplement large portions of the search for that to work).
     */
    public static void findInOpenDocuments(final String searchText, final boolean caseSensitive,
            final boolean wholeWord, final boolean isRegEx, IStatusLineManager statusLineManager) {

        IWorkbenchWindow window = EditorUtils.getActiveWorkbenchWindow();
        if (window == null) {
            if (statusLineManager != null)
                statusLineManager.setErrorMessage("Active workbench window is null.");
            return;
        }
        IWorkbenchPage activePage = window.getActivePage();
        if (activePage == null) {
            if (statusLineManager != null)
                statusLineManager.setErrorMessage("Active page is null.");
            return;
        }
        IEditorReference editorsArray[] = activePage.getEditorReferences();

        final List<IFile> files = new ArrayList<IFile>();
        for (int i = 0; i < editorsArray.length; i++) {
            IEditorPart realEditor = editorsArray[i].getEditor(true);
            if (realEditor != null) {
                if (realEditor instanceof MultiPageEditorPart) {
                    try {
                        Method getPageCount = MultiPageEditorPart.class.getDeclaredMethod("getPageCount");
                        getPageCount.setAccessible(true);
                        Method getEditor = MultiPageEditorPart.class.getDeclaredMethod("getEditor", int.class);
                        getEditor.setAccessible(true);

                        Integer pageCount = (Integer) getPageCount.invoke(realEditor);
                        for (int j = 0; j < pageCount; j++) {
                            IEditorPart part = (IEditorPart) getEditor.invoke(realEditor, j);
                            if (part != null) {
                                IEditorInput input = part.getEditorInput();
                                if (input != null) {
                                    IFile file = (IFile) input.getAdapter(IFile.class);
                                    if (file != null) {
                                        files.add(file);
                                    }
                                }
                            }
                        }
                    } catch (Throwable e1) {
                        //Log it but keep going on.
                        Log.log(e1);
                    }

                } else {
                    IEditorInput input = realEditor.getEditorInput();
                    if (input != null) {
                        IFile file = (IFile) input.getAdapter(IFile.class);
                        if (file != null) {
                            files.add(file);
                        } else {
                            //it has input, but it's not adaptable to an IFile!
                            if (statusLineManager != null)
                                statusLineManager
                                        .setMessage("Warning: Editors not in the workspace cannot be searched.");
                            //but we keep on going...
                        }
                    }
                }
            }
        }

        if (files.size() == 0) {
            if (statusLineManager != null)
                statusLineManager
                        .setMessage("No file was found to perform the search (editors not in the workspace cannot be searched).");
            return;
        }

        try {
            ISearchQuery query = TextSearchQueryProvider.getPreferred().createQuery(new TextSearchInput() {

                public boolean isRegExSearch() {
                    return isRegEx;
                }

                public boolean isCaseSensitiveSearch() {
                    return caseSensitive;
                }

                public String getSearchText() {
                    return searchText;
                }

                public FileTextSearchScope getScope() {
                    return FileTextSearchScope.newSearchScope(files.toArray(new IResource[files.size()]),
                            new String[] { "*" }, true);
                }
            });
            NewSearchUI.runQueryInBackground(query);
        } catch (CoreException e1) {
            Log.log(e1);
        }
    }
}
