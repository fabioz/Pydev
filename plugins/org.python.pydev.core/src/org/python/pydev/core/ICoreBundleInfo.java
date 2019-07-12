package org.python.pydev.core;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

public interface ICoreBundleInfo {

    /**
     * Should return a file from a relative path.
     *
     * @param relative
     * @return
     * @throws CoreException
     */
    File getRelativePath(IPath relative) throws CoreException;

    String getPluginID();
}
