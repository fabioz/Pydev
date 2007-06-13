/*
 * Created on Feb 11, 2006
 */
package org.python.pydev.parser.prettyprinter;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.SpecialStr;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.AugAssign;
import org.python.pydev.parser.jython.ast.BinOp;
import org.python.pydev.parser.jython.ast.BoolOp;
import org.python.pydev.parser.jython.ast.Break;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.Continue;
import org.python.pydev.parser.jython.ast.Delete;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.Exec;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.IfExp;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Index;
import org.python.pydev.parser.jython.ast.Lambda;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Print;
import org.python.pydev.parser.jython.ast.Raise;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.Slice;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.StrJoin;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.UnaryOp;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.comprehensionType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.excepthandlerType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.suiteType;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * statements that 'need' to be on a new line:
 * print
 * del
 * pass
 * flow
 * import
 * global
 * exec
 * assert
 * 
 * 
 * flow:
 * return
 * yield
 * raise
 * 
 * 
 * compound:
 * if
 * while
 * for
 * try
 * func
 * class
 * 
 * @author Fabio
 */
public class PrettyPrinter extends PrettyPrinterUtils{

    /**
     * If this is true, we don't add a new-line after the statement (when we would normally
     * add a new line for the next statement).
     */
    private boolean isSingleStmt;

    @Override
    protected boolean fixNewStatementCondition() throws IOException {
        boolean ret = false;
        if(!isSingleStmt){
            ret = super.fixNewStatementCondition();
        }
        return ret;
    }

    public PrettyPrinter(PrettyPrinterPrefs prefs, IWriterEraser writer){
        this(prefs, writer, false);
    }
    
    public PrettyPrinter(PrettyPrinterPrefs prefs, IWriterEraser writer, boolean isSingleStmt){
        this.prefs = prefs;
        this.isSingleStmt = isSingleStmt;
        state = new WriteState(writer, prefs);
        auxComment = new AuxSpecials(state, prefs);
    }
    

    @Override
    public Object visitModule(Module node) throws Exception {
        super.visitModule(node);
        if(node.specialsAfter != null){
            for(Object o :node.specialsAfter){
                commentType t = (commentType) o;
                String c = t.id.trim();
                state.write(c);
            }
        }
        return null;
    }
    
    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        
        
        auxComment.writeSpecialsBefore(node.module, new String[0], new String[0], true);
        auxComment.writeSpecialsBefore(node.module, null, null, false);
        
        state.write(((NameTok)node.module).id);
        auxComment.writeSpecialsAfter(node.module);
        
        for (aliasType name : node.names){
            auxComment.writeSpecialsBefore(name);
            name.accept(this);
            auxComment.writeSpecialsAfter(name);
        }
        afterNode(node);
        fixNewStatementCondition();
        return null;
    }
    
    @Override
    public Object visitImport(Import node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        
        for (aliasType name : node.names){
            auxComment.writeSpecialsBefore(name);
            name.accept(this);
            auxComment.writeSpecialsAfter(name);
        }
        afterNode(node);
        return null;
    }

    @Override
    public Object visitAssign(Assign node) throws Exception {
    	state.pushInStmt(node);
        auxComment.writeSpecialsBefore(node);
        for (int i = 0; i < node.targets.length; i++) {
            exprType target = node.targets[i];
            if(i == node.targets.length -1){
                //last one: cannot have comments, if it has, we will have to move them to the next node.
                auxComment.moveComments(target, node.value, false, true);
            }
            if(i >= 1){ //more than one assign
                state.write(" = ");
            }
            target.accept(this);
        }
        state.write(" = ");
        
        node.value.accept(this);

        checkEndRecord();
        if(auxComment.hasCommentsAfter(node)){
            auxComment.writeSpecialsAfter(node, false);
            checkEndRecord();
        }
        
        state.popInStmt();
        fixNewStatementCondition();
        return null;
    }
    
    public void visitBeforeLeft(SimpleNode node) throws IOException{
        auxComment.writeSpecialsBefore(node);
        state.pushInStmt(node);
    }
    public void visitAfterRight(SimpleNode node) throws IOException{
        state.popInStmt();
        
        auxComment.writeSpecialsAfter(node, false);
        
        if(!state.inStmt()){
            checkEndRecord();
        }
        
    }
    
    @Override
    public Object visitAugAssign(AugAssign node) throws Exception {
        visitBeforeLeft(node);
        node.target.accept(this);
        state.write(augOperatorMapping[node.op]);
        node.value.accept(this);
        visitAfterRight(node);
        return null;
    }
    
    @Override
    public Object visitBinOp(BinOp node) throws Exception {
        visitBeforeLeft(node);
        node.left.accept(this);
        state.write(operatorMapping[node.op]);
        node.right.accept(this);
        visitAfterRight(node);
        return null;
    }
    

    @Override
    public Object visitUnaryOp(UnaryOp node) throws Exception {
        visitBeforeLeft(node);
        state.write(unaryopOperatorMapping[node.op]);
        node.operand.accept(this);
        visitAfterRight(node);
        return null;
    }
    

    @Override
    public Object visitBoolOp(BoolOp node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        state.pushInStmt(node);
        for (int i = 0; i < node.values.length-1; i++) {
            node.values[i].accept(this);
            state.write(boolOperatorMapping[node.op]);
        }
        node.values[node.values.length-1].accept(this);
        visitAfterRight(node);
        return null;
    }
    
    @Override
    public Object visitSubscript(Subscript node) throws Exception {
        node.value.accept(this);
        
        visitBeforeLeft(node);
        node.slice.accept(this);
        visitAfterRight(node);
        return null;
    }

    @Override
    public Object visitCompare(Compare node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        node.left.accept(this);
        
        for (int i = 0; i < node.comparators.length; i++) {
            state.write(cmpop[node.ops[i]]);
            node.comparators[i].accept(this);
        }
        
        auxComment.writeSpecialsAfter(node);
        return null;
    }
    
    
    @Override
    public Object visitDict(Dict node) throws Exception {
        state.indent();
        auxComment.writeSpecialsBefore(node);
        exprType[] keys = node.keys;
        exprType[] values = node.values;
        for (int i = 0; i < values.length; i++) {
            prefs.enableSpacesAfterColon();
            keys[i].accept(this);
            values[i].accept(this);
            prefs.disableSpacesAfterColon();
        }
        auxComment.writeSpecialsAfter(node);
        dedent();
        return null;
    }
    
    @Override
    public Object visitLambda(Lambda node) throws Exception {
        genericBefore(node, false);
        state.pushInStmt(node);
        prefs.enableSpacesAfterColon();
        makeArgs(node.args.args, node.args);
        prefs.disableSpacesAfterColon();
        node.body.accept(this);
        state.popInStmt();
        genericAfter(node, false, false);
        return null;
    }
    
    @Override
    public Object visitList(org.python.pydev.parser.jython.ast.List node) throws Exception{
        return visitGeneric(node, "visitList", false, null, true);
    }

    @Override
    public Object visitDelete(Delete node) throws Exception {
    	return visitGeneric(node, "visitDelete", false);
    }

    @Override
    public Object visitListComp(ListComp node) throws Exception {
        beforeNode(node);
        node.elt.accept(this);
        for(comprehensionType c:node.generators){
            c.accept(this);
        }
        afterNode(node);
    	return null;
    }

    private SimpleNode[] reverseNodeArray(SimpleNode[] expressions) {
        java.util.List<SimpleNode> ifs = Arrays.asList(expressions);
        Collections.reverse(ifs);
        SimpleNode[] ifsInOrder = ifs.toArray(new SimpleNode[0]);
        return ifsInOrder;
    }

    @Override
    public Object visitComprehension(Comprehension node) throws Exception {
        beforeNode(node);
        node.target.accept(this);
        node.iter.accept(this);
        for(SimpleNode s:reverseNodeArray(node.ifs)){
            s.accept(this);
        }
        afterNode(node);
        return null;
    }
    
    @Override
    public Object visitExpr(Expr node) throws Exception {
        return visitGeneric(node, "visitExpr", false, null, false, false);
    }
    
    @Override
    public Object visitWhile(While node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        state.indent();
        state.pushInStmt(node.test);
        node.test.accept(this);
        state.popInStmt();
        afterNode(node);
        fixNewStatementCondition();
        for(SimpleNode n: node.body){
            n.accept(this);
        }
        dedent();
        
        if(node.orelse != null){
            state.indent();
            auxComment.writeSpecialsBefore(node.orelse);
            afterNode(node.orelse);
            node.orelse.accept(this);
            dedent();
        }
        return null;
    }
    
    
    @Override
    public Object visitFor(For node) throws Exception {
        //for a in b: xxx else: yyy
        
        state.pushInStmt(node);
        //for
        auxComment.writeSpecialsBefore(node);
        state.indent();
        
        //a
        node.target.accept(this);
        
        //in b
        state.pushInStmt(node.iter);
        node.iter.accept(this);
        state.popInStmt();
        afterNode(node);
        state.popInStmt();
        
        fixNewStatementCondition();
        for(SimpleNode n: node.body){
            n.accept(this);
        }
        dedent();
        
        if(node.orelse != null){
            state.indent();
            auxComment.writeSpecialsBefore(node.orelse);
            auxComment.writeSpecialsAfter(node.orelse);
            afterNode(node.orelse);
            node.orelse.accept(this);
            dedent();
        }
        return null;
    }


    @Override
    public Object visitTuple(Tuple node) throws Exception {
        visitGeneric(node, "visitTuple", false, null, true);
        return null;
    }

    
    
    
    @Override
    public Object visitRaise(Raise node) throws Exception {
        visitGeneric(node, "visitRaise", true);
        return null;
    }

    
    public void visitTryPart(SimpleNode node, stmtType[] body) throws Exception{
        //try:
        auxComment.writeSpecialsBefore(node);
        state.indent();
        fixNewStatementCondition();
        
        for(stmtType st:body){
            st.accept(this);
        }
        fixNewStatementCondition();

        dedent();
        auxComment.writeSpecialsAfter(node);

    }
    
    public void visitOrElsePart(suiteType orelse) throws Exception{
        if(orelse != null){
            auxComment.writeSpecialsBefore(orelse);
            state.indent();
            afterNode(orelse);
            for (stmtType st : orelse.body){
                st.accept(this);
            }
            auxComment.writeSpecialsAfter(orelse);
            dedent();
        }
    }
    @Override
    public Object visitExec(Exec node) throws Exception {
        return visitGeneric(node, "visitExec");
    }
    
    @Override
    public Object visitTryFinally(TryFinally node) throws Exception {
        visitTryPart(node, node.body);
        visitOrElsePart(node.finalbody);
        return null;
    }

    @Override
    public Object visitTryExcept(TryExcept node) throws Exception {
        visitTryPart(node, node.body);
        for(excepthandlerType h:node.handlers){
            state.pushInStmt(h);
            state.indent();
            auxComment.writeSpecialsBefore(h);
            
            if(h.type != null || h.name != null){
                state.write(" ");
            }
            if(h.type != null){
                h.type.accept(this);
            }
            if(h.name != null){
                h.name.accept(this);
            }
            state.popInStmt();
            auxComment.writeSpecialsAfter(h);
            fixNewStatementCondition();
            for (stmtType st : h.body) {
                st.accept(this);
            }
            dedent();
        }
        visitOrElsePart(node.orelse);
        return null;
    }
    
    @Override
    public Object visitSlice(Slice node) throws Exception {
        return visitGeneric(node, "visitSlice", false);
    }
    
    @Override
    public Object visitIndex(Index node) throws Exception {
        return visitGeneric(node, "visitIndex", false);
    }
    
    @Override
    public Object visitReturn(Return node) throws Exception {
    	return visitGeneric(node, "visitReturn", true);
    }

    @Override
    public Object visitPrint(Print node) throws Exception {
        return visitGeneric(node, "visitPrint", true, null, false, false);
    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        state.pushInStmt(node);
        auxComment.writeSpecialsBefore(node);
        node.value.accept(this);
        state.write(".");
        node.attr.accept(this);
        auxComment.writeSpecialsAfter(node);
        state.popInStmt();
        return null;
    }
    

    @Override
    public Object visitCall(Call node) throws Exception {
        
        //make the visit
        auxComment.writeSpecialsBefore(node, new String[]{"("}, null, false);
        state.pushInStmt(node);
        node.func.accept(this);
        state.popInStmt();
        auxComment.writeSpecialsBefore(node, null, new String[]{"("}, true);
        
        //print the arguments within the call
        printCallArguments(node, node.args, node.keywords, node.starargs, node.kwargs);
        
        auxComment.writeSpecialsAfter(node);
        if(!state.inStmt()){
            fixNewStatementCondition();
        }
        return null;
    }

    
    @Override
    public Object visitIf(If node) throws Exception {
        fixNewStatementCondition();
        auxComment.writeSpecialsBefore(node);
        auxComment.moveComments(node.test, node.body[0], false, true, true);
        state.pushInStmt(node.test);
        node.test.accept(this);
        state.popInStmt();
        
        //write the 'if test:'
        state.indent();
        if(!fixNewStatementCondition()){
            state.writeIndentString();
        }
		
		//write the body and dedent
        for (SimpleNode n : node.body){
            n.accept(this);
        }
        dedent();
        
        
        if(node.orelse != null && node.orelse.length > 0){
        	boolean inElse = false;
            auxComment.writeSpecialsAfter(node);
            
            //now, if it is an elif, it will end up calling the 'visitIf' again,
            //but if it is an 'else:' we will have to make the indent again
            if(node.specialsAfter != null && node.specialsAfter.contains(new SpecialStr("else:",0,0))){ //the SpecialStr only compares with its String
            	inElse = true;
            	state.indent();
                if(!fixNewStatementCondition()){
                    state.writeIndentString();
                }
            }
            for (SimpleNode n : node.orelse) {
                n.accept(this);
            }
            if(inElse){
            	dedent();
            }
        }
        
        return null;
    }

    @Override
    public Object visitStrJoin(StrJoin node) throws Exception {
        return super.visitGeneric(node, "visitStrJoin", false);
    }
    
    @Override
    public Object visitAssert(Assert node) throws Exception {
        auxComment.moveComments(node.test, node, true, false);
        return super.visitGeneric(node, "visitAssert", true, null, false, false);
    }
    
	@Override
    public Object visitStr(Str node) throws Exception {
    	auxComment.writeSpecialsBefore(node);
        
    	state.write(NodeUtils.getStringToPrint(node));
    	if(!state.inStmt()){
            fixNewStatementCondition();
    	}
    	auxComment.writeSpecialsAfter(node);
    	return null;
    }
    

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        fixNewStatementCondition();

        auxComment.writeSpecialsBefore(node.name);
        auxComment.writeSpecialsBefore(node);
        state.write("class ");
        
        
        NameTok name = (NameTok) node.name;

        state.write(name.id);
        //we want the comments to be indented too
        state.indent();
        {
        	auxComment.writeSpecialsAfter(name);
    
            if(node.bases.length > 0){
                for (exprType expr : node.bases) {
                    state.pushInStmt(expr);
                    expr.accept(this);
                    state.popInStmt();
                }
            }
            checkEndRecord();
            for(SimpleNode n: node.body){
                n.accept(this);
            }
        
            dedent();
        }   
        auxComment.writeSpecialsAfter(node, false);
        fixNewStatementCondition();
        state.writeLinesAfterClass();
        return null;
    }

    public void visitNode(SimpleNode node) throws Exception{
        if(node != null){
            beforeNode(node);
            node.accept(this);
            afterNode(node);
        }
    }
    
    public Object visitDecoratorsType(decoratorsType node) throws Exception {
        beforeNode(node);
        visitNode(node.func);
        if (node.args != null) {
            for (int i = node.args.length-1; i >= 0; i--) {
                if (node.args[i] != null)
                    node.args[i].accept(this);
            }
        }
        if (node.keywords != null) {
            for (int i = node.keywords.length-1; i >= 0; i--) {
                if (node.keywords[i] != null)
                    visitNode(node.keywords[i]);
            }
        }
        if (node.starargs != null)
            node.starargs.accept(this);
        if (node.kwargs != null)
            node.kwargs.accept(this);
        afterNode(node);
        return null;
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        decoratorsType[] decs = node.decs;
        for (decoratorsType dec : decs) {
            auxComment.writeSpecialsBefore(dec);
            fixNewStatementCondition();
            state.write("@");
            state.pushInStmt(node);
            visitDecoratorsType(dec);
            state.popInStmt();
            auxComment.writeSpecialsAfter(dec);
        }
        fixNewStatementCondition();
        auxComment.writeSpecialsBefore(node);
        state.write("def ");
        
        state.indent();
        int lastWrite = state.getLastWrite();
        node.name.accept(this);
        auxComment.writeStringsAfter(node);
        
        {
        	//arguments
        	makeArgs(node.args.args, node.args);
        	//end arguments
            if(!fixNewStatementCondition()){
                if(lastWrite == state.getLastWrite()){
                    state.writeIndentString();
                }
            }
            
            for(SimpleNode n: node.body){
                n.accept(this);
            }
        
            dedent();
        }
        auxComment.writeCommentsAfter(node);
        state.writeLinesAfterMethod();
        return null;
    }

    protected void makeArgs(exprType[] args, argumentsType completeArgs) throws Exception {
        exprType[] d = completeArgs.defaults;
        int argsLen = args.length;
        int defaultsLen = d.length;
        int diff = argsLen-defaultsLen;
        
        int i = 0;
        for (exprType type : args) {
            state.pushInStmt(type);
            if(i >= diff){
                state.pushTempBuffer();
                exprType arg = d[i-diff];
                if(arg != null){
                    arg.accept(this);
                    type.getSpecialsAfter().add(0, state.popTempBuffer());
                    type.getSpecialsAfter().add(0, "=");
                }else{
                    state.popTempBuffer();
                }
            }
            type.accept(this);
            i++;
            state.popInStmt();
        }

        if(completeArgs.vararg != null){
        	completeArgs.vararg.accept(this);
        }
        if(completeArgs.kwarg != null){
        	completeArgs.kwarg.accept(this);
        }
        
    }

    
    @Override
    public Object visitWith(With node) throws Exception{
        //with a as b: print b
        state.pushInStmt(node);
        
        //with
        auxComment.writeSpecialsBefore(node);
        state.indent();
        
        //with a
        node.context_expr.accept(this);
        
        //as b:
        if(node.optional_vars != null){
            node.optional_vars.accept(this);
        }
        state.popInStmt();
        
        //in b
        afterNode(node);
        
        fixNewStatementCondition();
        for(SimpleNode n: node.body.body){
            n.accept(this);
        }
        dedent();
        
        return null;
    }


    @Override
    public Object visitYield(Yield node) throws Exception {
        return visitGeneric(node, "visitYield");
    }
    
    @Override
    public Object visitGlobal(Global node) throws Exception {
        return visitGeneric(node, "visitGlobal");
    }

    @Override
    public Object visitPass(Pass node) throws Exception {
        return visitGeneric(node, "visitPass");
    }
    
    @Override
    public Object visitNum(Num node) throws Exception {
        return visitGeneric(node, "visitNum", false, node.n.toString());
    }

    @Override
    public Object visitName(Name node) throws Exception {
        return visitGeneric(node, "visitName", false, node.id, false, false);
    }

    @Override
    public Object visitBreak(Break node) throws Exception {
        return visitGeneric(node, "visitBreak");
    }
    
    @Override
    public Object visitContinue(Continue node) throws Exception {
        return visitGeneric(node, "visitContinue");
    }
    
    @Override
    public Object visitIfExp(IfExp node) throws Exception {
        //we have to change the order a bit...
        node.body.accept(this);
        node.test.accept(this);
        if(node.orelse != null){
            node.orelse.accept(this);
        }
        return null;
    }
    
    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        state.write(node.id);
        auxComment.writeSpecialsAfter(node, false);
        return null;
    }


    @Override
    public String toString() {
        return state.toString();
    }
}
