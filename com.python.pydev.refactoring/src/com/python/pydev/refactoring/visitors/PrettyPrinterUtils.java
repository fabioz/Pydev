/*
 * Created on Feb 15, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.python.pydev.core.REF;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.BoolOp;
import org.python.pydev.parser.jython.ast.Break;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.Continue;
import org.python.pydev.parser.jython.ast.Delete;
import org.python.pydev.parser.jython.ast.Exec;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.Index;
import org.python.pydev.parser.jython.ast.Lambda;
import org.python.pydev.parser.jython.ast.List;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Print;
import org.python.pydev.parser.jython.ast.Raise;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.Slice;
import org.python.pydev.parser.jython.ast.StrJoin;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.Yield;

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
    public static final String[] unaryopOperatorMapping = new String[] {
        "<undef>",
        "Invert",
        "not ",
        "UAdd",
        "-",
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
    
    public static final String[] augOperatorMapping = new String[] {
        "<undef>",
        " += ",
        " -= ",
        " *= ",
        " /= ",
        " %= ",
        " **= ",
        " <<= ",
        " >>= ",
        " |= ",
        " ^= ",
        " &= ",
        " //= ",
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
        addMethod("visitReturn" , "superReturn");
        addMethod("visitSlice" , "superSlice");
        addMethod("visitIndex" , "superIndex");
        addMethod("visitDelete" , "superDelete");
        addMethod("visitListComp" , "superListComp");
        addMethod("visitExec" , "superExec");
        addMethod("visitTuple" , "superTuple");
        addMethod("visitLambda" , "superLambda");
        addMethod("visitComprehension" , "superComprehension");
        addMethod("visitRaise" , "superRaise");
        addMethod("visitStrJoin" , "superStrJoin");
        addMethod("visitAssert" , "superAssert");
        addMethod("visitGlobal" , "superGlobal");
        addMethod("visitWith" , "superWith");
    }
    
    
    public Object visitGeneric(SimpleNode node, String superMethod) throws IOException{
        return visitGeneric(node, superMethod, true);
    }
    
    public Object visitGeneric(SimpleNode node, String superMethod, boolean requiresNewLine) throws IOException{
        return visitGeneric(node, superMethod, requiresNewLine, null);
    }
    
    public Object visitGeneric(SimpleNode node, String superMethod, boolean requiresNewLine, String strToWrite) throws IOException{
        return visitGeneric(node, superMethod, requiresNewLine, strToWrite, false);
    }
    
    public Object visitGeneric(SimpleNode node, String superMethod, boolean requiresNewLine, String strToWrite, boolean needIndent) throws IOException{
        if(needIndent){
            state.indent();
        }
        genericBefore(node, requiresNewLine);
        if(strToWrite != null){
            state.write(strToWrite);
        }else{
            REF.invoke(this, superMethods.get(superMethod), node);
        }
        genericAfter(node);
        if(needIndent){
            dedent();
        }
        return null;
    }

	protected void genericAfter(SimpleNode node) throws IOException {
		state.popInStmt();
        afterNode(node);
	}

	protected void genericBefore(SimpleNode node, boolean requiresNewLine) throws IOException {
		if(requiresNewLine){
            fixNewStatementCondition();
        }
        beforeNode(node);
        state.pushInStmt(node);
	}

    public Object superGlobal(Global node) throws Exception {
        return super.visitGlobal(node);
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
    
    public Object superReturn(Return node) throws Exception {
    	return super.visitReturn(node);
    }
    
    public Object superSlice(Slice node) throws Exception {
        return super.visitSlice(node);
    }
    
    public Object superIndex(Index node) throws Exception {
        return super.visitIndex(node);
    }
    
    public Object superDelete(Delete node) throws Exception {
    	return super.visitDelete(node);
    }
    
    public Object superListComp(ListComp node) throws Exception {
    	return super.visitListComp(node);
    }
    
    public Object superExec(Exec node) throws Exception {
        return super.visitExec(node);
    }
    
    public Object superTuple(Tuple node) throws Exception {
        return super.visitTuple(node);
    }
    
    public Object superLambda(Lambda node) throws Exception {
        return super.visitLambda(node);
    }
    
    public Object superComprehension(Comprehension node) throws Exception {
        return super.visitComprehension(node);
    }
    
    public Object superRaise(Raise node) throws Exception {
        return super.visitRaise(node);
    }
    
    public Object superStrJoin(StrJoin node) throws Exception {
        return super.visitStrJoin(node);
    }
    
    public Object superAssert(Assert node) throws Exception {
        return super.visitAssert(node);
    }
    
    public Object superWith(With node) throws Exception {
        return super.visitWith(node);
    }
    
}
