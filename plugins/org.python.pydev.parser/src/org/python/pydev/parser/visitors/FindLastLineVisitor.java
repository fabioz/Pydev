package org.python.pydev.parser.visitors;

import java.util.List;

import org.python.pydev.parser.jython.ISpecialStrOrToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.VisitorBase;

public class FindLastLineVisitor extends VisitorBase{

    private SimpleNode lastNode;
    private ISpecialStrOrToken lastSpecialStr;

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        this.lastNode = node;
        check(this.lastNode.specialsBefore);
        check(this.lastNode.specialsAfter);
        return null;
    }
    
    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        check(node.specialsBefore);
        if (node.attr != null)
            node.attr.accept(this);
        if (node.value != null)
            node.value.accept(this);
        check(node.specialsAfter);
        return null;
    }

    private void check(List<Object> specials) {
        if(specials==null){
            return;
        }
        for (Object obj : specials) {
            if(obj instanceof ISpecialStrOrToken){
                if(lastSpecialStr == null || lastSpecialStr.getBeginLine() <= ((ISpecialStrOrToken)obj).getBeginLine()){
                    lastSpecialStr = (ISpecialStrOrToken) obj;
                }
            }
        }
    }
    
    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        if (node.module != null){
            unhandled_node(node.module);
            node.module.accept(this);
        }
        
        if (node.names != null) {
            for (int i = 0; i < node.names.length; i++) {
                if (node.names[i] != null){
                    unhandled_node(node.names[i]);
                    node.names[i].accept(this);
                }
            }
        }
        unhandled_node(node);
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }
    
    public SimpleNode getLastNode(){
        return lastNode;
    }
    
    public ISpecialStrOrToken getLastSpecialStr(){
        return lastSpecialStr;
    }
    
}
