package org.python.pydev.core;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Bundle;
import org.python.pydev.shared_core.bundle.BundleUtils;

public class CoreBundleInfo implements ICoreBundleInfo {

    protected final Bundle bundle;

    public CoreBundleInfo(Bundle bundle) {
        this.bundle = bundle;
    }

    /**
     * @throws CoreException
     * @see org.python.pydev.shared_ui.bundle.IBundleInfo#getRelativePath(org.eclipse.core.runtime.IPath)
     */
    @Override
    public File getRelativePath(IPath relative) throws CoreException {
        return BundleUtils.getRelative(relative, bundle);
    }

    /**
     * @see org.python.pydev.shared_ui.bundle.IBundleInfo#getPluginID()
     */
    @Override
    public String getPluginID() {
        return bundle.getSymbolicName();
    }

}
