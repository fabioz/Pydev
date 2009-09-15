///*
// * Created on Feb 11, 2006
// */
//package org.python.pydev.parser.prettyprinter;
//
//import java.io.IOException;
//
//import org.python.pydev.core.structure.FastStack;
//import org.python.pydev.parser.jython.SimpleNode;
//
//public class WriteState implements IWriterEraser {
//
//    private IWriterEraser writer;
//    private PrettyPrinterPrefs prefs;
//    private StringBuffer indentation = new StringBuffer();
//    private FastStack<SimpleNode> stmtStack = new FastStack<SimpleNode>();
//    
//    public final static int INITIAL_STATE = -1;
//    public final static int LAST_STATE_NEW_LINE = 0;
//    public final static int LAST_STATE_INDENT = 1;
//    public final static int LAST_STATE_WRITE = 2;
//    
//    private int lastWrite = 0;
//    
//    int lastState=INITIAL_STATE;
//    
//    public WriteState(IWriterEraser writer, PrettyPrinterPrefs prefs) {
//        this.writer = writer;
//        this.prefs = prefs;
//    }
//    
//    public int getIndentLen(){
//        return indentation.length();
//    }
//    
//    public String getIndentChars(int numberOfChars){
//        return indentation.toString().substring(indentation.length()-numberOfChars);
//    }
//
//    public void indent() {
//        indentation.append(prefs.getIndent());
//    }
//
//    public void dedent() {
//        int len = indentation.length();
//        indentation.delete(len-prefs.getIndent().length(), len);
//    }
//
//    public void writeIndentString() throws IOException {
//        lastState = LAST_STATE_INDENT;
//        writer.write(prefs.getIndent());
//        lastWrite++;
//    }
//    
//    public void writeIndent() throws IOException {
//        lastState = LAST_STATE_INDENT;
//        writer.write(indentation.toString());
//        lastWrite++;
//    }
//    
//    public void writeNewLine() throws IOException {
//        writeNewLine(true);
//    }
//    
//    public boolean writeNewLine(boolean force) throws IOException {
//        if(force || lastState == LAST_STATE_WRITE){
//            lastState = LAST_STATE_NEW_LINE;
//            writer.write(prefs.getNewLine());
//            lastWrite++;
//            return true;
//        }
//        return false;
//    }
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
//
//    public void pushInStmt(SimpleNode node) {
//        stmtStack.push(node);
//    }
//
//    public SimpleNode popInStmt() {
//        return stmtStack.pop();
//    }
//
//    public boolean inStmt() {
//        return stmtStack.size() > 0;
//    }
//
//    public void eraseIndent() {
//        if(indentation.toString().length() > 0){
//            writer.erase(prefs.getIndent());
//        }
//    }
//
//    public void write(String o) throws IOException {
//        lastState = LAST_STATE_WRITE;
//        writer.write(o);
//        lastWrite++;
//    }
//
//    public void erase(String o) {
//        writer.erase(o);
//    }
//
//    public void pushTempBuffer() {
//        writer.pushTempBuffer();
//    }
//
//    public String popTempBuffer() {
//        return writer.popTempBuffer();
//    }
//
//    public boolean lastIsWrite() {
//        return lastState == LAST_STATE_WRITE;
//    }
//
//    public boolean lastIsIndent() {
//        return lastState == LAST_STATE_INDENT;
//    }
//    
//    public boolean lastIsNewLine() {
//        return lastState == LAST_STATE_NEW_LINE;
//    }
//    
//    @Override
//    public String toString() {
//        return writer.toString();
//    }
//
//    public void writeWithoutChangingState(String string) throws IOException {
//        writer.write(string);
//        lastWrite++;
//    }
//    
//    public int getLastWrite(){
//        return lastWrite;
//    }
//
//    public void writeLinesAfterMethod() {
//        for(int i=0;i<prefs.getLinesAfterMethod();i++){
//            try {
//                writeNewLine();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
//    
//    public void writeLinesAfterClass() {
//        for(int i=0;i<prefs.getLinesAfterClass();i++){
//            try {
//                writeNewLine();
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
//
//    public void writeSpacesBeforeComment() throws IOException {
//        if(lastState == LAST_STATE_WRITE){
//            write(prefs.getSpacesBeforeComment());
//        }
//    }
//
//}
