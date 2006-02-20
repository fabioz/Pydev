/*
 * Created on Feb 15, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.python.parser.SimpleNode;
import org.python.parser.ast.BoolOp;
import org.python.parser.ast.Break;
import org.python.parser.ast.Continue;
import org.python.parser.ast.Import;
import org.python.parser.ast.List;
import org.python.parser.ast.Pass;
import org.python.parser.ast.Print;
import org.python.parser.ast.Subscript;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.Yield;
import org.python.pydev.core.REF;

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
            if(!state.inStmt()){
                fixNewStatementCondition();
            }
        }
    }
    
    public static final String[] boolOperatorMapping = new String[] {
        "<undef>",
        " and ",
        " or ",
    };

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
     * @return whether we changed something in this method or not.
     * @throws IOException
     */
    protected boolean fixNewStatementCondition() throws IOException {
        if(state.lastIsWrite()){
            state.writeNewLine();
            state.writeIndent();
            return true;
        }else if(state.lastIsNewLine()){
            state.writeIndent();
            return true;
        }
        return false;
    }

    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    //useful for some reflection tricks, so that we can reuse some chunks of code...
    // -- sometimes I fell that python would make that so much easier... --
    public final static Map<String, Method> superMethods = new HashMap<String, Method>();
    public static void addMethod(String methodRep, String parentRep){
        superMethods.put(methodRep, REF.findMethod(PrettyPrinterUtils.class, parentRep, new Object[]{SimpleNode.class}));
    }
    static{
        addMethod("visitYield" , "superYield" );
        addMethod("visitPass"  , "superPass"  );
        addMethod("visitImport", "superImport");
        addMethod("visitPrint" , "superPrint");
        addMethod("visitSubscript" , "superSubscript");
        addMethod("visitList" , "superList");
        addMethod("visitBreak" , "superBreak");
        addMethod("visitContinue" , "superContinue");
        addMethod("visitBoolOp" , "superBoolOp");
    }
    
    
    public Object visitGeneric(SimpleNode node, String superMethod) throws IOException{
        return visitGeneric(node, superMethod, true);
    }
    
    public Object visitGeneric(SimpleNode node, String superMethod, boolean requiresNewLine) throws IOException{
        return visitGeneric(node, superMethod, requiresNewLine, null);
    }
    
    public Object visitGeneric(SimpleNode node, String superMethod, boolean requiresNewLine, String strToWrite) throws IOException{
        if(requiresNewLine){
            fixNewStatementCondition();
        }
        beforeNode(node);
        state.pushInStmt(node);
        if(strToWrite != null){
            state.write(strToWrite);
        }else{
            REF.invoke(this, superMethods.get(superMethod), node);
        }
        state.popInStmt();
        afterNode(node);
        return null;
    }

    public Object superYield(Yield node) throws Exception {
        return super.visitYield(node);
    }
    
    public Object superPass(Pass node) throws Exception {
        return super.visitPass(node);
    }
    
    public Object superImport(Import node) throws Exception {
        return super.visitImport(node);
    }
    
    public Object superPrint(Print node) throws Exception {
        return super.visitPrint(node);
    }
    
    public Object superSubscript(Subscript node) throws Exception {
        return super.visitSubscript(node);
    }
    
    public Object superList(List node) throws Exception {
        return super.visitList(node);
    }
    
    public Object superBreak(Break node) throws Exception {
        return super.visitBreak(node);
    }
    
    public Object superContinue(Continue node) throws Exception {
        return super.visitContinue(node);
    }
    
    public Object superBoolOp(BoolOp node) throws Exception {
        return super.visitBoolOp(node);
    }
}
