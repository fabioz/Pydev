package org.python.pydev.customizations.app_engine.util;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Shell;

/**
 * The window for handling app engine for a given project.
 */
public class AppEngineProcessWindow extends ProcessWindow{

    public AppEngineProcessWindow(Shell parentShell) {
        super(parentShell);
    }

    protected String[] getAvailableCommands(){
        return getAvailableCommands(container);
    }

    public static String[] getAvailableCommands(IResource resource){
        return new String[] { 
                getUpdateCommand(resource), 
                "rollback --secure " + resource.getLocation().toOSString(),
                "update_indexes --secure " + resource.getLocation().toOSString(),
                "vacuum_indexes --secure " + resource.getLocation().toOSString(),
                "request_logs --secure " + resource.getLocation().toOSString() + " my_output_file.log", };
    }

    public static String getUpdateCommand(IResource resource){
        return "update --secure " + resource.getLocation().toOSString();
    }

}
