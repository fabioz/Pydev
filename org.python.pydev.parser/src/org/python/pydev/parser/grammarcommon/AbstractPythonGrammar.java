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

public abstract class AbstractPythonGrammar {

    public IParserHost hostLiteralMkr;
    public SimpleNode prev;
    public static boolean DEBUG = false;
    public final static boolean DEFAULT_SEARCH_ON_LAST = false;

    protected void jjtreeOpenNodeScope(Node n) {
        if (DEBUG) {
            System.out.println("opening scope:" + n);
        }
        Token t = getToken(1);
        getJJTree().pushNodePos(t.beginLine, t.beginColumn);
    }

    protected abstract IJJTPythonGrammarState getJJTree();

    public abstract Token getToken(int i);

    protected abstract List<Object> getTokenSourceSpecialTokensList();

    protected abstract Token getJJLastPos();

    protected abstract Token getCurrentToken();

    protected void addToPeek(Object t, boolean after) {
        addToPeek(t, after, null);
    }

    protected void addToPeekCallFunc(Object t, boolean after) {
        Call n = (Call) getJJTree().peekNode();
        n.func.addSpecial(t, after);
    }

    @SuppressWarnings("unchecked")
    protected void addToPeek(Object t, boolean after, Class class_) {
        SimpleNode peeked = (SimpleNode) getJJTree().peekNode();
        addToPeek(peeked, t, after, class_);
    }

    @SuppressWarnings("unchecked")
    protected void addToPeek(SimpleNode peeked, Object t, boolean after, Class class_) {
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

    protected void jjtreeCloseNodeScope(Node n) {
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

    public SimpleNode findTokenToAdd(Token next) {
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

    public void addSpecialToken(Object o, int strategy) {
        o = convertStringToSpecialStr(o);
        getTokenSourceSpecialTokensList().add(new Object[] { o, strategy });
    }

    public Object convertStringToSpecialStr(Object o) {
        if (o instanceof String) {
            try {
                o = createSpecialStr((String) o);
            } catch (ParseException e) {
            }
        }
        return o;
    }

    public void addSpecialToken(Object o) {
        //the default is adding after the previous token
        getTokenSourceSpecialTokensList().add(new Object[] { o, STRATEGY_ADD_AFTER_PREV });
    }

    public boolean findTokenAndAdd(String token) throws ParseException {
        return findTokenAndAdd(token, token, DEFAULT_SEARCH_ON_LAST);
    }

    public Object createSpecialStr(String token) throws ParseException {
        return createSpecialStr(token, token);
    }

    public Object createSpecialStr(String token, boolean searchOnLast) throws ParseException {
        return createSpecialStr(token, token, searchOnLast);
    }

    public Object createSpecialStr(String token, String put) throws ParseException {
        return createSpecialStr(token, put, DEFAULT_SEARCH_ON_LAST);
    }

    public Object createSpecialStr(String token, String put, boolean searchOnLast) throws ParseException {
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
    public boolean findTokenAndAdd(String token, String put, boolean searchOnLast) throws ParseException {
        Object s = createSpecialStr(token, put, searchOnLast);
        getTokenSourceSpecialTokensList().add(new Object[] { s, STRATEGY_ADD_AFTER_PREV });
        return s instanceof SpecialStr;
    }

}
