package org.python.pydev.editor;

import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.ui.ColorAndStyleCache;

public abstract class AbstractStringScanner extends AbstractTokenScanner {

    protected Token fDocStringMarkupTextReturnToken;
    protected IToken fStringReturnToken;

    public AbstractStringScanner(ColorAndStyleCache colorCache) {
        super(colorCache);
    }

    /*
     * @see ITokenScanner#nextToken()
     */
    @Override
    public IToken nextToken() {
        fCurrentTokenIndexStartRelativeToInitialOffset = fCurrentIndexRelativeToInitialOffset;

        int c = read();
        if (c == -1) {
            //This isn't really in the contract, but it should work anyways: users do a setRange, then:
            //consume tokens until EOF (at which point we can clear our buffer).
            fChars = null;
            return Token.EOF;
        }
        if (Character.isWhitespace(c)) {
            while (Character.isWhitespace(c) && c != -1) {
                c = read();
            }
            unread();
            return fStringReturnToken;
        }

        if (c == '@' || c == ':') {
            //Looking for @ or : in the start of the line
            c = read();
            if (c == -1) {
                unread();
                return fDocStringMarkupTextReturnToken;
            }
            while (Character.isJavaIdentifierPart(c)) {
                c = read();
            }
            unread();
            return fDocStringMarkupTextReturnToken;

        } else {
            // read to the end of the line
            while (c != -1 && c != '\r' && c != '\n') {
                c = read();
            }
            if (c == -1) {
                unread();
                return fStringReturnToken;
            }
            while (c == '\r' && c == '\n') {
                c = read();
            }
            unread();
        }

        return fStringReturnToken;
    }

}
