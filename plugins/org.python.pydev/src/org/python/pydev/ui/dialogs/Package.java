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

import org.eclipse.core.resources.IFolder;

public class Package {
    public IFolder folder;
    public SourceFolder sourceFolder;

    public Package(IFolder folder, SourceFolder sourceFolder) {
        this.folder = folder;
        this.sourceFolder = sourceFolder;
    }

    public String getPackageName() {
        String srcPath = sourceFolder.folder.getFullPath().toString();
        String complete = folder.getFullPath().toString();
        if (complete.startsWith(srcPath)) {
            complete = complete.substring(srcPath.length()).replace('/', '.');
            if (complete.startsWith(".")) {
                complete = complete.substring(1);
            }
        }
        return complete;
    }

}
