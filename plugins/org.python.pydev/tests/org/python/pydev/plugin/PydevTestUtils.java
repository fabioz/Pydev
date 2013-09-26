/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin;

import java.io.File;
import java.io.IOException;

import org.python.pydev.core.TestDependent;
import org.python.pydev.shared_core.io.FileUtils;

/**
 * @author fabioz
 *
 */
public class PydevTestUtils {

    public static File setTestPlatformStateLocation() {
        if (PydevPlugin.location != null) {
            return PydevPlugin.location;
        }
        File baseDir = new File(TestDependent.TEST_PYDEV_PLUGIN_LOC, "data_temporary_for_testing");
        try {
            FileUtils.deleteDirectoryTree(baseDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        PydevPlugin.location = baseDir;
        return baseDir;
    }

}
