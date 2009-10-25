package org.python.pydev.parser.prettyprinter;

import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.prettyprinterv2.MakeAstValidForPrettyPrintingVisitor;

public class MessLinesAndColumnsVisitor extends MakeAstValidForPrettyPrintingVisitor{

    protected void fixNode(SimpleNode node) {
        node.beginLine = -1;
        node.beginColumn = -1;
        handleSpecials(node.specialsBefore);
        handleSpecials(node.specialsAfter);
        
    }
    


    private void handleSpecials(List<Object> specials)  {
        if(specials != null){
            for(Object o:specials){
                if(o instanceof commentType){
                    fixNode((SimpleNode) o);
                }
            }
        }
    }

}
