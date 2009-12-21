package com.leosoto.bingo;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class BingoPlugin extends Plugin {

	private static BingoPlugin plugin;


    /**
     * The constructor.
     */
    public BingoPlugin() {
        plugin = this;
    }


    /**
     * This method is called when the plug-in is stopped
     */
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }

    /**
     * Returns the shared instance.
     */
    public static BingoPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given
     * plug-in relative path.
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin("com.leosoto.bingo", path);
    }

    public static String getPluginID() {
        return getDefault().getBundle().getSymbolicName();
    }

}
