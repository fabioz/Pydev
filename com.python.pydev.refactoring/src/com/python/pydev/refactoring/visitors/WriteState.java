/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.IOException;
import java.util.Stack;

import org.python.parser.SimpleNode;

public class WriteState implements IWriterEraser {

    private IWriterEraser writer;
    private PrettyPrinterPrefs prefs;
    private StringBuffer indentation = new StringBuffer();
    private Stack<SimpleNode> stmtStack = new Stack<SimpleNode>();
    
    public final static int INITIAL_STATE = -1;
    public final static int LAST_STATE_NEW_LINE = 0;
    public final static int LAST_STATE_INDENT = 1;
    public final static int LAST_STATE_WRITE = 2;
    
    int lastState=INITIAL_STATE;
    
    public WriteState(IWriterEraser writer, PrettyPrinterPrefs prefs) {
        this.writer = writer;
        this.prefs = prefs;
    }

    public void indent() {
        indentation.append(prefs.getIndent());
    }

    public void dedent() {
        int len = indentation.length();
        indentation.delete(len-prefs.getIndent().length(), len);
    }

    public void writeIndentString() throws IOException {
        lastState = LAST_STATE_INDENT;
        writer.write(prefs.getIndent());
    }
    
    public void writeIndent() throws IOException {
        lastState = LAST_STATE_INDENT;
        writer.write(indentation.toString());
    }
    
    public void writeNewLine() throws IOException {
        lastState = LAST_STATE_NEW_LINE;
        writer.write(prefs.getNewLine());
    }

    public void writeIndent(int i) throws IOException {
        lastState = LAST_STATE_INDENT;
        writeIndent();
        String indent = prefs.getIndent();
        for (int j = 0; j < i; j++) {
            writer.write(indent);
        }
        
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
		if(indentation.toString().length() > 0){
			writer.erase(prefs.getIndent());
		}
	}

    public void write(String o) {
        lastState = LAST_STATE_WRITE;
        writer.write(o);
    }

    public void erase(String o) {
        writer.erase(o);
    }

    public void pushTempBuffer() {
        writer.pushTempBuffer();
    }

    public String popTempBuffer() {
        return writer.popTempBuffer();
    }

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

    public void writeWithoutChangingState(String string) {
        writer.write(string);
    }
}
