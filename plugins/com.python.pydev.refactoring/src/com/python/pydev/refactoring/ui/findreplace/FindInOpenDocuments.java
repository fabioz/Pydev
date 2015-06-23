/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.ui.findreplace;

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

        final List<Object> opened = EditorUtils.getFilesInOpenEditors(statusLineManager);
        final List<IFile> files = new ArrayList<>(opened.size());
        for (Object object : opened) {
            if (object instanceof IFile) {
                files.add((IFile) object);
            }
        }

        if (files.size() == 0) {
            if (statusLineManager != null) {
                statusLineManager
                        .setMessage(
                                "No file was found to perform the search (editors not in the workspace cannot be searched).");
            }
            return;
        }

        try {
            ISearchQuery query = TextSearchQueryProvider.getPreferred().createQuery(new TextSearchInput() {

                @Override
                public boolean isRegExSearch() {
                    return isRegEx;
                }

                @Override
                public boolean isCaseSensitiveSearch() {
                    return caseSensitive;
                }

                @Override
                public String getSearchText() {
                    return searchText;
                }

                @Override
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
