package com.python.pydev.refactoring.wizards.rename.visitors;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Visitor;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.Name;

/**
 * This visitor is used to find a call given its ast
 * 
 * @author Fabio
 */
public class FindCallVisitor extends Visitor{

    private Name name;
    private Call call;

    public FindCallVisitor(Name name) {
        this.name = name;
    }

    public Call getCall() {
        return call;
    }
    
    @Override
    public Object visitCall(Call node) throws Exception {
        if(node.func == name){
            this.call = node;
            return null;
        }
        return super.visitCall(node);
    }
    
    
    public static Call findCall(Name name, SimpleNode root){
        FindCallVisitor visitor = new FindCallVisitor(name);
        try {
            visitor.traverse(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor.call;
    }
    
}
