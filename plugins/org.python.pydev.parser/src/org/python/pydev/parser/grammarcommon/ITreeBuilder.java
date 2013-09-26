/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.grammarcommon;

import org.python.pydev.parser.jython.SimpleNode;

public interface ITreeBuilder {

    public static final boolean DEBUG_TREE_BUILDER = false;

    public abstract SimpleNode closeNode(SimpleNode sn, int num) throws Exception;

    public abstract SimpleNode openNode(int jjtfileInput);

    public abstract SimpleNode getLastOpened();

}
