/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 11, 2006
 */
package org.python.pydev.parser.prettyprinterv2;

import java.io.IOException;

import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * Helper to write things in the document marking the last thing written, indent, etc.
 *
 */
public class WriteStateV2 implements IWriterEraser {

    private IWriterEraser writer;
    private IPrettyPrinterPrefs prefs;
    private FastStringBuffer indentation = new FastStringBuffer(40);
    private boolean nextMustBeNewLineOrComment = false;
    private boolean nextMustBeNewLine = true;

    public final static int INITIAL_STATE = -1;
    public final static int LAST_STATE_NEW_LINE = 0;
    public final static int LAST_STATE_INDENT = 1;
    public final static int LAST_STATE_WRITE = 2;

    private int lastWrite = 0;

    int lastState = INITIAL_STATE;

    public WriteStateV2(IWriterEraser writer, IPrettyPrinterPrefs prefs) {
        this.writer = writer;
        this.prefs = prefs;
    }

    public String getIndentString() {
        return indentation.toString();
    }

    public int getIndentLen() {
        return indentation.length();
    }

    public String getIndentChars(int numberOfChars) {
        return indentation.toString().substring(indentation.length() - numberOfChars);
    }

    public void indent() {
        indentation.append(prefs.getIndent());
    }

    public void dedent() {
        int len = indentation.length();
        int indentLen = prefs.getIndent().length();
        try {
            indentation.delete(len - indentLen, len);
        } catch (Exception e) {
            Log.log(e);
        }
        eraseIndent();
    }

    public void eraseIndent() {
        if (indentation.length() > 0) {
            writer.erase(prefs.getIndent());
        }
    }

    //Writing

    public void writeIndent() throws IOException {
        lastState = LAST_STATE_INDENT;
        writer.write(indentation.toString());
        lastWrite++;
    }

    public void writeNewLine() throws IOException {
        writeNewLine(true);
    }

    public boolean writeNewLine(boolean force) throws IOException {
        if (force || lastState == LAST_STATE_WRITE) {
            FastStringBuffer buffer = writer.getBuffer();
            if (buffer.endsWith(": ")) {
                buffer.deleteLast();
            }

            if (lastState == LAST_STATE_NEW_LINE) {
                this.writeIndent();
            }

            this.nextMustBeNewLineOrComment = false;
            this.nextMustBeNewLine = false;
            lastState = LAST_STATE_NEW_LINE;
            writer.write(prefs.getNewLine());
            lastWrite++;
            return true;
        }
        return false;
    }

    /**
     * Writes something, but indents if the last thing written was a new line.
     */
    @Override
    public void write(String o) throws IOException {
        if ((nextMustBeNewLineOrComment || nextMustBeNewLine) && this.getBuffer().length() > 0
                && lastState != LAST_STATE_NEW_LINE && lastState != LAST_STATE_INDENT) {
            if (nextMustBeNewLine) {
                this.writeNewLine();

            } else if (nextMustBeNewLineOrComment && !o.trim().startsWith("#")) {
                this.writeNewLine();
            }
        }
        nextMustBeNewLineOrComment = false;
        nextMustBeNewLine = false;
        if (lastState == LAST_STATE_NEW_LINE) {
            this.writeIndent();
        }
        FastStringBuffer buf = this.getBuffer();
        if (buf.endsWith("\r") || buf.endsWith("\n") || buf.endsWith(" ") || buf.endsWith("\t")) {
            writeRaw(StringUtils.leftTrim(o));

        } else {
            writeRaw(o);
        }
    }

    /**
     * Writes something as it comes (independent on the state)
     */
    public void writeRaw(String o) throws IOException {
        lastState = LAST_STATE_WRITE;
        writer.write(o);
        lastWrite++;
    }

    public void writeWithoutChangingState(String string) throws IOException {
        writer.write(string);
        lastWrite++;
    }

    public void writeSpacesBeforeComment() throws IOException {
        if (lastState == LAST_STATE_WRITE) {
            if (!this.writer.endsWithSpace()) {
                writeRaw(prefs.getSpacesBeforeComment());
            }
        }
    }

    // Erase
    @Override
    public void erase(String o) {
        writer.erase(o);
    }

    // Temp buffer
    @Override
    public void pushTempBuffer() {
        writer.pushTempBuffer();
    }

    @Override
    public String popTempBuffer() {
        return writer.popTempBuffer();
    }

    // State

    public boolean lastIsWrite() {
        return lastState == LAST_STATE_WRITE;
    }

    public boolean lastIsIndent() {
        return lastState == LAST_STATE_INDENT;
    }

    public boolean lastIsNewLine() {
        return lastState == LAST_STATE_NEW_LINE;
    }

    @Override
    public String toString() {
        return writer.toString();
    }

    public int getLastWrite() {
        return lastWrite;
    }

    public void requireNextNewLineOrComment() {
        this.nextMustBeNewLineOrComment = true;
    }

    public void requireNextNewLine() {
        this.nextMustBeNewLine = true;
    }

    @Override
    public boolean endsWithSpace() {
        return this.writer.endsWithSpace();
    }

    @Override
    public FastStringBuffer getBuffer() {
        return writer.getBuffer();
    }

}
