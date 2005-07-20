/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis.visitors;

import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.VisitorBase;
import org.python.pydev.plugin.nature.PythonNature;

public class OcurrencesVisitor extends VisitorBase{

    private PythonNature nature;

    public OcurrencesVisitor(PythonNature nature) {
        this.nature = nature;
    }
    
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    public void traverse(SimpleNode node) throws Exception {
        node.accept(this);
    }
    
    public Object visitClassDef(ClassDef node) throws Exception {
        startScope();
        Object object = super.visitClassDef(node);
        endScope();
        return object;
    }

    private void startScope() {
    }
    
    private void endScope() {
    }

    public Object visitFunctionDef(FunctionDef node) throws Exception {
        startScope();
        Object object = super.visitFunctionDef(node);
        endScope();
        return object;
    }

}
