/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui;

import java.lang.reflect.Field;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_ui.bundle.BundleInfo;
import org.python.pydev.shared_ui.bundle.IBundleInfo;
import org.python.pydev.shared_ui.utils.UIUtils;

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
            SharedUiPlugin bundle = SharedUiPlugin.getDefault();
            if (bundle == null) {
                return null;
            }
            SharedUiPlugin.info = new BundleInfo(bundle.getBundle());
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
    public static SharedUiPlugin getDefault() {
        return plugin;
    }

    private static ImageCache imageCache = null;

    /**
     * @return the cache that should be used to access images within the pydev plugin.
     */
    public static ImageCache getImageCache() {
        if (imageCache == null) {
            IBundleInfo bundleInfo = SharedUiPlugin.getBundleInfo();
            if (bundleInfo == null) {
                return null;
            }
            imageCache = bundleInfo.getImageCache();
        }
        return imageCache;
    }

    public ImageDescriptor getImageDescriptor(String key) {
        return getImageRegistry().getDescriptor(key);
    }

    @SuppressWarnings("restriction")
    public static void setCssId(Object control, String id, boolean applyToChildren) {
        try {
            IStylingEngine engine = (IStylingEngine) UIUtils.getActiveWorkbenchWindow().
                    getService(IStylingEngine.class);
            if (engine != null) {
                engine.setId(control, id);
                IThemeEngine themeEngine = (IThemeEngine) Display.getDefault().getData(
                        "org.eclipse.e4.ui.css.swt.theme");
                themeEngine.applyStyles(control, applyToChildren);
            }
        } catch (Throwable e) {
            //Ignore: older versions of Eclipse won't have it!
            // e.printStackTrace();
        }
    }

    public static void fixSelectionStatusDialogStatusLineColor(Object dialog, Color color) {
        //TODO: Hack: remove when MessageLine is styleable.
        try {
            Field field = org.eclipse.ui.dialogs.SelectionStatusDialog.class
                    .getDeclaredField("fStatusLine");
            field.setAccessible(true);
            Control messageLine = (Control) field.get(dialog);
            messageLine.setBackground(color);
        } catch (Exception e) {
            Log.log(e);
        }
    }

    public static IStatus makeErrorStatus(Exception e, boolean useErrorMessage) {
        String message = "";
        if (useErrorMessage) {
            message = e.getMessage();
            if (message == null) {
                message = "null";
            }
        }
        return new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, message, e);
    }
}
