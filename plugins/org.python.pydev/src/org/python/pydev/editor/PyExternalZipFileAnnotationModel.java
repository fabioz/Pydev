/******************************************************************************
* Copyright (C) 2011  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.editor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.python.pydev.shared_ui.editor_input.PydevZipFileEditorInput;

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
