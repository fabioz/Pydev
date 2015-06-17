package org.python.pydev.shared_ui.search;

import org.eclipse.search.ui.ISearchQuery;
import org.python.pydev.shared_core.string.StringUtils;

public abstract class AbstractSearchIndexQuery implements ISearchQuery, ICustomSearchQuery {

    public final String text;

    protected ScopeAndData scopeAndData;

    public AbstractSearchIndexQuery(String text) {
        this.text = text;
        this.scopeAndData = new ScopeAndData(SearchIndexData.SCOPE_WORKSPACE, "");
    }

    public String getResultLabel(int nMatches) {
        String searchString = text;
        if (nMatches == 1) {
            return StringUtils.format("%s - 1 match", searchString);
        }
        return StringUtils.format("%s - %s matches", searchString, nMatches);
    }

}
