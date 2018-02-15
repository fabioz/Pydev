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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.python.pydev.core.IPySyntaxHighlightingAndCodeCompletionEditor;
import org.python.pydev.editor.codecompletion.PyContentAssistant;
import org.python.pydev.editor.correctionassist.PyCorrectionAssistant;
import org.python.pydev.editor.correctionassist.PythonCorrectionProcessor;
import org.python.pydev.editor.hover.PyAnnotationHover;
import org.python.pydev.editor.hover.PyEditorTextHoverDescriptor;
import org.python.pydev.editor.hover.PyEditorTextHoverProxy;
import org.python.pydev.editor.hover.PyHoverPreferencesPage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.utils.ArrayUtils;
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
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
        /**
         * Return the combining hover if the preferences are set accordingly and the state mask matches.
         */
        if (PyHoverPreferencesPage.getCombineHoverInfo()) {
            PyEditorTextHoverDescriptor combiningHover = PydevPlugin.getCombiningHoverDescriptor();
            if (combiningHover.getStateMask() == stateMask) {
                return new PyEditorTextHoverProxy(combiningHover, contentType);
            }
        }

        /**
         * We return the highest priority registered hover whose state mask matches. If two or more hovers
         * have the highest priority, it is indeterminate which will be selected. The proper way to combine
         * hover info is to select that option on the PyDev->Editor->Hover preference page. This will cause
         * the combining hover to be returned by the code above.
         */
        PyEditorTextHoverDescriptor[] hoverDescs = PydevPlugin.getDefault().getPyEditorTextHoverDescriptors();
        int i = 0;
        while (i < hoverDescs.length) {
            if (hoverDescs[i].isEnabled() && hoverDescs[i].getStateMask() == stateMask) {
                return new PyEditorTextHoverProxy(hoverDescs[i], contentType);
            }
            i++;
        }

        return null;
    }

    /*
     * @see SourceViewerConfiguration#getTextHover(ISourceViewer, String)
     */
    @Override
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        return getTextHover(sourceViewer, contentType, ITextViewerExtension2.DEFAULT_HOVER_STATE_MASK);
    }

    /*
     * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getHyperlinkDetectorTargets(org.eclipse.jface.text.source.ISourceViewer)
     * @since 3.3
     */
    @Override
    protected Map<String, IAdaptable> getHyperlinkDetectorTargets(
            ISourceViewer sourceViewer) {
        Map<String, IAdaptable> targets = super.getHyperlinkDetectorTargets(
                sourceViewer);
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
        PyContentAssistant contentAssistant = (PyContentAssistant) SetupContentAssist.configContentAssistant(
                edit, pyContentAssistant);
        pyContentAssistant.setRestoreCompletionProposalSize(getSettings("pydev_completion_proposal_size"));
        if (sourceViewer != null) {
            pyContentAssistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
        }

        return contentAssistant;
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

    /*
     * @see SourceViewerConfiguration#getConfiguredTextHoverStateMasks(ISourceViewer, String)
     * Implementation copied from org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration and adapted
     * for PyDev.
     */
    @Override
    public int[] getConfiguredTextHoverStateMasks(ISourceViewer sourceViewer, String contentType) {
        PyEditorTextHoverDescriptor[] hoverDescs = PydevPlugin.getDefault().getPyEditorTextHoverDescriptors();
        hoverDescs = ArrayUtils.concatArrays(hoverDescs,
                new PyEditorTextHoverDescriptor[] { PydevPlugin.getCombiningHoverDescriptor() });
        int stateMasks[] = new int[hoverDescs.length];
        int stateMasksLength = 0;
        for (int i = 0; i < hoverDescs.length; i++) {
            if (hoverDescs[i].isEnabled()) {
                int j = 0;
                int stateMask = hoverDescs[i].getStateMask();
                while (j < stateMasksLength) {
                    if (stateMasks[j] == stateMask) {
                        break;
                    }
                    j++;
                }
                if (j == stateMasksLength) {
                    stateMasks[stateMasksLength++] = stateMask;
                }
            }
        }
        if (stateMasksLength == hoverDescs.length) {
            return stateMasks;
        }

        int[] shortenedStateMasks = new int[stateMasksLength];
        System.arraycopy(stateMasks, 0, shortenedStateMasks, 0, stateMasksLength);
        return shortenedStateMasks;
    }
}