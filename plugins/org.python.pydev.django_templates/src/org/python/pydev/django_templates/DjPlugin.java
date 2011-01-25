/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.python.pydev.core.bundle.BundleInfo;
import org.python.pydev.core.bundle.IBundleInfo;
import org.python.pydev.core.bundle.ImageCache;

/**
 * The activator class controls the plug-in life cycle
 */
public class DjPlugin extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.python.pydev.django_templates"; //$NON-NLS-1$

    // The shared instance
    private static DjPlugin plugin;
    
    // ----------------- SINGLETON THINGS -----------------------------
    public static IBundleInfo info;
    public static IBundleInfo getBundleInfo(){
        if(DjPlugin.info == null){
            DjPlugin.info = new BundleInfo(DjPlugin.getDefault().getBundle());
        }
        return DjPlugin.info;
    }
    public static void setBundleInfo(IBundleInfo b){
        DjPlugin.info = b;
    }
    /**
     * @return the cache that should be used to access images within the pydev plugin.
     */
    public static ImageCache getImageCache(){
        return DjPlugin.getBundleInfo().getImageCache();
    }
    // ----------------- END BUNDLE INFO THINGS --------------------------
    

    /**
     * The constructor
     */
    public DjPlugin() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
     * )
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
     * )
     */
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static DjPlugin getDefault() {
        return plugin;
    }

}
