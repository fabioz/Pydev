/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.python.pydev.plugin.IBundleInfo;


public class BundleInfoStub implements IBundleInfo {

    public File getRelativePath(IPath relative) throws CoreException {
        if(relative.toString().indexOf("interpreterInfo.py") != -1){
            return new File("./PySrc/interpreterInfo.py");
        }
        if(relative.toString().indexOf("pycompletionserver.py") != -1){
            return new File("./PySrc/pycompletionserver.py");
        }
        if(relative.toString().indexOf("jycompletionserver.py") != -1){
            return new File("./PySrc/jycompletionserver.py");
        }
        throw new RuntimeException("Not available info on: "+relative);
    }

    public String getPluginID() {
        return "plugin_id";
    }

    public ImageCache getImageCache() {
        return null;
    }
}