/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.stmtType;

/**
 * This class should be used to do changes in the ast.
 *  
 * @author Fabio
 */
public class PyASTChanger {

    /**
     * This is the initial AST
     */
    private SimpleNode initialAst;
    
    /**
     * This is a 'working copy' ast
     */
    private SimpleNode ast;
    
    /**
     * These are the changes that'll apply to the document
     */
    private List<IChanges> changes = new ArrayList<IChanges>();
    
    /**
     * This is the document where the changes will be applied
     */
    private Document doc;

    public PyASTChanger(Document doc) {
        this.doc = doc;
        Tuple<SimpleNode, Throwable> ret = PyParser.reparseDocument(new PyParser.ParserInfo(doc, true, -1));
        initialAst = ret.o1;
        if(ret.o1 == null){
            if(ret.o2 != null){
                throw new RuntimeException(ret.o2);
            }else{
                throw new RuntimeException("Unable to generate ast");
            }
        }
    }

    public SimpleNode getInitialAST() {
        return this.initialAst;
    }

    public void addStmt(SimpleNode m, String attr, int pos, stmtType stmt) {
        changes.add(new AddStmtChange(m, attr, pos, stmt));
    }

    public void apply() {
        ast = initialAst;
        for (IChanges change : changes) {
            try {
                ast = change.apply(ast, doc);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

}
