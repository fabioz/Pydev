/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.bundle.IBundleInfo;
import org.python.pydev.core.bundle.ImageCache;


public class BundleInfoStub implements IBundleInfo {

    public File getRelativePath(IPath relative) throws CoreException {
        if(relative.toString().indexOf("interpreterInfo.py") != -1){
            return new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"PySrc/interpreterInfo.py");
        }
        if(relative.toString().indexOf("pycompletionserver.py") != -1){
            return new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"PySrc/pycompletionserver.py");
        }
        if(relative.toString().indexOf("jycompletionserver.py") != -1){
            return new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"PySrc/jycompletionserver.py");
        }
        if(relative.toString().indexOf("indent.py") != -1){
            return new File(TestDependent.TEST_PYDEV_JYTHON_PLUGIN_LOC+"jysrc/indent.py");
        }
        if(relative.toString().indexOf("PySrc/pydev_sitecustomize") != -1){
        	return new File(TestDependent.TEST_PYDEV_PLUGIN_LOC+"PySrc/pydev_sitecustomize");
        }
        throw new RuntimeException("Not available info on: "+relative);
    }

    public String getPluginID() {
        return "plugin_id";
    }

    public ImageCache getImageCache() {
        try {
            return new ImageCache(new URL("file:///" + TestDependent.TEST_PYDEV_PLUGIN_LOC));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}