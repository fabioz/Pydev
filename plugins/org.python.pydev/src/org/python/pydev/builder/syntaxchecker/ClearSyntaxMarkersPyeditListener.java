/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.builder.syntaxchecker;

import java.util.ListResourceBundle;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.IPyEditListener3;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.parser.PyParser;

/**
 * When the editor is disposed, if needed this class will remove the markers from the related
 * file (if no other editor is still editing the same file).
 * 
 * @author Fabio
 */
public class ClearSyntaxMarkersPyeditListener implements IPyEditListener, IPyEditListener3 {

    public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor) {
    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor) {
        //remove the markers if we want problems only in the active editor.
        IEditorInput input = edit.getEditorInput();
        removeMarkersFromInput(input);
    }

    public void onSave(PyEdit edit, IProgressMonitor monitor) {
    }

    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {
    }

    public void onInputChanged(PyEdit edit, IEditorInput oldInput, IEditorInput input, IProgressMonitor monitor) {
        removeMarkersFromInput(oldInput);
    }

    /**
     * This function will remove the markers from the passed input.
     * @param input the input
     */
    private void removeMarkersFromInput(IEditorInput input) {
        if (input != null && PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor()) {
            if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                Log.toLogFile(this, "removing syntax error markers from editor.");
            }
            IFile relatedFile = (IFile) input.getAdapter(IFile.class);

            if (relatedFile != null && relatedFile.exists()) {
                //when disposing, remove all markers
                try {
                    PyParser.deleteErrorMarkers(relatedFile);
                } catch (CoreException e) {
                    Log.log(e);
                }
            }
        }
    }

}
