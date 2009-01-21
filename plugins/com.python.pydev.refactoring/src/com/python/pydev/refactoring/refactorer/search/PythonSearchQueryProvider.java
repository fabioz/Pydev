package com.python.pydev.refactoring.refactorer.search;

import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;

import com.python.pydev.refactoring.refactorer.RefactorerFindReferences.PyTextSearchInput;


public class PythonSearchQueryProvider {

    public static ISearchQuery createQuery(PyTextSearchInput input) {
        FileTextSearchScope scope= input.getScope();
        String text= input.getSearchText();
        return new PythonFileSearchQuery(text, scope);
    }

}
