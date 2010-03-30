/*
 * Created on 18/08/2005
 */
package org.python.pydev.editorinput;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This class is also added to the plugin.xml so that we map the pydev document provider to this class.
 * 
 * Note: as of 3.3, it might be worth using FileStoreEditorInput (but only when the support for 3.2 is dropped).
 * 
 * @author Fabio
 */
public class PydevFileEditorInput implements IPathEditorInput, ILocationProvider {

    /**
     * The workbench adapter which simply provides the label.
     *
     * @since 3.1
     */
    private static class WorkbenchAdapter implements IWorkbenchAdapter {
        /*
         * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
         */
        public Object[] getChildren(Object o) {
            return null;
        }

        /*
         * @see org.eclipse.ui.model.IWorkbenchAdapter#getImageDescriptor(java.lang.Object)
         */
        public ImageDescriptor getImageDescriptor(Object object) {
            return null;
        }

        /*
         * @see org.eclipse.ui.model.IWorkbenchAdapter#getLabel(java.lang.Object)
         */
        public String getLabel(Object o) {
            return ((PydevFileEditorInput)o).getName();
        }

        /*
         * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
         */
        public Object getParent(Object o) {
            return null;
        }
    }

    private File fFile;
    private WorkbenchAdapter fWorkbenchAdapter= new WorkbenchAdapter();

    public PydevFileEditorInput(File file) {
        super();
        fFile= file;
        fWorkbenchAdapter= new WorkbenchAdapter();
    }
    /*
     * @see org.eclipse.ui.IEditorInput#exists()
     */
    public boolean exists() {
        return fFile.exists();
    }

    /*
     * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
     */
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    /*
     * @see org.eclipse.ui.IEditorInput#getName()
     */
    public String getName() {
        return fFile.getName();
    }

    /*
     * @see org.eclipse.ui.IEditorInput#getPersistable()
     */
    public IPersistableElement getPersistable() {
        return null;
    }

    /*
     * @see org.eclipse.ui.IEditorInput#getToolTipText()
     */
    public String getToolTipText() {
        return fFile.getAbsolutePath();
    }

    /*
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    public Object getAdapter(Class adapter) {
        if (ILocationProvider.class.equals(adapter))
            return this;
        if (IWorkbenchAdapter.class.equals(adapter))
            return fWorkbenchAdapter;
        return Platform.getAdapterManager().getAdapter(this, adapter);
    }

    /*
     * @see org.eclipse.ui.editors.text.ILocationProvider#getPath(java.lang.Object)
     */
    public IPath getPath(Object element) {
        if (element instanceof PydevFileEditorInput) {
            PydevFileEditorInput input= (PydevFileEditorInput) element;
            return Path.fromOSString(input.fFile.getAbsolutePath());
        }
        return null;
    }

    /*
     * @see org.eclipse.ui.IPathEditorInput#getPath()
     * @since 3.1
     */
    public IPath getPath() {
        return Path.fromOSString(fFile.getAbsolutePath());
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o == this){
            return true;
        }

        if (o instanceof PydevFileEditorInput) {
            PydevFileEditorInput input= (PydevFileEditorInput) o;
            return fFile.equals(input.fFile);
        }

        if (o instanceof IFileEditorInput) {
            IFileEditorInput input = (IFileEditorInput) o;
            IFile file = input.getFile();
            String resourceOSString = PydevPlugin.getIResourceOSString(file);
            if(resourceOSString == null){
                //the resource does not exist anymore (unable to get location)
                return false;
            }
            File otherFile = new File(resourceOSString);
            return fFile.equals(otherFile);
        }
        
        if (o instanceof IPathEditorInput) {
            IPathEditorInput input= (IPathEditorInput)o;
            return getPath().equals(input.getPath());
        }
        
        try {
			if (o instanceof IURIEditorInput) {
				IURIEditorInput iuriEditorInput = (IURIEditorInput) o;
				return new File(iuriEditorInput.getURI()).equals(fFile);
			}
		} catch (Throwable e) {
			//IURIEditorInput not added until eclipse 3.3
		}

        return false;
    }

    /*
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return fFile.hashCode();
    }
    
    public File getFile() {
        return fFile;
    }
}

