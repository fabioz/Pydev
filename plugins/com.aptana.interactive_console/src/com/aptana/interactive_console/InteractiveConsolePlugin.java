/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.interactive_console;

import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.aptana.interactive_console.console.ui.ScriptConsoleUIConstants;

/**
 * The main plugin class to be used in the desktop.
 */
public class InteractiveConsolePlugin extends AbstractUIPlugin {
    //The shared instance.
    private static InteractiveConsolePlugin plugin;
    //Resource bundle.
    private ResourceBundle resourceBundle;

    /**
     * The constructor.
     */
    public InteractiveConsolePlugin() {
        super();
        plugin = this;
        try {
            resourceBundle = ResourceBundle
                    .getBundle("com.aptana.interactive_console.InteractiveConsolePluginResources");
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
    }

    /**
     * This method is called upon plug-in activation
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
    }

    /**
     * This method is called when the plug-in is stopped
     */
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
    }

    /**
     * Returns the shared instance.
     */
    public static InteractiveConsolePlugin getDefault() {
        return plugin;
    }

    /**
     * Returns the string from the plugin's resource bundle,
     * or 'key' if not found.
     */
    public static String getResourceString(String key) {
        ResourceBundle bundle = InteractiveConsolePlugin.getDefault().getResourceBundle();
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

    //Images for the console
    private static final String[][] IMAGES = new String[][] { { "icons/save.gif", //$NON-NLS-1$
            ScriptConsoleUIConstants.SAVE_SESSION_ICON }, { "icons/terminate.gif", //$NON-NLS-1$
            ScriptConsoleUIConstants.TERMINATE_ICON } };

    @Override
    protected void initializeImageRegistry(ImageRegistry registry) {
        for (int i = 0; i < IMAGES.length; ++i) {
            URL url = getDefault().getBundle().getEntry(IMAGES[i][0]);
            registry.put(IMAGES[i][1], ImageDescriptor.createFromURL(url));
        }
    }

    public ImageDescriptor getImageDescriptor(String key) {
        return getImageRegistry().getDescriptor(key);
    }

}
