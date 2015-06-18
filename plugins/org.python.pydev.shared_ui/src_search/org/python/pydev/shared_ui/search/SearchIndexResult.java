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
package org.python.pydev.shared_ui.search;

import java.util.HashSet;
import java.util.Set;

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
import org.python.pydev.shared_core.log.Log;

/**
 * Based on org.eclipse.search.internal.ui.text.FileSearchResult
 */
public abstract class SearchIndexResult extends AbstractTextSearchResult
        implements IEditorMatchAdapter, IFileMatchAdapter {

    private AbstractSearchIndexQuery query;
    private final Match[] EMPTY_ARR = new Match[0];

    public SearchIndexResult(AbstractSearchIndexQuery searchIndexQuery) {
        this.query = searchIndexQuery;
    }

    @Override
    public String getLabel() {
        return query.getResultLabel(getMatchCount());
    }

    @Override
    public String getTooltip() {
        return getLabel();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return SearchPluginImages.DESC_OBJ_TSEARCH_DPDN;
    }

    @Override
    public ISearchQuery getQuery() {
        return query;
    }

    @Override
    public IEditorMatchAdapter getEditorMatchAdapter() {
        return this;
    }

    @Override
    public IFileMatchAdapter getFileMatchAdapter() {
        return this;
    }

    @Override
    public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
        return getMatches(file);
    }

    private static final Set<Class<?>> warned = new HashSet<>();

    @Override
    public IFile getFile(Object element) {
        if (element instanceof IFile) {
            return (IFile) element;
        }
        if (element != null) {
            Class<? extends Object> class1 = element.getClass();
            warned.add(class1);
            Log.log("Unable to get file from: " + element + " - " + class1);

        }
        return null;
    }

    @Override
    public boolean isShownInEditor(Match match, IEditorPart editor) {
        IEditorInput ei = editor.getEditorInput();
        if (ei instanceof IFileEditorInput) {
            IFileEditorInput fi = (IFileEditorInput) ei;
            return match.getElement().equals(fi.getFile());
        }
        return false;
    }

    @Override
    public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
        IEditorInput ei = editor.getEditorInput();
        if (ei instanceof IFileEditorInput) {
            IFileEditorInput fi = (IFileEditorInput) ei;
            return getMatches(fi.getFile());
        }
        return EMPTY_ARR;
    }

}
