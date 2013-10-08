/******************************************************************************
* Copyright (C) 2005-2012  Fabio Zadrozny
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

import org.python.pydev.parser.jython.Node;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;

public abstract class AbstractJJTPythonGrammarState {

    public ITreeBuilder builder;

    public void openNodeScope(Node jjtn000) {

    }

    public void clearNodeScope(Node jjtn000) {

    }

    public SimpleNode popNode() {

        return null;
    }

    public void closeNodeScope(Node jjtn000, boolean b) throws ParseException {

    }

    public void closeNodeScope(Node jjtn005, int i) throws ParseException {

    }

    public int nodeArity() {

        return 0;
    }

    public SimpleNode getLastOpened() {

        return null;
    }

    public SimpleNode peekNode(int i) {

        return null;
    }

    public SimpleNode peekNode() {

        return null;
    }

}
