/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.IOException;
import java.util.Stack;

import org.python.parser.SimpleNode;
import org.python.parser.ast.commentType;

/**
 * This class is used as a helper to write special tokens, such as comments and other literals.
 */
public class AuxSpecials {

    public static class AuxState{
        boolean writtenComment;
    }
    
    private WriteState state;
    private IWriterEraser writer;
    private PrettyPrinterPrefs prefs;
    private Stack<AuxState> auxState = new Stack<AuxState>();

    public AuxSpecials(WriteState state, IWriterEraser writer, PrettyPrinterPrefs prefs) {
        this.state = state;
        this.writer = writer;
        this.prefs = prefs;
    }

    public void writeSpecialsBefore(SimpleNode node) throws IOException {
        for (Object c : node.specialsBefore){
            if(c instanceof commentType){
                writer.write(((commentType)c).id);
                state.writeNewLine();
                state.writeIndent();
                setStateWritten();
            }else if(c instanceof String){
                writer.write(prefs.getReplacement((String)c));
            }else{
                throw new RuntimeException("Unexpected special: "+node);
            }
        }
    }

    private void setStateWritten() {
        if(auxState.size() > 0){
            auxState.peek().writtenComment = true;
        }
    }

    public void writeSpecialsAfter(SimpleNode node) throws IOException {
        for (Object o : node.specialsAfter){
            if(o instanceof commentType){
                writer.write(((commentType)o).id);
                state.writeNewLine();
                state.writeIndent();
                setStateWritten();
            }else if(o instanceof String){
                writer.write(prefs.getReplacement((String)o));
            }else{
                throw new RuntimeException("Unexpected special: "+node);
            }
        }
    }

    public void writeStringsAfter(SimpleNode node) throws IOException {
        for (Object o : node.specialsAfter){
            if(o instanceof String){
                writer.write(prefs.getReplacement((String)o));
            }
        }
    }

    
    public void writeCommentsAfter(SimpleNode node) throws IOException {
        for (Object o : node.specialsAfter){
            if(o instanceof commentType){
                commentType c = (commentType) o;
                writer.write(((commentType)o).id);
                state.writeNewLine();
                state.writeIndent();
                setStateWritten();
            }
        }
    }

    public AuxState startRecord() {
        return auxState.push(new AuxState());
    }

    public AuxState endRecord() {
        return auxState.pop();
    }

    public boolean wasNewLineWritten() {
        return auxState.peek().writtenComment;
    }

    
}
