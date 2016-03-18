/******************************************************************************
* Copyright (C) 2012  Fabio Zadrozny
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

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Name;

public final class NullTreeBuilder implements ITreeBuilder {

    private final Name nameConstant = new Name("", Name.Load, false);

    @Override
    public SimpleNode closeNode(SimpleNode sn, int num) throws Exception {
        return null;
    }

    @Override
    public SimpleNode openNode(int id) {
        switch (id) {

            case ITreeConstants.JJTNAME:
            case ITreeConstants.JJTDOTTED_NAME:
                //If null is returned here, we may have an NPE.
                return nameConstant;

        }
        return null;
    }

    @Override
    public SimpleNode getLastOpened() {
        return null;
    }

}
