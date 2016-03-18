/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.builder;

import java.util.ListResourceBundle;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;
import org.python.pydev.shared_ui.editor.IPyEditListener3;

/**
 * When the editor is disposed, if needed this class will remove the markers from the related
 * file (if no other editor is still editing the same file) and will remove the hash from the
 * additional info.
 * 
 * @author Fabio
 */
public class ClearAnalysisMarkersPyEditListener implements IPyEditListener, IPyEditListener3 {

    @Override
    public void onCreateActions(ListResourceBundle resources, BaseEditor baseEditor, IProgressMonitor monitor) {

    }

    @Override
    public void onDispose(BaseEditor baseEditor, IProgressMonitor monitor) {
        PyEdit edit = (PyEdit) baseEditor;
        IEditorInput input = edit.getEditorInput();
        //remove the markers if we want problems only in the active editor.
        removeMarkersFromInput(input);
    }

    @Override
    public void onSave(BaseEditor baseEditor, IProgressMonitor monitor) {

    }

    @Override
    public void onSetDocument(IDocument document, BaseEditor baseEditor, IProgressMonitor monitor) {

    }

    @Override
    public void onInputChanged(BaseEditor baseEditor, IEditorInput oldInput, IEditorInput input,
            IProgressMonitor monitor) {
        removeMarkersFromInput(oldInput);
    }

    /**
     * Removes the markers from the given input
     * 
     * @param input the input that has a related resource that should have markers removed
     */
    private void removeMarkersFromInput(IEditorInput input) {
        if (input != null && PyDevBuilderPrefPage.getAnalyzeOnlyActiveEditor()
                && PyDevBuilderPrefPage.getRemoveErrorsWhenEditorIsClosed()) {
            IFile relatedFile = (IFile) input.getAdapter(IFile.class);

            if (relatedFile != null && relatedFile.exists()) {
                //when disposing, remove all markers
                AnalysisRunner.deleteMarkers(relatedFile);
            }
        }
    }

}
