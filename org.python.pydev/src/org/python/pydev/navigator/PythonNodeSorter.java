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
        return 0; //we don't want to sort it... just show it in the order it is found in the file
    }
}
