/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.ArrayList;

import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;

public class AssignCompletionInfo {

    public final ArrayList<IToken> completions;
    public final Definition[] defs;

    public AssignCompletionInfo(Definition[] defs, ArrayList<IToken> ret) {
        this.defs = defs;
        this.completions = ret;
    }

}
