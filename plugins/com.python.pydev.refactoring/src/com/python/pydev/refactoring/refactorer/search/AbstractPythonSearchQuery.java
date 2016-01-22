/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.search;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.python.pydev.shared_ui.search.ICustomSearchQuery;
import org.python.pydev.shared_ui.search.replace.PatternConstructor;

import com.python.pydev.refactoring.refactorer.search.copied.SearchResultUpdater;

public abstract class AbstractPythonSearchQuery implements ISearchQuery, ICustomSearchQuery {

    public AbstractPythonSearchQuery(String searchText) {
        this(searchText, false, true, true, null);
    }

    public boolean canRerun() {
        return false;
    }

    public boolean canRunInBackground() {
        return true;
    }

    public String getLabel() {
        return "Python Search";
    }

    protected boolean isScopeAllFileTypes() {
        return false;
    }

    public abstract String getResultLabel(int nMatches);

    private final FileTextSearchScope fScope;
    private final String fSearchText;
    private final boolean fIsRegEx;
    private final boolean fIsCaseSensitive;

    private PythonFileSearchResult fResult;
    private boolean fIsWholeWord;

    public AbstractPythonSearchQuery(String searchText, boolean isRegEx, boolean isCaseSensitive, boolean isWholeWord,
            FileTextSearchScope scope) {
        fSearchText = searchText;
        fIsRegEx = isRegEx;
        fIsCaseSensitive = isCaseSensitive;
        fIsWholeWord = isWholeWord;
        fScope = scope;
    }

    public FileTextSearchScope getSearchScope() {
        return fScope;
    }

    public abstract IStatus run(final IProgressMonitor monitor);

    public String getSearchString() {
        return fSearchText;
    }

    protected Pattern getSearchPattern() {
        return PatternConstructor.createPattern(fSearchText, fIsRegEx, true, fIsCaseSensitive, fIsWholeWord);
    }

    @Override
    public boolean isWholeWord() {
        return fIsWholeWord;
    }

    public boolean isFileNameSearch() {
        return fSearchText.length() == 0;
    }

    public boolean isRegexSearch() {
        return fIsRegEx;
    }

    public boolean isCaseSensitive() {
        return fIsCaseSensitive;
    }

    public ISearchResult getSearchResult() {
        if (fResult == null) {
            fResult = new PythonFileSearchResult(this);
            new SearchResultUpdater(fResult);
        }
        return fResult;
    }

}
