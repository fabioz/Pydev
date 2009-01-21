package org.python.pydev.editorinput;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PlatformUI;

/**
 * This editor input enables Eclipse to open and show the contents of a file within a zip file.
 * 
 * @author Fabio
 */
public class PydevZipFileEditorInput implements IStorageEditorInput{

    /**
     * This is the file that we're wrapping in this editor input.
     */
    private final PydevZipFileStorage storage;

    public PydevZipFileEditorInput(PydevZipFileStorage storage){
        this.storage = storage;
    }
    
    public IStorage getStorage() throws CoreException {
        return this.storage;
    }

    public boolean exists() {
        return true;
    }

    public ImageDescriptor getImageDescriptor() {
        IEditorRegistry registry= PlatformUI.getWorkbench().getEditorRegistry();
        return registry.getImageDescriptor(getContentType());
    }

    public String getName() {
        return this.storage.getName();
    }

    public IPersistableElement getPersistable() {
        return null;
    }
    
    public String getContentType() {
        return this.storage.getFullPath().getFileExtension();
    }

    public String getToolTipText() {
        IPath fullPath= storage.getFullPath();
        if (fullPath == null)
            return null;
        return fullPath.toString();
    }

    public Object getAdapter(Class adapter) {
        return null;
    }

}
