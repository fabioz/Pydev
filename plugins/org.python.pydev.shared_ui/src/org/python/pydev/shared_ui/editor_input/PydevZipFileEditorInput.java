/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.editor_input;

import java.io.File;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PlatformUI;

/**
 * This editor input enables Eclipse to open and show the contents of a file within a zip file.
 *
 * @author Fabio
 */
public class PydevZipFileEditorInput implements IStorageEditorInput, IPathEditorInput, IPersistableElement {

    /**
     * This is the file that we're wrapping in this editor input.
     */
    private final PydevZipFileStorage storage;

    public PydevZipFileEditorInput(PydevZipFileStorage storage) {
        this.storage = storage;
    }

    public IStorage getStorage() throws CoreException {
        return this.storage;
    }

    public File getFile() {
        return this.storage.zipFile;
    }

    public String getZipPath() {
        return this.storage.zipPath;
    }

    public boolean exists() {
        return true;
    }

    public ImageDescriptor getImageDescriptor() {
        IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
        return registry.getImageDescriptor(getContentType());
    }

    public String getName() {
        return this.storage.getName();
    }

    public IPersistableElement getPersistable() {
        return this;
    }

    public String getContentType() {
        return this.storage.getFullPath().getFileExtension();
    }

    public String getToolTipText() {
        IPath fullPath = storage.getFullPath();
        if (fullPath == null) {
            return null;
        }
        return fullPath.toString();
    }

    public Object getAdapter(Class adapter) {
        if (adapter.isInstance(this)) {
            return this;
        }
        return null;
    }

    public IPath getPath() {
        return storage.getFullPath();
    }

    public void saveState(IMemento memento) {
        PyEditorInputFactory.saveState(memento, this);
    }

    public String getFactoryId() {
        return PyEditorInputFactory.FACTORY_ID;
    }

    // It seems that it's not possible to define an URI to an element inside a zip file,
    // so, we can't properly implement ILocationProvider nor ILocationProviderExtension (meaning that the document connect
    // needs to be overridden to deal with external files).
    //
    //    public IPath getPath(Object element) {
    //        if(element instanceof PydevZipFileEditorInput){
    //            PydevZipFileEditorInput editorInput = (PydevZipFileEditorInput) element;
    //            return editorInput.getPath();
    //
    //        }
    //        return null;
    //    }
    //
    //    public URI getURI(Object element) {
    //        if(element instanceof PydevZipFileEditorInput){
    //            try {
    //                PydevZipFileEditorInput editorInput = (PydevZipFileEditorInput) element;
    //                URL url = editorInput.storage.zipFile.toURI().toURL();
    //                String externalForm = url.toExternalForm();
    //                return new URL("zip:"+externalForm+"!"+editorInput.storage.zipPath).toURI();
    //            } catch (Exception e) {
    //                Log.log(e);
    //            }
    //
    //        }
    //        return null;
    //    }

}
