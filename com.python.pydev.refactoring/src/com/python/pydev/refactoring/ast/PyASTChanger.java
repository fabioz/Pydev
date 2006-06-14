/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.stmtType;

/**
 * This class should be used to do changes in the ast.
 *  
 * @author Fabio
 */
public class PyASTChanger extends AbstractPyASTChanger {

    /**
     * @param doc the document we're dealing with.
     */
    public PyASTChanger(IDocument doc) {
        super(doc);
    }

    public PyASTChanger(IDocument doc, SimpleNode ast) {
        super(doc, ast);
    }

    /**
     * Adds an add statement change
     * 
     * @param node the node where the change will be made
     * @param attr the attribute (that must be an array of stmtType) where the change will be done
     * @param pos the position of the array where the change should be done
     * @param stmt the statement that should be added
     */
    public void addStmtToNode(SimpleNode node, String attr, int pos, stmtType stmt, boolean addNewLineAfterStmt) {
        changes.add(new AddStmtChange(node, attr, pos, stmt, addNewLineAfterStmt));
    }

    /**
     * Adds a delete statement change
     * 
     * @param node the node where the change will be made
     * @param attr the attribute (that must be an array of stmtType) where the change will be done
     * @param pos the position of the array where the change should be done
     */
	public void delStmtFromNode(SimpleNode node, String attr, int pos) {
		changes.add(new DelStmtChange(node, attr, pos));
	}

    /**
     * Adds a delete statement change
     * 
     * @param node the node that will be deleted
     */
    public void delStmt(SimpleNode node) {
        changes.add(new DelStmtChange(node));
    }
    
}
