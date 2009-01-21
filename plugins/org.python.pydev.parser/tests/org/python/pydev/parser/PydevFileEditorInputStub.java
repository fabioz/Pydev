package org.python.pydev.parser;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class PydevFileEditorInputStub implements IEditorInput {

    public boolean exists() {
        return true;
    }

    public ImageDescriptor getImageDescriptor() {
        throw new RuntimeException("Not implemented");
    }

    public String getName() {
        throw new RuntimeException("Not implemented");
    }

    public IPersistableElement getPersistable() {
        throw new RuntimeException("Not implemented");
    }

    public String getToolTipText() {
        throw new RuntimeException("Not implemented");
    }

    public Object getAdapter(Class adapter) {
        throw new RuntimeException("Not implemented");
    }

}
