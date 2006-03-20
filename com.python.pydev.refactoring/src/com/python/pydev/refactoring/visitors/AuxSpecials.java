/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.SpecialStr;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.exprType;

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
        writeSpecialsBefore(node, null, null, true);
    }
    public void writeSpecialsBefore(SimpleNode node, String[] ignore, String[] write, boolean writeComments) throws IOException {
        for (Object c : node.specialsBefore){
            if(c instanceof commentType){
                if(writeComments){
                    state.write(((commentType)c).id);
                    state.writeNewLine();
                    state.writeIndent();
                    setStateWritten();
                }
            }else if(c instanceof String){
                String str = (String) c;
                if(canWrite(str, ignore, write)){
                    state.write(prefs.getReplacement(str));
                }
            }else if(c instanceof SpecialStr){
            	SpecialStr s = (SpecialStr) c;
            	String str = s.str;
            	if(canWrite(str, ignore, write)){
            	    state.write(prefs.getReplacement(str));
                }
            }else{
                throw new RuntimeException("Unexpected special: "+node);
            }
        }
    }

    private boolean canWrite(String str, String[] ignore, String[] write) {
        if(ignore == null && write == null){
            return true;
        }
        //ignore is a black-list
        if(ignore != null){
            for (String s : ignore) {
                if(s.equals(str)){
                    return false;
                }
            }
        }
        //write is a white-list
        if(write != null){
            for (String s : write) {
                if(s.equals(str)){
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private void setStateWritten() {
        for(AuxState st : auxState){
            st.writtenComment = true;
        }
    }

    public void writeSpecialsAfter(SimpleNode node) throws IOException {
    	writeSpecialsAfter(node, true);
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
                		state.writeIndent();
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
                state.write(((commentType)o).id);
                state.writeNewLine();
                state.writeIndent();
                setStateWritten();
            }
        }
    }
    public boolean hasCommentsAfter(SimpleNode node) {
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

    /**
     * Moves all the comments after a node to the start of the other
     * @param from comments will be removed from this node
     * @param to comments will be added to this node
     */
    public void moveComments(SimpleNode from, SimpleNode to) {
        for (Iterator iter = from.specialsAfter.iterator(); iter.hasNext();) {
            Object o = iter.next();
            if(o instanceof commentType){
                to.addSpecial(o, false);
                iter.remove();
            }
        }
    }



    
}
