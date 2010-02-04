package org.python.pydev.navigator.filters;

import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.navigator.elements.PythonNode;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.NodeUtils;

public class CommentsFilter extends AbstractFilter{

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if(element instanceof PythonNode){
            PythonNode node = (PythonNode) element;
            SimpleNode n = node.entry.getAstThis().node;
            if(NodeUtils.isComment(n)){
                return false;
            }
        }
        return true;
    }

}
