/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_core;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class SharedCorePlugin extends Plugin {

    public static final String PLUGIN_ID = "org.python.pydev.shared_core";

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
