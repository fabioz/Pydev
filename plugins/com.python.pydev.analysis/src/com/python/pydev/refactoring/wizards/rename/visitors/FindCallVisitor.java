/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.wizards.rename.visitors;

import java.util.Stack;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Visitor;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;

/**
 * This visitor is used to find a call given its ast
 * 
 * @author Fabio
 */
public class FindCallVisitor extends Visitor {

    private Name name;
    private NameTok nameTok;
    private Call call;
    private Stack<Call> lastCall = new Stack<Call>();

    public FindCallVisitor(Name name) {
        this.name = name;
    }

    public FindCallVisitor(NameTok nameTok) {
        this.nameTok = nameTok;
    }

    public Call getCall() {
        return call;
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        if (this.call != null) {
            return null;
        }

        if (node.func == name) {
            //check the name (direct)
            this.call = node;

        } else if (nameTok != null) {
            //check the name tok (inside of attribute)
            lastCall.push(node);
            Object r = super.visitCall(node);
            lastCall.pop();
            if (this.call != null) {
                return null;
            }
            return r;
        }
        if (this.call != null) {
            return null;
        }
        return super.visitCall(node);
    }

    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        if (node == nameTok) {
            if (lastCall.size() > 0) {
                call = lastCall.peek();
            }
            return null;
        }
        return super.visitNameTok(node);
    }

    public static Call findCall(NameTok nametok, SimpleNode root) {
        FindCallVisitor visitor = new FindCallVisitor(nametok);
        try {
            visitor.traverse(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor.call;
    }

    public static Call findCall(Name name, SimpleNode root) {
        FindCallVisitor visitor = new FindCallVisitor(name);
        try {
            visitor.traverse(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return visitor.call;
    }

}
