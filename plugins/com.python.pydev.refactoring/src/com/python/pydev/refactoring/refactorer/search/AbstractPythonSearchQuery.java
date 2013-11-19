/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.search;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.Match;

import com.python.pydev.refactoring.refactorer.search.copied.PatternConstructor;
import com.python.pydev.refactoring.refactorer.search.copied.SearchResultUpdater;
import com.python.pydev.ui.search.FileMatch;
import com.python.pydev.ui.search.LineElement;

public abstract class AbstractPythonSearchQuery implements ISearchQuery {

    public AbstractPythonSearchQuery(String searchText) {
        this(searchText, false, true, null);
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

    private final static class TextSearchResultCollector extends TextSearchRequestor {

        private final AbstractTextSearchResult fResult;
        private final boolean fIsFileSearchOnly;
        private final boolean fSearchInBinaries;
        private ArrayList<FileMatch> fCachedMatches;

        private TextSearchResultCollector(AbstractTextSearchResult result, boolean isFileSearchOnly,
                boolean searchInBinaries) {
            fResult = result;
            fIsFileSearchOnly = isFileSearchOnly;
            fSearchInBinaries = searchInBinaries;

        }

        public boolean acceptFile(IFile file) throws CoreException {
            if (fIsFileSearchOnly) {
                fResult.addMatch(new FileMatch(file));
            }
            flushMatches();
            return true;
        }

        /* (non-Javadoc)
         * @see org.eclipse.search.core.text.TextSearchRequestor#reportBinaryFile(org.eclipse.core.resources.IFile)
         */
        public boolean reportBinaryFile(IFile file) {
            return fSearchInBinaries;
        }

        public boolean acceptPatternMatch(TextSearchMatchAccess matchRequestor) throws CoreException {
            int matchOffset = matchRequestor.getMatchOffset();

            LineElement lineElement = getLineElement(matchOffset, matchRequestor);
            if (lineElement != null) {
                FileMatch fileMatch = new FileMatch(matchRequestor.getFile(), matchOffset,
                        matchRequestor.getMatchLength(), lineElement);
                fCachedMatches.add(fileMatch);
            }
            return true;
        }

        private LineElement getLineElement(int offset, TextSearchMatchAccess matchRequestor) {
            int lineNumber = 1;
            int lineStart = 0;
            if (!fCachedMatches.isEmpty()) {
                // match on same line as last?
                FileMatch last = (FileMatch) fCachedMatches.get(fCachedMatches.size() - 1);
                LineElement lineElement = last.getLineElement();
                if (lineElement.contains(offset)) {
                    return lineElement;
                }
                // start with the offset and line information from the last match
                lineStart = lineElement.getOffset() + lineElement.getLength();
                lineNumber = lineElement.getLine() + 1;
            }
            if (offset < lineStart) {
                return null; // offset before the last line
            }

            int i = lineStart;
            int contentLength = matchRequestor.getFileContentLength();
            while (i < contentLength) {
                char ch = matchRequestor.getFileContentChar(i++);
                if (ch == '\n' || ch == '\r') {
                    if (ch == '\r' && i < contentLength && matchRequestor.getFileContentChar(i) == '\n') {
                        i++;
                    }
                    if (offset < i) {
                        String lineContent = getContents(matchRequestor, lineStart, i); // include line delimiter
                        return new LineElement(matchRequestor.getFile(), lineNumber, lineStart, lineContent);
                    }
                    lineNumber++;
                    lineStart = i;
                }
            }
            if (offset < i) {
                String lineContent = getContents(matchRequestor, lineStart, i); // until end of file
                return new LineElement(matchRequestor.getFile(), lineNumber, lineStart, lineContent);
            }
            return null; // offset outside of range
        }

        private static String getContents(TextSearchMatchAccess matchRequestor, int start, int end) {
            StringBuffer buf = new StringBuffer();
            for (int i = start; i < end; i++) {
                char ch = matchRequestor.getFileContentChar(i);
                if (Character.isWhitespace(ch) || Character.isISOControl(ch)) {
                    buf.append(' ');
                } else {
                    buf.append(ch);
                }
            }
            return buf.toString();
        }

        public void beginReporting() {
            fCachedMatches = new ArrayList<FileMatch>();
        }

        public void endReporting() {
            flushMatches();
            fCachedMatches = null;
        }

        private void flushMatches() {
            if (!fCachedMatches.isEmpty()) {
                fResult.addMatches((Match[]) fCachedMatches.toArray(new Match[fCachedMatches.size()]));
                fCachedMatches.clear();
            }
        }
    }

    private final FileTextSearchScope fScope;
    private final String fSearchText;
    private final boolean fIsRegEx;
    private final boolean fIsCaseSensitive;

    private PythonFileSearchResult fResult;

    public AbstractPythonSearchQuery(String searchText, boolean isRegEx, boolean isCaseSensitive,
            FileTextSearchScope scope) {
        fSearchText = searchText;
        fIsRegEx = isRegEx;
        fIsCaseSensitive = isCaseSensitive;
        fScope = scope;
    }

    public FileTextSearchScope getSearchScope() {
        return fScope;
    }

    public IStatus run(final IProgressMonitor monitor) {
        AbstractTextSearchResult textResult = (AbstractTextSearchResult) getSearchResult();
        textResult.removeAll();

        Pattern searchPattern = getSearchPattern();
        boolean searchInBinaries = !isScopeAllFileTypes();

        TextSearchResultCollector collector = new TextSearchResultCollector(textResult, isFileNameSearch(),
                searchInBinaries);
        return TextSearchEngine.create().search(fScope, collector, searchPattern, monitor);
    }

    public String getSearchString() {
        return fSearchText;
    }

    /**
     * @param result all result are added to this search result
     * @param monitor the progress monitor to use
     * @param file the file to search in
     * @return returns the status of the operation
     */
    public IStatus searchInFile(final AbstractTextSearchResult result, final IProgressMonitor monitor, IFile file) {
        FileTextSearchScope scope = FileTextSearchScope.newSearchScope(new IResource[] { file },
                new String[] { "*" }, true); //$NON-NLS-1$

        Pattern searchPattern = getSearchPattern();
        TextSearchResultCollector collector = new TextSearchResultCollector(result, isFileNameSearch(), true);

        return TextSearchEngine.create().search(scope, collector, searchPattern, monitor);
    }

    protected Pattern getSearchPattern() {
        return PatternConstructor.createPattern(fSearchText, fIsCaseSensitive, fIsRegEx);
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
