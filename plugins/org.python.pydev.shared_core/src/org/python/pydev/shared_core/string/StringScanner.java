/******************************************************************************
* Copyright (C) 2015  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.string;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.python.pydev.shared_core.structure.FastStack;

public class StringScanner implements ICharacterScanner {

    private final String contents;
    private int offset;
    private final int length;

    public StringScanner(String contents) {
        this.contents = contents;
        this.length = contents.length();
    }

    public int read() {
        if (offset < length) {
            // Most common case first.
            char ret = contents.charAt(offset);
            offset += 1;
            return ret;
        }
        if (offset == length) {
            offset++;
            return StringScanner.EOF; //EOF
        }
        // offset > length!
        throw new RuntimeException("Reading past EOF!");
    }

    // peek(0) reads the char at the current offset, peek(-1) reads the previous, peek(1) reads the next
    // and so on (all without changing the current offset).
    public int peek(int i) {
        int checkAt = offset + i;
        if (checkAt >= length || checkAt < 0) {
            return StringScanner.EOF;
        }
        return contents.charAt(checkAt);
    }

    public void unread() {
        offset -= 1;
        if (offset < 0) {
            throw new RuntimeException("Reading before begin of stream.");
        }
    }

    public int getMark() {
        return offset;
    }

    public String getContents() {
        return this.contents;
    }

    final FastStack<char[]> endLevel = new FastStack<>(3);

    public void addLevelFinishingAt(char... endLevelChar) {
        endLevel.push(endLevelChar);
    }

    public void popLevel() {
        endLevel.pop();
    }

    public void setMark(int mark) {
        this.offset = mark;
    }

    public boolean isEndLevelChar(int c) {
        if (!endLevel.isEmpty()) {
            char[] peek = endLevel.peek();
            int len = peek.length;
            for (int i = 0; i < len; i++) {
                if (peek[i] == c) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getLevel() {
        return endLevel.size();
    }

    @Override
    public char[][] getLegalLineDelimiters() {
        throw new AssertionError("Not implemented");
    }

    @Override
    public int getColumn() {
        throw new AssertionError("Not implemented");
    }

}