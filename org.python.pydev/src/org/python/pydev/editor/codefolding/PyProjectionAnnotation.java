/*
 * Created on Jul 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codefolding;

import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.python.pydev.editor.model.AbstractNode;

/**
 * This class was created so that we can check if some annotation reappears
 * (if this happens, we don't want to delete it).
 * 
 * @author Fabio Zadrozny
 */
public class PyProjectionAnnotation extends ProjectionAnnotation{

    public AbstractNode node;

    public PyProjectionAnnotation(AbstractNode node){
        this.node = node;
    }

    /**
     * @param node2
     * @return
     */
    public boolean appearsSame(AbstractNode node2) {
        
        if(node2.getClass().equals(node.getClass()) == false)
            return false;

        if(getCompleteName(node2).equals(getCompleteName(node)) == false)
            return false;
        
        return true;
    }

    /**
     * @param node2
     */
    private String getCompleteName(AbstractNode node2) {
        
        String ret = node2.getName();
        
        while(node2.getParent() != null){
            ret = node2.getParent().getName() + "."+ ret;
            node2 = node2.getParent();
        }
        
        return ret;
    }

}
