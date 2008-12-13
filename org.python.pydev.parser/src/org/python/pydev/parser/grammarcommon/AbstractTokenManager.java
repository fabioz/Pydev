package org.python.pydev.parser.grammarcommon;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.Token;

public abstract class AbstractTokenManager {
    
    protected final int indentation[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    protected int level = 0;
    protected int parens = 0;
    protected int indent;

    public boolean expect_indent = false;

    public boolean compound = false;

    public final List<Object> specialTokens = new ArrayList<Object>();

    private final int DEDENT;
    private final int EOF;
    private final int DEFAULT;
    private final int NEWLINE;
    
    protected abstract int getDedentId();
    protected abstract int getEofId();
    protected abstract int getDefaultId();
    protected abstract int getNewlineId();
    protected abstract int getCurLexState();
    
    protected AbstractTokenManager(){
        DEDENT = getDedentId();
        EOF = getEofId();
        DEFAULT = getDefaultId();
        NEWLINE = getNewlineId();
    }

    protected final Token addDedent(Token previous) {
        Token t = new Token();
        t.kind = getDedentId();
        t.beginLine = previous.beginLine;
        t.endLine = previous.endLine;
        t.beginColumn = previous.beginColumn;
        t.endColumn = previous.endColumn;
        t.image = "<DEDENT>";
        t.specialToken = null;
        t.next = null;
        previous.next = t;
        return t;
    }

    protected final void CommonTokenAction(Token t) {
        /*
           if not partial: EOF is expanded to token sequences comprising
               [NEWLINE] necessary DEDENT EOF
           if partial: EOF expansion happens only if EOF preceded by empty line (etc),
           i.e. lexer is in MAYBE_FORCE_NEWLINE_IF_EOF state
           System.out.println("Token:'"+t+"'");
           System.out.println("Special:'"+t.specialToken+"'");
        */

        int i = specialTokens.size();
        while (t.specialToken != null) {
            this.specialTokens.add(i, t.specialToken);
            t = t.specialToken;
        }

        if (t.kind == EOF) {
            // System.out.println(curLexState+", "+level);
            if (getCurLexState() == DEFAULT) {
                t.kind = NEWLINE;
            } else {
                t.kind = DEDENT;
                if (level >= 0)
                    level -= 1;
            }
            while (level >= 0) {
                level--;
                t = addDedent(t);
            }
            t.kind = EOF;
            t.image = "<EOF>";
        }
    }

}
