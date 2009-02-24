package org.python.pydev.parser.grammarcommon;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.REF;
import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.Token;

/**
 * 
 * Note that this class actually has a tight coupling with subclasses, searching directly for some attributes
 * (e.g.: curLexState so that we don't need to override a get for each token manager class)
 * 
 * Some lexer states:
 
  int DEFAULT = 0;
  int FORCE_NEWLINE1 = 1;
  int FORCE_NEWLINE2 = 2;
  int INDENTING = 3;
  int INDENTATION_UNCHANGED = 4;
  int UNREACHABLE = 5;
  int IN_STRING11 = 6;
  int IN_STRING21 = 7;
  int IN_STRING13 = 8;
  int IN_STRING23 = 9;
  int IN_BSTRING11 = 10;
  int IN_BSTRING21 = 11;
  int IN_BSTRING13 = 12;
  int IN_BSTRING23 = 13;
  int IN_USTRING11 = 14;
  int IN_USTRING21 = 15;
  int IN_USTRING13 = 16;
  int IN_USTRING23 = 17;
  int IN_STRING1NLC = 18;
  int IN_STRING2NLC = 19;
  int IN_USTRING1NLC = 20;
  int IN_USTRING2NLC = 21;
  int IN_BSTRING1NLC = 22;
  int IN_BSTRING2NLC = 23;
  
 *
 *
 * @author Fabio
 *
 */
public abstract class AbstractTokenManager implements ITreeConstants{
    
    protected final int indentation[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    protected int level = 0;
    protected int parens = 0;
    protected int indent;

    public boolean expect_indent = false;

    public boolean compound = false;

    public final List<Object> specialTokens = new ArrayList<Object>();
    private final int passId;
    private final int dedentId;
    private final int indentId;
    private final int defaultId;
    private final int newlineId;
    private final int eofId;
    private final int rparenId;
    private final int lparenId;
    private final int singleLineCommentId;
    private final int nameId;
    private final int colonId;
    
    private FastCharStream inputStream;

    @SuppressWarnings("unchecked")
    protected abstract Class getConstantsClass();
    
    @SuppressWarnings("unchecked")
    protected int getFromConstants(String constant){
        try {
            Class c = getConstantsClass();
            Field declaredField = c.getDeclaredField(constant);
            return declaredField.getInt(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
    }
    
    //constants
    protected final int getPassId(){return passId;}
    protected final int getDedentId(){return dedentId;}
    protected final int getIndentId(){return indentId;}
    protected final int getDefaultId(){return defaultId;}
    protected final int getNewlineId(){return newlineId;}
    protected final int getEofId(){return eofId;}
    protected final int getRParenId(){return rparenId;}
    protected final int getLParenId(){return lparenId;}
    protected final int getColonId(){return colonId;}
    protected final int getNameId(){return nameId;}
    protected final int getSingleLineCommentId(){return singleLineCommentId;}
    
    protected final FastCharStream getInputStream(){
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
        passId = getFromConstants("PASS");
        dedentId = getFromConstants("DEDENT");
        indentId = getFromConstants("INDENT");
        defaultId = getFromConstants("DEFAULT");
        newlineId =  getFromConstants("NEWLINE");
        rparenId =  getFromConstants("RPAREN");
        lparenId =  getFromConstants("LPAREN");
        nameId =  getFromConstants("NAME");
        eofId =  getFromConstants("EOF");
        colonId =  getFromConstants("COLON");
        singleLineCommentId =  getFromConstants("SINGLE_LINE_COMMENT");
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
    
    public boolean addCustom(Token curr, String token) {
        if(")".equals(token)){
            createFromAndSetAsNext(curr, getRParenId(), ")");
            return true;
        }
        if(":".equals(token)){
            createFromAndSetAsNext(curr, getColonId(), ":");
            return true;
        }
        if("(".equals(token)){
            createFromAndSetAsNext(curr, getLParenId(), "(");
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
            if (getCurLexState() == getDefaultId()) {
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

}
