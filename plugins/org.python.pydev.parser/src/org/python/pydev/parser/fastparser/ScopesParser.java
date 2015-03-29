/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.fastparser;

import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.docutils.TabNannyDocIterator;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.parsing.IScopesParser;
import org.python.pydev.shared_core.parsing.Scopes;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple3;

/**
 * This parser is a bit different from the others, as its output is not an AST, but a structure defining the scopes
 * in a document (used for doing the scope selection action).
 *
 * @author fabioz
 */
public class ScopesParser implements IScopesParser {

    public Scopes createScopes(IDocument doc) {
        this.scopes = new Scopes();
        this.doc = doc;
        this.lineOffsetToIndent = new TreeMap<Integer, Integer>();

        try {
            TabNannyDocIterator nannyDocIterator = new TabNannyDocIterator(doc, true, false);
            while (nannyDocIterator.hasNext()) {
                Tuple3<String, Integer, Boolean> next = nannyDocIterator.next();
                this.lineOffsetToIndent.put(next.o2, next.o1.length());
            }
        } catch (BadLocationException e1) {
            throw new RuntimeException(e1);
        }

        try {
            return this.createScopes();
        } catch (SyntaxErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private Scopes scopes;
    private SortedMap<Integer, Integer> lineOffsetToIndent;
    private IDocument doc;

    public ScopesParser() {

    }

    private Scopes createScopes() throws SyntaxErrorException {
        int globalScope = this.scopes.startScope(0, Scopes.TYPE_MODULE);
        int offset = createInternalScopes(ParsingUtils.create(doc, true), 0);
        this.scopes.endScope(globalScope, offset, Scopes.TYPE_MODULE);

        return this.scopes;

    }

    private int createInternalScopes(ParsingUtils parsingUtils, int offsetDelta) {
        int docLen = parsingUtils.len();
        int offset = 0;
        FastStringBuffer lineMemo = new FastStringBuffer();
        int memoStart = 0;
        int id;

        for (; offset < docLen; offset++) {
            char ch = parsingUtils.charAt(offset);

            switch (ch) {

                case '#':
                    id = this.scopes.startScope(offsetDelta + offset, Scopes.TYPE_COMMENT);
                    offset = parsingUtils.eatComments(null, offset);
                    this.scopes.endScope(id, offsetDelta + offset, Scopes.TYPE_COMMENT);
                    break;

                case '{':
                case '[':
                case '(':
                    int baseOffset = offset;
                    try {
                        offset = parsingUtils.eatPar(offset, null, ch); //If a SyntaxError is raised here, we won't create a scope!
                        id = this.scopes.startScope(offsetDelta + baseOffset + 1, Scopes.TYPE_PEER);

                        try {
                            String cs = doc.get(offsetDelta + baseOffset + 1, offset - baseOffset - 1);
                            createInternalScopes(ParsingUtils.create(cs, true), offsetDelta + baseOffset + 1);
                        } catch (BadLocationException e1) {
                            Log.log(e1);
                        }

                        this.scopes.endScope(id, offsetDelta + offset, Scopes.TYPE_PEER);

                    } catch (SyntaxErrorException e2) {

                    }

                    break;

                case '\'':
                    //Fallthrough

                case '\"':
                    baseOffset = offset;

                    try {
                        offset = parsingUtils.eatLiterals(null, offset); //If a SyntaxError is raised here, we won't create a scope!
                        id = this.scopes.startScope(offsetDelta + baseOffset, Scopes.TYPE_STRING);
                        this.scopes.endScope(id, offsetDelta + offset + 1, Scopes.TYPE_STRING);
                    } catch (SyntaxErrorException e1) {

                    }
                    break;

                case ':':
                    if (PySelection.startsWithIndentToken(lineMemo.toString().trim())) {
                        SortedMap<Integer, Integer> subMap = lineOffsetToIndent.tailMap(offsetDelta + memoStart + 1);
                        Integer level = lineOffsetToIndent.get(offsetDelta + memoStart);
                        if (level == null) {
                            //It's a ':' inside a parens
                            continue;
                        }

                        Set<Entry<Integer, Integer>> entrySet = subMap.entrySet();
                        boolean found = false;
                        id = this.scopes.startScope(memoStart + level, Scopes.TYPE_SUITE);

                        int id2 = -1;
                        for (int j = offset + 1; j < docLen; j++) {
                            char c = parsingUtils.charAt(j);
                            if (Character.isWhitespace(c)) {
                                continue;
                            }
                            if (c == '#') {
                                j = parsingUtils.eatComments(null, j);
                                continue;
                            }
                            id2 = this.scopes.startScope(offsetDelta + j, Scopes.TYPE_SUITE);
                            break;
                        }

                        for (Entry<Integer, Integer> entry : entrySet) {
                            if (level >= entry.getValue()) {
                                found = true;
                                Integer scopeEndOffset = entry.getKey();
                                try {
                                    int line = doc.getLineOfOffset(scopeEndOffset);
                                    if (line > 0) {
                                        //We want it to end at the end of the previous line (not at the start of the next scope)
                                        IRegion lineInformation = doc.getLineInformation(line - 1);
                                        scopeEndOffset = lineInformation.getOffset() + lineInformation.getLength();
                                    }
                                } catch (BadLocationException e) {
                                    Log.log(e);
                                }
                                this.scopes.endScope(id, scopeEndOffset, Scopes.TYPE_SUITE);
                                if (id2 > 0) {
                                    this.scopes.endScope(id2, scopeEndOffset, Scopes.TYPE_SUITE);
                                }
                                break;
                            }
                        }
                        if (!found) {
                            //Ends at the end of the document!
                            this.scopes.endScope(id, offsetDelta + parsingUtils.len(), Scopes.TYPE_SUITE);
                            if (id2 > 0) {
                                this.scopes.endScope(id2, offsetDelta + parsingUtils.len(), Scopes.TYPE_SUITE);
                            }
                        }
                    }
                    break;

                case '\r':
                    //Fallthrough

                case '\n':
                    //Note that we don't add the \r nor \n to the memo (but we clear it if the  line did not end with a \).
                    if (lineMemo.length() > 0 && lineMemo.lastChar() != '\\') {
                        lineMemo.clear();
                    }
                    break;

                default:
                    if (lineMemo.length() == 0) {
                        memoStart = offset;
                    }
                    lineMemo.append(ch);
                    break;
            }

        }
        return offset;
    }

}
