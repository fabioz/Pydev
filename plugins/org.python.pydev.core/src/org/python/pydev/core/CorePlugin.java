/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.QualifiedName;
import org.osgi.framework.BundleContext;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.PyDevCorePreferencesInitializer;
import org.python.pydev.shared_core.io.FileUtils;

/**
 * The main plugin class to be used in the desktop.
 */
public class CorePlugin extends Plugin {
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

        // As it starts things in the org.python.pydev node for backward-compatibility, we must
        // initialize it now.
        PyDevCorePreferencesInitializer.initializeDefaultPreferences();

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

    public static File getPep8Location() {
        return getPepModuleLocation("pycodestyle.py");
    }

    /**
     * @param moduleFilename: i.e.: pycodestyle.py, autopep8.py
     * @return
     */
    public static File getPepModuleLocation(String moduleFilename) {
        try {
            String pep8Location = getScriptWithinPySrc(
                    new Path("third_party").append("pep8").append(moduleFilename).toString()).toString();
            File pep8Loc = new File(pep8Location);
            if (!pep8Loc.exists()) {
                Log.log("Specified location for " + moduleFilename + " does not exist (" + pep8Location + ").");
                return null;
            }
            return pep8Loc;
        } catch (CoreException e) {
            Log.log("Error getting " + moduleFilename + " location", e);
            return null;
        }
    }

    public static File getPySrcPath() throws CoreException {
        IPath relative = new Path("pysrc");
        return getBundleInfo().getRelativePath(relative);
    }

    public static File getTypeshedPath() throws CoreException {
        IPath relative = new Path("typeshed");
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

    public static File pydevStatelocation;

    /**
     * Loads from the workspace metadata a given object (given the filename)
     */
    public static File getWorkspaceMetadataFile(String fileName) {
        if (pydevStatelocation == null) {
            throw new RuntimeException(
                    "pydevStatelocation not set. If running in tests, call: setTestPlatformStateLocation");
        }
        return new File(pydevStatelocation, fileName);
    }

    /**
     * Gotten from:
     * org.eclipse.ui.ide.IDE.EDITOR_KEY
     */
    public static final QualifiedName EDITOR_KEY = new QualifiedName(
            "org.eclipse.ui.internal.registry.ResourceEditorRegistry", "EditorProperty");//$NON-NLS-2$//$NON-NLS-1$

    /**
     * @return true if PyEdit.EDITOR_ID is set as the persistent property (only if the file does not have an extension).
     */
    public static boolean markAsPyDevFileIfDetected(IFile file) {
        String name = file.getName();
        if (name == null || name.indexOf('.') != -1) {
            return false;
        }

        String editorID;
        try {
            editorID = file.getPersistentProperty(EDITOR_KEY);
            if (editorID == null) {
                // Ignore zero-length files
                IFileStore store = EFS.getStore(file.getLocationURI());
                if (store != null && store.fetchInfo().getLength() <= 0) {
                    return false;
                }

                InputStream contents = file.getContents(true);
                Reader inputStreamReader = new InputStreamReader(new BufferedInputStream(contents));
                if (FileUtils.hasPythonShebang(inputStreamReader)) {
                    // IDE.setDefaultEditor(file, IPyEdit.EDITOR_ID); -- Remove IDE dependency
                    try {
                        file.setPersistentProperty(EDITOR_KEY, editorID);
                    } catch (CoreException e) {
                        // Ignore
                    }
                    return true;
                }
            } else {
                return IPyEdit.EDITOR_ID.equals(editorID);
            }

        } catch (Exception e) {
            if (file.exists()) {
                Log.log(e);
            }
        }
        return false;
    }
}
