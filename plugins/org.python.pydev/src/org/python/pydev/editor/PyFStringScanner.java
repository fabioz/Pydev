package org.python.pydev.editor;

import java.util.Queue;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.parser.fastparser.grammar_fstrings_common.FStringsAST;
import org.python.pydev.parser.fastparser.grammar_fstrings_common.SimpleNode;
import org.python.pydev.parser.grammar_fstrings.FStringsGrammar;
import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.shared_core.partitioner.SubRuleToken;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;
import org.python.pydev.ui.ColorAndStyleCache;

public class PyFStringScanner implements ITokenScanner {

    private Token fStringReturnToken;
    private Token fStringExpressionReturnToken;
    protected final ColorAndStyleCache colorCache;

    public PyFStringScanner(ColorAndStyleCache colorCache) {
        this.colorCache = colorCache;
        updateColorAndStyle();
    }

    public void updateColorAndStyle() {
        fStringReturnToken = new Token(colorCache.getUnicodeTextAttribute());
        fStringExpressionReturnToken = new Token(colorCache.getStringTextAttribute());
    }

    /*
     * @see ITokenScanner#setRange(IDocument, int, int)
     */
    @Override
    public void setRange(final IDocument document, final int offset, final int length) {
        Assert.isLegal(document != null);
        final int documentLength = document.getLength();
        checkRange(offset, length, documentLength);

        cachedSubTokens.clear();
        cachedSubTokens.add(new SubRuleToken(fStringReturnToken, offset, length));

        char[] chars;
        try {
            chars = document.get(offset, length).toCharArray();
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }

        FastCharStream in = new FastCharStream(chars);
        FStringsGrammar fStringsGrammar = new FStringsGrammar(in);
        FStringsAST ast = null;
        try {
            ast = fStringsGrammar.f_string();
        } catch (Throwable e) {
            // Just ignore any errors for this.
        }

        if (ast != null && ast.hasChildren()) {
            // ast.dump();
            // We have children -- clear the one initially set and set the proper children.
            cachedSubTokens.clear();
            int currOffset = 0;
            for (SimpleNode node : ast.getFStringExpressions()) {
                if (currOffset != node.beginColumn - 1) {
                    cachedSubTokens.add(new SubRuleToken(fStringReturnToken,
                            offset + currOffset, node.beginColumn - 1 - currOffset));
                }
                cachedSubTokens.add(new SubRuleToken(fStringExpressionReturnToken,
                        offset + node.beginColumn - 1, node.endColumn - node.beginColumn + 1));
                currOffset = node.endColumn;
            }

            if (currOffset != length) {
                cachedSubTokens.add(new SubRuleToken(fStringReturnToken,
                        offset + currOffset, length - currOffset));
            }
        }
    }

    private void checkRange(int offset, int length, int documentLength) {
        Assert.isLegal(offset > -1);
        Assert.isLegal(length > -1);
        Assert.isLegal(offset + length <= documentLength);
    }

    private final Queue<SubRuleToken> cachedSubTokens = new LinkedListWarningOnSlowOperations<SubRuleToken>();
    private SubRuleToken curr = null;
    private int fOffset;
    private int fLen;

    /*
     * @see ITokenScanner#getTokenOffset()
     */
    @Override
    public int getTokenOffset() {
        return this.fOffset;
    }

    /*
     * @see ITokenScanner#getTokenLength()
     */
    @Override
    public int getTokenLength() {
        return this.fLen;
    }

    @Override
    public IToken nextToken() {
        this.curr = cachedSubTokens.poll();
        if (this.curr != null) {
            this.fOffset = this.curr.offset;
            this.fLen = this.curr.len;
            return this.curr.token;
        }
        // Reached end
        this.fOffset += fLen;
        this.fLen = 0;
        return Token.EOF;
    }
}
