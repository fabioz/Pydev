/******************************************************************************
* Copyright (C) 2008-2012  Fabio Zadrozny
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

import java.util.Iterator;
import java.util.List;

import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.ISpecialStr;
import org.python.pydev.parser.jython.Node;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.SpecialStr;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.decoratorsType;

public final class DefaultPythonGrammarActions implements IPythonGrammarActions {

    private final AbstractPythonGrammar grammar;
    private ISpecialStr lastSpecial;
    private SimpleNode lastNodeWithSpecial;
    private SimpleNode prev;

    /*default*/DefaultPythonGrammarActions(AbstractPythonGrammar grammar) {
        this.grammar = grammar;
    }

    public void markDecoratorWithCall() {
        decoratorsType d = (decoratorsType) this.prev;
        d.isCall = true;
    }

    public ISpecialStr convertStringToSpecialStr(Object o) throws ParseException {
        if (o instanceof ISpecialStr) {
            return (ISpecialStr) o;
        } else {
            if (o instanceof Token) {
                return ((Token) o).asSpecialStr();
            }
            return createSpecialStr(((String) o).trim(), AbstractPythonGrammar.DEFAULT_SEARCH_ON_LAST, false);
        }
    }

    private ParseException createException(String token, final Token currentToken) {
        ParseException e;
        //return put;
        if (currentToken != null) {
            e = new ParseException("Expected:" + token, currentToken);

        } else if (grammar.getJJLastPos() != null) {
            e = new ParseException("Expected:" + token, grammar.getJJLastPos());

        } else {
            e = new ParseException("Expected:" + token);
        }
        return e;
    }

    public void setImportFromLevel(int level) {
        ((ImportFrom) grammar.getJJTree().peekNode()).level = level;
    }

    public final ISpecialStr createSpecialStr(String token) throws ParseException {
        return createSpecialStr(token, AbstractPythonGrammar.DEFAULT_SEARCH_ON_LAST);
    }

    /**
     * This is where we do a lookahead to see if we find some token and if we do find it, but not on the correct
     * position, we skip some tokens to go to it.
     */
    public final ISpecialStr createSpecialStr(String token, boolean searchOnLast) throws ParseException {
        return createSpecialStr(token, searchOnLast, true);
    }

    /**
     * This is where we do a lookahead to see if we find some token and if we do find it, but not on the correct
     * position, we skip some tokens to go to it.
     */
    public ISpecialStr createSpecialStr(String token, boolean searchOnLast, boolean throwException)
            throws ParseException {
        final Token currentToken = grammar.getCurrentToken();

        Token firstTokenToIterate;
        if (searchOnLast) {
            firstTokenToIterate = grammar.getJJLastPos();
        } else {
            firstTokenToIterate = currentToken;
        }
        Token foundToken = null;

        int foundAtPos = 0;

        //lot's of tokens, but we'll bail out on an indent, or dedent, so, that's OK.
        TokensIterator iterTokens = grammar.getTokensIterator(firstTokenToIterate, 50, true);
        while (iterTokens.hasNext()) {
            foundAtPos += 1;
            Token next = iterTokens.next();
            if (next.image != null && next.image.equals(token)) {
                //Found what we were looking for!
                foundToken = next;
                break;
            }
        }

        if (foundToken != null) {
            if (foundAtPos <= 2 //found at correct position. 
                    || searchOnLast //we already matched it... right now we're just adding it to the stack!
            ) {
                return foundToken.asSpecialStr();

            }
        }

        if (throwException) {
            ParseException e = createException(token, currentToken);

            //we found it at the wrong position!
            if (foundToken != null) {
                //we found it, but not on the position we were expecting, so, we must skip some tokens to get to it --
                //and report the needed exception)
                grammar.addAndReport(e, "Found at wrong position: " + foundToken);
                Token beforeLastReturned = iterTokens.getBeforeLastReturned();
                grammar.setCurrentToken(beforeLastReturned);
                return foundToken.asSpecialStr();
            }

            //create a 'synthetic token' in the place we were expecting it.
            if (currentToken != null) {
                AbstractTokenManager tokenManager = grammar.getTokenManager();
                FastCharStream inputStream = tokenManager.getInputStream();

                final int created = tokenManager.addCustom(currentToken, token);
                if (created != AbstractTokenManager.CUSTOM_NOT_CREATED) {
                    if (created == AbstractTokenManager.CUSTOM_CREATED_WAS_PARENS) {
                        //if we had a parens, let's clear the tokens we iterated because we can have skipped indentations!
                        currentToken.next.next = null;

                        //EOF was already found... let's restore the previous indentation level!
                        if (tokenManager.levelBeforeEof != -1) {
                            tokenManager.indentation.level = tokenManager.levelBeforeEof;
                            tokenManager.levelBeforeEof = -1; //mark it as not found again.
                        }
                        inputStream.restoreLineColPos(currentToken.endLine, currentToken.endColumn);
                    }
                    grammar.addAndReport(e, "Created custom token: " + token);
                    return new SpecialStr(token, currentToken.beginLine, currentToken.beginColumn);
                }
            }
            throw e;
        }
        return null;
    }

    /**
     * Adds a special token to the current token that's in the top of the stack (the peeked token)
     * Considers that the token at the stack is a Call and adds it to its function.
     */
    public void addToPeekCallFunc(Object t, boolean after) {
        Call n = (Call) grammar.getJJTree().peekNode();
        addSpecial(n.func, t, after);
    }

    public void addSpecialTokenToLastOpened(Object o) throws ParseException {
        o = convertStringToSpecialStr(o);
        if (o != null) {
            SimpleNode lastOpened = grammar.getJJTree().getLastOpened();
            if (o instanceof ISpecialStr) {
                lastSpecial = (ISpecialStr) o;
                lastNodeWithSpecial = lastOpened;
            }

            lastOpened.getSpecialsBefore().add(o);

        }
    }

    /**
     * Adds a special token to the current token that's in the top of the stack (the peeked token)
     */
    public final void addToPeek(Object t, boolean after) throws ParseException {
        addToPeek(t, after, null);
    }

    /**
     * Adds a special token to the current token that's in the top of the stack (the peeked token)
     * @return the peeked node.
     */
    @SuppressWarnings("rawtypes")
    public SimpleNode addToPeek(Object t, boolean after, Class class_) throws ParseException {
        SimpleNode peeked = grammar.getJJTree().peekNode();
        addToPeek(peeked, t, after, class_);
        return peeked;
    }

    /**
     * Adds a special token to the current token that's in the top of the stack (the peeked token)
     */
    @SuppressWarnings("rawtypes")
    public void addToPeek(SimpleNode peeked, Object t, boolean after, Class class_) throws ParseException {
        if (class_ != null) {
            // just check if it is the class we were expecting.
            if (peeked.getClass().equals(class_) == false) {
                throw new RuntimeException("Error, expecting class:" + class_ + " received class:" + peeked.getClass()
                        + " Representation:" + peeked);
            }
        }
        t = convertStringToSpecialStr(t);
        if (t != null) {
            addSpecial(peeked, t, after);
        }

    }

    /**
     * Closes a node scope
     * 
     * @param n the node that should have its scope closed.
     * @throws ParseException 
     */
    public void jjtreeCloseNodeScope(Node n) throws ParseException {
        SimpleNode peeked = grammar.getJJTree().peekNode();
        List<Object> specialTokens = grammar.getTokenSourceSpecialTokensList();
        boolean after = true;
        if (n instanceof SimpleNode) {
            if (specialTokens.size() > 0) {
                if (this.prev == null) {
                    // it was not previously set, let's get the current and add it before that token
                    after = false;
                    this.prev = peeked;
                }

                for (Iterator<Object> iter = specialTokens.iterator(); iter.hasNext();) {
                    Object next = iter.next();
                    int strategy = AbstractPythonGrammar.STRATEGY_ADD_AFTER_PREV; // default strategy
                    if (next instanceof Object[]) {
                        strategy = (Integer) ((Object[]) next)[1];
                        next = ((Object[]) next)[0];
                    }

                    if (strategy == AbstractPythonGrammar.STRATEGY_BEFORE_NEXT) { // try to set 'before the next' and not after prev token
                        addToPeek(peeked, next, false, null);
                    } else {
                        // may still add before the next, if there was no prev (we can check that by the 'after' variable)
                        // in this case, we'll do some checks to see if it is really correct (checking for the line and column)

                        if (next instanceof Token) {
                            addSpecial(findTokenToAdd((Token) next), next, after);
                        } else {
                            addSpecial(this.prev, next, after);
                        }
                    }
                }
                specialTokens.clear();
            }
            this.prev = peeked;
        }
    }

    private SimpleNode findTokenToAdd(Token next) {
        SimpleNode curr = grammar.getJJTree().peekNode();
        if (curr != this.prev) {
            //let's see which one is better suited
            if (this.prev.beginLine == next.beginLine) {
                return this.prev;
            }
            if (curr.beginLine == next.beginLine) {
                return curr;
            }
            //if it was found later than both, let's get the current
            if (next.beginLine > this.prev.beginLine && next.beginLine > curr.beginLine) {
                return curr;
            }

        }
        return this.prev;
    }

    private void addSpecial(SimpleNode node, Object special, boolean after) {
        if (special instanceof Token) {
            Token t = (Token) special;
            if (t.toString().trim().startsWith("#")) {
                commentType comment = new commentType(t.image.trim());
                comment.beginColumn = t.beginColumn;
                comment.beginLine = t.beginLine;
                special = comment;

                if (node.beginLine != comment.beginLine) {
                    if (lastSpecial != null && lastNodeWithSpecial != null) {
                        if (comment.beginLine < lastSpecial.getBeginLine()
                                || (comment.beginLine == lastSpecial.getBeginLine() && comment.beginColumn < lastSpecial
                                        .getBeginCol())) {
                            List<Object> specialsBefore = lastNodeWithSpecial.getSpecialsBefore();
                            specialsBefore.add(specialsBefore.indexOf(lastSpecial), comment);
                            return;
                        }
                    }
                }
            } else {
                special = t.asSpecialStr();
            }
        }

        node.addSpecial(special, after);
    }

    public void addSpecialToken(Object o, int strategy) throws ParseException {
        ISpecialStr t = convertStringToSpecialStr(o);
        if (t != null) {
            grammar.getTokenSourceSpecialTokensList().add(new Object[] { t, strategy });
        }
    }

    public void addSpecialToken(Object o) throws ParseException {
        if (!(o instanceof ISpecialStr)) {
            o = convertStringToSpecialStr(o);
        }
        //the default is adding after the previous token
        grammar.getTokenSourceSpecialTokensList()
                .add(new Object[] { o, AbstractPythonGrammar.STRATEGY_ADD_AFTER_PREV });
    }

    public void findTokenAndAdd(String token) throws ParseException {
        ISpecialStr s = createSpecialStr(token, AbstractPythonGrammar.DEFAULT_SEARCH_ON_LAST, true);
        grammar.getTokenSourceSpecialTokensList()
                .add(new Object[] { s, AbstractPythonGrammar.STRATEGY_ADD_AFTER_PREV });
    }

    /**
     * @param s the string found without any preceding char to identify the radix.
     * @param radix the radix in which it was found (octal=8, decimal=10, hex=16)
     * @param token this is the image of the object (the exact way it was found in the file)
     * @param numberToFill the Num object that should be set given the other parameters
     * @throws ParseException 
     */
    public void makeInt(Token t, int radix, Token token, Num numberToFill) throws ParseException {
        makeInt(t.image, radix, token, numberToFill);
    }

    public void makeIntSub2(Token t, int radix, Token token, Num numberToFill) throws ParseException {
        makeInt(t.image.substring(2, t.image.length()), radix, token, numberToFill);
    }

    public void makeIntSub2CheckingOct(Token t, int radix, Token token, Num numberToFill) throws ParseException {
        String s = t.image;
        if (s.length() >= 2) {
            char c = s.charAt(1);
            if (c == 'o' || c == 'O') {
                s = t.image.substring(2, t.image.length());
            }
        }
        makeInt(s, radix, token, numberToFill);
    }

    private void makeInt(String s, int radix, Token token, Num numberToFill) throws ParseException {
        numberToFill.num = token.image;

        if (s.endsWith("L") || s.endsWith("l")) {
            s = s.substring(0, s.length() - 1);
            numberToFill.n = new java.math.BigInteger(s, radix);
            numberToFill.type = Num.Long;
            return;
        }
        int ndigits = s.length();
        int i = 0;
        while (i < ndigits && s.charAt(i) == '0') {
            i++;
        }
        if ((ndigits - i) > 11) {
            numberToFill.n = new java.math.BigInteger(s, radix);
            numberToFill.type = Num.Long;
            return;
        }

        long l;
        try {
            l = Long.valueOf(s, radix).longValue();
        } catch (NumberFormatException e) {
            handleNumberFormatException(token, e);
            l = 0;
        }
        if (l > 0xffffffffl || (radix == 10 && l > Integer.MAX_VALUE)) {
            numberToFill.n = new java.math.BigInteger(s, radix);
            numberToFill.type = Num.Long;
            return;
        }
        numberToFill.n = (int) l;
        numberToFill.type = Num.Int;
    }

    public void makeFloat(Token t, Num numberToFill) throws ParseException {
        String s = t.image;
        numberToFill.num = s;
        try {
            numberToFill.n = Float.valueOf(s);
        } catch (NumberFormatException e) {
            handleNumberFormatException(t, e);
        }
        numberToFill.type = Num.Float;
    }

    private void handleNumberFormatException(Token t, NumberFormatException e) throws ParseException {
        grammar.addAndReport(new ParseException("Unable to parse number: " + t.image, t), e.getMessage());
    }

    public void makeComplex(Token t, Num numberToFill) throws ParseException {
        String s = t.image;
        String compNumber = s.substring(0, s.length() - 1);
        numberToFill.num = s;
        try {
            numberToFill.n = Double.valueOf(compNumber);
        } catch (NumberFormatException e) {
            handleNumberFormatException(t, e);
        }
        numberToFill.type = Num.Comp;
    }

    /**
     * Fills the string properly according to the representation found.
     * 
     * 0 = the string
     * 1 = boolean indicating unicode
     * 2 = boolean indicating raw
     * 3 = style
     * 4 = boolean indicating binary
     */
    public void makeString(Token t, int quotes, Str strToFill) {
        String s = t.image;
        //System.out.println("enter: "+s);
        char quoteChar = s.charAt(0);
        int start = 0;
        boolean ustring = false;
        boolean bstring = false;
        if (quoteChar == 'u' || quoteChar == 'U') {
            ustring = true;
            start++;
        } else if (quoteChar == 'b' || quoteChar == 'B') {
            bstring = true;
            start++;
        }
        quoteChar = s.charAt(start);
        if (quoteChar == 'r' || quoteChar == 'R') {
            //raw string (does not decode slashes)
            String str = s.substring(quotes + start + 1, s.length() - quotes);
            //System.out.println("out: "+str);
            strToFill.type = getType(s.charAt(start + 1), quotes);
            strToFill.s = str;
            strToFill.unicode = ustring;
            strToFill.raw = true;
            strToFill.binary = bstring;

        } else {
            int n = s.length() - quotes;
            int i = quotes + start;

            String str = s.substring(i, n);
            //System.out.println("out: "+str);
            strToFill.type = getType(s.charAt(start), quotes);
            strToFill.s = str;
            strToFill.unicode = ustring;
            strToFill.raw = false;
            strToFill.binary = bstring;
        }
    }

    /**
     * @return the tipe of a given string given the char that starts it and the number of quotes used.
     */
    private final int getType(char c, int quotes) {
        switch (c) {
            case '\'':
                return quotes == 1 ? Str.SingleSingle : Str.TripleSingle;
            case '"':
                return quotes == 1 ? Str.SingleDouble : Str.TripleDouble;
        }
        throw new RuntimeException("Unable to determine type. Char: " + c + " quotes:" + quotes);
    }

    public void addSpecialToPrev(Object special, boolean after) {
        this.prev.addSpecial(special, after);
    }
}
