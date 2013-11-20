/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.search;

import java.lang.reflect.Field;
import java.util.Set;

import org.eclipse.search.ui.SearchResultEvent;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.search.ui.text.MatchEvent;
import org.python.pydev.core.log.Log;

import com.python.pydev.refactoring.refactorer.search.copied.FileSearchPage;

public class FindOccurrencesSearchResultPage extends FileSearchPage {

    private static boolean logged = false;

    /**
     * Handles a search result event for the current search result.
     * 
     * @since 3.2
     */
    protected void handleSearchResultChanged(final SearchResultEvent e) {
        if (e instanceof MatchEvent) {
            try {
                //Don't you just HATE when the field you want is not accessible?
                //That's not really needed in eclipse 3.4 (as it'll already call the evaluateChangedElements), but
                //it should do no harm either.
                //
                //If postUpdate was protected, that'd be a good alternative too.
                Field field = AbstractTextSearchViewPage.class.getDeclaredField("fBatchedUpdates");
                field.setAccessible(true);
                Set set = (Set) field.get(this);

                MatchEvent matchEvent = ((MatchEvent) e);
                Match[] matches = matchEvent.getMatches();
                for (int i = 0; i < matches.length; i++) {
                    set.add(matches[i].getElement());
                }

                evaluateChangedElements(matches, set);
            } catch (Throwable e1) {
                if (!logged) {
                    logged = true; //just log it once.
                    Log.log(e1);
                }
            }
        }
        super.handleSearchResultChanged(e);
    }

}
