package org.python.pydev.parser.jython;

import java.io.IOException;

import org.python.pydev.core.ObjectsPool;
import org.python.pydev.core.ObjectsPool.ObjectsPoolMap;
import org.python.pydev.core.log.Log;

/**
 * An implementation of interface CharStream, where the data is read from a Reader. Completely recreated so that we can read data directly from a String, as the
 * initial implementation was highly inefficient when working only with a string (actually, if it was small, there would be no noticeable
 * delays, but if it became big, then the improvement would be HUGE).
 * 
 * It keeps the same semantics for line and column stuff (and shares the previous approach of keeping a buffer for this info).
 * If we wanted we could optimize it for also taking less memory, but as there is usually not so many concurrent parses, this 
 * is probably not worth it -- and it would probably be a little slower)
 */

public final class FastCharStream implements CharStream {

    private char[] buffer;

    private int bufline[];

    private int bufcolumn[];

    private boolean prevCharIsCR = false;

    private boolean prevCharIsLF = false;

    private int column = 0;

    private int line = 1;

    private int bufpos = -1;

    private int updatePos;
    
    private int tokenBegin;
    
    private static IOException ioException;

    private static final boolean DEBUG = false;

    public FastCharStream(char cs[]) {
        this.buffer = cs;
        this.bufline = new int[cs.length];
        this.bufcolumn = new int[cs.length];
    }
    
    public int getCurrentPos() {
        return bufpos;
    }
    
    public void restorePos(int pos) {
        bufpos = pos;
    }
    
    /**
     * Restores a previous position.
     * Don't forget to restore the level if eof was already found!
     */
    public void restoreLineColPos(final int endLine, final int endColumn) {
        final int initialBufPos = bufpos;
        final int currLine = getEndLine();
        
        int attempts = 0;
        if(currLine < endLine){
            //note: we could do it, but it's not what we want!
            Log.log("Cannot backtrack to a later position -- current line: "+getEndLine()+" requested line:"+endLine);
            return;
        }else if(currLine == endLine && getEndColumn() < endColumn){
            Log.log("Cannot backtrack to a later position -- current col: "+getEndColumn()+" requested col:"+endColumn);
            return;
        }
        
        while((getEndLine() != endLine || getEndColumn() != endColumn) && bufpos >=0){
            attempts += 1;
            bufpos--;
        }
        
        if(bufpos < 0 || getEndLine() != endLine){
            //we couldn't find it. Let's restore the position when we started it.
            bufpos = initialBufPos;
            Log.log("Couldn't backtrack to position: line"+endLine+ " -- col:"+endColumn);
        }
    }



    public final char readChar() throws IOException {
        try {
            bufpos++;
            char r = this.buffer[bufpos];
            
            if(bufpos >= updatePos){
                updatePos++;
                
                //start UpdateLineCol
                column++;
                
                if (prevCharIsLF) {
                    prevCharIsLF = false;
                    line += (column = 1);
                    
                } else if (prevCharIsCR) {
                    
                    prevCharIsCR = false;
                    if (r == '\n') {
                        prevCharIsLF = true;
                    } else {
                        line += (column = 1);
                    }
                }
                
                if(r == '\r'){
                    prevCharIsCR = true;
                    
                }else if(r == '\n'){
                    prevCharIsLF = true;
                    
                }
                
                bufline[bufpos] = line;
                bufcolumn[bufpos] = column;
                //end UpdateLineCol
            }
            
            return r;
        } catch (ArrayIndexOutOfBoundsException e) {
            bufpos--;
            if (ioException == null){
                ioException = new IOException();
            }
            throw ioException;
        }
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
            System.out.println("FastCharStream: backup >>"+amount+"<<");
        }
        bufpos -= amount;
    }

    public final char BeginToken() throws IOException {
        char c = readChar();
        tokenBegin = bufpos;
        if(DEBUG){
            System.out.println("FastCharStream: BeginToken >>"+(int)c+"<<");
        }
        return c;
    }

    private final ObjectsPoolMap interned = new ObjectsPoolMap(); 
    
    public final String GetImage() {
        if (bufpos >= tokenBegin) {
            return ObjectsPool.internLocal(interned, new String(buffer, tokenBegin, bufpos - tokenBegin+1));
        } else {
            return ObjectsPool.internLocal(interned, new String(buffer, tokenBegin, buffer.length - tokenBegin+1));
        }
    }

    public final char[] GetSuffix(int len) {

        char[] ret = new char[len];
        if (len > 0) {
            try {
                int initial = bufpos - len +1;
                if(initial < 0){
                    int initial0 = initial;
                    len += initial;
                    initial = 0;
                    System.arraycopy(buffer, initial, ret, -initial0, len);
                }else{
                    System.arraycopy(buffer, initial, ret, 0, len);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(DEBUG){
            System.out.println("FastCharStream: GetSuffix:"+len+" >>"+new String(ret)+"<<");
        }
        return ret;
    }

    public final void Done() {
        buffer = null;
        bufline = null;
        bufcolumn = null;
    }



}
