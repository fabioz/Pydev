/*
 * Created on Oct 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.debug.codecoverage;


/**
 * @author Fabio Zadrozny
 */
public class ErrorFileNode {
    public Object node;
    public String desc;
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if(!(obj instanceof ErrorFileNode)){
            return false;
        }
        
        ErrorFileNode f = (ErrorFileNode) obj;
        return f.node.equals(node) && f.desc == desc; 
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return FileNode.getName(node.toString()) + "   " +desc;
    }
    
    

}
