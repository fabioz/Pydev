/*
 * Created on Jan 15, 2006
 */
package com.python.pydev.actions;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public final class ShowOutlineLabelProvider extends LabelProvider {
    
    public Image getImage(Object element) {
        SimpleNode n = ((ASTEntry)element).node;
        if(n instanceof ClassDef){
            return PyCodeCompletion.getImageForType(PyCodeCompletion.TYPE_CLASS);
        }
        if(n instanceof FunctionDef){
            return PyCodeCompletion.getImageForType(PyCodeCompletion.TYPE_FUNCTION);
        }
        return PyCodeCompletion.getImageForType(PyCodeCompletion.TYPE_ATTR);
    }

    public String getText(Object element) {
        return NodeUtils.getFullRepresentationString(((ASTEntry)element).node);
    }
}