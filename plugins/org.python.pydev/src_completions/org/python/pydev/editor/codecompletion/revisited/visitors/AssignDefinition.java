/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 19, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModule;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.exprType;

public class AssignDefinition extends Definition {

    /**
     * This is the token name.
     */
    public final String target;

    /**
     * This is the position in the target.
     *
     * e.g. if we have:
     *
     * a, b = someCall()
     *
     * and we're looking for b, target pos would be 1
     * if we were looking for a, target pos would be 0
     */
    public final int targetPos;

    /**
     * Determines that a 'global' was added for the target before this assign
     */
    public boolean foundAsGlobal;

    /**
     * This is the value node found (can be used later to determine if it's a
     * Call or some regular attribute.
     */
    public final exprType nodeValue;

    /**
     * If it's an assign we should unpack.
     *
     * I.e.: a, b = x
     *
     * When searching for 'a' it'll be 0 and for 'b' it'll be 1 (if
     * it should not be unpacked, it'll be -1).
     */
    public final int unpackPos;

    /**
     * Constructor.
     *
     * @param assign
     * @param ast
     * @param line
     * @param col
     * @param nodeValue
     * @param unpackPos
     */
    public AssignDefinition(String value, String target, int targetPos, Assign ast, int line, int col,
            ILocalScope scope, IModule module, exprType nodeValue, int unpackPos) {
        super(line, col, value, ast, scope, module);
        this.target = target;
        this.targetPos = targetPos;
        this.nodeValue = nodeValue;
        this.unpackPos = unpackPos;
    }
}