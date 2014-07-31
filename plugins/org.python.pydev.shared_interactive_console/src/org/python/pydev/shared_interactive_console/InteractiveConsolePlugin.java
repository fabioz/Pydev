/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_interactive_console;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsoleUIConstants;

/**
 * The main plugin class to be used in the desktop.
 */
public class InteractiveConsolePlugin extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "org.python.pydev.shared_interactive_console";

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
                    .getBundle("org.python.pydev.shared.interactive_console.InteractiveConsolePluginResources");
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
        for (ILaunch l : new ArrayList<ILaunch>(consoleLaunches)) {
            try {
                this.removeConsoleLaunch(l);
            } catch (Exception e) {
                Log.log(e);
            }
        }
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
    private static final String[][] IMAGES = new String[][] {
            { "icons/save.gif", ScriptConsoleUIConstants.SAVE_SESSION_ICON },
            { "icons/terminate.gif", ScriptConsoleUIConstants.TERMINATE_ICON },
            { "icons/interrupt.gif", ScriptConsoleUIConstants.INTERRUPT_ICON },
    };

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

    /**
     * Holds the console launches that should be terminated.
     */
    private List<ILaunch> consoleLaunches = new ArrayList<ILaunch>();

    /**
     * Adds launch to the list of launches managed by pydev. Added launches will be shutdown
     * if they are not removed before the plugin shutdown.
     *
     * @param launch launch to be added
     */
    public void addConsoleLaunch(ILaunch launch) {
        consoleLaunches.add(launch);
    }

    /**
     * Removes a launch from a pydev console and stops the related process.
     *
     * @param launch the launch to be removed
     */
    public void removeConsoleLaunch(ILaunch launch) {
        if (consoleLaunches.remove(launch)) {
            IProcess[] processes = launch.getProcesses();
            if (processes != null) {
                for (IProcess p : processes) {
                    try {
                        p.terminate();
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
            }
        }
    }
}
