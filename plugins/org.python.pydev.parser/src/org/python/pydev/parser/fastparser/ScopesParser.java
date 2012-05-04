/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.fastparser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;

/**
 * This parser is a bit different from the others, as its output is not an AST, but a structure defining the scopes
 * in a document (used for doing the scope selection action).
 * 
 * @author fabioz
 */
public class ScopesParser {

    public static class ScopeEntry {

        public final int type;
        public final boolean open;
        public final int id;
        public final int offset;

        public ScopeEntry(int id, int type, boolean open, int offset) {
            this.type = type;
            this.open = open;
            this.id = id;
            this.offset = offset;
        }

        public void toString(FastStringBuffer temp) {
            if (open) {
                temp.append('[');
                temp.append(id);
                temp.append(' ');
            } else {
                temp.append(' ');
                temp.append(id);
                temp.append(']');
            }
        }

    }

    public static class Scopes {

        public static int TYPE_COMMENT = 1;
        public static final int TYPE_PEER = 2;
        public static final int TYPE_STRING = 3;
        public static final int TYPE_MODULE = 4;
        public static final int TYPE_SUITE = 5;

        /**
         * Structure mapping the offset to the scope entries at that offset.
         * 
         * At a given position, opening entries should appear before the position and closing entries after the position.
         */
        private Map<Integer, List<ScopeEntry>> offsetToEntries = new HashMap<Integer, List<ScopeEntry>>();
        private int scopeId = 0;
        private Map<Integer, Tuple<ScopeEntry, ScopeEntry>>  idToStartEnd = new HashMap<Integer, Tuple<ScopeEntry,ScopeEntry>>();

        private List<ScopeEntry> getAtOffset(int offset) {
            List<ScopeEntry> list = offsetToEntries.get(offset);
            if (list == null) {
                list = new ArrayList<ScopeEntry>();
                offsetToEntries.put(offset, list);
            }
            return list;
        }
        
        public IRegion getScopeForSelection(final int offset, final int len) {
            final int endOffset = offset+len-1;
            for(int i=offset;i>=0;i--){
                //We have to get a scope that starts before the current offset and ends after offset+len
                //If it's the same, we must expand to an outer scope!
                List<ScopeEntry> list = offsetToEntries.get(i);
                if(list != null){
                    ListIterator<ScopeEntry> listIterator = list.listIterator(list.size());
                    while(listIterator.hasPrevious()){
                        ScopeEntry scopeEntry = listIterator.previous();
                        if(scopeEntry.open){
                            //Only interested in the opening ones at this point
                            Tuple<ScopeEntry, ScopeEntry> tup = idToStartEnd.get(scopeEntry.id);
                            if(i == offset && endOffset == tup.o2.offset){
                                continue;
                            }
                            if(endOffset > tup.o2.offset){
                                continue;
                            }
                            
                            return new Region(tup.o1.offset, tup.o2.offset-tup.o1.offset+1);
                        }
                    }
                }
            }
            return null;
        }


        public int startScope(int offset, int type) {
            scopeId++;
            List<ScopeEntry> list = getAtOffset(offset);
            ScopeEntry startEntry = new ScopeEntry(scopeId, type, true, offset);
            list.add(startEntry);
            idToStartEnd.put(scopeId, new Tuple(startEntry, null));
            return scopeId;
        }

        public void endScope(int id, int offset, int type) {
            offset--;
            List<ScopeEntry> list = getAtOffset(offset);
            ScopeEntry endEntry = new ScopeEntry(id, type, false, offset);
            idToStartEnd.get(id).o2 = endEntry;
            list.add(endEntry);
        }

        public FastStringBuffer debugString(Object doc) {
            ParsingUtils utils = ParsingUtils.create(doc);
            FastStringBuffer temp = new FastStringBuffer(utils.len() + (utils.len() / 10));

            int len = utils.len();
            for (int i = 0; i < len; i++) {
                char c = utils.charAt(i);
                printEntries(temp, i, true);
                temp.append(c);
                printEntries(temp, i, false);
            }
            return temp;
        }

        private void printEntries(FastStringBuffer temp, int i, boolean opening) {
            List<ScopeEntry> list = offsetToEntries.get(i);
            if (list != null) {
                for (ScopeEntry e : list) {
                    if (e.open == opening) {
                        e.toString(temp);
                    }
                }
            }
        }
    }

    public static Scopes createScopes(IDocument doc) {
        ScopesParser scopesParser = new ScopesParser(doc);
        try {
            return scopesParser.createScopes();
        } catch (SyntaxErrorException e) {
            throw new RuntimeException(e);
        }
    }

    private Scopes scopes;
    private SortedMap<Integer, Integer> lineOffsetToIndent = new TreeMap<Integer, Integer>();
    private IDocument doc;

    private ScopesParser(IDocument doc) {
        this.scopes = new Scopes();
        this.doc = doc;

        try {
            TabNannyDocIterator nannyDocIterator = new TabNannyDocIterator(doc, true, false);
            while (nannyDocIterator.hasNext()) {
                Tuple3<String, Integer, Boolean> next = nannyDocIterator.next();
                this.lineOffsetToIndent.put(next.o2, next.o1.length());
            }
        } catch (BadLocationException e1) {
            throw new RuntimeException(e1);
        }

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
        FastStringBuffer buf = new FastStringBuffer();
        FastStringBuffer lineMemo = new FastStringBuffer();
        int memoStart = 0;
        int id;

        for (; offset < docLen; offset++) {
            char ch = parsingUtils.charAt(offset);

            switch (ch) {

            case '#':
                id = this.scopes.startScope(offsetDelta + offset, Scopes.TYPE_COMMENT);
                offset = parsingUtils.eatComments(buf.clear(), offset);
                this.scopes.endScope(id, offsetDelta + offset, Scopes.TYPE_COMMENT);
                break;

            case '{':
            case '[':
            case '(':
                int baseOffset = offset;
                try {
                    offset = parsingUtils.eatPar(offset, buf.clear(), ch); //If a SyntaxError is raised here, we won't create a scope!
                    id = this.scopes.startScope(offsetDelta + baseOffset + 1, Scopes.TYPE_PEER);
                    
                    try {
                        String cs = doc.get(offsetDelta+baseOffset+1, offset-baseOffset-1);
                        createInternalScopes(ParsingUtils.create(cs, true), offsetDelta+baseOffset+1);
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
                    offset = parsingUtils.eatLiterals(buf.clear(), offset); //If a SyntaxError is raised here, we won't create a scope!
                    id = this.scopes.startScope(offsetDelta + baseOffset, Scopes.TYPE_STRING);
                    this.scopes.endScope(id, offsetDelta + offset + 1, Scopes.TYPE_STRING);
                } catch (SyntaxErrorException e1) {
                    
                }
                break;

            case ':':
                if (PySelection.startsWithIndentToken(lineMemo.toString().trim())) {
                    SortedMap<Integer, Integer> subMap = lineOffsetToIndent.tailMap(offsetDelta + memoStart + 1);
                    Integer level = lineOffsetToIndent.get(offsetDelta + memoStart);
                    if(level == null){
                        //It's a ':' inside a parens
                        continue;
                    }

                    Set<Entry<Integer, Integer>> entrySet = subMap.entrySet();
                    boolean found = false;
                    id = this.scopes.startScope(memoStart + level, Scopes.TYPE_SUITE);
                    
                    int id2 = -1;
                    for(int j=offset+1;j<docLen;j++ ){
                        char c = parsingUtils.charAt(j);
                        if(Character.isWhitespace(c)){
                            continue;
                        }
                        if(c == '#'){
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
                            if(id2 >0){
                                this.scopes.endScope(id2, scopeEndOffset, Scopes.TYPE_SUITE);
                            }
                            break;
                        }
                    }
                    if (!found) {
                        //Ends at the end of the document!
                        this.scopes.endScope(id, offsetDelta + parsingUtils.len(), Scopes.TYPE_SUITE);
                        if(id2 >0){
                            this.scopes.endScope(id2,  offsetDelta + parsingUtils.len(), Scopes.TYPE_SUITE);
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
