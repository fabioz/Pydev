/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion.revisited;

import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.core.TokensList;

public class AssignCompletionInfo {

    public final TokensList completions;
    public final Definition[] defs;

    public AssignCompletionInfo(Definition[] defs, TokensList ret) {
        this.defs = defs;
        this.completions = ret;
    }

}
