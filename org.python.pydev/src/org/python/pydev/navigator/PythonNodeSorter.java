/*
 * Created on Oct 8, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

public class PythonNodeSorter extends ViewerSorter{

    @SuppressWarnings("unchecked")
    public int compare(Viewer viewer, Object e1, Object e2) {
        if(e1 instanceof Comparable && e2 instanceof Comparable){
            return ((Comparable) e1).compareTo(e2);
        }
        return super.compare(viewer, e1, e2);
    }
}
