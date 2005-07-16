/*
 * Created on 13/07/2005
 */
package com.python.pydev.fastparser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.parser.SimpleNode;
import org.python.parser.ast.VisitorBase;
import org.python.pydev.parser.visitors.NodeUtils;

public class MemoVisitor extends VisitorBase{

    List visited = new ArrayList();
    
    protected Object unhandled_node(SimpleNode node) throws Exception {
        visited.add(node);
        return null;
    }

    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }
    
    public boolean equals(Object obj) {
    
        MemoVisitor other = (MemoVisitor) obj;
        Iterator iter1 = other.visited.iterator();
        
        for (Iterator iter = visited.iterator(); iter.hasNext();) {
            SimpleNode n = (SimpleNode) iter.next();
            SimpleNode n1 = (SimpleNode) iter1.next();
            
            System.out.println(n.getClass());
            if(n.getClass().equals(n1.getClass()) == false){
                System.out.println("n.getClass() != n1.getClass() "+ n.getClass() +" != "+ n1.getClass());
                return false;
            }
            if(n.beginColumn != n1.beginColumn){
                System.out.println("n.beginColumn != n1.beginColumn "+ n.beginColumn +" != "+ n1.beginColumn);
                return false;
            }
            if(n.beginLine != n1.beginLine){
                System.out.println("n.beginLine != n1.beginLine "+ n.beginLine +" != "+ n1.beginLine);
                return false;
            }
            
            String s1 = NodeUtils.getFullRepresentationString(n);
            String s2 = NodeUtils.getFullRepresentationString(n1);
            if((s1 == null && s2 != null) || (s1 != null && s2 == null)){
                System.out.println("(s1 == null && s2 != null) || (s1 != null && s2 == null)");
                return false;
            }
            
            if(s1 != null && s1.equals(s2) == false){
                System.out.println("s1 != s2 "+ s1 +" != "+ s2);
                return false;
            }
        }
        
        return true;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        for (Iterator iter = visited.iterator(); iter.hasNext();) {
            buffer.append(iter.next().toString());
            buffer.append("\n");
        }
        return buffer.toString();
    }
}
    
 
