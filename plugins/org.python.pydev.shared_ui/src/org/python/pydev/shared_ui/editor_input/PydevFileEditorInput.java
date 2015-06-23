/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 18/08/2005
 */
package org.python.pydev.shared_ui.editor_input;

import java.io.File;
import java.net.URI;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.editors.text.ILocationProvider;
import org.eclipse.ui.editors.text.ILocationProviderExtension;
import org.eclipse.ui.model.IWorkbenchAdapter;

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

    File fFile;
    private WorkbenchAdapter fWorkbenchAdapter = new WorkbenchAdapter();

    public PydevFileEditorInput(File file) {
        super();
        fFile = file;
        fWorkbenchAdapter = new WorkbenchAdapter();
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

    /*
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof IEditorInput)) {
            return false;
        }
        File file = EditorInputUtils.getFile((IEditorInput) o);
        return fFile.equals(file);
    }

    /*
     * @see java.lang.Object#hashCode()
     */
    @Override
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
