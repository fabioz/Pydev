package org.python.pydev.shared_ui.search;

import org.eclipse.core.resources.IResource;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

public interface ICustomLineElement {

    int getLine();

    Match[] getMatches(AbstractTextSearchResult input);

    String getContents();

    int getOffset();

    int getLength();

    IResource getParent();

    int getNumberOfMatches(AbstractTextSearchResult input);

}
