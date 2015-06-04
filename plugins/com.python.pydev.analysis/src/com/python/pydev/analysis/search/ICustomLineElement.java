package com.python.pydev.analysis.search;

import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

public interface ICustomLineElement {

    int getLine();

    Match[] getMatches(AbstractTextSearchResult input);

    String getContents();

    int getOffset();

    int getLength();

    Object getParent();

    int getNumberOfMatches(AbstractTextSearchResult input);

}
