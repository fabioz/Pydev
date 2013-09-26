/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.launching;

import java.io.File;

import org.eclipse.core.resources.IResource;

/**
 * @author fabioz
 *
 */
public class FileOrResource {

    public final IResource resource;
    public final File file;

    /**
     * @param resource
     */
    public FileOrResource(IResource resource) {
        this.resource = resource;
        this.file = null;
    }

    /**
     * @param editorFile
     */
    public FileOrResource(File editorFile) {
        this.file = editorFile;
        this.resource = null;
    }

    /**
     * @param array
     * @return
     */
    public static FileOrResource[] createArray(IResource[] array) {
        FileOrResource[] ret = new FileOrResource[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = new FileOrResource(array[i]);
        }
        return ret;
    }

    /**
     * @param resource2
     * @return
     */
    public static IResource[] createIResourceArray(FileOrResource[] array) {
        IResource[] ret = new IResource[array.length];
        for (int i = 0; i < array.length; i++) {
            ret[i] = array[i].resource;
        }
        return ret;
    }

}
