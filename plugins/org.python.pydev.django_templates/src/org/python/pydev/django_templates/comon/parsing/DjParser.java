/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.comon.parsing;

import java.io.IOException;

import org.python.pydev.django_templates.common.parsing.lexer.DjangoTemplatesTokens;

import beaver.Symbol;

import com.aptana.editor.common.parsing.CompositeParser;
import com.aptana.editor.common.parsing.CompositeParserScanner;
import com.aptana.parsing.IParseState;
import com.aptana.parsing.WorkingParseResult;
import com.aptana.parsing.ast.IParseNode;
import com.aptana.parsing.ast.ParseNode;
import com.aptana.parsing.ast.ParseRootNode;

public abstract class DjParser extends CompositeParser {

    private String language;

    public DjParser(CompositeParserScanner defaultScanner, String primaryParserLanguage, String language) {
        super(defaultScanner, primaryParserLanguage);
        this.language = language;
    }

    @Override
    protected IParseNode processEmbeddedlanguage(IParseState parseState, WorkingParseResult working) throws Exception {
        String source = parseState.getSource();
        int startingOffset = parseState.getStartingOffset();
        IParseNode root = new ParseRootNode(language, new ParseNode[0], startingOffset, startingOffset
                + source.length() - 1);

        advance();
        short id = getCurrentSymbol().getId();
        while (id != DjangoTemplatesTokens.EOF) {
            // only cares about django templates tokens
            switch (id) {
                case DjangoTemplatesTokens.DJ_START:
                    processDjBlock(root);
                    break;
            }
            advance();
            id = getCurrentSymbol().getId();
        }
        return root;
    }

    private void processDjBlock(IParseNode root) throws IOException, Exception {
        Symbol startTag = getCurrentSymbol();
        advance();

        // finds the entire django templates block
        int start = getCurrentSymbol().getStart();
        int end = start;
        short id = getCurrentSymbol().getId();
        while (id != DjangoTemplatesTokens.DJ_END && id != DjangoTemplatesTokens.EOF) {
            end = getCurrentSymbol().getEnd();
            advance();
            id = getCurrentSymbol().getId();
        }

        ParseNode parseNode = new ParseNode(language);
        parseNode.setLocation(start, end);
        Symbol endTag = getCurrentSymbol();
        DjangoTemplatesNode node = new DjangoTemplatesNode(language, parseNode, startTag.value.toString(),
                endTag.value.toString());
        node.setLocation(startTag.getStart(), endTag.getEnd());
        root.addChild(node);
    }

}
