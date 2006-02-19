/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;


import java.io.IOException;

import org.python.parser.SimpleNode;
import org.python.parser.SpecialStr;
import org.python.parser.ast.Assign;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.BinOp;
import org.python.parser.ast.Call;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.Compare;
import org.python.parser.ast.Dict;
import org.python.parser.ast.For;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.If;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Name;
import org.python.parser.ast.NameTok;
import org.python.parser.ast.Num;
import org.python.parser.ast.Pass;
import org.python.parser.ast.Print;
import org.python.parser.ast.Raise;
import org.python.parser.ast.Return;
import org.python.parser.ast.Str;
import org.python.parser.ast.Subscript;
import org.python.parser.ast.TryExcept;
import org.python.parser.ast.Tuple;
import org.python.parser.ast.UnaryOp;
import org.python.parser.ast.While;
import org.python.parser.ast.Yield;
import org.python.parser.ast.aliasType;
import org.python.parser.ast.argumentsType;
import org.python.parser.ast.decoratorsType;
import org.python.parser.ast.excepthandlerType;
import org.python.parser.ast.exprType;
import org.python.parser.ast.keywordType;
import org.python.parser.ast.stmtType;
import org.python.pydev.core.REF;

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

    
    public PrettyPrinter(PrettyPrinterPrefs prefs, IWriterEraser writer){
        this.prefs = prefs;
        state = new WriteState(writer, prefs);
        auxComment = new AuxSpecials(state, prefs);
    }
    
    
    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        auxComment.startRecord();
        node.module.accept(this);
        
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
        for (SimpleNode target : node.targets) {
            target.accept(this);
        }
        state.write(" = ");
        auxComment.startRecord();
        node.value.accept(this);
        checkEndRecord();

        if(auxComment.hasCommentsAfter(node)){
            auxComment.startRecord();
            auxComment.writeSpecialsAfter(node);
            checkEndRecord();
        }
        
        state.popInStmt();
        fixNewStatementCondition();
        return null;
    }
    
    
    @Override
    public Object visitBinOp(BinOp node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        state.pushInStmt(node);
        node.left.accept(this);
        state.write(operatorMapping[node.op]);
        node.right.accept(this);
        state.popInStmt();

        if(!state.inStmt()){
            auxComment.startRecord();
        }
        auxComment.writeSpecialsAfter(node);
        
        if(!state.inStmt()){
            checkEndRecord();
        }
        return null;
    }
    
    @Override
    public Object visitCompare(Compare node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        node.left.accept(this);
        
        for(int op : node.ops){
            state.write(cmpop[op]);
        }
        
        for (SimpleNode n : node.comparators){
            n.accept(this);
        }
        auxComment.writeSpecialsAfter(node);
        return null;
    }
    
    
    @Override
    public Object visitDict(Dict node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        exprType[] keys = node.keys;
        exprType[] values = node.values;
        //start a new record because if there is an outer record it will not want to know about that...
        auxComment.startRecord();
        for (int i = 0; i < values.length; i++) {
            keys[i].accept(this);
            values[i].accept(this);
        }
        auxComment.endRecord();
        auxComment.writeSpecialsAfter(node);
        return null;
    }
    
    @Override
    public Object visitList(org.python.parser.ast.List node) throws Exception{
        return visitGeneric(node, "visitList", false);
    }
    
    
    @Override
    public Object visitWhile(While node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        state.indent();
        auxComment.startRecord();
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
            auxComment.startRecord();
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
    public Object visitFor(For node) throws Exception {
        //for a in b: xxx else: yyy
        
        state.pushInStmt(node);
        //for
        auxComment.writeSpecialsBefore(node);
        state.indent();
        auxComment.startRecord();
        
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
            auxComment.startRecord();
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
    public Object visitUnaryOp(UnaryOp node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        state.write(node.operand.toString());
        auxComment.writeSpecialsAfter(node);
        return null;
    }

    @Override
    public Object visitTuple(Tuple node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        //start a new record because if there is an outer record it will not want to know about that...
        auxComment.startRecord();
        super.visitTuple(node);
        auxComment.endRecord();
        auxComment.writeSpecialsAfter(node);
        return null;
    }

    
    
    
    @Override
    public Object visitRaise(Raise node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        auxComment.startRecord();
        super.visitRaise(node);
        afterNode(node);
        return null;
    }
    
    @Override
    public Object visitTryExcept(TryExcept node) throws Exception {
        state.indent();
        //try:
        auxComment.writeSpecialsBefore(node);
        fixNewStatementCondition();
        
        for(stmtType st:node.body){
            st.accept(this);
        }
        fixNewStatementCondition();

        dedent();
        //except:
        auxComment.writeSpecialsAfter(node);
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
            fixNewStatementCondition();
            for (stmtType st : h.body) {
                st.accept(this);
            }
            dedent();
            auxComment.writeSpecialsAfter(h);
        }
        if(node.orelse != null){
            auxComment.startRecord();
            auxComment.writeSpecialsBefore(node.orelse);
            state.indent();
            afterNode(node.orelse);
            for (stmtType st : node.orelse.body){
                st.accept(this);
            }
            auxComment.writeSpecialsAfter(node.orelse);
            dedent();
        }
        return null;
    }
    
    @Override
    public Object visitSubscript(Subscript node) throws Exception {
        return visitGeneric(node, "visitSubscript", false);
    }
    
    
    @Override
    public Object visitReturn(Return node) throws Exception {
        return super.visitReturn(node);
    }

    @Override
    public Object visitPrint(Print node) throws Exception {
        return visitGeneric(node, "visitPrint");
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
        state.pushInStmt(node);
        node.func.accept(this);
        state.popInStmt();
        auxComment.writeSpecialsBefore(node);
        exprType[] args = node.args;
        state.indent();
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null){
                state.pushInStmt(args[i]);
                args[i].accept(this);
                state.popInStmt();
            }
        }
        dedent();
        state.pushInStmt(node);
        keywordType[] keywords = node.keywords;
        if (keywords != null) {
            for (int i = 0; i < keywords.length; i++) {
                if (keywords[i] != null){
                    auxComment.writeSpecialsBefore(keywords[i]);
                    state.indent();
                    keywords[i].accept(this);
                    auxComment.writeSpecialsAfter(keywords[i]);
                    dedent();
                }
            }
        }
        exprType starargs = node.starargs;
        if (starargs != null){
            starargs.accept(this);
        }
        exprType kwargs = node.kwargs;
        if (kwargs != null){
            kwargs.accept(this);
        }
        state.popInStmt();
        auxComment.writeCommentsAfter(node);
        if(state.lastIsIndent()){ //we must indent one more level (because we had the dedent)
            state.writeIndent(1);
        }
        auxComment.writeStringsAfter(node);
        if(!state.inStmt()){
            state.writeNewLine();
        }
        return null;
    }
    
    @Override
    public Object visitIf(If node) throws Exception {
        auxComment.writeSpecialsBefore(node);
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
            if(node.specialsAfter.contains(new SpecialStr("else:",0,0))){ //the SpecialStr only compares with its String
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
    public Object visitStr(Str node) throws Exception {
    	auxComment.writeSpecialsBefore(node);
        if(node.unicode){
            state.write("u");
        }
        if(node.raw){
            state.write("r");
        }
        final String s = strTypes[node.type-1];
        
    	state.write(s);
    	state.write(node.s);
    	state.write(s);
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

        auxComment.startRecord();
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
        auxComment.writeSpecialsAfter(node);
        fixNewStatementCondition();
        return null;
    }


    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        fixNewStatementCondition();
        decoratorsType[] decs = node.decs;
        for (decoratorsType dec : decs) {
            auxComment.writeSpecialsBefore(dec);
            auxComment.writeSpecialsAfter(dec);
        }
        auxComment.writeSpecialsBefore(node);
        auxComment.writeSpecialsBefore(node.name);
        state.write("def ");
        
        
        NameTok name = (NameTok) node.name;
        state.write(name.id);
        auxComment.writeSpecialsAfter(node);
        auxComment.writeSpecialsAfter(node.name);
        state.indent();
        
        {
        	//arguments
            auxComment.startRecord();
        	makeArgs(node.args.args, node.args);
        	//end arguments
            if(!fixNewStatementCondition()){
                if(!auxComment.endRecord().writtenComment){
                    state.writeIndentString();
                }
            }else{
                auxComment.endRecord();
            }
            for(SimpleNode n: node.body){
                n.accept(this);
            }
        
            dedent();
        }
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
                    type.specialsAfter.add(0, state.popTempBuffer());
                    type.specialsAfter.add(0, "=");
                }else{
                    state.popTempBuffer();
                }
            }
            type.accept(this);
            i++;
            state.popInStmt();
        }
    }


    @Override
    public Object visitYield(Yield node) throws Exception {
        return visitGeneric(node, "visitYield");
    }

    @Override
    public Object visitPass(Pass node) throws Exception {
        return visitGeneric(node, "visitPass");
    }
    
    @Override
    public Object visitImport(Import node) throws Exception {
        return visitGeneric(node, "visitImport");
    }

    @Override
    public Object visitNum(Num node) throws Exception {
        return visitGeneric(node, "visitNum", false, node.n.toString());
    }

    @Override
    public Object visitName(Name node) throws Exception {
        return visitGeneric(node, "visitName", false, node.id);
//        auxComment.writeSpecialsBefore(node);
//        state.write(node.id);
//        auxComment.writeSpecialsAfter(node);
//        return null;
    }
    
    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        state.write(node.id);
        auxComment.writeSpecialsAfter(node);
        return null;
    }


    @Override
    public String toString() {
        return state.toString();
    }
}
