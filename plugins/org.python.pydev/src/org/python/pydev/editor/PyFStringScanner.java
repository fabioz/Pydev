package org.python.pydev.editor;

import java.util.Queue;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.shared_core.partitioner.SubRuleToken;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;
import org.python.pydev.ui.ColorAndStyleCache;

public class PyFStringScanner extends AbstractTokenScanner {

    private Token fStringReturnToken;
    private Token fStringExpressionReturnToken;
    private PyCodeScanner pyCodeScanner;

    public PyFStringScanner(ColorAndStyleCache colorCache) {
        super(colorCache);
        pyCodeScanner = new PyCodeScanner(colorCache);
    }

    @Override
    public void updateColorAndStyle() {
        fStringReturnToken = new Token(colorCache.getUnicodeTextAttribute());
        fStringExpressionReturnToken = new Token(colorCache.getStringTextAttribute());
        if (pyCodeScanner != null) {
            pyCodeScanner.updateColors();
        }
    }

    private final Queue<SubRuleToken> cachedSubTokens = new LinkedListWarningOnSlowOperations<SubRuleToken>();
    private SubRuleToken curr = null;

    /*
     * @see ITokenScanner#getTokenOffset()
     */
    @Override
    public int getTokenOffset() {
        return this.curr != null ? this.curr.offset : super.getTokenOffset();
    }

    /*
     * @see ITokenScanner#getTokenLength()
     */
    @Override
    public int getTokenLength() {
        return this.curr != null ? this.curr.len : super.getTokenLength();
    }

    @Override
    public IToken nextToken() {
        this.curr = cachedSubTokens.poll();
        if (this.curr != null) {
            return this.curr.token;
        }
        fCurrentTokenIndexStartRelativeToInitialOffset = fCurrentIndexRelativeToInitialOffset;

        int c = read();
        if (c == -1) {
            //This isn't really in the contract, but it should work anyways: users do a setRange, then:
            //consume tokens until EOF (at which point we can clear our buffer).
            fChars = null;
            return Token.EOF;
        }

        if (c == '{') {
            int currIndex = fCurrentIndexRelativeToInitialOffset;
            while (c != -1 && c != '\r' && c != '\n' && c != '}') {
                c = read();
            }
            if (c != '}') {
                // { without } is just a regular string up to this point (nothing special about it).
            } else {
                Document doc = new Document(
                        new String(this.fChars, currIndex, fCurrentIndexRelativeToInitialOffset - currIndex - 1));
                pyCodeScanner.setRange(doc, 0, doc.getLength());
                cachedSubTokens.clear();
                cachedSubTokens.add(new SubRuleToken(fStringExpressionReturnToken,
                        fInitialOffset + fCurrentTokenIndexStartRelativeToInitialOffset, 1));
                while (true) {
                    IToken nextToken = pyCodeScanner.nextToken();
                    if (nextToken.isEOF()) {
                        break;
                    }
                    cachedSubTokens.add(
                            new SubRuleToken(nextToken, fInitialOffset + currIndex + pyCodeScanner.getTokenOffset(),
                                    pyCodeScanner.getTokenLength()));
                }
                cachedSubTokens.add(
                        new SubRuleToken(fStringExpressionReturnToken,
                                fInitialOffset + fCurrentIndexRelativeToInitialOffset - 1, 1));
                this.curr = cachedSubTokens.poll();
                return this.curr.token;
            }
        }

        while (c != -1 && c != '\r' && c != '\n' && c != '{') {
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
        return fStringReturnToken;

    }
}
