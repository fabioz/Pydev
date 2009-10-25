package org.python.pydev.parser.prettyprinter;

import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.commentType;

public class MessLinesAndColumnsVisitor extends VisitorBase{

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        node.beginLine = -1;
        node.beginColumn = -1;
        handleSpecials(node.specialsBefore);
        handleSpecials(node.specialsAfter);
        return null;
    }

    private void handleSpecials(List<Object> specials) throws Exception {
        if(specials != null){
            for(Object o:specials){
                if(o instanceof commentType){
                    unhandled_node((SimpleNode) o);
                }
            }
        }
    }

}
