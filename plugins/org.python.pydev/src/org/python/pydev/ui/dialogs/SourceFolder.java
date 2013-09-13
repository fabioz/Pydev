/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 21, 2006
 */
package org.python.pydev.ui.dialogs;

import org.eclipse.core.resources.IContainer;

public class SourceFolder {
    public IContainer folder;

    public SourceFolder(IContainer folder) {
        this.folder = folder;
    }
}
