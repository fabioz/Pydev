/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui;

import org.python.pydev.shared_ui.utils.IViewWithControls;

public interface IViewCreatedObserver {

    /**
     * Called for:
     * - PydevPackageExplorer
     * - PyOutlinePage  
     * etc.
     */
    void notifyViewCreated(IViewWithControls view);

}
