/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import java.io.File;

import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * This is the modules key that should be used if we have an entry in a zip file.
 *
 * @author Fabio
 */
public class ModulesKeyForZip extends ModulesKey {

    /**
     * 1L = just name and file
     * 2L = + zipModulePath
     */
    private static final long serialVersionUID = 2L;

    /**
     * This should be null if it's from a file in the filesystem, now, if we're dealing with a zip file,
     * the file should be the zip file and this the path under which it was found in the zip file.
     *
     * Some cases can be considered:
     * - if it was found from jython this is a dir from the zip file
     * - if it was from a zip file from python this is a the .py file path inside the zip file
     */
    public String zipModulePath;

    /**
     * Determines if this module was created because it was found as a file or a folder.
     */
    public boolean isFile;

    /**
     * Creates the module key. File may be null
     */
    public ModulesKeyForZip(String name, File f, String zipModulePath, boolean isFile) {
        super(name, f);
        this.zipModulePath = zipModulePath;
        this.isFile = isFile;
    }

    @Override
    public String toString() {
        FastStringBuffer ret = new FastStringBuffer(name, 40);
        if (file != null) {
            ret.append(" - ");
            ret.appendObject(file);
        }
        if (zipModulePath != null) {
            ret.append(" - zip path:");
            ret.append(zipModulePath);
        }
        return ret.toString();
    }

    @Override
    public void toIO(FastStringBuffer buf) {
        super.toIO(buf);
        buf.append('|').append(zipModulePath).append('|').append(isFile ? '1' : '0');
    }
}
