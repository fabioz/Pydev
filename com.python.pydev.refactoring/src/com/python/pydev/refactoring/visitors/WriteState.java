/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.IOException;
import java.util.Stack;

import org.python.parser.ast.stmtType;

public class WriteState {

    private IWriterEraser writer;
    private PrettyPrinterPrefs prefs;
    private StringBuffer indentation = new StringBuffer();
    private Stack<stmtType> stmtStack = new Stack<stmtType>();
    
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

    public void writeIndent() throws IOException {
        writer.write(indentation.toString());
    }
    
    public void writeNewLine() throws IOException {
        writer.write(prefs.getNewLine());
    }

    public void writeIndent(int i) throws IOException {
        writeIndent();
        String indent = prefs.getIndent();
        for (int j = 0; j < i; j++) {
            writer.write(indent);
        }
        
    }

	public void pushInStmt(stmtType node) {
		stmtStack.push(node);
	}

	public stmtType popInStmt() {
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


}
