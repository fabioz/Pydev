/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

/**
 * Line part that marks the start or end of a statement in the document.
 */
public interface ILinePartStatementMark {

    /**
     * @return true if we're starting the statement and false otherwise.
     */
    public boolean isStart();

}
