package org.python.pydev.parser.grammarcommon;

import java.util.Iterator;
import java.util.List;

import org.python.pydev.parser.jython.IParserHost;
import org.python.pydev.parser.jython.Node;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.SpecialStr;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Str;

public abstract class AbstractPythonGrammar implements ITreeConstants{

    public IParserHost hostLiteralMkr;
    public SimpleNode prev;
    public static boolean DEBUG = false;
    public final static boolean DEFAULT_SEARCH_ON_LAST = false;

    /**
     * @return the actual jjtree used to build the nodes (tree)
     */
    protected abstract IJJTPythonGrammarState getJJTree();

    /**
     * @return the token at the given location in the stack.
     */
    public abstract Token getToken(int i);

    /**
     * @return the list of special added to the token manager (used so that we
     * can add more info to it later on)
     */
    protected abstract List<Object> getTokenSourceSpecialTokensList();

    /**
     * @return the last pos.
     */
    protected abstract Token getJJLastPos();

    /**
     * @return the current token
     */
    protected abstract Token getCurrentToken();

    protected final void addToPeek(Object t, boolean after) {
        addToPeek(t, after, null);
    }

    protected final void addToPeekCallFunc(Object t, boolean after) {
        Call n = (Call) getJJTree().peekNode();
        n.func.addSpecial(t, after);
    }

    @SuppressWarnings("unchecked")
    protected final void addToPeek(Object t, boolean after, Class class_) {
        SimpleNode peeked = (SimpleNode) getJJTree().peekNode();
        addToPeek(peeked, t, after, class_);
    }

    @SuppressWarnings("unchecked")
    protected final void addToPeek(SimpleNode peeked, Object t, boolean after, Class class_) {
        if (class_ != null) {
            // just check if it is the class we were expecting.
            if (peeked.getClass().equals(class_) == false) {
                throw new RuntimeException("Error, expecting class:" + class_ + " received class:" + peeked.getClass()
                        + " Representation:" + peeked);
            }
        }
        t = convertStringToSpecialStr(t);
        peeked.addSpecial(t, after);

    }
    
    /**
     * Opens a node scope
     * 
     * @param n the node marking the beginning of the scope.
     */
    protected final void jjtreeOpenNodeScope(Node n) {
        if (DEBUG) {
            System.out.println("opening scope:" + n);
        }
        Token t = getToken(1);
        getJJTree().pushNodePos(t.beginLine, t.beginColumn);
    }

    /**
     * Closes a node scope
     * 
     * @param n the node that should have its scope closed.
     */
    protected final void jjtreeCloseNodeScope(Node n) {
        if (DEBUG) {
            System.out.println("closing scope:" + n);
        }
        SimpleNode peeked = getJJTree().setNodePos();
        List<Object> specialTokens = getTokenSourceSpecialTokensList();
        boolean after = true;
        if (n instanceof SimpleNode) {
            if (specialTokens.size() > 0) {
                if (prev == null) {
                    // it was not previously set, let's get the current and add it before that token
                    after = false;
                    prev = peeked;
                }
                if (DEBUG) {
                    System.out.println("closing scope " + peeked.getClass());
                }

                for (Iterator<Object> iter = specialTokens.iterator(); iter.hasNext();) {
                    Object next = iter.next();
                    int strategy = STRATEGY_ADD_AFTER_PREV; // default strategy
                    if (next instanceof Object[]) {
                        strategy = (Integer) ((Object[]) next)[1];
                        next = ((Object[]) next)[0];
                    }

                    if (strategy == STRATEGY_BEFORE_NEXT) { // try to set 'before the next' and not after prev token
                        addToPeek(peeked, next, false, null);
                    } else {
                        // may still add before the next, if there was no prev (we can check that by the 'after' variable)
                        // in this case, we'll do some checks to see if it is really correct (checking for the line and column)

                        if (next instanceof Token) {
                            findTokenToAdd((Token) next).addSpecial(next, after);
                        } else {
                            prev.addSpecial(next, after);
                        }
                    }
                }
                specialTokens.clear();
            }
            prev = (SimpleNode) peeked;
        }
    }

    /**
     * Default: add after the previous found token
     */
    public static final int STRATEGY_ADD_AFTER_PREV = 0;

    /**
     * Add before the 'next token' strategy
     */
    public static final int STRATEGY_BEFORE_NEXT = 1;

    public final SimpleNode findTokenToAdd(Token next) {
        SimpleNode curr = (SimpleNode) getJJTree().peekNode();
        if (curr != prev) {
            //let's see which one is better suited
            if (prev.beginLine == next.beginLine) {
                return prev;
            }
            if (curr.beginLine == next.beginLine) {
                return curr;
            }
            //if it was found later than both, let's get the current
            if (next.beginLine > prev.beginLine && next.beginLine > curr.beginLine) {
                return curr;
            }

        }
        return prev;

    }

    public final void addSpecialTokenToLastOpened(Object o){
        o = convertStringToSpecialStr(o);
        getJJTree().getLastOpened().getSpecialsBefore().add(o);
    }
    
    public final void addSpecialToken(Object o, int strategy) {
        o = convertStringToSpecialStr(o);
        getTokenSourceSpecialTokensList().add(new Object[] { o, strategy });
    }

    public final Object convertStringToSpecialStr(Object o) {
        if (o instanceof String) {
            try {
                o = createSpecialStr((String) o);
            } catch (ParseException e) {
            }
        }
        return o;
    }

    public final void addSpecialToken(Object o) {
        //the default is adding after the previous token
        getTokenSourceSpecialTokensList().add(new Object[] { o, STRATEGY_ADD_AFTER_PREV });
    }

    public final boolean findTokenAndAdd(String token) throws ParseException {
        return findTokenAndAdd(token, token, DEFAULT_SEARCH_ON_LAST);
    }

    public final Object createSpecialStr(String token) throws ParseException {
        return createSpecialStr(token, token);
    }

    public final Object createSpecialStr(String token, boolean searchOnLast) throws ParseException {
        return createSpecialStr(token, token, searchOnLast);
    }

    public final Object createSpecialStr(String token, String put) throws ParseException {
        return createSpecialStr(token, put, DEFAULT_SEARCH_ON_LAST);
    }

    public final Object createSpecialStr(String token, String put, boolean searchOnLast) throws ParseException {
        Token t;
        if (searchOnLast) {
            t = getJJLastPos();
        } else {
            t = getCurrentToken();
        }
        while (t != null && t.image != null && t.image.equals(token) == false) {
            t = t.next;
        }
        if (t != null) {
            return new SpecialStr(put, t.beginLine, t.beginColumn);
        }
        //return put;
        if (getCurrentToken() != null) {
            throw new ParseException("Expected:" + token, getCurrentToken());
        } else if (getJJLastPos() != null) {
            throw new ParseException("Expected:" + token, getJJLastPos());
        } else {
            throw new ParseException("Expected:" + token);
        }
    }

    /**
     * This is so that we add the String with the beginLine and beginColumn
     * @throws ParseException 
     */
    public final boolean findTokenAndAdd(String token, String put, boolean searchOnLast) throws ParseException {
        Object s = createSpecialStr(token, put, searchOnLast);
        getTokenSourceSpecialTokensList().add(new Object[] { s, STRATEGY_ADD_AFTER_PREV });
        return s instanceof SpecialStr;
    }

    /**
     * @param s the string found without any preceding char to identify the radix.
     * @param radix the radix in which it was found (octal=8, decimal=10, hex=16)
     * @param token this is the image of the object (the exact way it was found in the file)
     * @param numberToFill the Num object that should be set given the other parameters
     */
    protected final void makeInt(String s, int radix, String token, Num numberToFill) {
        numberToFill.num = token;
        
        if (s.endsWith("L") || s.endsWith("l")) {
            s = s.substring(0, s.length() - 1);
            numberToFill.n = hostLiteralMkr.newLong(new java.math.BigInteger(s, radix));
            numberToFill.type = Num.Long;
            return;
        }
        int ndigits = s.length();
        int i = 0;
        while (i < ndigits && s.charAt(i) == '0')
            i++;
        if ((ndigits - i) > 11) {
            numberToFill.n = hostLiteralMkr.newLong(new java.math.BigInteger(s, radix));
            numberToFill.type = Num.Long;
            return;
        }

        long l = Long.valueOf(s, radix).longValue();
        if (l > 0xffffffffl || (radix == 10 && l > Integer.MAX_VALUE)) {
            numberToFill.n = hostLiteralMkr.newLong(new java.math.BigInteger(s, radix));
            numberToFill.type = Num.Long;
            return;
        }
        numberToFill.n = hostLiteralMkr.newInteger((int) l);
        numberToFill.type = Num.Int;
    }

    protected final void makeFloat(String s, Num numberToFill) {
        numberToFill.num = s;
        numberToFill.n = hostLiteralMkr.newFloat(Double.valueOf(s).doubleValue());
        numberToFill.type = Num.Float;
    }

    protected final void makeLong(String s, Num numberToFill) {
        numberToFill.num = s;
        numberToFill.n = hostLiteralMkr.newLong(s);
        numberToFill.type = Num.Long;
    }

    protected final void makeComplex(String s, Num numberToFill) {
        String compNumber = s.substring(0, s.length() - 1);
        numberToFill.num = s;
        numberToFill.n = hostLiteralMkr.newImaginary(Double.valueOf(compNumber).doubleValue());
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
    protected final void makeString(String s, int quotes, Str strToFill) {
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

            String str = hostLiteralMkr.decode_UnicodeEscape(s, i, n, "strict", ustring);
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
}
