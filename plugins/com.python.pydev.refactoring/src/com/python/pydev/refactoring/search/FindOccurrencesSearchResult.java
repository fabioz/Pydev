/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.search;

import com.python.pydev.refactoring.refactorer.search.AbstractPythonSearchQuery;
import com.python.pydev.refactoring.refactorer.search.PythonFileSearchResult;

public class FindOccurrencesSearchResult extends PythonFileSearchResult {

    public FindOccurrencesSearchResult(AbstractPythonSearchQuery query) {
        super(query);
    }
}
