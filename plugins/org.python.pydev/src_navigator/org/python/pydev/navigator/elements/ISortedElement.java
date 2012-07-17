/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.elements;

public interface ISortedElement {

    int RANK_ERROR = -1;
    int RANK_SOURCE_FOLDER = 0;
    int RANK_PYTHON_FOLDER = 1;
    int RANK_LIBS = 2;
    int RANK_PYTHON_FILE = 3;
    int RANK_PYTHON_RESOURCE = 4;
    int RANK_PYTHON_NODE = 5;

    /**
     * @return the ranking for the object. Lower values have higher priorities
     */
    int getRank();
}
