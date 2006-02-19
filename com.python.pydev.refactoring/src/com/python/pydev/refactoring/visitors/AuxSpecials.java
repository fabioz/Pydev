/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.IOException;
import java.util.Stack;

import org.python.parser.SimpleNode;
import org.python.parser.SpecialStr;
import org.python.parser.ast.Assign;
import org.python.parser.ast.commentType;

/**
 * This class is used as a helper to write special tokens, such as comments and other literals.
 */
public class AuxSpecials {

    public static class AuxState{
        boolean writtenComment;
    }
    
    private WriteState state;
    private PrettyPrinterPrefs prefs;
    private Stack<AuxState> auxState = new Stack<AuxState>();

    public AuxSpecials(WriteState state, PrettyPrinterPrefs prefs) {
        this.state = state;
        this.prefs = prefs;
    }

    public void writeSpecialsBefore(SimpleNode node) throws IOException {
        for (Object c : node.specialsBefore){
            if(c instanceof commentType){
                state.write(((commentType)c).id);
                state.writeNewLine();
                state.writeIndent();
                setStateWritten();
            }else if(c instanceof String){
                state.write(prefs.getReplacement((String)c));
            }else if(c instanceof SpecialStr){
            	SpecialStr s = (SpecialStr) c;
            	state.write(prefs.getReplacement(s.str));
            }else{
                throw new RuntimeException("Unexpected special: "+node);
            }
        }
    }

    private void setStateWritten() {
        for(AuxState st : auxState){
            st.writtenComment = true;
        }
    }

    public void writeSpecialsAfter(SimpleNode node) throws IOException {
    	writeSpecialsAfter(node, false);
    }
    public void writeSpecialsAfter(SimpleNode node, boolean isNewScope) throws IOException {
    	int line = node.beginLine;
    	
        for (Object o : node.specialsAfter){
            if(o instanceof commentType){
                commentType c = (commentType)o;
                if(c.beginLine > line){
                	state.writeNewLine();
                	if(isNewScope){
                		state.writeIndent(1);
                	}else{
                		state.writeIndent(1);
                	}
                }
				state.write(c.id);
                state.writeNewLine();
                line = c.beginLine + 1;
                
                state.writeIndent();
                setStateWritten();
                
            }else if(o instanceof SpecialStr){
            	SpecialStr s = (SpecialStr) o;
            	state.write(prefs.getReplacement(s.str));
            	line = s.beginLine;
            	
            }else if(o instanceof String){
                state.write(prefs.getReplacement((String)o));
            }else{
                throw new RuntimeException("Unexpected special: "+node);
            }
        }
    }

    public void writeStringsAfter(SimpleNode node) throws IOException {
        for (Object o : node.specialsAfter){
            if(o instanceof String){
                state.write(prefs.getReplacement((String)o));
            }else if(o instanceof SpecialStr){
            	state.write(prefs.getReplacement(o.toString()));
            }
        }
    }

    
    public void writeCommentsAfter(SimpleNode node) throws IOException {
        for (Object o : node.specialsAfter){
            if(o instanceof commentType){
                commentType c = (commentType) o;
                state.write(((commentType)o).id);
                state.writeNewLine();
                state.writeIndent();
                setStateWritten();
            }
        }
    }
    public boolean hasCommentsAfter(Assign node) {
        for (Object o : node.specialsAfter){
            if(o instanceof commentType){
                return true;
            }
        }
        return false;
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

    public boolean inRecord() {
        return auxState.size() > 0;
    }
    
    @Override
    public String toString() {
        return "AuxSpecials<auxState size="+auxState.size()+">";
    }


    
}
