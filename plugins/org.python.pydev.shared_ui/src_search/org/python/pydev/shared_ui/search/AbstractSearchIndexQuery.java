/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.search;

import org.eclipse.search.ui.ISearchQuery;
import org.python.pydev.shared_core.string.StringUtils;

public abstract class AbstractSearchIndexQuery implements ISearchQuery, ICustomSearchQuery {

    public final String text;

    protected ScopeAndData scopeAndData;

    private boolean caseSensitive = true;

    public AbstractSearchIndexQuery(String text) {
        this.text = text;
        this.scopeAndData = new ScopeAndData(SearchIndexData.SCOPE_WORKSPACE, "");
    }

    public AbstractSearchIndexQuery(SearchIndexData data) {
        this.text = data.textPattern;
        this.caseSensitive = data.isCaseSensitive;
        this.scopeAndData = new ScopeAndData(data.scope, data.scopeData);
    }

    public boolean getIgnoreCase() {
        return !this.caseSensitive;
    }

    @Override
    public String getSearchString() {
        return this.text;
    }

    @Override
    public boolean isCaseSensitive() {
        return this.caseSensitive;
    }

    /**
     * Used for replace later on (we can't do a regexp replace because we don't have a pattern for ${0}, ${1}, ...)
     */
    @Override
    public boolean isRegexSearch() {
        return false;
    }

    public String getResultLabel(int nMatches) {
        String searchString = text;
        if (nMatches == 1) {
            return StringUtils.format("%s - 1 match", searchString);
        }
        return StringUtils.format("%s - %s matches", searchString, nMatches);
    }

    @Override
    public boolean canRerun() {
        return true;
    }

    @Override
    public boolean canRunInBackground() {
        return true;
    }

    public StringMatcherWithIndexSemantics createStringMatcher() {
        boolean ignoreCase = getIgnoreCase();
        StringMatcherWithIndexSemantics stringMatcher = new StringMatcherWithIndexSemantics(text, ignoreCase);
        return stringMatcher;
    }

}
