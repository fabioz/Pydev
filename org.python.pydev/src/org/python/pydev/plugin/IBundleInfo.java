/*
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.plugin;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * @author Fabio Zadrozny
 */
public interface IBundleInfo {
    
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
