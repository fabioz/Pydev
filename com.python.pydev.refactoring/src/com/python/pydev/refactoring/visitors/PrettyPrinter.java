/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.IOException;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Assign;
import org.python.parser.ast.BinOp;
import org.python.parser.ast.Call;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.Compare;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.If;
import org.python.parser.ast.Name;
import org.python.parser.ast.NameTok;
import org.python.parser.ast.Num;
import org.python.parser.ast.Pass;
import org.python.parser.ast.Str;
import org.python.parser.ast.UnaryOp;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.argumentsType;
import org.python.parser.ast.exprType;
import org.python.parser.ast.keywordType;

public class PrettyPrinter extends VisitorBase{

    protected PrettyPrinterPrefs prefs;
    private IWriterEraser writer;
    private WriteState state;
    private AuxSpecials auxComment;

    public PrettyPrinter(PrettyPrinterPrefs prefs, IWriterEraser writer){
        this.prefs = prefs;
        this.writer = writer;
        state = new WriteState(writer, prefs);
        auxComment = new AuxSpecials(state, writer, prefs);
    }
    
    @Override
    public Object visitAssign(Assign node) throws Exception {
    	state.pushInStmt(node);
        auxComment.writeSpecialsBefore(node);
        for (SimpleNode target : node.targets) {
            target.accept(this);
        }
        writer.write(" = ");
        auxComment.startRecord();
        node.value.accept(this);
        auxComment.writeSpecialsAfter(node);
        
        if(!auxComment.endRecord().writtenComment){
            state.writeNewLine();
            state.writeIndent();
        }
        state.popInStmt();
        return null;
    }
    
    @Override
    public Object visitUnaryOp(UnaryOp node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        writer.write(node.operand.toString());
        auxComment.writeSpecialsAfter(node);
        return null;
    }
    
    
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

    @Override
    public Object visitBinOp(BinOp node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        state.pushInStmt(node);
        node.left.accept(this);
        writer.write(operatorMapping[node.op]);
        node.right.accept(this);
        state.popInStmt();

        auxComment.startRecord();
        auxComment.writeSpecialsAfter(node);
        
        if(!state.inStmt()){
            if(!auxComment.endRecord().writtenComment){
                state.writeNewLine();
                state.writeIndent();
            }
        }
        return null;
    }
    
    public static final String[] cmpop= new String[] {
        "<undef>",
        " == ",
        " != ",
        "Lt",
        "LtE",
        "Gt",
        "GtE",
        " is ",
        " is not ",
        " in ",
        " not in ",
    };

    @Override
    public Object visitCompare(Compare node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        node.left.accept(this);
        
        for(int op : node.ops){
            writer.write(cmpop[op]);
        }
        
        for (SimpleNode n : node.comparators){
            n.accept(this);
        }
        auxComment.writeSpecialsAfter(node);
        return null;
    }
    
    @Override
    public Object visitNum(Num node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        writer.write(node.n.toString());
        auxComment.writeSpecialsAfter(node);
        return null;
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        
        //make the visit
        node.func.accept(this);
        exprType[] args = node.args;
        boolean startedRecord = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null){
                if(i == args.length-1){
                    auxComment.startRecord();
                }
                args[i].accept(this);
            }
        }
        if(args.length == 0){
            auxComment.startRecord();
        }
        keywordType[] keywords = node.keywords;
        if (keywords != null) {
            for (int i = 0; i < keywords.length; i++) {
                if (keywords[i] != null){
                    auxComment.writeSpecialsBefore(keywords[i]);
                    keywords[i].arg.accept(this);
                    writer.write("=");
                    keywords[i].value.accept(this);
                    auxComment.writeSpecialsAfter(keywords[i]);
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
        
        auxComment.writeCommentsAfter(node);
        if(auxComment.endRecord().writtenComment){
            //some comment was written
            state.writeIndent(1);
        }
        auxComment.writeStringsAfter(node);
        state.writeNewLine();
        return null;
    }
    
    @Override
    public Object visitIf(If node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        auxComment.startRecord();
        node.test.accept(this);
        
        //write the 'if test:'
        makeIfIndent();
		
		//write the body and dedent
        for (SimpleNode n : node.body){
            auxComment.writeSpecialsBefore(n);
            n.accept(this);
            auxComment.writeSpecialsAfter(n);
        }
        makeEndIfDedent();
        
        
        if(node.orelse != null && node.orelse.length > 0){
        	boolean inElse = false;
        	auxComment.startRecord();
            auxComment.writeSpecialsAfter(node);
            
            //now, if it is an elif, it will end up calling the 'visitIf' again,
            //but if it is an 'else:' we will have to make the indent again
            if(node.specialsAfter.contains("else:")){
            	inElse = true;
            	makeIfIndent();
            }
            for (SimpleNode n : node.orelse) {
                auxComment.writeSpecialsBefore(n);
                n.accept(this);
//                auxComment.writeSpecialsAfter(n); // same as the initial
            }
            if(inElse){
            	makeEndIfDedent();
            }
        }
        
        return null;
    }

	private void makeEndIfDedent() {
		state.eraseIndent();
        state.dedent();
	}

	private void makeIfIndent() throws IOException {
		state.indent();
        boolean writtenComment = auxComment.endRecord().writtenComment;
		if(!writtenComment){
        	state.writeNewLine();
        }
		state.writeIndent();
	}
    
    private static final String[] strTypes = new String[]{
        "'''",
        "\"\"\"",
        "'",
        "\""
    };
    
    @Override
    public Object visitStr(Str node) throws Exception {
    	auxComment.writeSpecialsBefore(node);
        if(node.unicode){
            writer.write("u");
        }
        if(node.raw){
            writer.write("r");
        }
        final String s = strTypes[node.type-1];
        
    	writer.write(s);
    	writer.write(node.s);
    	writer.write(s);
    	if(!state.inStmt()){
	    	state.writeNewLine();
	    	state.writeIndent();
    	}
    	auxComment.writeSpecialsAfter(node);
    	return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        auxComment.writeSpecialsBefore(node.name);
        auxComment.writeSpecialsBefore(node);
        writer.write("class ");
        
        
        NameTok name = (NameTok) node.name;

        auxComment.startRecord();
        writer.write(name.id);
        //we want the comments to be indented too
        state.indent();
        {
        	auxComment.writeSpecialsAfter(name);
    
            if(node.bases.length > 0){
//                writer.write("(");
                for (exprType expr : node.bases) {
                    expr.accept(this);
                }
            }
            if(!auxComment.endRecord().writtenComment){
                state.writeNewLine();
                state.writeIndent();
            }
            for(SimpleNode n: node.body){
                n.accept(this);
            }
        
            state.dedent();
        }   
        auxComment.writeSpecialsAfter(node);
        return null;
    }

    

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        auxComment.writeSpecialsBefore(node.name);
        writer.write("def ");
        
        
        NameTok name = (NameTok) node.name;
        writer.write(name.id);
        auxComment.writeSpecialsAfter(node);
//        writer.write("(");

        state.indent();
        
        {
        	//arguments
        	boolean writtenNewLine = makeArgs(node.args.args, node.args);
        	//end arguments
        	if(!writtenNewLine){
        		state.writeNewLine();
        		state.writeIndent();
        	}
            for(SimpleNode n: node.body){
                n.accept(this);
            }
        
            state.dedent();
        }
        auxComment.writeSpecialsAfter(node.name);
        return null;
    }
    
    private boolean makeArgs(exprType[] args, argumentsType completeArgs) throws Exception {
        boolean written = false;
        exprType[] d = completeArgs.defaults;
        int argsLen = args.length;
        int defaultsLen = d.length;
        int diff = argsLen-defaultsLen;
        
        int i = 0;
        for (exprType type : args) {
            auxComment.startRecord();
            if(i >= diff){
                writer.pushTempBuffer();
                exprType arg = d[i-diff];
                if(arg != null){
                    arg.accept(this);
                    type.specialsAfter.add(0, writer.popTempBuffer());
                    type.specialsAfter.add(0, "=");
                }else{
                    writer.popTempBuffer();
                }
            }
            type.accept(this);
            written = auxComment.endRecord().writtenComment;
            i++;
        }
        return written;
    }

    @Override
    public Object visitPass(Pass node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        writer.write("pass");
        auxComment.startRecord();
        auxComment.writeSpecialsAfter(node);
        if(!auxComment.endRecord().writtenComment){
            state.writeNewLine();
            state.writeIndent();
        }
        return null;
    }
    
    @Override
    public Object visitName(Name node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        writer.write(node.id);
        auxComment.writeStringsAfter(node);
        auxComment.writeCommentsAfter(node);
        return null;
    }
    
    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        writer.write(node.id);
        auxComment.writeSpecialsAfter(node);
        return null;
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

}
