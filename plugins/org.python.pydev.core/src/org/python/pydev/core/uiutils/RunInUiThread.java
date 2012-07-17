/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.uiutils;

import org.eclipse.swt.widgets.Display;
import org.python.pydev.core.CorePlugin;

public class RunInUiThread {

    public static void sync(Runnable r) {
        if (CorePlugin.getDefault() == null) {
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
        if (CorePlugin.getDefault() == null) {
            //Executing in tests: run it now!
            r.run();
            return;
        }

        Display current = Display.getCurrent();
        if (current == null) {
            Display.getDefault().asyncExec(r);
        } else {
            current.asyncExec(r);
        }
    }
}
