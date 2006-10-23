/*
 * Created on Oct 8, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IContributorResourceAdapter;

/**
 * @author Fabio
 */
public class PythonSourceFolder implements IWrappedResource, IAdaptable{

    public IFolder folder;
    public Object parentElement;
    public Map<Object, IWrappedResource> children = new HashMap<Object, IWrappedResource>();
    public Map<IContainer, List<IResource>> childrenForContainer = new HashMap<IContainer, List<IResource>>();

    public PythonSourceFolder(Object parentElement, IFolder folder) {
        this.parentElement = parentElement;
        this.folder = folder;
        //System.out.println("Created PythonSourceFolder:"+this+" - "+folder+" parent:"+parentElement);
    }

	public Object getParentElement() {
		return parentElement;
	}

	public Object getActualObject() {
		return folder;
	}

	public PythonSourceFolder getSourceFolder() {
		return this;
	}
	
	public void addChild(IResource actualObject, IWrappedResource child){
	    //System.out.println("Adding child:"+child+" for resource:"+actualObject);
		children.put(actualObject, child);
        
        IContainer container = actualObject.getParent();
        List<IResource> l = childrenForContainer.get(container);
        if(l == null){
            l = new ArrayList<IResource>();
            childrenForContainer.put(container, l);
        }
        l.add(actualObject);
	}
	
	public void removeChild(IResource actualObject){
	    //System.out.println("Removing child:"+actualObject);
        children.remove(actualObject);
        if(actualObject instanceof IContainer){
            List<IResource> l = childrenForContainer.get(actualObject);
            if(l != null){
                for (IResource resource : l) {
                    removeChild(resource);
                }
                childrenForContainer.remove(actualObject);
            }
        }
	}
	
	public Object getChild(IResource actualObject){
		IWrappedResource ret = children.get(actualObject);
		//System.out.println("Gotten child:"+ret+" for resource:"+actualObject);
        return ret;
	}
    
    
	
	public int getRank() {
	    return IWrappedResource.RANK_SOURCE_FOLDER;
	}
    

    public IResource getAdaptedResource(IAdaptable adaptable) {
        return (IResource) getActualObject();
    }

    public Object getAdapter(Class adapter) {
        if(adapter == IContributorResourceAdapter.class){
            return this;
        }
        Object ret = ((IResource)this.getActualObject()).getAdapter(adapter);
        return ret;
    }

}
