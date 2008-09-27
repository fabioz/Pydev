package com.python.pydev.refactoring.refactorer.search;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.FileTextSearchScope;
import org.eclipse.search.ui.text.Match;
import org.python.pydev.core.docutils.StringUtils;

import com.python.pydev.ui.search.FileMatch;

/**
 * Based on the org.eclipse.search.internal.ui.text.FileSearchQuery
 */
public class PythonFileSearchQuery extends AbstractPythonSearchQuery implements ISearchQuery {
    
    private final static class TextSearchResultCollector extends TextSearchRequestor {
        
        private final AbstractTextSearchResult fResult;
        private final boolean fIsFileSearchOnly;
        private final boolean fSearchInBinaries;
        private ArrayList<Match> fCachedMatches;
        
        private TextSearchResultCollector(AbstractTextSearchResult result, boolean isFileSearchOnly, boolean searchInBinaries) {
            fResult= result;
            fIsFileSearchOnly= isFileSearchOnly;
            fSearchInBinaries= searchInBinaries;
            
        }
        
        public boolean acceptFile(IFile file) throws CoreException {
            if (fIsFileSearchOnly) {
                fResult.addMatch(new FileMatch(file, 0, 0));
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

        @SuppressWarnings("unchecked")
        public boolean acceptPatternMatch(TextSearchMatchAccess matchRequestor) throws CoreException {
            fCachedMatches.add(new FileMatch(matchRequestor.getFile(), matchRequestor.getMatchOffset(), matchRequestor.getMatchLength()));
            return true;
        }

        public void beginReporting() {
            fCachedMatches= new ArrayList<Match>();
        }
        
        public void endReporting() {
            flushMatches();
            fCachedMatches= null;
        }

        private void flushMatches() {
            if (!fCachedMatches.isEmpty()) {
                fResult.addMatches(fCachedMatches.toArray(new Match[fCachedMatches.size()]));
                fCachedMatches.clear();
            }
        }
    }
    
    private final FileTextSearchScope fScope;
    private PythonFileSearchResult fResult;
    
    public PythonFileSearchQuery(String searchText, FileTextSearchScope scope) {
        super(searchText);
        fScope= scope;
    }
    
    public FileTextSearchScope getSearchScope() {
        return fScope;
    }

    public IStatus run(final IProgressMonitor monitor) {
        AbstractTextSearchResult textResult= (AbstractTextSearchResult) getSearchResult();
        textResult.removeAll();
        
        boolean searchInBinaries= !isScopeAllFileTypes();
        
        TextSearchResultCollector collector= new TextSearchResultCollector(textResult, false, searchInBinaries);
        return new PythonTextSearchVisitor(collector, getSearchString()).search(fScope, monitor);
    }

    
    public String getResultLabel(int nMatches) {
        String searchString= getSearchString();
        if (searchString.length() > 0) {
            // text search
            if (isScopeAllFileTypes()) {
                // search all file extensions
                if (nMatches == 1) {
                    return StringUtils.format("%s - 1 match in %s", searchString, getDescription() );
                }
                return StringUtils.format("%s - {1} matches in %s", searchString, new Integer(nMatches), getDescription() ); 
            }
            // search selected file extensions
            if (nMatches == 1) {
                return StringUtils.format("%s - 1 match in %s (%s)", searchString, getDescription(), getFilterDescription() );
            }
            return StringUtils.format("%s - {1} matches in %s (%s)", searchString, new Integer(nMatches), getDescription(), getFilterDescription() );
        }
        // file search
        if (nMatches == 1) {
            return StringUtils.format("1 file name matching %s in %s", getFilterDescription(), getDescription() ); 
        }
        return StringUtils.format("%s file names matching %s in %s", getFilterDescription(), new Integer(nMatches), getDescription() ); 
    }

    
    protected String getFilterDescription() {
        return fScope.getFilterDescription();
    }
    
    protected String getDescription() {
        return fScope.getDescription();
    }


    public ISearchResult getSearchResult() {
        if (fResult == null) {
            fResult= new PythonFileSearchResult(this);
            new PythonSearchResultUpdater(fResult);
        }
        return fResult;
    }
}
