/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.visitors;

import org.python.pydev.parser.jython.ast.stmtType;

/**
 * @author fabioz
 *
 */
public class StopVisitingException extends Exception {

    public final stmtType lastStmtFound;

    /**
     * @param lastStmtFound
     */
    public StopVisitingException(stmtType lastStmtFound) {
        this.lastStmtFound = lastStmtFound;
    }

}
