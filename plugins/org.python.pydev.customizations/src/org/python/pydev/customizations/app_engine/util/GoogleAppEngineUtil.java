package org.python.pydev.customizations.app_engine.util;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.plugin.nature.PythonNature;

public class GoogleAppEngineUtil{

    public static IPythonPathNature getPythonPathNatureFromObject(Object receiver){
        IContainer container = getContainerFromObject(receiver);
        
        if(container == null){
            return null;
        }
        
        IProject project = container.getProject();
        if(project == null){
            return null;
        }
        
        IPythonPathNature nature = PythonNature.getPythonPathNature(project);
        return nature;
    }
    

    
    public static IContainer getContainerFromObject(Object receiver){
        if(receiver instanceof IWrappedResource){
            IWrappedResource wrappedResource = (IWrappedResource) receiver;
            Object actualObject = wrappedResource.getActualObject();
            if(actualObject instanceof IContainer){
                return (IContainer) actualObject;
            }
        }
        if(receiver instanceof IContainer){
            return (IContainer) receiver;
        }
        return null;
    }
}
