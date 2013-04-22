/**
 * Copyright (c) 2013 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.python.pydev.shared_ui.bundle.BundleInfo;
import org.python.pydev.shared_ui.bundle.IBundleInfo;

/**
 * The main plugin class to be used in the desktop.
 */
public class SharedUiPlugin extends AbstractUIPlugin {
    public static final String PLUGIN_ID = "org.python.pydev.shared_ui";

    //The shared instance.
    private static SharedUiPlugin plugin;

    // ----------------- SINGLETON THINGS -----------------------------
    public static IBundleInfo info;

    public static IBundleInfo getBundleInfo() {
        if (SharedUiPlugin.info == null) {
            SharedUiPlugin.info = new BundleInfo(SharedUiPlugin.getDefault().getBundle());
        }
        return SharedUiPlugin.info;
    }

    public static void setBundleInfo(IBundleInfo b) {
        SharedUiPlugin.info = b;
    }

    // ----------------- END BUNDLE INFO THINGS --------------------------

    /**
     * The constructor.
     */
    public SharedUiPlugin() {
        super();
        plugin = this;
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
    public static SharedUiPlugin getDefault() {
        return plugin;
    }

    private static ImageCache imageCache = null;

    /**
     * @return the cache that should be used to access images within the pydev plugin.
     */
    public static ImageCache getImageCache() {
        if (imageCache == null) {
            imageCache = SharedUiPlugin.getBundleInfo().getImageCache();
        }
        return imageCache;
    }

    public ImageDescriptor getImageDescriptor(String key) {
        return getImageRegistry().getDescriptor(key);
    }

}
