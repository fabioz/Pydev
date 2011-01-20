/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
