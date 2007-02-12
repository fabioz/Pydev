package org.python.pydev.navigator;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.IContributorResourceAdapter;

public class ModelAdapter implements IAdapterFactory{

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if(adaptableObject instanceof IAdaptable){
            IAdaptable adaptable = (IAdaptable) adaptableObject;
            Object adapter = adaptable.getAdapter(adapterType);
            //System.out.println("Returning adapter:"+adapter+"\tfor:"+adapterType+"\tfor object:"+adaptableObject);
            return adapter;
            
        }
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[]{IWrappedResource.class, ResourceMapping.class, IResource.class, IFolder.class, IFile.class, 
                IContainer.class, IContributorResourceAdapter.class};
    }

}
