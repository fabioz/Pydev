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

public interface IPythonGrammarActions {

    void markDecoratorWithCall();

    ISpecialStr convertStringToSpecialStr(Object o) throws ParseException;

    public ISpecialStr createSpecialStr(String token) throws ParseException;

    public ISpecialStr createSpecialStr(String token, boolean searchOnLast) throws ParseException;

    public ISpecialStr createSpecialStr(String token, boolean searchOnLast, boolean throwException)
            throws ParseException;

    /**
     * Adds a special token to the current token that's in the top of the stack (the peeked token)
     * Considers that the token at the stack is a Call and adds it to its function.
     */
    void addToPeekCallFunc(Object t, boolean after);

    void addSpecialTokenToLastOpened(Object o) throws ParseException;

    void addToPeek(Object t, boolean after) throws ParseException;

    @SuppressWarnings("rawtypes")
    void addToPeek(SimpleNode peeked, Object t, boolean after, Class class_) throws ParseException;

    @SuppressWarnings("rawtypes")
    SimpleNode addToPeek(Object t, boolean after, Class class_) throws ParseException;

    void jjtreeCloseNodeScope(Node n) throws ParseException;

    void addSpecialToken(Object o, int strategy) throws ParseException;

    void addSpecialToken(Object o) throws ParseException;

    /**
     * @param t the string found without any preceding char to identify the radix.
     * @param radix the radix in which it was found (octal=8, decimal=10, hex=16)
     * @param token this is the image of the object (the exact way it was found in the file)
     * @param numberToFill the Num object that should be set given the other parameters
     * @throws ParseException
     */
    void makeInt(Token t, int radix, Token token, Num numberToFill) throws ParseException;

    void makeIntSub2(Token t, int radix, Token token, Num numberToFill) throws ParseException;

    void makeIntSub2CheckingOct(Token t, int radix, Token token, Num numberToFill) throws ParseException;

    void makeFloat(Token t, Num numberToFill) throws ParseException;

    void makeComplex(Token t, Num numberToFill) throws ParseException;

    /**
     * Fills the string properly according to the representation found.
     *
     * 0 = the string
     * 1 = boolean indicating unicode
     * 2 = boolean indicating raw
     * 3 = style
     * 4 = boolean indicating binary
     */
    void makeString(Token t, int quotes, Str strToFill);

    ISpecialStr findTokenAndAdd(String token) throws ParseException;

    void addSpecialToPrev(Object special, boolean after);

    void setImportFromLevel(int level);

    void popStarExpr();

    // Starred.Store or Starred.Load
    void pushStarExpr(int store);

    int getStarExprScope();

    void markEndDefColon(ISpecialStr s, SimpleNode node);

}
