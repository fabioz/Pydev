package org.python.pydev.navigator;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.python.pydev.core.resource_stubs.AbstractIWorkspaceRootStub;

public class WorkspaceRootStub extends AbstractIWorkspaceRootStub implements IWorkbenchAdapter{

    
    public Object getAdapter(Class adapter) {
        if(adapter == IWorkbenchAdapter.class){
            return this;
        }
        throw new RuntimeException("Not implemented for: "+adapter);
    }
    
    //IWorkbenchAdapter
    List<Object> children = new ArrayList<Object>();
    public void addChild(Object child){
        children.add(child);
    }

    public Object[] getChildren(Object o) {
        return children.toArray();
    }

    public ImageDescriptor getImageDescriptor(Object object) {
        throw new RuntimeException("Not implemented");
    }

    public String getLabel(Object o) {
        throw new RuntimeException("Not implemented");
    }

    public Object getParent(Object o) {
        throw new RuntimeException("Not implemented");
    }
    
    public IProject getProject() {
        return null;
    }
    
    public IContainer getParent() {
        return null;
    }

}
