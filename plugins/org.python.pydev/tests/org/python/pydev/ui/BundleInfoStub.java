/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.bundle.IBundleInfo;

public class BundleInfoStub implements IBundleInfo {

    @Override
    public File getRelativePath(IPath relative) throws CoreException {
        if (relative.toString().indexOf("interpreterInfo.py") != -1) {
            return new File(TestDependent.PYSRC_LOC + "interpreterInfo.py");
        }
        if (relative.toString().indexOf("pycompletionserver.py") != -1) {
            return new File(TestDependent.PYSRC_LOC + "pycompletionserver.py");
        }
        if (relative.toString().indexOf("jycompletionserver.py") != -1) {
            return new File(TestDependent.PYSRC_LOC + "jycompletionserver.py");
        }
        if (relative.toString().indexOf("indent.py") != -1) {
            return new File(TestDependent.TEST_PYDEV_JYTHON_PLUGIN_LOC + "jysrc/indent.py");
        }
        if (relative.toString().indexOf("pysrc/pydev_sitecustomize") != -1) {
            return new File(TestDependent.PYSRC_LOC + "pydev_sitecustomize");
        }
        if (relative.toString().indexOf("pysrc/stubs/_django_manager_body.py") != -1) {
            return new File(
                    TestDependent.PYSRC_LOC + "stubs/_django_manager_body.py");
        }
        if (relative.toString().indexOf("pysrc/third_party/cython_json.py") != -1) {
            return new File(
                    TestDependent.PYSRC_LOC + "third_party/cython_json.py");
        }
        if (relative.toString().indexOf("pysrc/third_party/isort_container") != -1) {
            return new File(
                    TestDependent.PYSRC_LOC + "third_party/isort_container");
        }
        if (relative.toString().indexOf("typeshed") != -1) {
            return new File(new File(
                    TestDependent.PYSRC_LOC).getParentFile(), "typeshed");
        }
        if (relative.toString().indexOf("helpers/load-conda-vars.bat") != -1) {
            return new File(
                    TestDependent.HELPERS_LOC + "load-conda-vars.bat");
        }
        if (relative.toString().indexOf("helpers/load-conda-vars") != -1) {
            return new File(
                    TestDependent.HELPERS_LOC + "load-conda-vars");
        }
        throw new RuntimeException("Not available info on: " + relative);
    }

    @Override
    public String getPluginID() {
        return "plugin_id";
    }

    @Override
    public ImageCache getImageCache() {
        try {
            return new ImageCache(new URL("file:///" + TestDependent.TEST_PYDEV_PLUGIN_LOC));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}