/*
 * Created on Oct 8, 2006
 * @author Fabio
 */
package org.python.pydev.navigator;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IAdaptable;

/**
 * @author Fabio
 */
public class PythonSourceFolder implements IChildResource, IAdaptable{

    public IFolder folder;
    public Object parentElement;
    public Map<Object, IChildResource> children = new HashMap<Object, IChildResource>();

    public PythonSourceFolder(Object parentElement, IFolder folder) {
        this.parentElement = parentElement;
        this.folder = folder;
    }

	public Object getParent() {
		return parentElement;
	}

	public Object getActualObject() {
		return folder;
	}

	public PythonSourceFolder getSourceFolder() {
		return this;
	}
	
	public void addChild(Object actualObject, IChildResource child){
		children.put(actualObject, child);
	}
	
	public void removeChild(Object actualObject){
		children.remove(actualObject);
	}
	
	public Object getChild(Object actualObject){
		return children.get(actualObject);
	}

    public Object getAdapter(Class adapter) {
        return folder.getAdapter(adapter);
    }

    public int getRank() {
        return IChildResource.RANK_SOURCE_FOLDER;
    }
}
