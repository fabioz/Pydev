/*
 * Created on Oct 8, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.sorter;

import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.python.pydev.core.structure.TreeNode;
import org.python.pydev.navigator.elements.ISortedElement;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.navigator.elements.ProjectConfigError;
import org.python.pydev.navigator.elements.PythonNode;

public class PythonModelSorter extends ViewerSorter{

    public int compare(Viewer viewer, Object e1, Object e2) {
        if(e1 instanceof PythonNode || e2 instanceof PythonNode){
            return 0; //we don't want to sort it... just show it in the order it is found in the file
        }
        
        //now, on to the priorities (if both have different classes)
        if(e1 instanceof ISortedElement && e2 instanceof ISortedElement){
            int r1 = ((ISortedElement) e1).getRank();
            int r2 = ((ISortedElement) e2).getRank();
            if(r1 == r2){
                if(e1 instanceof IWrappedResource && e2 instanceof IWrappedResource){
                    return super.compare(viewer, 
                            ((IWrappedResource)e1).getActualObject(), 
                            ((IWrappedResource)e2).getActualObject());
                }else{
                    return 0;
                }
            }else if(r1 < r2){
                return -1;
            }else{
                return 1;
            }
        }
        //Wrapped resource/config error elements always have priority over non-sorted resources
        if(e1 instanceof IWrappedResource || e1 instanceof ProjectConfigError){
            return -1;
        }
        if(e2 instanceof IWrappedResource || e2 instanceof ProjectConfigError){
            return 1;
        }
        
        if(e1 instanceof IContainer && e2 instanceof IContainer){
            return super.compare(viewer, e1, e2);
        }
        if(e1 instanceof IContainer){
            return -1;
        }
        if(e2 instanceof IContainer){
            return 1;
        }
        
        //Tree nodes coe right after containers.
        if(e1 instanceof TreeNode<?>){
            return -1;
        }
        if(e2 instanceof TreeNode<?>){
            return 1;
        }
        return super.compare(viewer, e1, e2);
    }
}
