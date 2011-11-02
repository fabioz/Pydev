package org.python.pydev.editor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.python.pydev.editorinput.PydevZipFileEditorInput;

public class PyExternalZipFileAnnotationModel extends AbstractMarkerAnnotationModel {

    public PyExternalZipFileAnnotationModel(PydevZipFileEditorInput element) {
    }

    @Override
    protected IMarker[] retrieveMarkers() throws CoreException {
        return new IMarker[0];
    }

    @Override
    protected void deleteMarkers(IMarker[] markers) throws CoreException {
    }

    @Override
    protected void listenToMarkerChanges(boolean listen) {
    }

    @Override
    protected boolean isAcceptable(IMarker marker) {
        return false;
    }

}
