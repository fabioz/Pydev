package com.python.pydev.refactoring.search;

import com.python.pydev.refactoring.refactorer.search.AbstractPythonSearchQuery;
import com.python.pydev.refactoring.refactorer.search.PythonFileSearchResult;

public class FindOccurrencesSearchResult extends PythonFileSearchResult {

    public FindOccurrencesSearchResult(AbstractPythonSearchQuery query) {
        super(query);
    }
}
