/*
 * Created on Feb 11, 2006
 */
package org.python.pydev.parser.prettyprinterv2;

import java.io.IOException;

import org.python.pydev.core.structure.FastStack;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.prettyprinter.IPrettyPrinterPrefs;
import org.python.pydev.parser.prettyprinter.IWriterEraser;

public class WriteStateV2 implements IWriterEraser {

    private IWriterEraser writer;
    private IPrettyPrinterPrefs prefs;
    private StringBuffer indentation = new StringBuffer();
    private FastStack<SimpleNode> stmtStack = new FastStack<SimpleNode>();
    private boolean nextMustBeNewLineOrComment=false;
    
    public final static int INITIAL_STATE = -1;
    public final static int LAST_STATE_NEW_LINE = 0;
    public final static int LAST_STATE_INDENT = 1;
    public final static int LAST_STATE_WRITE = 2;
    
    private int lastWrite = 0;
    
    int lastState=INITIAL_STATE;
    
    public WriteStateV2(IWriterEraser writer, IPrettyPrinterPrefs prefs) {
        this.writer = writer;
        this.prefs = prefs;
    }
    
    public String getIndentString(){
        return indentation.toString();
    }
    
    public int getIndentLen(){
        return indentation.length();
    }
    
    public String getIndentChars(int numberOfChars){
        return indentation.toString().substring(indentation.length()-numberOfChars);
    }

    public void indent() {
        indentation.append(prefs.getIndent());
    }

    public void dedent() {
        int len = indentation.length();
        int indentLen = prefs.getIndent().length();
        try{
            indentation.delete(len-indentLen, len);
        }catch(Exception e){
            e.printStackTrace();
        }
        eraseIndent();
    }
    

    public void pushInStmt(SimpleNode node) {
        stmtStack.push(node);
    }

    public SimpleNode popInStmt() {
        return stmtStack.pop();
    }

    public boolean inStmt() {
        return stmtStack.size() > 0;
    }

    public void eraseIndent() {
        if(indentation.length() > 0){
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
        if(force || lastState == LAST_STATE_WRITE){
            FastStringBuffer buffer = writer.getBuffer();
            if(buffer.endsWith(": ")){
                buffer.deleteLast();
            }
            
            if(lastState == LAST_STATE_NEW_LINE){
                this.writeIndent();
            }
            
            this.nextMustBeNewLineOrComment = false;
            lastState = LAST_STATE_NEW_LINE;
            writer.write(prefs.getNewLine());
            lastWrite++;
            return true;
        }
        return false;
    }
//
//    public void writeIndent(int i) throws IOException {
//        lastState = LAST_STATE_INDENT;
//        writeIndent();
//        String indent = prefs.getIndent();
//        for (int j = 0; j < i; j++) {
//            writer.write(indent);
//            lastWrite++;
//        }
//        
//    }
    
    
    /**
     * Writes something, but indents if the last thing written was a new line.
     */
    public void write(String o) throws IOException {
        if(nextMustBeNewLineOrComment && this.getBuffer().length() > 0 && lastState != LAST_STATE_NEW_LINE && lastState != LAST_STATE_INDENT){
            if(!o.trim().startsWith("#")){
                this.writeNewLine();
            }
        }
        nextMustBeNewLineOrComment = false;
        if(lastState == LAST_STATE_NEW_LINE){
            this.writeIndent();
        }
        writeRaw(o);
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
        if(lastState == LAST_STATE_WRITE){
            if(!this.writer.endsWithSpace()){
                writeRaw(prefs.getSpacesBeforeComment());
            }
        }
    }


    
    // Erase
    public void erase(String o) {
        writer.erase(o);
    }

    
    // Temp buffer
    public void pushTempBuffer() {
        writer.pushTempBuffer();
    }

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

    
    public int getLastWrite(){
        return lastWrite;
    }


    public void requireNextNewLineOrComment() {
        this.nextMustBeNewLineOrComment=true;
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
