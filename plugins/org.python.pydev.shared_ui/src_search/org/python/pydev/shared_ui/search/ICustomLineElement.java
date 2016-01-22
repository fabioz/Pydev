/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
