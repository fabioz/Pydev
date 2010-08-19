package org.python.pydev.django_templates.html.parsing;

import java.io.IOException;

import org.python.pydev.django_templates.IDjConstants;
import org.python.pydev.django_templates.parsing.lexer.DjangoTemplatesTokens;

import beaver.Symbol;

import com.aptana.editor.common.parsing.CompositeParser;
import com.aptana.editor.html.parsing.IHTMLParserConstants;
import com.aptana.parsing.IParseState;
import com.aptana.parsing.ast.IParseNode;
import com.aptana.parsing.ast.ParseNode;
import com.aptana.parsing.ast.ParseRootNode;

public class DjHTMLParser extends CompositeParser {

    public DjHTMLParser() {
        super(new DjHTMLParserScanner(), IHTMLParserConstants.LANGUAGE);
    }

    @Override
    protected IParseNode processEmbeddedlanguage(IParseState parseState) throws Exception {
        String source = new String(parseState.getSource());
        int startingOffset = parseState.getStartingOffset();
        IParseNode root = new ParseRootNode(IDjConstants.LANGUAGE_DJANGO_TEMPLATES, new ParseNode[0], startingOffset, startingOffset
                + source.length());

        advance();
        short id = getCurrentSymbol().getId();
        while (id != DjangoTemplatesTokens.EOF) {
            // only cares about django templates tokens
            switch (id) {
            case DjangoTemplatesTokens.DJHTML_START:
                processDjHtmlBlock(root);
                break;
            }
            advance();
            id = getCurrentSymbol().getId();
        }
        return root;
    }

    private void processDjHtmlBlock(IParseNode root) throws IOException, Exception {
        Symbol startTag = getCurrentSymbol();
        advance();

        // finds the entire django templates block
        int start = getCurrentSymbol().getStart();
        int end = start;
        short id = getCurrentSymbol().getId();
        while (id != DjangoTemplatesTokens.DJHTML_END && id != DjangoTemplatesTokens.EOF) {
            end = getCurrentSymbol().getEnd();
            advance();
            id = getCurrentSymbol().getId();
        }

        ParseNode parseNode = new ParseNode(IDjConstants.LANGUAGE_DJANGO_TEMPLATES);
        parseNode.setLocation(start, end);
        Symbol endTag = getCurrentSymbol();
        DjangoTemplatesNode node = new DjangoTemplatesNode(parseNode, startTag.value.toString(), endTag.value.toString());
        node.setLocation(startTag.getStart(), endTag.getEnd());
        root.addChild(node);
    }
}
