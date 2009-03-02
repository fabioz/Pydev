package org.python.pydev.parser.grammarcommon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.python.pydev.core.REF;
import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.Token;

/**
 * 
 * Note that this class actually has a tight coupling with subclasses, searching directly for some attributes
 * (e.g.: curLexState so that we don't need to override a get for each token manager class)
 *
 *
 * @author Fabio
 *
 */
public abstract class AbstractTokenManager extends AbstractTokenManagerWithConstants implements ITreeConstants, ITokenManager{
    
    protected final int indentation[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    protected int level = 0;
    protected int parens = 0;
    protected int indent;

    public boolean expect_indent = false;

    public boolean compound = false;

    public final List<Object> specialTokens = new ArrayList<Object>();
    
    private FastCharStream inputStream;


    
    public final FastCharStream getInputStream(){
        if(this.inputStream == null){
            this.inputStream =  (FastCharStream) REF.getAttrObj(this, "input_stream", true);
        }
        return inputStream;
    }
    
    //must be calculated
    protected final int getCurLexState(){
        return (Integer)REF.getAttrObj(this, "curLexState", true);
    }
    
    public abstract void SwitchTo(int lexState);
    public abstract Token getNextToken();
    
    protected AbstractTokenManager(){
    }

    protected final Token createFrom(Token previous, int kind, String image) {
        Token t = new Token();
        t.kind = kind;
        t.beginLine = previous.beginLine;
        t.endLine = previous.endLine;
        t.beginColumn = previous.beginColumn;
        t.endColumn = previous.endColumn;
        t.image = image;
        t.specialToken = null;
        t.next = null;
        return t;
    }
    
    /**
     * Creates a new token based on the coordinates on the previous and sets it as the next from the previous
     */
    protected final Token createFromAndSetAsNext(Token previous, int kind, String image) {
        Token t = createFrom(previous, kind, image);
        Token oldNext = previous.next;
        previous.next = t;
        t.next = oldNext;
        return t;
    }
    
    protected final Token addDedent(Token previous) {
        return createFromAndSetAsNext(previous, getDedentId(), "<DEDENT>");
    }
    
    protected final Token addIndent(Token previous) {
        return createFromAndSetAsNext(previous, getIndentId(), "<INDENT>");
    }
    
    public Token createCustom(Token curr, String token) {
        if("\n".equals(token)){
            return createFromAndSetAsNext(curr, getNewlineId(), "\n");
        }
        return null;
    }
    
    
    private Map<String, Integer> tokenToId;
    private Map<String, Integer> getTokenToId() {
        if(tokenToId == null){
            tokenToId = new HashMap<String, Integer>();
            tokenToId.put(")", getRparenId());
            tokenToId.put("]", getRbracketId());
            tokenToId.put("}", getRbraceId());
            tokenToId.put(":", getColonId());
            tokenToId.put("(", getLparenId());
            tokenToId.put(",", getCommaId());
        }
        return tokenToId;
    }
    
    public boolean addCustom(Token curr, String token) {
        Integer id = getTokenToId().get(token);
        if(id != null){
            createFromAndSetAsNext(curr, id, token);
            return true;
        }
        return false;
    }


    protected final void CommonTokenAction(final Token initial) {
        /*
           if not partial: EOF is expanded to token sequences comprising
               [NEWLINE] necessary DEDENT EOF
           if partial: EOF expansion happens only if EOF preceded by empty line (etc),
           i.e. lexer is in MAYBE_FORCE_NEWLINE_IF_EOF state
           System.out.println("Token:'"+t+"'");
           System.out.println("Special:'"+t.specialToken+"'");
        */
        Token t = initial;

        int i = specialTokens.size();
        while (t.specialToken != null) {
            this.specialTokens.add(i, t.specialToken);
            t = t.specialToken;
        }

        //Now, we must check the actual token here for EOF. 
        t = initial;
        
        //This is the place we check if we have to add dedents so that the parsing ends 'gracefully' when
        //we find and EOF.
        if (t.kind == getEofId()) {
            if (getCurLexState() == getLexerDefaultId()) {
                t.kind = getNewlineId();
            } else {
                t.kind = getDedentId();
                if (level >= 0)
                    level -= 1;
            }
            while (level >= 0) {
                level--;
                t = addDedent(t);
            }
            t.kind = getEofId();
            t.image = "<EOF>";
        }
        
    }

    /**
     * Must be called right after a new line with 0 as a parameter. Identifies the number of whitespaces in the current line.
     */
    public abstract void indenting(int i);
    
    
    /**
     * @return The current level of the indentation in the current line.
     */
    public abstract int getCurrentLineIndentation();
    
    /**
     * @return The current level of the indentation.
     */
    public abstract int getLastIndentation();

}
