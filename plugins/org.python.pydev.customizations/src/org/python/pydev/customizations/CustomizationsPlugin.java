/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.customizations;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.bundle.BundleInfo;
import org.python.pydev.shared_ui.bundle.IBundleInfo;

/**
 * The main plugin class to be used in the desktop.
 */
public class CustomizationsPlugin extends AbstractUIPlugin {
    //The shared instance.
    private static CustomizationsPlugin plugin;
    //Resource bundle.
    private ResourceBundle resourceBundle;

    /**
     * The constructor.
     */
    public CustomizationsPlugin() {
        super();
        plugin = this;
        try {
            resourceBundle = ResourceBundle.getBundle("org.python.pydev.customizations.CustomizationsPluginResources");
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
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
    public static CustomizationsPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle,
     * or 'key' if not found.
     */
    public static String getResourceString(String key) {
        ResourceBundle bundle = CustomizationsPlugin.getDefault().getResourceBundle();
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

    // ----------------- SINGLETON THINGS -----------------------------
    public static IBundleInfo info;

    public static IBundleInfo getBundleInfo() {
        if (CustomizationsPlugin.info == null) {
            CustomizationsPlugin.info = new BundleInfo(CustomizationsPlugin.getDefault().getBundle());
        }
        return CustomizationsPlugin.info;
    }

    public static void setBundleInfo(IBundleInfo b) {
        CustomizationsPlugin.info = b;
    }

    // ----------------- END BUNDLE INFO THINGS --------------------------
    /**
     * @return the cache that should be used to access images within the pydev plugin.
     */
    public static ImageCache getImageCache() {
        return CustomizationsPlugin.getBundleInfo().getImageCache();
    }
}
