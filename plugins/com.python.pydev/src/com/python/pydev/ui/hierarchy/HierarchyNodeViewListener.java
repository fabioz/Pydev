/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
