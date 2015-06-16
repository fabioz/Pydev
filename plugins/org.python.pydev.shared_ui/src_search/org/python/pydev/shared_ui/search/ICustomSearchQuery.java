package org.python.pydev.shared_ui.search;

import org.eclipse.search.ui.ISearchQuery;

public interface ICustomSearchQuery extends ISearchQuery {

    String getSearchString();

    boolean isCaseSensitive();

    boolean isRegexSearch();

}
