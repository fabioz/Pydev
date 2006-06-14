/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;

public abstract class AbstractPyASTChanger {

    /**
     * This is a 'working copy' ast
     */
    protected SimpleNode ast;
    
    /**
     * These are the changes that'll apply to the document
     */
    protected List<IChanges> changes = new ArrayList<IChanges>();
    
    /**
     * This is the document where the changes will be applied
     */
    protected IDocument doc;

    public AbstractPyASTChanger(IDocument doc, SimpleNode ast) {
        this.doc = doc;
        this.ast = ast;
    }
    
    public AbstractPyASTChanger(IDocument doc) {
        this.doc = doc;
        Tuple<SimpleNode, Throwable> ret = PyParser.reparseDocument(new PyParser.ParserInfo(doc, true, -1));
        ast = ret.o1;
        if(ret.o1 == null){
            if(ret.o2 != null){
                throw new RuntimeException(ret.o2);
            }else{
                throw new RuntimeException("Unable to generate ast");
            }
        }
    }



    /**
     * Gets the change and applies it
     * @throws CoreException
     */
    public void apply(IProgressMonitor monitor) throws CoreException {
        getChange().perform(monitor);
    }
    
    public void getChange(Tuple<DocumentChange, MultiTextEdit> tup) {
        for (IChanges change : changes) {
            try {
                change.getDocChange(doc, tup); //actually, the changes will be filled in the passed tuple
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    /**
     * @return the change to be applied to the document
     */
    public Change getChange() {
        List<Change> l = new ArrayList<Change>();
        for (IChanges change : changes) {
            try {
                Change c = change.getChange(doc);
                l.add(c);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return new CompositeChange("AST Change", l.toArray(new Change[0]));
    }

    /**
     * @return the ast we're going to change
     */
    public SimpleNode getAST() {
        return this.ast;
    }


}
