package org.python.pydev.core.bundle;

import java.io.File;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Bundle;

public class BundleUtils {

	public static File getRelative(IPath relative, Bundle bundle) {
		try {
			URL bundleURL = FileLocator.find(bundle, relative, null);
		    URL fileURL;
	        fileURL = FileLocator.toFileURL(bundleURL);
	        File f = new File(fileURL.getPath());
	
	        return f;
	    } catch (Exception e) {
	        throw new RuntimeException("Can't find relative path:"+relative+" within:"+bundle, e);
	    }
	}

}
