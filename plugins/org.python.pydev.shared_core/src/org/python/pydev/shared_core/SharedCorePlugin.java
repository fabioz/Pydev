/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core;

import java.io.File;
import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;
import org.python.pydev.shared_core.io.FileUtils;

/**
 * The main plugin class to be used in the desktop.
 */
public class SharedCorePlugin extends Plugin {

    public static final String SHARED_CORE_PLUGIN_ID = "org.python.pydev.shared_core";

    public static final String PYDEV_PLUGIN_ID = "org.python.pydev";

    //The shared instance.
    private static SharedCorePlugin plugin;

    /**
     * The constructor.
     */
    public SharedCorePlugin() {
        super();
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation
     */
    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
    }

    /**
     * This method is called when the plug-in is stopped
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
    }

    /**
     * Given a resource get the string in the filesystem for it.
     */
    public static String getIResourceOSString(IResource f) {
        URI locationURI = f.getLocationURI();
        if (locationURI != null) {
            try {
                //RTC source control not return a valid uri
                return FileUtils.getFileAbsolutePath(new File(locationURI));
            } catch (IllegalArgumentException e) {
            }
        }
    
        IPath rawLocation = f.getRawLocation();
        if (rawLocation == null) {
            return null; //yes, we could have a resource that was deleted but we still have it's representation...
        }
        String fullPath = rawLocation.toOSString();
        //now, we have to make sure it is canonical...
        File file = new File(fullPath);
        if (file.exists()) {
            return FileUtils.getFileAbsolutePath(file);
        } else {
            //it does not exist, so, we have to check its project to validate the part that we can
            IProject project = f.getProject();
            IPath location = project.getLocation();
            File projectFile = location.toFile();
            if (projectFile.exists()) {
                String projectFilePath = FileUtils.getFileAbsolutePath(projectFile);
    
                if (fullPath.startsWith(projectFilePath)) {
                    //the case is all ok
                    return fullPath;
                } else {
                    //the case appears to be different, so, let's check if this is it...
                    if (fullPath.toLowerCase().startsWith(projectFilePath.toLowerCase())) {
                        String relativePart = fullPath.substring(projectFilePath.length());
    
                        //at least the first part was correct
                        return projectFilePath + relativePart;
                    }
                }
            }
        }
    
        //it may not be correct, but it was the best we could do...
        return fullPath;
    }

    public static Status makeStatus(int errorLevel, String message, Throwable e) {
        return new Status(errorLevel, PYDEV_PLUGIN_ID, errorLevel, message, e);
    }

    /**
     * Returns the shared instance.
     */
    public static SharedCorePlugin getDefault() {
        return plugin;
    }

    /**
     * When true it means we're not in test mode.
     */
    private static boolean testModeReturnFalse = false;

    /**
     * Return true if we are running JUnit non-workbench tests.
     * In the past we relied on bundles not being activated
     * and therefore getDefault returning null, however with
     * Tycho, Maven and the joys of OSGi, the default approach
     * is to run unit tests as JUnit plug-in tests with no
     * application (headless).
     *
     * We are in test when one of two cases is true:
     * a) plugin == null meaning we have not been activated
     * b) System property PyDevInTestMode == true
     * c) Environment variable PyDevInTestMode == true
     */
    public static boolean inTestMode() {
        if (testModeReturnFalse) {
            return false;
        }

        if (plugin == null) {
            return true;
        }

        if ("true".equals(System.getProperty("PyDevInTestMode"))) {
            return true;
        }

        if ("true".equals(System.getenv("PyDevInTestMode"))) {
            return true;
        }

        //If we returned once that we should not use the test mode, cache it.
        //(i.e.: it's Ok having more overhead when running tests, but try to be as
        //efficient as possible when on a real run).
        testModeReturnFalse = true;
        return false;
    }

    public static boolean skipKnownFailures() {
        return true;
    }
}
