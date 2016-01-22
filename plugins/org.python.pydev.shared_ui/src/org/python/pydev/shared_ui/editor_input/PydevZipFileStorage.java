/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.editor_input;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;
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
            InputStream inputStream = null;
            try {
                inputStream = f.getInputStream(f.getEntry(this.zipPath));
                //Note: read to memory and return a byte array input stream so that we don't lock
                //the zip file.
                FastStringBuffer streamContents = FileUtils.getStreamContents(inputStream, null,
                        null,
                        FastStringBuffer.class);
                return new ByteArrayInputStream(streamContents.getBytes());
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
                try {
                    f.close();
                } catch (Exception e) {
                    Log.log(e);
                }
            }

        } catch (Exception e) {
            throw new CoreException(
                    new Status(IStatus.ERROR, SharedCorePlugin.PLUGIN_ID, "Error getting contents from zip file", e));
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
