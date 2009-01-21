package com.python.pydev.debug.ui.launching;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.launching.AbstractLaunchShortcut;

public class PydevdServerLaunchShortcut extends AbstractLaunchShortcut {
    
    @Override    
    protected ILaunchConfiguration createDefaultLaunchConfiguration( IResource[] resources ) {    
        ILaunchManager manager = org.eclipse.debug.core.DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType type = manager.getLaunchConfigurationType(getLaunchConfigurationType());
        if (type == null) {
            reportError("Python launch configuration not found", null);
            return null;
        }

        StringBuffer buffer = new StringBuffer("Debug Server");
        String name = buffer.toString().trim();
        
        try {

            ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, name);
            // Python Main Tab Arguments
            workingCopy.setAttribute(Constants.ATTR_PROJECT,"Pydevd Debug Server");
            workingCopy.setAttribute(Constants.ATTR_RESOURCE_TYPE,1);
            
            workingCopy.setAttribute(IDebugUIConstants.ATTR_LAUNCH_IN_BACKGROUND, false);

            // Common Tab Arguments
            CommonTab tab = new CommonTab();
            tab.setDefaults(workingCopy);
            tab.dispose();
            return workingCopy.doSave();
        } catch (CoreException e) {
            reportError(null, e);
            return null;
        }
    }
    
    @Override
    public void launch(IResource[] file, String mode, String targetAttribute) {
        ILaunchConfiguration conf = createDefaultLaunchConfiguration(file);                
        DebugUITools.launch(conf, mode);    
    }
    
    @Override
    protected String getLaunchConfigurationType() {
        return "com.python.pydev.debug.pydevdServerLaunchConfigurationType";
    }
}
