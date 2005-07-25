/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.python.pydev.plugin.IBundleInfo;


public class BundleInfoStub implements IBundleInfo {
    private ImageCache imageCache;

    public File getRelativePath(IPath relative) throws CoreException {
        if(relative.toString().indexOf("interpreterInfo.py") != -1){
            return new File("./PySrc/interpreterInfo.py");
        }
        if(relative.toString().indexOf("pycompletionserver.py") != -1){
            return new File("./PySrc/pycompletionserver.py");
        }
        throw new RuntimeException("Not available info on: "+relative);
    }

    public String getPluginID() {
        return "plugin_id";
    }

    public ImageCache getImageCache() {
        try {
            if(imageCache == null){
                imageCache = new ImageCache(new URL("file://D:\\dev_programs\\eclipse_3\\eclipse\\workspace\\org.python.pydev\\"));
            }
            return imageCache;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}