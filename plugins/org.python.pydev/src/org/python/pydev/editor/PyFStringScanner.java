package org.python.pydev.editor;

import java.util.Queue;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.parser.fastparser.grammar_fstrings_common.FStringsAST;
import org.python.pydev.parser.fastparser.grammar_fstrings_common.SimpleNode;
import org.python.pydev.parser.grammar_fstrings.FStringsGrammar;
import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.shared_core.partitioner.SubRuleToken;
import org.python.pydev.shared_core.string.TextSelectionUtils;
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

        String str;
        try {
            str = document.get(offset, length);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
        FStringInfo fstringInfo = extractFStringInfo(str);
        String buf = fstringInfo.buf;
        int startInternalStrOffset = fstringInfo.startInternalStrOffset;
        int endInternalStrOffet = fstringInfo.endInternalStrOffet;
        if (buf.length() < 0) {
            return;
        }

        FastCharStream in = new FastCharStream(buf.toCharArray());
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
            Document doc = new Document(buf.toString());
            int currOffset = 0;

            if (startInternalStrOffset != 0) {
                cachedSubTokens.add(new SubRuleToken(fStringReturnToken,
                        offset, startInternalStrOffset));
            }

            for (SimpleNode node : ast.getFStringExpressions()) {
                int startRelOffset = TextSelectionUtils.getAbsoluteCursorOffset(doc, node.beginLine - 1,
                        node.beginColumn - 1);
                int endRelOffset = TextSelectionUtils.getAbsoluteCursorOffset(doc, node.endLine - 1, node.endColumn);

                if (currOffset != startRelOffset) {
                    cachedSubTokens.add(new SubRuleToken(fStringReturnToken,
                            offset + startInternalStrOffset + currOffset, startRelOffset - currOffset));
                }
                cachedSubTokens.add(new SubRuleToken(fStringExpressionReturnToken,
                        offset + startInternalStrOffset + startRelOffset, endRelOffset - startRelOffset));
                currOffset = endRelOffset;
            }

            if (currOffset != doc.getLength()) {
                cachedSubTokens.add(new SubRuleToken(fStringReturnToken,
                        offset + startInternalStrOffset + currOffset, doc.getLength() - currOffset));
            }

            if (endInternalStrOffet != 0) {
                cachedSubTokens.add(new SubRuleToken(fStringReturnToken,
                        offset + startInternalStrOffset + doc.getLength(), endInternalStrOffet));
            }
        }
    }

    public static class FStringInfo {

        public final int startInternalStrOffset;
        public final int endInternalStrOffet;
        public final String buf;

        public FStringInfo(int startInternalStrOffset, int endInternalStrOffet, String buf) {
            this.startInternalStrOffset = startInternalStrOffset;
            this.endInternalStrOffet = endInternalStrOffet;
            this.buf = buf;
        }

    }

    /**
     * May return null;
     */
    public static FStringInfo extractFStringInfo(String str) {
        ParsingUtils pu = ParsingUtils.create(str);
        int len = pu.len();
        int startInternalStrOffset = 0;
        int endInternalStrOffet = 0;
        String buf = "";
        for (; startInternalStrOffset < len; startInternalStrOffset++) {
            char c = pu.charAt(startInternalStrOffset);
            if (c == '\'' || c == '"') {
                if (startInternalStrOffset > 2) {
                    //Something went wrong, this should be after f' or at most fr'
                    return null;
                }
                try {
                    int endPos = pu.getLiteralEnd(startInternalStrOffset, c);
                    boolean isMulti = pu.isMultiLiteral(startInternalStrOffset, c);
                    if (isMulti) {

                        startInternalStrOffset += 3;
                        boolean reachedEndBecauseOfEndOfString = endPos <= startInternalStrOffset;
                        if (!reachedEndBecauseOfEndOfString) {
                            for (int i = endPos - 2; i < endPos; i++) {
                                if (pu.charAt(i) != c) {
                                    reachedEndBecauseOfEndOfString = true;
                                    break;
                                }
                            }
                        }

                        if (reachedEndBecauseOfEndOfString) {
                            buf = str.substring(startInternalStrOffset, endPos);
                            endInternalStrOffet = 0;
                        } else {
                            buf = str.substring(startInternalStrOffset, endPos - 2);
                            endInternalStrOffet = 3;
                        }
                    } else {
                        startInternalStrOffset += 1;
                        boolean reachedEndBecauseOfEndOfString = endPos <= startInternalStrOffset || endPos >= pu.len();

                        buf = str.substring(startInternalStrOffset, endPos);
                        if (reachedEndBecauseOfEndOfString) {
                            endInternalStrOffet = 0;

                        } else {
                            endInternalStrOffet = 1;
                        }
                    }
                    break;
                } catch (SyntaxErrorException e) {
                    return null; // Something went wrong... 
                }
            }
        }
        return new FStringInfo(startInternalStrOffset, endInternalStrOffet, buf);
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
