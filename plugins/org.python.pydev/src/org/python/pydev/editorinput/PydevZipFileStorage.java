/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editorinput;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * This class enables Eclipse to get the contents from a file that was found within a zip file. 
 * 
 * @author Fabio
 */
public class PydevZipFileStorage implements IStorage {

    public final File zipFile;
    public final String zipPath;

    public PydevZipFileStorage(File zipFile, String zipPath) {
        this.zipFile = zipFile;
        this.zipPath = zipPath;
    }

    public InputStream getContents() throws CoreException {
        try {
            ZipFile f = new ZipFile(this.zipFile);
            return f.getInputStream(f.getEntry(this.zipPath));
        } catch (Exception e) {
            throw new CoreException(PydevPlugin.makeStatus(IStatus.ERROR, "Error getting contents from zip file", e));
        }
    }

    public IPath getFullPath() {
        return Path.fromOSString(this.zipFile.getAbsolutePath()).append(new Path(this.zipPath));
    }

    public String getName() {
        List<String> split = StringUtils.split(zipPath, '/');
        if (split.size() > 0) {
            return split.get(split.size() - 1);
        }
        return this.zipPath;
    }

    public boolean isReadOnly() {
        return true;
    }

    public Object getAdapter(Class adapter) {
        return null;
    }

}
