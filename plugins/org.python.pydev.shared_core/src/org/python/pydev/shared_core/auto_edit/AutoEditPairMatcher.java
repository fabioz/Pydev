/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.auto_edit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.python.pydev.shared_core.partitioner.PartitionCodeReader;
import org.python.pydev.shared_core.string.ICharacterPairMatcher2;
import org.python.pydev.shared_core.string.StringUtils;

public class AutoEditPairMatcher extends DefaultCharacterPairMatcher implements ICharacterPairMatcher2 {

    private final char[] fChars;
    private final String contentType;

    public AutoEditPairMatcher(char[] chars, String contentType) {
        super(chars, contentType);
        this.fChars = chars;
        this.contentType = contentType;
    }

    public int searchForClosingPeer(int offset, char openingPeer, char closingPeer, IDocument document) {
        try {
            PartitionCodeReader reader = new PartitionCodeReader(contentType);
            reader.configureForwardReader(document, offset, document.getLength());

            int stack = 1;
            int c = reader.read();
            while (c != PartitionCodeReader.EOF) {
                if (c == openingPeer && c != closingPeer) {
                    stack++;
                } else if (c == closingPeer) {
                    stack--;
                }

                if (stack <= 0) { //<= 0 because if we have a closing peer without an opening one, we'll return it.
                    return reader.getOffset();
                }

                c = reader.read();
            }

            return -1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int searchForOpeningPeer(int offset, char openingPeer, char closingPeer, IDocument document) {
        try {
            PartitionCodeReader fReader = new PartitionCodeReader(contentType);
            fReader.configureBackwardReader(document, offset);

            int stack = 1;
            int c = fReader.read();
            while (c != PartitionCodeReader.EOF) {
                if (c == closingPeer && c != openingPeer) {
                    stack++;
                } else if (c == openingPeer) {
                    stack--;
                }

                if (stack <= 0) {//<= 0 because if we have an opening peer without a closing one, we'll return it.
                    return fReader.getOffset();
                }

                c = fReader.read();
            }

            return -1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int searchForAnyOpeningPeer(int offset, IDocument document) {
        try {
            PartitionCodeReader fReader = new PartitionCodeReader(contentType);
            fReader.configureBackwardReader(document, offset);

            Map<Character, Integer> stack = new HashMap<Character, Integer>();

            HashSet<Character> closing = new HashSet<Character>();
            HashSet<Character> opening = new HashSet<Character>();

            for (int i = 0; i < fChars.length; i++) {
                stack.put(fChars[i], 1);
                if (i % 2 == 0) {
                    opening.add(fChars[i]);
                } else {
                    closing.add(fChars[i]);
                }
            }

            int c = fReader.read();
            while (c != PartitionCodeReader.EOF) {
                if (closing.contains((char) c)) { // c == ')' || c == ']' || c == '}' 
                    char peer = StringUtils.getPeer((char) c);
                    Integer iStack = stack.get(peer);
                    iStack++;
                    stack.put(peer, iStack);

                } else if (opening.contains((char) c)) { //c == '(' || c == '[' || c == '{'
                    Integer iStack = stack.get((char) c);
                    iStack--;
                    stack.put((char) c, iStack);

                    if (iStack == 0) {
                        return fReader.getOffset();
                    }
                }

                c = fReader.read();
            }

            return -1;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
