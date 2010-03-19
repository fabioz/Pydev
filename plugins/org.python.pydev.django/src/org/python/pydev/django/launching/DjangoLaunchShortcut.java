package org.python.pydev.django.launching;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.launching.AbstractLaunchShortcut;
import org.python.pydev.plugin.nature.PythonNature;

public class DjangoLaunchShortcut extends AbstractLaunchShortcut {

    
    protected String getLaunchConfigurationType() {
        return DjangoConstants.DJANGO_LAUNCH_CONFIGURATION_TYPE;
    }

    
    /**
     * The only thing different is that we have to override the creation of the default launch configuration.
     */
    @Override
    public ILaunchConfiguration createDefaultLaunchConfiguration(IResource[] resources) {
        
        try{
            ILaunchConfigurationWorkingCopy workingCopy = super.createDefaultLaunchConfigurationWithoutSaving(resources);
            
            
            //manage.py [options] runserver
            String mainDir = workingCopy.getAttribute(Constants.ATTR_LOCATION, "");
            //the attr location is something as ${workspace_loc:django2}
            workingCopy.setAttribute(Constants.ATTR_LOCATION, mainDir+"/${"+DjangoConstants.DJANGO_MANAGE_VARIABLE+"}");
            workingCopy.setAttribute(Constants.ATTR_PROGRAM_ARGUMENTS, "runserver --noreload");
            
            
            return workingCopy.doSave();
        } catch (CoreException e) {
            reportError(null, e);
            return null;
        }
    }
    
    @Override
    protected IInterpreterManager getInterpreterManager(IProject project){
        return PythonNature.getPythonNature(project).getRelatedInterpreterManager();
    }
}
