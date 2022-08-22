/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin;

import java.io.File;

import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.TestDependent;

/**
 * @author fabioz
 *
 */
public class PydevTestUtils {

    public static File setTestPlatformStateLocation() {
        if (CorePlugin.pydevStatelocation != null) {
            return CorePlugin.pydevStatelocation;
        }
        File baseDir = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC, "data_temporary_for_testing");
        baseDir.mkdirs();
        //        try {
        //            FileUtils.deleteDirectoryTree(baseDir);
        //        } catch (IOException e) {
        //            e.printStackTrace();
        //        }
        CorePlugin.pydevStatelocation = baseDir;
        return baseDir;
    }

}
