/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import java.io.File;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class CorePlugin extends AbstractUIPlugin {
    //The shared instance.
    private static CorePlugin plugin;
    //Resource bundle.
    private ResourceBundle resourceBundle;

    public static ICoreBundleInfo info;

    public static ICoreBundleInfo getBundleInfo() {
        if (CorePlugin.info == null) {
            CorePlugin.info = new CoreBundleInfo(CorePlugin.getDefault().getBundle());
        }
        return CorePlugin.info;
    }

    public static void setBundleInfo(ICoreBundleInfo b) {
        CorePlugin.info = b;
    }

    /**
     * The constructor.
     */
    public CorePlugin() {
        super();
        plugin = this;
        try {
            resourceBundle = ResourceBundle.getBundle("org.python.pydev.core.CorePluginResources");
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
    }

    public static String getPluginID() {
        return getDefault().getBundle().getSymbolicName();
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
     * Returns the shared instance.
     */
    public static CorePlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle,
     * or 'key' if not found.
     */
    public static String getResourceString(String key) {
        ResourceBundle bundle = CorePlugin.getDefault().getResourceBundle();
        try {
            return (bundle != null) ? bundle.getString(key) : key;
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Returns the plugin's resource bundle,
     */
    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }

    /**
     * @return
     * @throws CoreException
     */
    public static File getPySrcPath() throws CoreException {
        IPath relative = new Path("pysrc");
        return getBundleInfo().getRelativePath(relative);
    }

    /**
     * @return the script to get the variables.
     *
     * @throws CoreException
     */
    public static File getScriptWithinPySrc(String targetExec) throws CoreException {
        IPath relative = new Path("pysrc").addTrailingSeparator().append(targetExec);
        return getBundleInfo().getRelativePath(relative);
    }
}
