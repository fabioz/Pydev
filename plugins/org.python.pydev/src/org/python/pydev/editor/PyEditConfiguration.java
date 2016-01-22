/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: July 10, 2003
 */

package org.python.pydev.editor;

import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferencesPage;
import org.python.pydev.editor.codecompletion.PythonCompletionProcessor;
import org.python.pydev.editor.codecompletion.PythonStringCompletionProcessor;
import org.python.pydev.editor.correctionassist.PyCorrectionAssistant;
import org.python.pydev.editor.correctionassist.PythonCorrectionProcessor;
import org.python.pydev.editor.hover.PyAnnotationHover;
import org.python.pydev.editor.hover.PyTextHover;
import org.python.pydev.editor.simpleassist.SimpleAssistProcessor;
import org.python.pydev.ui.ColorAndStyleCache;

/**
 * Adds simple partitioner, and specific behaviors like double-click actions to the TextWidget.
 * 
 * <p>
 * Implements a simple partitioner that does syntax highlighting.
 * 
 * Changed to a subclass of TextSourceViewerConfiguration as of pydev 1.3.5 
 */
public class PyEditConfiguration extends PyEditConfigurationWithoutEditor {

    private IPySyntaxHighlightingAndCodeCompletionEditor edit;

    /**
     * @param edit The edit to set.
     */
    private void setEdit(IPySyntaxHighlightingAndCodeCompletionEditor edit) {
        this.edit = edit;
    }

    /**
     * @return Returns the edit.
     */
    private IPySyntaxHighlightingAndCodeCompletionEditor getEdit() {
        return edit;
    }

    public PyEditConfiguration(ColorAndStyleCache colorManager, IPySyntaxHighlightingAndCodeCompletionEditor edit,
            IPreferenceStore preferenceStore) {
        super(colorManager, preferenceStore, edit);
        this.setEdit(edit);
    }

    @Override
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
        return new PyAnnotationHover(sourceViewer);
    }

    @Override
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        return new PyTextHover(sourceViewer, contentType);
    }

    /*
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getHyperlinkDetectorTargets(org.eclipse.jface.text.source.ISourceViewer)
     * @since 3.3
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, IPySyntaxHighlightingAndCodeCompletionEditor> getHyperlinkDetectorTargets(
            ISourceViewer sourceViewer) {
        Map<String, IPySyntaxHighlightingAndCodeCompletionEditor> targets = super
                .getHyperlinkDetectorTargets(sourceViewer);
        targets.put("org.python.pydev.editor.PythonEditor", edit); //$NON-NLS-1$
        return targets;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getContentAssistant(org.eclipse.jface.text.source.ISourceViewer)
     */
    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        // next create a content assistant processor to populate the completions window
        IContentAssistProcessor processor = new SimpleAssistProcessor(edit, new PythonCompletionProcessor(edit,
                pyContentAssistant), pyContentAssistant);

        PythonStringCompletionProcessor stringProcessor = new PythonStringCompletionProcessor(edit, pyContentAssistant);

        pyContentAssistant.setRestoreCompletionProposalSize(getSettings("pydev_completion_proposal_size"));

        // No code completion in comments
        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_SINGLELINE_BYTES1);
        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_SINGLELINE_BYTES2);
        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_MULTILINE_BYTES1);
        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_MULTILINE_BYTES2);

        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_SINGLELINE_UNICODE1);
        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_SINGLELINE_UNICODE2);
        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_MULTILINE_UNICODE1);
        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_MULTILINE_UNICODE2);

        pyContentAssistant
                .setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_SINGLELINE_BYTES_OR_UNICODE1);
        pyContentAssistant
                .setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_SINGLELINE_BYTES_OR_UNICODE2);
        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_MULTILINE_BYTES_OR_UNICODE1);
        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_MULTILINE_BYTES_OR_UNICODE2);

        pyContentAssistant.setContentAssistProcessor(stringProcessor, IPythonPartitions.PY_COMMENT);
        pyContentAssistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
        pyContentAssistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
        pyContentAssistant.enableAutoActivation(true); //always true, but the chars depend on whether it is activated or not in the preferences

        //note: delay and auto activate are set on PyContentAssistant constructor.

        pyContentAssistant.setDocumentPartitioning(IPythonPartitions.PYTHON_PARTITION_TYPE);
        pyContentAssistant.setAutoActivationDelay(PyCodeCompletionPreferencesPage.getAutocompleteDelay());

        return pyContentAssistant;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getQuickAssistAssistant(org.eclipse.jface.text.source.ISourceViewer)
     */
    @Override
    public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
        // create a content assistant:
        PyCorrectionAssistant assistant = new PyCorrectionAssistant();

        // next create a content assistant processor to populate the completions window
        IQuickAssistProcessor processor = new PythonCorrectionProcessor(this.getEdit());

        // Correction assist works on all
        assistant.setQuickAssistProcessor(processor);
        assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

        //delay and auto activate set on PyContentAssistant constructor.

        return assistant;
    }
}