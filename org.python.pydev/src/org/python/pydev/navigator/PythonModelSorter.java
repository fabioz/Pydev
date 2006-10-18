/*
 * Created on Oct 8, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class PythonModelSorter extends ViewerSorter{

    @SuppressWarnings("unchecked")
    public int compare(Viewer viewer, Object e1, Object e2) {
        if(e1 instanceof PythonNode || e2 instanceof PythonNode){
            return 0; //we don't want to sort it... just show it in the order it is found in the file
        }
        
        //now, on to the priorities (if both have different classes)
        if(e1 instanceof IWrappedResource && e2 instanceof IWrappedResource){
            IWrappedResource resource1 = (IWrappedResource) e1;
            IWrappedResource resource2 = (IWrappedResource) e2;
            int r1 = resource1.getRank();
            int r2 = resource2.getRank();
            if(r1 == r2){
                return super.compare(viewer, resource1.getActualObject(), resource2.getActualObject());
            }else if(r1 < r2){
                return -1;
            }else{
                return 1;
            }
        }
        return 0;
    }
}
