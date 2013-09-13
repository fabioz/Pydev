/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import java.util.ArrayList;
import java.util.Collections;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.comprehensionType;

public final class ComprehensionCollection extends SimpleNode {

    public ArrayList<Comprehension> added = new ArrayList<Comprehension>();

    public comprehensionType[] getGenerators() {
        ArrayList<Comprehension> f = added;
        added = null;
        Collections.reverse(f);
        return f.toArray(new comprehensionType[0]);
    }
}