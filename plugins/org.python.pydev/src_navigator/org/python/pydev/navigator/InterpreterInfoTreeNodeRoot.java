/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.core.IInterpreterInfo;


/**
 * @author fabioz
 *
 */
public class InterpreterInfoTreeNodeRoot<X> extends InterpreterInfoTreeNode<X>{

    public final IInterpreterInfo interpreterInfo;

    public InterpreterInfoTreeNodeRoot(IInterpreterInfo interpreterInfo, Object parent, X data) {
        super(parent, data);
        this.interpreterInfo = interpreterInfo;
        Assert.isNotNull(interpreterInfo);
    }

}
