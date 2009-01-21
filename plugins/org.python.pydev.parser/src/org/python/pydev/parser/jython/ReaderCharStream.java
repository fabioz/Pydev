package org.python.pydev.parser.jython;

/**
 * An implementation of interface CharStream, where the data is read from a Reader. This file started life as a copy of ASCII_CharStream.java.
 */

public final class ReaderCharStream implements CharStream {
    int bufsize;

    int available;

    int tokenBegin;

    public int bufpos = -1;

    private int bufline[];

    private int bufcolumn[];

    private int column = 0;

    private int line = 1;

    private boolean prevCharIsCR = false;

    private boolean prevCharIsLF = false;

    private java.io.Reader inputStream;

    private char[] buffer;

    private int maxNextCharInd = 0;

    private int inBuf = 0;
    
    private static final boolean DEBUG = false;

    private final void ExpandBuff(boolean wrapAround) {
        char[] newbuffer = new char[bufsize + 2048];
        int newbufline[] = new int[bufsize + 2048];
        int newbufcolumn[] = new int[bufsize + 2048];

        try {
            if (wrapAround) {
                System.arraycopy(buffer, tokenBegin, newbuffer, 0, bufsize - tokenBegin);
                System.arraycopy(buffer, 0, newbuffer, bufsize - tokenBegin, bufpos);
                buffer = newbuffer;

                System.arraycopy(bufline, tokenBegin, newbufline, 0, bufsize - tokenBegin);
                System.arraycopy(bufline, 0, newbufline, bufsize - tokenBegin, bufpos);
                bufline = newbufline;

                System.arraycopy(bufcolumn, tokenBegin, newbufcolumn, 0, bufsize - tokenBegin);
                System.arraycopy(bufcolumn, 0, newbufcolumn, bufsize - tokenBegin, bufpos);
                bufcolumn = newbufcolumn;

                maxNextCharInd = (bufpos += (bufsize - tokenBegin));
            } else {
                System.arraycopy(buffer, tokenBegin, newbuffer, 0, bufsize - tokenBegin);
                buffer = newbuffer;

                System.arraycopy(bufline, tokenBegin, newbufline, 0, bufsize - tokenBegin);
                bufline = newbufline;

                System.arraycopy(bufcolumn, tokenBegin, newbufcolumn, 0, bufsize - tokenBegin);
                bufcolumn = newbufcolumn;

                maxNextCharInd = (bufpos -= tokenBegin);
            }
        } catch (Throwable t) {
            throw new Error(t.getMessage());
        }

        bufsize += 2048;
        available = bufsize;
        tokenBegin = 0;
    }

    private final void FillBuff() throws java.io.IOException {
        if (maxNextCharInd == available) {
            if (available == bufsize) {
                if (tokenBegin > 2048) {
                    bufpos = maxNextCharInd = 0;
                    available = tokenBegin;
                } else if (tokenBegin < 0)
                    bufpos = maxNextCharInd = 0;
                else
                    ExpandBuff(false);
            } else if (available > tokenBegin)
                available = bufsize;
            else if ((tokenBegin - available) < 2048)
                ExpandBuff(true);
            else
                available = tokenBegin;
        }

        int i;
        try {
            if ((i = inputStream.read(buffer, maxNextCharInd, available - maxNextCharInd)) == -1) {
                inputStream.close();
                throw new java.io.IOException();
            } else
                maxNextCharInd += i;
            return;
        } catch (java.io.IOException e) {
            --bufpos;
            backup(0);
            if (tokenBegin == -1)
                tokenBegin = bufpos;
            throw e;
        }
    }

    public final char BeginToken() throws java.io.IOException {
        tokenBegin = -1;
        char c = readChar();
        tokenBegin = bufpos;

        if(DEBUG){
            System.out.println("ReaderCharStream: BeginToken >>"+(int)c+"<<");
        }
        return c;
    }

    private final void UpdateLineColumn(char c) {
        column++;

        if (prevCharIsLF) {
            prevCharIsLF = false;
            line += (column = 1);
        } else if (prevCharIsCR) {
            prevCharIsCR = false;
            if (c == '\n') {
                prevCharIsLF = true;
            } else
                line += (column = 1);
        }

        switch (c) {
        case '\r':
            prevCharIsCR = true;
            break;
        case '\n':
            prevCharIsLF = true;
            break;
        // ok, this was commented out because the position would not reflect correctly the positions found in the ast.
        // this may have other problems, but they have to be analyzed better to see the problems this may bring
        // (files that mix tabs and spaces may suffer, but I could not find out very well the problems -- anyway,
        // restricting the analysis to files that have only tabs or only spaces seems reasonable -- shortcuts are available
        // so that we can convert a file from one type to another, so, what remains is making some lint analysis to be sure of it).
        // case '\t' :
        // column--;
        // column += (8 - (column & 07));
        // break;
        default:
            break;
        }

        bufline[bufpos] = line;
        bufcolumn[bufpos] = column;
    }

    public final char readChar() throws java.io.IOException {
        if (inBuf > 0) {
            --inBuf;
            return buffer[(bufpos == bufsize - 1) ? (bufpos = 0) : ++bufpos];
        }

        if (++bufpos >= maxNextCharInd)
            FillBuff();

        char c = buffer[bufpos];

        UpdateLineColumn(c);
        if(DEBUG){
            System.out.println("ReaderCharStream: readChar >>"+(int)c+"<<");
        }
        return (c);
    }

    /**
     * @deprecated
     * @see #getEndColumn
     */

    public final int getColumn() {
        return bufcolumn[bufpos];
    }

    /**
     * @deprecated
     * @see #getEndLine
     */

    public final int getLine() {
        return bufline[bufpos];
    }

    public final int getEndColumn() {
        return bufcolumn[bufpos];
    }

    public final int getEndLine() {
        return bufline[bufpos];
    }

    public final int getBeginColumn() {
        return bufcolumn[tokenBegin];
    }

    public final int getBeginLine() {
        return bufline[tokenBegin];
    }

    public final void backup(int amount) {
        if(DEBUG){
            System.out.println("ReaderCharStream: backup >>"+amount+"<<");
        }

        inBuf += amount;
        if ((bufpos -= amount) < 0)
            bufpos += bufsize;
    }

    public ReaderCharStream(java.io.Reader dstream) {
        inputStream = dstream;
        line = 1;
        column = 0;

        available = bufsize = 4096;
        buffer = new char[bufsize];
        bufline = new int[bufsize];
        bufcolumn = new int[bufsize];
    }

    public final String GetImage() {
        String s = null;
        if (bufpos >= tokenBegin)
            s = new String(buffer, tokenBegin, bufpos - tokenBegin + 1);
        else
            s = new String(buffer, tokenBegin, bufsize - tokenBegin) + new String(buffer, 0, bufpos + 1);
        
        if(DEBUG){
            System.out.println("ReaderCharStream: GetImage >>"+s+"<<");
        }
        return s;
    }

    public final char[] GetSuffix(int len) {
        char[] ret = new char[len];

        if ((bufpos + 1) >= len)
            System.arraycopy(buffer, bufpos - len + 1, ret, 0, len);
        else {
            System.arraycopy(buffer, bufsize - (len - bufpos - 1), ret, 0, len - bufpos - 1);
            System.arraycopy(buffer, 0, ret, len - bufpos - 1, bufpos + 1);
        }
        if(DEBUG){
            System.out.println("ReaderCharStream: GetSuffix:"+len+" >>"+new String(ret)+"<<");
        }

        return ret;
    }

    public void Done() {
        buffer = null;
        bufline = null;
        bufcolumn = null;
    }

}
