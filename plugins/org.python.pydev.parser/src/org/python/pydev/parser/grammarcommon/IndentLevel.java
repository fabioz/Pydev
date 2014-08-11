/******************************************************************************
* Copyright (C) 2014  Brainwy Software Ltda
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.parser.grammarcommon;

/**
 * Important: note that accessing level/indentation vars is allowed, but raising the indent level should
 * always be done through pushLevel (decreasing should be done directly through changing the level var).
 */
public final class IndentLevel {

    public int indentation[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }; //Initially allocate for 10 indentation levels.

    /**
     * The current indentation level.
     */
    public int level = 0;

    public void pushLevel(int indent) {
        level += 1;
        if (indentation.length <= level) {
            int[] newstack = new int[indentation.length * 2];
            System.arraycopy(indentation, 0, newstack, 0, indentation.length);
            indentation = newstack;
        }
        this.indentation[level] = indent;
    }

    public int atLevel() {
        return this.indentation[level];
    }
}
