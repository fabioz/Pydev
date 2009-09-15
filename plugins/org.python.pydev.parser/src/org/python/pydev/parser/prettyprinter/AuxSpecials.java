///*
// * Created on Feb 11, 2006
// */
//package org.python.pydev.parser.prettyprinter;
//
//import java.io.IOException;
//import java.util.Iterator;
//
//import org.python.pydev.parser.jython.SimpleNode;
//import org.python.pydev.parser.jython.SpecialStr;
//import org.python.pydev.parser.jython.ast.commentType;
//import org.python.pydev.parser.jython.ast.exprType;
//
///**
// * This class is used as a helper to write special tokens, such as comments and other literals.
// */
//public class AuxSpecials {
//
//    
//    private WriteState state;
//    private PrettyPrinterPrefs prefs;
//    private int writeSpecialsUntilLine=-1;
//    private int writeSpecialsFromLine=-1;
//    private boolean writeCommentsBeforeEnabled = true;
//
//    public AuxSpecials(WriteState state, PrettyPrinterPrefs prefs) {
//        this.state = state;
//        this.prefs = prefs;
//    }
//
//    public void writeSpecialsBefore(SimpleNode node) throws IOException {
//        writeSpecialsBefore(node, null, null, true);
//    }
//    public void writeSpecialsBefore(SimpleNode node, String[] ignore, String[] write, boolean writeComments) throws IOException {
//        writeSpecialsBefore(node, ignore, write, writeComments, true);
//    }
//    public void writeSpecialsBefore(SimpleNode node, String[] ignore, String[] write, boolean writeComments, boolean writeNoComents) throws IOException {
//        if(node == null||node.specialsBefore == null){
//            return;
//        }
//        for (Object c : node.specialsBefore){
//            if(c instanceof commentType){
//                if(writeComments && this.writeCommentsBeforeEnabled){
//                    state.write(((commentType)c).id);
//                    state.writeNewLine();
//                    state.writeIndent();
//                }
//            }else if(c instanceof String){
//                if(writeNoComents){
//                    String str = (String) c;
//                    if(canWrite(str, ignore, write)){
//                        state.write(prefs.getReplacement(str));
//                    }
//                }
//            }else if(c instanceof SpecialStr){
//                if(writeNoComents){
//                    SpecialStr s = (SpecialStr) c;
//                    String str = s.str;
//                    if(canWrite(str, ignore, write)){
//                        state.write(prefs.getReplacement(str));
//                    }
//                }
//            }else{
//                throw new RuntimeException("Unexpected special: "+node);
//            }
//        }
//    }
//
//    private boolean canWrite(String str, String[] ignore, String[] write) {
//        if(ignore == null && write == null){
//            return true;
//        }
//        //ignore is a black-list
//        if(ignore != null){
//            for (String s : ignore) {
//                if(s.equals(str)){
//                    return false;
//                }
//            }
//        }
//        //write is a white-list
//        if(write != null){
//            for (String s : write) {
//                if(s.equals(str)){
//                    return true;
//                }
//            }
//            return false;
//        }
//        return true;
//    }
//
//    public void writeSpecialsAfter(SimpleNode node) throws IOException {
//        writeSpecialsAfter(node, true);
//    }
//    
//    public void writeSpecialsAfter(SimpleNode node, boolean isNewScope) throws IOException {
//        int line = node.beginLine;
//        
//        if(node.specialsAfter == null){
//            return;   
//        }
//        
//        for (Object o : node.specialsAfter){
//            if(o instanceof commentType){
//                commentType c = (commentType)o;
//                
//                //Let's see if we can actually write it...
//                if(this.writeSpecialsUntilLine != -1){
//                    if(c.beginLine > this.writeSpecialsUntilLine){
//                        continue;
//                    }
//                }
//                if(this.writeSpecialsFromLine != -1){
//                    if(c.beginLine < this.writeSpecialsFromLine){
//                        continue;
//                    }
//                }
//                
//                
//                if(c.beginLine > line){
//                    if(state.writeNewLine(false)){
//                        if(isNewScope){
//                            state.writeIndent(1);
//                        }else{
//                            state.writeIndent();
//                        }
//                    }
//                }else{
//                    state.writeSpacesBeforeComment();
//                }
//                
//                int indentLen = state.getIndentLen(); //0-based
//                int beginColForComment = c.beginColumn; //1-based
//                if(beginColForComment-1 < indentLen){
//                    //leave the comment in the old indent.
//                    int diff = indentLen - (beginColForComment-1);
//                    if(diff >= indentLen){
//                        //Only do that if we have some indentation there
//                        state.erase(state.getIndentChars(diff));
//                    }
//                }
//                
//                
//                state.write(c.id);
//                
//                state.writeNewLine();
//                line = c.beginLine + 1;
//                
//                state.writeIndent();
//                
//                
//            }else if(o instanceof SpecialStr){
//                SpecialStr s = (SpecialStr) o;
//                state.write(prefs.getReplacement(s.str));
//                line = s.beginLine;
//                
//                
//            }else if(o instanceof String){
//                state.write(prefs.getReplacement((String)o));
//                
//            }else{
//                throw new RuntimeException("Unexpected special: "+node);
//            }
//        }
//    }
//
//    public void writeStringsAfter(SimpleNode node) throws IOException {
//        if(node.specialsAfter == null){
//            return;
//        }
//        for (Object o : node.specialsAfter){
//            if(o instanceof String){
//                state.write(prefs.getReplacement((String)o));
//            }else if(o instanceof SpecialStr){
//                state.write(prefs.getReplacement(o.toString()));
//            }
//        }
//    }
//
//    
//    public void writeCommentsAfter(SimpleNode node) throws IOException {
//        if(node.specialsAfter == null){
//            return;
//        }
//        for (Object o : node.specialsAfter){
//            if(o instanceof commentType){
//                commentType type = (commentType) o;
//                if(type.beginColumn == 1 && state.lastIsIndent()){
//                    state.eraseIndent();
//                }
//                state.write(type.id);
//                state.writeNewLine();
//                state.writeIndent();
//            }
//        }
//    }
//    
//    public void writeComentsBefore(SimpleNode node) throws IOException {
//        if(node.specialsBefore == null){
//            return;
//        }
//        for (Object o : node.specialsBefore){
//            if(o instanceof commentType){
//                commentType type = (commentType) o;
//                if(type.beginColumn == 1 && state.lastIsIndent()){
//                    state.eraseIndent();
//                }
//                state.write(type.id);
//                state.writeNewLine();
//                state.writeIndent();
//            }
//        }    
//    }
//
//    public boolean hasCommentsAfter(SimpleNode node) {
//        if(node.specialsAfter != null){
//            for (Object o : node.specialsAfter){
//                if(o instanceof commentType){
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//    
//    public void moveComments(SimpleNode from, SimpleNode to, boolean moveBefore, boolean moveAfter) {
//        moveComments(from, to, moveBefore, moveAfter, false);
//    }
//
//    /**
//     * Moves all the comments after a node to the start of the other
//     * @param from comments will be removed from this node
//     * @param to comments will be added to this node
//     */
//    public void moveComments(SimpleNode from, SimpleNode to, boolean moveBefore, boolean moveAfter, boolean onlyInDifferentLine) {
//        if(moveBefore){
//            if(from.specialsBefore != null){
//                for (Iterator<Object> iter = from.specialsBefore.iterator(); iter.hasNext();) {
//                    Object o = iter.next();
//                    if(o instanceof commentType){
//                        to.addSpecial(o, false);
//                        iter.remove();
//                    }
//                }
//            }
//        }
//        
//        if(moveAfter){
//            if(from.specialsAfter != null){
//                for (Iterator<Object> iter = from.specialsAfter.iterator(); iter.hasNext();) {
//                    Object o = iter.next();
//                    if(o instanceof commentType){
//                        commentType c = (commentType) o;
//                        if(onlyInDifferentLine){
//                            if(from.beginLine != c.beginLine){
//                                to.addSpecial(o, false);
//                                iter.remove();
//                            }
//                        }else{
//                            to.addSpecial(o, false);
//                            iter.remove();
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    public void setWriteSpecialsUntilLine(int line) {
//        this.writeSpecialsUntilLine = line;
//    }
//
//
//    public void setWriteSpecialsFromLine(int line) {
//        this.writeSpecialsFromLine = line;
//    }
//
//    public void setWriteCommentsBeforeEnabled(boolean enable) {
//        writeCommentsBeforeEnabled  = enable;
//    }
//
//    
//    
//
//    
//}
