/*
 * Created on Apr 11, 2006
 */
package com.python.pydev.ui.hierarchy;

import edu.umd.cs.piccolo.event.PInputEvent;

public interface HierarchyNodeViewListener {

    /**
     * Notification of click in some node.
     */
    void onClick(HierarchyNodeView view, PInputEvent event);

}
