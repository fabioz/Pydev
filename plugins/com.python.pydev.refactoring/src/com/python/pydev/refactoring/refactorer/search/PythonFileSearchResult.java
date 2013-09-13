/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.search;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.internal.ui.SearchPluginImages;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

public class PythonFileSearchResult extends AbstractTextSearchResult implements IEditorMatchAdapter, IFileMatchAdapter {

    private final Match[] EMPTY_ARR = new Match[0];

    private AbstractPythonSearchQuery fQuery;

    public PythonFileSearchResult(AbstractPythonSearchQuery query) {
        fQuery = query;
    }

    public ImageDescriptor getImageDescriptor() {
        return SearchPluginImages.DESC_OBJ_TSEARCH_DPDN;
    }

    public String getLabel() {
        return fQuery.getResultLabel(getMatchCount());
    }

    public String getTooltip() {
        return getLabel();
    }

    public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
        return getMatches(file);
    }

    public IFile getFile(Object element) {
        if (element instanceof IFile)
            return (IFile) element;
        return null;
    }

    public boolean isShownInEditor(Match match, IEditorPart editor) {
        IEditorInput ei = editor.getEditorInput();
        if (ei instanceof IFileEditorInput) {
            IFileEditorInput fi = (IFileEditorInput) ei;
            return match.getElement().equals(fi.getFile());
        }
        return false;
    }

    public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
        IEditorInput ei = editor.getEditorInput();
        if (ei instanceof IFileEditorInput) {
            IFileEditorInput fi = (IFileEditorInput) ei;
            return getMatches(fi.getFile());
        }
        return EMPTY_ARR;
    }

    public ISearchQuery getQuery() {
        return fQuery;
    }

    public IFileMatchAdapter getFileMatchAdapter() {
        return this;
    }

    public IEditorMatchAdapter getEditorMatchAdapter() {
        return this;
    }
}
