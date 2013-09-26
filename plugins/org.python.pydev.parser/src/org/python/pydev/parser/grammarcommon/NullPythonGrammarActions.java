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

import org.python.pydev.parser.jython.ISpecialStr;
import org.python.pydev.parser.jython.Node;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Str;

public final class NullPythonGrammarActions implements IPythonGrammarActions {

    public void markDecoratorWithCall() {
    }

    public ISpecialStr convertStringToSpecialStr(Object o) throws ParseException {
        return null;
    }

    public void addToPeekCallFunc(Object t, boolean after) {

    }

    public void addSpecialTokenToLastOpened(Object o) throws ParseException {

    }

    @SuppressWarnings("rawtypes")
    public void addToPeek(SimpleNode peeked, Object t, boolean after, Class class_) throws ParseException {

    }

    @SuppressWarnings("rawtypes")
    public SimpleNode addToPeek(Object t, boolean after, Class class_) throws ParseException {
        return null;
    }

    public void jjtreeCloseNodeScope(Node n) throws ParseException {

    }

    public void addSpecialToken(Object o, int strategy) throws ParseException {

    }

    public void addSpecialToken(Object o) throws ParseException {

    }

    public void makeFloat(Token t, Num numberToFill) throws ParseException {

    }

    public void makeLong(Token t, Num numberToFill) throws ParseException {

    }

    public void makeComplex(Token t, Num numberToFill) throws ParseException {

    }

    public void makeString(Token t, int quotes, Str strToFill) {

    }

    public void findTokenAndAdd(String token) throws ParseException {

    }

    public void addSpecialToPrev(Object special, boolean after) {

    }

    public ISpecialStr createSpecialStr(String token) throws ParseException {

        return null;
    }

    public ISpecialStr createSpecialStr(String token, boolean searchOnLast) throws ParseException {

        return null;
    }

    public ISpecialStr createSpecialStr(String token, boolean searchOnLast, boolean throwException)
            throws ParseException {

        return null;
    }

    public void addToPeek(Object t, boolean after) throws ParseException {

    }

    public void makeInt(Token t, int radix, Token token, Num numberToFill) throws ParseException {

    }

    public void makeIntSub2(Token t, int radix, Token token, Num numberToFill) throws ParseException {

    }

    public void makeIntSub2CheckingOct(Token t, int radix, Token token, Num numberToFill) throws ParseException {
    }

    public void setImportFromLevel(int level) {

    }

}
