/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 08/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.copiedfromeclipsesrc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JythonModulesManagerUtils;
import org.python.pydev.utils.ICallback;

/**
 * copied from org.eclipse.jdt.internal.launching.StandardVMType
 * 
 * @author Fabio Zadrozny
 */
public class JavaVmLocationFinder {

    private JavaVmLocationFinder() {
        super();
    }

    /**
     * @return the default java executable configured in the jdt plugin
     */
    public static File findDefaultJavaExecutable() throws JDTNotAvailableException {
        try {
            return (File) callbackJavaExecutable.call(null);
        } catch (Exception e) {
            JythonModulesManagerUtils.tryRethrowAsJDTNotAvailableException(e);
            throw new RuntimeException("Should never get here", e);
        }
    }

    /**
     * @return the default java jars (rt.jar ... )
     */
    @SuppressWarnings("unchecked")
    public static List<File> findDefaultJavaJars() throws JDTNotAvailableException {
        try {
            return (List<File>) callbackJavaJars.call(null);
        } catch (Exception e) {
            JythonModulesManagerUtils.tryRethrowAsJDTNotAvailableException(e);
            throw new RuntimeException("Should never get here", e);
        }
    }

    /**
     * Might be changed for tests (if not in the eclipse env)
     */
    public static ICallback callbackJavaExecutable = new ICallback() {

        @Override
        public Object call(Object args) throws Exception {
            try {
                IVMInstall defaultVMInstall = JavaRuntime.getDefaultVMInstall();
                File installLocation = defaultVMInstall.getInstallLocation();
                return StandardVMType.findJavaExecutable(installLocation);
            } catch (Throwable e) {
                JythonModulesManagerUtils.tryRethrowAsJDTNotAvailableException(e);
                throw new RuntimeException("Should never get here", e);
            }
        }
    };

    /**
     * Might be changed for tests (if not in the eclipse env)
     */
    public static ICallback callbackJavaJars = new ICallback() {

        @Override
        public Object call(Object args) throws Exception {
            try {
                IVMInstall defaultVMInstall = JavaRuntime.getDefaultVMInstall();
                LibraryLocation[] libraryLocations = JavaRuntime.getLibraryLocations(defaultVMInstall);

                ArrayList<File> jars = new ArrayList<File>();
                for (LibraryLocation location : libraryLocations) {
                    jars.add(location.getSystemLibraryPath().toFile());
                }
                return jars;
            } catch (Throwable e) {
                JythonModulesManagerUtils.tryRethrowAsJDTNotAvailableException(e);
                throw new RuntimeException("Should never get here", e);
            }
        }
    };
}
