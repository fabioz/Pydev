/*
 * Created on Feb 15, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.IOException;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Pass;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.Yield;

public class PrettyPrinterUtils extends VisitorBase{

    protected PrettyPrinterPrefs prefs;
    protected WriteState state;
    protected AuxSpecials auxComment;

    
    protected void checkEndRecord() throws IOException {
        afterNode(null);
    }

    /**
     * @param node this is the node that should 'end recording'. We will write the specials following
     * it if it is not null.
     */
    protected void afterNode(SimpleNode node) throws IOException {
        if(node != null){
            auxComment.writeSpecialsAfter(node);
        }
    
        if(!auxComment.endRecord().writtenComment){
            state.writeNewLine();
            state.writeIndent();
        }
    }

    public static final String[] operatorMapping = new String[] {
            "<undef>",
            " + ",
            " - ",
            " * ",
            " / ",
            " % ",
            " ** ",
            " << ",
            " >> ",
            " | ",
            " ^ ",
            " & ",
            " // ",
        };
    public static final String[] cmpop = new String[] {
            "<undef>",
            " == ",
            " != ",
            " < ",
            " <= ",
            " > ",
            " >= ",
            " is ",
            " is not ",
            " in ",
            " not in ",
        };

    /**
     * Does the indent, and when ending the recording, if some comment was written, a new line will be added.
     */
    protected void makeIfIndent() throws IOException {
    	state.indent();
        boolean writtenComment = auxComment.endRecord().writtenComment;
    	if(!writtenComment){
        	state.writeNewLine();
        }
    	state.writeIndent();
    }

    protected static final String[] strTypes = new String[]{
            "'''",
            "\"\"\"",
            "'",
            "\""
        };

    /**
     * 
     */
    protected void dedent() {
        if(state.lastIsIndent()){
            state.eraseIndent();
        }
        state.dedent();
    }

    /**
     * Writes the specials before and starts recording
     */
    protected void beforeNode(SimpleNode node) throws IOException {
        auxComment.writeSpecialsBefore(node);
        auxComment.startRecord();
    }

    /**
     * @throws IOException
     */
    protected void fixNewStatementCondition() throws IOException {
        if(state.lastIsWrite()){
            state.writeNewLine();
            state.writeIndent();
        }else if(state.lastIsNewLine()){
            state.writeIndent();
        }
    }

    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    public Object superYield(Yield node) throws Exception {
        return super.visitYield(node);
    }
    
    public Object superPass(Pass node) throws Exception {
        return super.visitPass(node);
    }
}
