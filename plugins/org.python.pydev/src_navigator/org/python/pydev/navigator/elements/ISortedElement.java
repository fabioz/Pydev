/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.elements;

class counter {
    private static int curr = -1;

    static int next() {
        curr += 1;
        return curr;
    }
}

public interface ISortedElement {

    int RANK_ERROR = counter.next();

    int RANK_SOURCE_FOLDER = counter.next();
    int RANK_PYTHON_FOLDER = counter.next();

    int RANK_PYTHON_FILE = counter.next();
    int RANK_PYTHON_RESOURCE = counter.next();

    int RANK_REGULAR_FOLDER = counter.next();
    int RANK_REGULAR_FILE = counter.next();
    int RANK_REGULAR_RESOURCE = counter.next();

    int RANK_LIBS = counter.next();

    int RANK_PYTHON_NODE = counter.next();

    //Used if we don't know how to categorize it.
    int UNKNOWN_ELEMENT = counter.next();

    //Tree nodes come after everything
    int RANK_TREE_NODE = counter.next();

    /**
     * @return the ranking for the object. Lower values have higher priorities
     */
    int getRank();
}
