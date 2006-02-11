/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.IOException;
import java.io.Writer;

import org.python.parser.SimpleNode;
import org.python.parser.ast.NameTok;
import org.python.parser.ast.commentType;

/**
 * This class is used as a helper to write special tokens, such as comments and other literals.
 */
public class AuxSpecials {

    private WriteState state;
    private Writer writer;
    private PrettyPrinterPrefs prefs;

    public AuxSpecials(WriteState state, Writer writer, PrettyPrinterPrefs prefs) {
        this.state = state;
        this.writer = writer;
        this.prefs = prefs;
    }

    public void writeSpecialsBefore(SimpleNode node) throws IOException {
        for (Object c : node.specialsBefore){
            state.writeIndent();
            if(c instanceof commentType){
                writer.write(((commentType)c).id);
                state.writeNewLine();
            }else if(c instanceof String){
                writer.write((String)c);
            }else{
                throw new RuntimeException("Unexpected special: "+node);
            }
        }
    }

    public void writeSpecialsAfter(SimpleNode node) throws IOException {
        for (Object o : node.specialsAfter){
            if(o instanceof commentType){
                commentType c = (commentType) o;
                if(node.beginLine != c.beginLine){
                    state.writeIndent();
                }
                writer.write(((commentType)o).id);
                state.writeNewLine();
            }else if(o instanceof String){
                writer.write((String)o);
            }else{
                throw new RuntimeException("Unexpected special: "+node);
            }
        }
    }

    public boolean writeStringsAfter(SimpleNode node) throws IOException {
        boolean written = false;
        for (Object o : node.specialsAfter){
            if(o instanceof String){
                writer.write((String)o);
                written = true;
            }
        }
        return written;
    }

    
    public boolean writeCommentsAfter(SimpleNode node) throws IOException {
        boolean written = false;
        for (Object o : node.specialsAfter){
            if(o instanceof commentType){
                commentType c = (commentType) o;
                if(node.beginLine != c.beginLine){
                    state.writeIndent();
                }
                writer.write(((commentType)o).id);
                state.writeNewLine();
                written = true;
            }
        }
        return written;
    }
    
}
