/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.customizations.app_engine.util;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Shell;
import org.python.pydev.customizations.common.ProcessWindow;

/**
 * The window for handling app engine for a given project.
 */
public class AppEngineProcessWindow extends ProcessWindow {

    public AppEngineProcessWindow(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected String[] getAvailableCommands() {
        return getAvailableCommands(container);
    }

    public static String[] getAvailableCommands(IResource resource) {
        String loc = getResourceLoc(resource);
        return new String[] { getUpdateCommand(resource), "rollback --secure " + loc,
                "update_indexes --secure " + getResourceLoc(resource),
                "vacuum_indexes --secure " + getResourceLoc(resource),
                "request_logs --secure " + getResourceLoc(resource) + " my_output_file.log", };
    }

    private static String getResourceLoc(IResource resource) {
        String loc = resource.getLocation().toOSString();
        if (loc.indexOf(' ') != -1) {
            loc = "\"" + loc + "\""; //add escaping to the argument
        }
        return loc;
    }

    public static String getUpdateCommand(IResource resource) {
        return "update --secure " + getResourceLoc(resource);
    }

}
