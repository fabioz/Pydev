/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.utils;

import org.eclipse.swt.widgets.Display;
import org.python.pydev.shared_core.SharedCorePlugin;

public class RunInUiThread {

    public static void sync(Runnable r) {
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
