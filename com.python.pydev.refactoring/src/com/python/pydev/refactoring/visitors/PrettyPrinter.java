/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.Writer;

import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Name;
import org.python.parser.ast.NameTok;
import org.python.parser.ast.Pass;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.exprType;

public class PrettyPrinter extends VisitorBase{

    private PrettyPrinterPrefs prefs;
    private Writer writer;
    private WriteState state;
    private AuxSpecials auxComment;

    public PrettyPrinter(PrettyPrinterPrefs prefs, Writer writer){
        this.prefs = prefs;
        this.writer = writer;
        state = new WriteState(writer, prefs);
        auxComment = new AuxSpecials(state, writer, prefs);
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        auxComment.writeSpecialsBefore(node.name);
        writer.write("class ");
        
        
        NameTok name = (NameTok) node.name;
        writer.write(name.id);
        if(node.bases.length > 0){
            writer.write("(");
            for (exprType expr : node.bases) {
                expr.accept(this);
            }
        }

        auxComment.writeStringsAfter(name);
        state.indent();
        {
            boolean written = auxComment.writeCommentsAfter(name);
            if(!written){
                state.writeNewLine();
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
        state.writeIndent();
        writer.write("def ");
        
        
        NameTok name = (NameTok) node.name;
        writer.write(name.id);
        writer.write("(");

        //arguments
        boolean writtenNewLine = makeArgs(node.args.args);
        //end arguments
        
        if(!writtenNewLine){
            state.writeNewLine();
        }
        
        state.indent();
        {
            for(SimpleNode n: node.body){
                n.accept(this);
            }
        
            state.dedent();
        }
        auxComment.writeSpecialsAfter(node.name);
        auxComment.writeSpecialsAfter(node);
        return null;
    }
    
    private boolean makeArgs(exprType[] args) throws Exception {
        boolean writtenNewLine = false;
        exprType prev = null;
        for (exprType type : args) {
            writtenNewLine = false;
            if(prev != null && prev.specialsAfter.size() > 1){
                //has some comment (not only ',')
                state.writeIndent(1);
                writtenNewLine = true;
            }
            type.accept(this);
            prev = type;
        }
        return writtenNewLine;
    }

    @Override
    public Object visitPass(Pass node) throws Exception {
        state.writeIndent();
        auxComment.writeSpecialsBefore(node);
        writer.write("pass");
        auxComment.writeSpecialsAfter(node);
        state.writeNewLine();
        return null;
    }
    
    @Override
    public Object visitName(Name node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        writer.write(node.id);
        auxComment.writeStringsAfter(node);
        System.out.println("written:"+auxComment.writeCommentsAfter(node));
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
