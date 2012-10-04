/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.codecoverage;

import java.util.ListResourceBundle;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.python.pydev.builder.PydevMarkerUtils;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;

/**
 * @author fabioz
 *
 */
public class RemoveCoverageMarkersListener implements IDocumentListener, IPyEditListener {

    private IDocument doc;
    private PyEdit edit;
    private IFile file;

    public RemoveCoverageMarkersListener(IDocument document, PyEdit edit, IFile file) {
        this.doc = document;
        this.edit = edit;
        this.file = file;

        document.addDocumentListener(this);
        edit.addPyeditListener(this);
    }

    public void onSave(PyEdit edit, IProgressMonitor monitor) {
        removeMarkersAndStopListening();
    }

    public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor) {
    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor) {
        removeMarkersAndStopListening();
    }

    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {
        removeMarkersAndStopListening();
    }

    public void documentAboutToBeChanged(DocumentEvent event) {
    }

    public void documentChanged(DocumentEvent event) {
        removeMarkersAndStopListening();
    }

    /**
     * 
     */
    private void removeMarkersAndStopListening() {
        PydevMarkerUtils.removeMarkers(file, PyCodeCoverageView.PYDEV_COVERAGE_MARKER);
        this.doc.removeDocumentListener(this);
        this.edit.removePyeditListener(this);

    }

}
