/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.utils;

import org.eclipse.swt.widgets.Display;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallbackWithListeners;

public class RunInUiThread {

    public static final ICallbackWithListeners<RunInUiThreadInfo> listeners = new CallbackWithListeners<>();

    public static class RunInUiThreadInfo {

        public final Runnable runnable;
        public final boolean async;
        public final boolean runNowIfInUiThread;

        public RunInUiThreadInfo(Runnable r, boolean async, boolean runNowIfInUiThread) {
            this.runnable = r;
            this.async = async;
            this.runNowIfInUiThread = runNowIfInUiThread;
        }
    }

    public static void sync(Runnable r) {
        listeners.call(new RunInUiThreadInfo(r, false, true));
        if (SharedCorePlugin.inTestMode()) {
            //Executing in tests: run it now!
            r.run();
            return;
        }

        if (Display.getCurrent() == null) {
            Display.getDefault().syncExec(r);
        } else {
            //We already have a hold to it
            r.run();
        }
    }

    public static void async(Runnable r) {
        async(r, false);
    }

    public static void async(Runnable r, boolean runNowIfInUiThread) {
        listeners.call(new RunInUiThreadInfo(r, true, runNowIfInUiThread));
        if (SharedCorePlugin.inTestMode()) {
            //Executing in tests: run it now!
            r.run();
            return;
        }

        Display current = Display.getCurrent();
        if (current == null) {
            Display.getDefault().asyncExec(r);
        } else {
            if (runNowIfInUiThread) {
                r.run();
            } else {
                current.asyncExec(r);
            }
        }
    }
}
