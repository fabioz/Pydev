/*
 * Created on Jan 15, 2006
 */
package com.python.pydev.actions;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.PyCodeCompletionImages;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public final class ShowOutlineLabelProvider extends LabelProvider {
    
    public Image getImage(Object element) {
        SimpleNode n = ((ASTEntry)element).node;
        if(n instanceof ClassDef){
            return PyCodeCompletionImages.getImageForType(IToken.TYPE_CLASS);
        }
        if(n instanceof FunctionDef){
            return PyCodeCompletionImages.getImageForType(IToken.TYPE_FUNCTION);
        }
        return PyCodeCompletionImages.getImageForType(IToken.TYPE_ATTR);
    }

    public String getText(Object element) {
        return NodeUtils.getFullRepresentationString(((ASTEntry)element).node);
    }
}