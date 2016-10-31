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
import org.python.pydev.parser.jython.ast.Starred;
import org.python.pydev.parser.jython.ast.Str;

public final class NullPythonGrammarActions implements IPythonGrammarActions {

    @Override
    public void markDecoratorWithCall() {
    }

    @Override
    public ISpecialStr convertStringToSpecialStr(Object o) throws ParseException {
        return null;
    }

    @Override
    public void addToPeekCallFunc(Object t, boolean after) {

    }

    @Override
    public void addSpecialTokenToLastOpened(Object o) throws ParseException {

    }

    @Override
    @SuppressWarnings("rawtypes")
    public void addToPeek(SimpleNode peeked, Object t, boolean after, Class class_) throws ParseException {

    }

    @Override
    @SuppressWarnings("rawtypes")
    public SimpleNode addToPeek(Object t, boolean after, Class class_) throws ParseException {
        return null;
    }

    @Override
    public void jjtreeCloseNodeScope(Node n) throws ParseException {

    }

    @Override
    public void addSpecialToken(Object o, int strategy) throws ParseException {

    }

    @Override
    public void addSpecialToken(Object o) throws ParseException {

    }

    @Override
    public void makeFloat(Token t, Num numberToFill) throws ParseException {

    }

    public void makeLong(Token t, Num numberToFill) throws ParseException {

    }

    @Override
    public void makeComplex(Token t, Num numberToFill) throws ParseException {

    }

    @Override
    public void makeString(Token t, int quotes, Str strToFill) {

    }

    @Override
    public void findTokenAndAdd(String token) throws ParseException {

    }

    @Override
    public void addSpecialToPrev(Object special, boolean after) {

    }

    @Override
    public ISpecialStr createSpecialStr(String token) throws ParseException {

        return null;
    }

    @Override
    public ISpecialStr createSpecialStr(String token, boolean searchOnLast) throws ParseException {

        return null;
    }

    @Override
    public ISpecialStr createSpecialStr(String token, boolean searchOnLast, boolean throwException)
            throws ParseException {

        return null;
    }

    @Override
    public void addToPeek(Object t, boolean after) throws ParseException {

    }

    @Override
    public void makeInt(Token t, int radix, Token token, Num numberToFill) throws ParseException {

    }

    @Override
    public void makeIntSub2(Token t, int radix, Token token, Num numberToFill) throws ParseException {

    }

    @Override
    public void makeIntSub2CheckingOct(Token t, int radix, Token token, Num numberToFill) throws ParseException {
    }

    @Override
    public void setImportFromLevel(int level) {

    }

    @Override
    public void popStarExpr() {

    }

    @Override
    public void pushStarExpr(int store) {

    }

    @Override
    public int getStarExprScope() {
        return Starred.Load;
    }

}
