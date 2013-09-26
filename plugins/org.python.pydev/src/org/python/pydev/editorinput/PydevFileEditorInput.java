/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 18/08/2005
 */
package org.python.pydev.editorinput;

import java.io.File;
import java.net.URI;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.editors.text.ILocationProviderExtension;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.io.FileUtils;

/**
 * This class is also added to the plugin.xml so that we map the pydev document provider to this class.
 * 
 * Note: as of 3.3, it might be worth using FileStoreEditorInput (but only when the support for 3.2 is dropped).
 * 
 * @author Fabio
 */
public class PydevFileEditorInput implements IPathEditorInput, ILocationProvider, ILocationProviderExtension,
        IURIEditorInput, IPersistableElement {

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
            return ((PydevFileEditorInput) o).getName();
        }

        /*
         * @see org.eclipse.ui.model.IWorkbenchAdapter#getParent(java.lang.Object)
         */
        public Object getParent(Object o) {
            return null;
        }
    }

    private File fFile;
    private WorkbenchAdapter fWorkbenchAdapter = new WorkbenchAdapter();

    private PydevFileEditorInput(File file) {
        super();
        fFile = file;
        fWorkbenchAdapter = new WorkbenchAdapter();
    }

    /**
     * Creates an editor input for the passed file.
     * 
     * If forceExternalFile is True, it won't even try to create a FileEditorInput, otherwise,
     * it will try to create it with the most suitable type it can 
     * (i.e.: FileEditorInput, FileStoreEditorInput, PydevFileEditorInput, ...)
     */
    public static IEditorInput create(File file, boolean forceExternalFile) {
        IPath path = Path.fromOSString(FileUtils.getFileAbsolutePath(file));

        if (!forceExternalFile) {
            //May call again to this method (but with forceExternalFile = true)
            IEditorInput input = new PySourceLocatorBase().createEditorInput(path, false, null, null);
            if (input != null) {
                return input;
            }
        }

        IPath zipPath = new Path("");
        while (path.segmentCount() > 0) {
            if (path.toFile().exists()) {
                break;
            }
            zipPath = new Path(path.lastSegment()).append(zipPath);
            path = path.uptoSegment(path.segmentCount() - 1);
        }

        if (zipPath.segmentCount() > 0 && path.segmentCount() > 0) {
            return new PydevZipFileEditorInput(new PydevZipFileStorage(path.toFile(), zipPath.toPortableString()));
        }

        try {
            URI uri = file.toURI();
            return new FileStoreEditorInput(EFS.getStore(uri));
        } catch (Throwable e) {
            //not always available! (only added in eclipse 3.3)
            return new PydevFileEditorInput(file);
        }
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
        return this;
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
        if (adapter.isInstance(this)) {
            return this;
        }
        if (IWorkbenchAdapter.class.equals(adapter)) {
            return fWorkbenchAdapter;
        }
        return Platform.getAdapterManager().getAdapter(this, adapter);
    }

    /*
     * @see org.eclipse.ui.editors.text.ILocationProvider#getPath(java.lang.Object)
     */
    public IPath getPath(Object element) {
        if (element instanceof PydevFileEditorInput) {
            PydevFileEditorInput input = (PydevFileEditorInput) element;
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

    /**
     * @return a file that the passed editor input wraps or null if it can't find out about it.
     */
    public static File getFile(IEditorInput o) {
        if (o == null) {
            return null;
        }
        if (o instanceof PydevFileEditorInput) {
            PydevFileEditorInput input = (PydevFileEditorInput) o;
            return input.fFile;
        }

        if (o instanceof IFileEditorInput) {
            IFileEditorInput input = (IFileEditorInput) o;
            IFile file = input.getFile();
            String resourceOSString = PydevPlugin.getIResourceOSString(file);
            if (resourceOSString == null) {
                //the resource does not exist anymore (unable to get location)
                return null;
            }
            return new File(resourceOSString);
        }

        if (o instanceof IPathEditorInput) {
            IPathEditorInput input = (IPathEditorInput) o;
            return new File(input.getPath().toOSString());
        }

        try {
            if (o instanceof IURIEditorInput) {
                IURIEditorInput iuriEditorInput = (IURIEditorInput) o;
                return new File(iuriEditorInput.getURI());
            }
        } catch (Throwable e) {
            //IURIEditorInput not added until eclipse 3.3
        }
        return null;
    }

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof IEditorInput)) {
            return false;
        }
        File file = getFile((IEditorInput) o);
        return fFile.equals(file);
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

    public URI getURI(Object element) {
        if (element instanceof IURIEditorInput) {
            IURIEditorInput editorInput = (IURIEditorInput) element;
            return editorInput.getURI();
        }
        return null;
    }

    public URI getURI() {
        return fFile.toURI();
    }

    public void saveState(IMemento memento) {
        PyEditorInputFactory.saveState(memento, this);
    }

    public String getFactoryId() {
        return PyEditorInputFactory.FACTORY_ID;
    }
}
