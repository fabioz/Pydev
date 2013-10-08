/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core.parsing;

public interface IParserObserver3 {

    /**
     * Receives a structure (which can grow in attributes)
     */
    void parserChanged(ChangedParserInfoForObservers info);

    /**
     * Receives a structure (which can grow in attributes)
     */
    void parserError(ErrorParserInfoForObservers info);

}
