package org.python.pydev.parser.grammarcommon;

import java.util.Iterator;
import java.util.List;

import org.python.pydev.parser.IGrammar;
import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.IParserHost;
import org.python.pydev.parser.jython.ISpecialStrOrToken;
import org.python.pydev.parser.jython.Node;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.SpecialStr;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.commentType;

public abstract class AbstractPythonGrammar extends AbstractGrammarErrorHandlers implements ITreeConstants, IGrammar{

    public IParserHost hostLiteralMkr;
    public SimpleNode prev;
    public final static boolean DEFAULT_SEARCH_ON_LAST = false;

    /**
     * @return the token at the given location in the stack.
     */
    public abstract Token getToken(int i);

    /**
     * @return the list of special added to the token manager (used so that we
     * can add more info to it later on)
     */
    public abstract List<Object> getTokenSourceSpecialTokensList();

    /**
     * @return the last pos.
     */
    protected abstract Token getJJLastPos();
    
    protected ISpecialStrOrToken temporaryToken;

    
    //---------------------------- Helpers to add special tokens.
    
    /**
     * Adds a special token to the current token that's in the top of the stack (the peeked token)
     */
    protected final void addToPeek(Object t, boolean after) throws ParseException {
        addToPeek(t, after, null);
    }

    /**
     * Adds a special token to the current token that's in the top of the stack (the peeked token)
     * Considers that the token at the stack is a Call and adds it to its function.
     */
    protected final void addToPeekCallFunc(Object t, boolean after) {
        Call n = (Call) getJJTree().peekNode();
        addSpecial(n.func, t, after);
    }


    private ISpecialStrOrToken lastSpecial; 
    private SimpleNode lastNodeWithSpecial; 
    
    public final void addSpecialTokenToLastOpened(Object o) throws ParseException{
        o = convertStringToSpecialStr(o);
        if(o != null){
            SimpleNode lastOpened = getJJTree().getLastOpened();
            if(o instanceof ISpecialStrOrToken){
                lastSpecial = (ISpecialStrOrToken) o;
                lastNodeWithSpecial = lastOpened;
            }
            
            lastOpened.getSpecialsBefore().add(o);

        }
    }
    
    
    private void addSpecial(SimpleNode node, Object special, boolean after) {
        if(special instanceof Token && special.toString().trim().startsWith("#")){
            Token t = (Token) special;
            commentType comment = new commentType(t.image.trim());
            comment.beginColumn = t.beginColumn;
            comment.beginLine = t.beginLine;
            special = comment;
            
            if(node.beginLine != comment.beginLine){
                if(lastSpecial != null && lastNodeWithSpecial != null){
                    if(comment.beginLine < lastSpecial.getBeginLine() ||
                            (comment.beginLine == lastSpecial.getBeginLine() && comment.beginColumn < lastSpecial.getBeginCol())){
                        List<Object> specialsBefore = lastNodeWithSpecial.getSpecialsBefore();
                        specialsBefore.add(specialsBefore.indexOf(lastSpecial), comment);
                        return;
                    }
                }
            }
        }
        
        node.addSpecial(special, after);
    }
    

    /**
     * Adds a special token to the current token that's in the top of the stack (the peeked token)
     */
    @SuppressWarnings("unchecked")
    protected final void addToPeek(Object t, boolean after, Class class_) throws ParseException {
        SimpleNode peeked = (SimpleNode) getJJTree().peekNode();
        addToPeek(peeked, t, after, class_);
    }

    /**
     * Adds a special token to the current token that's in the top of the stack (the peeked token)
     */
    @SuppressWarnings("unchecked")
    protected final void addToPeek(SimpleNode peeked, Object t, boolean after, Class class_) throws ParseException {
        if (class_ != null) {
            // just check if it is the class we were expecting.
            if (peeked.getClass().equals(class_) == false) {
                throw new RuntimeException("Error, expecting class:" + class_ + " received class:" + peeked.getClass()
                        + " Representation:" + peeked);
            }
        }
        t = convertStringToSpecialStr(t);
        if(t != null){
            addSpecial(peeked, t, after);
        }

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
     * @throws ParseException 
     */
    protected final void jjtreeCloseNodeScope(Node n) throws ParseException {
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
                            addSpecial(findTokenToAdd((Token) next), next, after);
                        } else {
                            addSpecial(prev, next, after);
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

    private final SimpleNode findTokenToAdd(Token next) {
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
    
    public final void addSpecialToken(Object o, int strategy) throws ParseException {
        ISpecialStrOrToken t = convertStringToSpecialStr(o);
        if(t != null){
            getTokenSourceSpecialTokensList().add(new Object[] { t, strategy });
        }
    }

    private final ISpecialStrOrToken convertStringToSpecialStr(Object o) throws ParseException{
        if (!(o instanceof ISpecialStrOrToken)) {
            return createSpecialStr(((String) o).trim(), DEFAULT_SEARCH_ON_LAST, false);
        }else{
            return (ISpecialStrOrToken) o;
        }
    }

    public final void addSpecialToken(Object o) throws ParseException {
        if (!(o instanceof ISpecialStrOrToken)) {
            o = convertStringToSpecialStr(o);
        }
        //the default is adding after the previous token
        getTokenSourceSpecialTokensList().add(new Object[] { o, STRATEGY_ADD_AFTER_PREV });
    }


    public final ISpecialStrOrToken createSpecialStr(String token) throws ParseException {
        return createSpecialStr(token, DEFAULT_SEARCH_ON_LAST);
    }
    
    public final ISpecialStrOrToken createSpecialStr(String token, boolean searchOnLast) throws ParseException {
        return createSpecialStr(token, searchOnLast, true);
    }
    
    /**
     * This is where we do a lookahead to see if we find some token and if we do find it, but not on the correct
     * position, we skip some tokens to go to it.
     */
    public final ISpecialStrOrToken createSpecialStr(String token, boolean searchOnLast, boolean throwException) throws ParseException {
        final Token currentToken = getCurrentToken();
        
        Token firstTokenToIterate;
        if (searchOnLast) {
            firstTokenToIterate = getJJLastPos();
        } else {
            firstTokenToIterate = currentToken;
        }
        Token foundToken = null;
        
        
        int foundAtPos = 0;
        
        //lot's of tokens, but we'll bail out on an indent, or dedent, so, that's OK.
        TokensIterator iterTokens = this.getTokensIterator(firstTokenToIterate, 50, true);
        while(iterTokens.hasNext()){
            foundAtPos += 1;
            Token next = iterTokens.next();
            if(next.image != null && next.image.equals(token)){
                //Found what we were looking for!
                foundToken = next;
                break;
            }
        }
        
        
        if (foundToken != null) {
            if(foundAtPos <= 2 //found at correct position. 
                || searchOnLast //we already matched it... right now we're just adding it to the stack!
                ){
                return foundToken;
                
            }
        }
        
        if(throwException){
            ParseException e = createException(token, currentToken);
            
            //we found it at the wrong position!
            if(foundToken != null){
                //we found it, but not on the position we were expecting, so, we must skip some tokens to get to it --
                //and report the needed exception)
                addAndReport(e, "Found at wrong position: "+foundToken);
                Token beforeLastReturned = iterTokens.getBeforeLastReturned();
                setCurrentToken(beforeLastReturned);
                return foundToken;
            }
            
            //create a 'synthetic token' in the place we were expecting it.
            if(currentToken != null){
                AbstractTokenManager tokenManager = getTokenManager();
                FastCharStream inputStream = tokenManager.getInputStream();

                final int created = tokenManager.addCustom(currentToken, token);
                if(created != AbstractTokenManager.CUSTOM_NOT_CREATED){
                    if(created == AbstractTokenManager.CUSTOM_CREATED_WAS_PARENS){
                        //if we had a parens, let's clear the tokens we iterated because we can have skipped indentations!
                        currentToken.next.next = null;
                        
                        //EOF was already found... let's restore the previous indentation level!
                        if(tokenManager.levelBeforeEof != -1){
                            tokenManager.level = tokenManager.levelBeforeEof;
                            tokenManager.levelBeforeEof = -1; //mark it as not found again.
                        }
                        inputStream.restoreLineColPos(currentToken.endLine, currentToken.endColumn);
                    }
                    addAndReport(e, "Created custom token: "+token);
                    return new SpecialStr(token, currentToken.beginLine, currentToken.beginColumn);
                }
            }
            throw e;
        }
        return null;
    }

    
    private ParseException createException(String token, final Token currentToken) {
        ParseException e;
        //return put;
        if (currentToken != null) {
            e = new ParseException("Expected:" + token, currentToken);
            
        } else if (getJJLastPos() != null) {
            e = new ParseException("Expected:" + token, getJJLastPos());
            
        } else {
            e = new ParseException("Expected:" + token);
        }
        return e;
    }

    

    public final boolean findTokenAndAdd(String token) throws ParseException {
        return findTokenAndAdd(token, DEFAULT_SEARCH_ON_LAST);
    }
    
    /**
     * This is so that we add the String with the beginLine and beginColumn
     * @throws ParseException 
     */
    public final boolean findTokenAndAdd(String token, boolean searchOnLast) throws ParseException {
        ISpecialStrOrToken s = createSpecialStr(token, searchOnLast);
        getTokenSourceSpecialTokensList().add(new Object[] { s, STRATEGY_ADD_AFTER_PREV });
        return s instanceof ISpecialStrOrToken;
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
