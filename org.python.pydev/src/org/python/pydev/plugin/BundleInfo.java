/*
 * Created on May 11, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.plugin;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Bundle;
import org.python.pydev.core.bundle.BundleUtils;
import org.python.pydev.ui.ImageCache;

/**
 * @author Fabio Zadrozny
 */
public class BundleInfo implements IBundleInfo{

    // ----------------- SINGLETON THINGS -----------------------------
    private static IBundleInfo info;
    public static void setBundleInfo(IBundleInfo b){
        info = b;
    }
    public static IBundleInfo getBundleInfo(){
        if(info == null){
            info = new BundleInfo();
        }
        return info;
    }
    // ----------------- END SINGLETON THINGS --------------------------
    
    /**
     * @throws CoreException
     * @see org.python.pydev.plugin.IBundleInfo#getRelativePath(org.eclipse.core.runtime.IPath)
     */
    public File getRelativePath(IPath relative) throws CoreException {
        Bundle bundle = PydevPlugin.getDefault().getBundle();

        return BundleUtils.getRelative(relative, bundle);
    }
    
	/**
     * @see org.python.pydev.plugin.IBundleInfo#getPluginID()
     */
    public String getPluginID() {
        return PydevPlugin.getDefault().getBundle().getSymbolicName();
    }

    
    private ImageCache imageCache;
    
    /**
     * @see org.python.pydev.plugin.IBundleInfo#getImageCache()
     */
    public ImageCache getImageCache() {
        if(imageCache == null){
            imageCache = new ImageCache(PydevPlugin.getDefault().getBundle().getEntry("/"));
        }
        return imageCache;
    }
    
    

}
