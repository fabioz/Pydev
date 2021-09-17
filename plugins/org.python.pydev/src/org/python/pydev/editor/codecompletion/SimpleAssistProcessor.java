/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */
package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalSorter;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.python.pydev.ast.codecompletion.ProposalsComparator;
import org.python.pydev.ast.codecompletion.ProposalsComparator.CompareContext;
import org.python.pydev.ast.codecompletion.PyCodeCompletionPreferences;
import org.python.pydev.ast.simpleassist.ISimpleAssistParticipant;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IPySyntaxHighlightingAndCodeCompletionEditor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PyDevUiPrefs;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;

/**
 * This processor controls the completion cycle (and also works as a 'delegator' to the processor that deals
 * with actual python completions -- which may be a bit slower that simple completions).
 *
 * @author Fabio
 */
public class SimpleAssistProcessor implements IContentAssistProcessor {

    private class ContextInformationDelegator implements IContextInformationValidator, IContextInformationPresenter {
        private final IContextInformationValidator defaultContextInformationValidator;

        private ContextInformationDelegator(IContextInformationValidator defaultContextInformationValidator) {
            Assert.isTrue(defaultContextInformationValidator instanceof IContextInformationPresenter);
            this.defaultContextInformationValidator = defaultContextInformationValidator;
        }

        @Override
        public void install(IContextInformation info, ITextViewer viewer, int offset) {
            defaultContextInformationValidator.install(info, viewer, offset);
        }

        @Override
        public boolean isContextInformationValid(int offset) {
            if (showDefault()) {
                return defaultContextInformationValidator.isContextInformationValid(offset);
            }
            return true;
        }

        @Override
        public boolean updatePresentation(int offset, TextPresentation presentation) {
            return ((IContextInformationPresenter) defaultContextInformationValidator).updatePresentation(offset,
                    presentation);
        }
    }

    public static final char[] ALL_ASCII_CHARS = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
            'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F',
            'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '_' };

    //-------- cycling through simple completions and default processor
    private static final int SHOW_SIMPLE = 1;
    private static final int SHOW_DEFAULT = 2;
    private int whatToShow = SHOW_SIMPLE;

    public void startCycle() {
        whatToShow = SHOW_SIMPLE;
    }

    public void doCycle() {
        if (whatToShow == SHOW_SIMPLE) {
            whatToShow = SHOW_DEFAULT;
        }
        //cycles only once here
    }

    public void updateStatus() {
        if (whatToShow == SHOW_SIMPLE) {
            assistant.setIterationStatusMessage("Press %s for default completions.");
        }
    }

    //-------- end cycling through regular completions and templates

    /**
     * The editor that contains this processor
     */
    private IPySyntaxHighlightingAndCodeCompletionEditor edit;

    /**
     * The 'default' processor (gets python completions)
     */
    private PythonCompletionProcessor defaultPythonProcessor;

    /**
     * The content assistant that contains this processor
     */
    private PyContentAssistant assistant;

    /**
     * Participants for a simple completion
     */
    private List<ISimpleAssistParticipant> participants;

    /**
     * Whether the last completion was auto-activated or not
     */
    private boolean lastCompletionAutoActivated;

    /**
     * Whether we should use the default auto-completion on all ascii chars
     * Cleared when the property cache is updated (based on autoActivationCharsCache)
     */
    private volatile static boolean useAutocompleteOnAllAsciiCharsCache;

    /**
     * Cache with the chars that should be used for auto-activation
     * Cleared when the property cache is updated
     */
    private volatile static char[] autoActivationCharsCache;

    /**
     * The last error that occurred while requesting a completion.
     */
    private String lastError = null;

    private KeepProposalsComparatorSynched keepSynched;

    @SuppressWarnings("unchecked")
    public SimpleAssistProcessor(final IPySyntaxHighlightingAndCodeCompletionEditor edit,
            PythonCompletionProcessor defaultPythonProcessor, final PyContentAssistant assistant) {
        this.edit = edit;
        this.defaultPythonProcessor = defaultPythonProcessor;
        this.assistant = assistant;
        this.assistant.setSorter(new ICompletionProposalSorter() {

            @Override
            public int compare(ICompletionProposal p1, ICompletionProposal p2) {
                return sorter.compare((ICompletionProposalHandle) p1, (ICompletionProposalHandle) p2);
            }
        });

        //Note: in practice, we'll always have at least one participart (for the keywords)
        this.participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_SIMPLE_ASSIST);

        assistant.addCompletionListener(new ICompletionListener() {
            @Override
            public void assistSessionEnded(ContentAssistEvent event) {
                if (keepSynched != null) {
                    keepSynched.dispose();
                    keepSynched = null;
                }
            }

            @Override
            public void assistSessionStarted(ContentAssistEvent event) {
                startCycle();
                lastCompletionAutoActivated = assistant.getLastCompletionAutoActivated();
                if (!lastCompletionAutoActivated) {
                    //user request... cycle to the default completions at once
                    doCycle();
                }
            }

            @Override
            public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
                //ignore
            }
        });

    }

    private static class KeepProposalsComparatorSynched
            implements Listener {

        private ITextViewer viewer;
        private ProposalsComparator sorter;

        public KeepProposalsComparatorSynched(ITextViewer viewer, IDocument doc, int offset,
                ProposalsComparator sorter) {
            this.viewer = viewer;
            this.sorter = sorter;
            viewer.getTextWidget().addListener(SWT.Modify, this);
            viewer.getTextWidget().addListener(SWT.KeyUp, this);
        }

        public void dispose() {
            if (this.viewer != null) {
                viewer.getTextWidget().removeListener(SWT.Modify, this);
                viewer.getTextWidget().removeListener(SWT.KeyUp, this);
                this.viewer = null;
                this.sorter = null;
            }
        }

        @Override
        public void handleEvent(Event event) {
            updateQualifier();
        }

        private void updateQualifier() {
            IDocument doc = this.viewer.getDocument();

            String[] strs = PySelection.getActivationTokenAndQualifier(doc, this.viewer.getSelectedRange().x, false);

            String qualifier = strs[1];
            this.sorter.setQualifier(qualifier);
        }

    }

    private final ProposalsComparator sorter = new ProposalsComparator("", null);

    /**
     * Computes the proposals (may forward for simple or 'complete' proposals)
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
        try {
            IDocument doc = viewer.getDocument();
            String[] strs = PySelection.getActivationTokenAndQualifier(doc, offset, false);

            String activationToken = strs[0];
            String qualifier = strs[1];
            IPythonNature pythonNature = edit.getPythonNature();
            this.sorter.setQualifier(qualifier);
            this.sorter.setCompareContext(new CompareContext(pythonNature));

            if (this.keepSynched != null) {
                this.keepSynched.dispose();
                this.keepSynched = null;
            }
            if (viewer.getTextWidget() != null) {
                this.keepSynched = new KeepProposalsComparatorSynched(viewer, doc, offset, this.sorter);
            }

            if (showDefault()) {
                ICompletionProposal[] ret = defaultPythonProcessor.computeCompletionProposals(viewer, offset);
                return ret;

            } else {
                updateStatus();

                PySelection ps = edit.createPySelection();
                if (ps == null) {
                    ps = new PySelection(doc, offset);
                }
                List<ICompletionProposalHandle> results = new ArrayList<ICompletionProposalHandle>();

                for (ISimpleAssistParticipant participant : participants) {
                    results.addAll(
                            participant.computeCompletionProposals(activationToken, qualifier, ps, edit, offset));
                }

                //don't matter the result... next time we won't ask for simple stuff
                doCycle();
                if (results.size() == 0) {
                    if (!lastCompletionAutoActivated || defaultAutoActivated(viewer, offset)
                            || useAutocompleteOnAllAsciiCharsCache) {
                        return defaultPythonProcessor.computeCompletionProposals(viewer, offset);
                    }
                    return new ICompletionProposal[0];
                } else {
                    Collections.sort(results, this.sorter);
                    return results.toArray(new ICompletionProposal[0]);
                }
            }
        } catch (Exception e) {
            Log.log(e);
            CompletionError completionError = new CompletionError(e);
            this.lastError = completionError.getErrorMessage();
            //Make the error visible to the user!
            return new ICompletionProposal[] { completionError };
        }
    }

    /**
     * Determines whether it was auto-activated on the default completion or in the simple one.
     * @param viewer the viewer for which this completion was requested
     * @param offset the offset at which it was requested
     * @return true if it was auto-activated for the default completion (and false if it was for the simple)
     */
    private boolean defaultAutoActivated(ITextViewer viewer, int offset) {
        try {
            char docChar = viewer.getDocument().getChar(offset - 1);
            for (char c : this.defaultPythonProcessor.getCompletionProposalAutoActivationCharacters()) {
                if (c == docChar) {
                    return true;
                }
            }

        } catch (BadLocationException e) {
        }
        return false;
    }

    /**
     * @return true if we should show the default completions (and false if we shouldn't)
     */
    private boolean showDefault() {
        return whatToShow == SHOW_DEFAULT;
    }

    /**
     * Compute context information
     */
    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
        if (showDefault()) {
            return defaultPythonProcessor.computeContextInformation(viewer, offset);
        }
        return null;
    }

    /**
     * only very simple proposals should be here, as it is auto-activated for any character
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
     */
    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        return getStaticAutoActivationCharacters();
    }

    /**
     * Attribute that determines if the listener that'll clear the auto activation chars is already in place.
     */
    private volatile static boolean listenerToClearAutoActivationAlreadySetup = false;

    /**
     * @return the auto-activation chars that should be used: always all chars ascii chars + default options.
     *
     * The logic is that the first is always for the 'simple' keywords (i.e.: print, self, etc.)
     */
    public synchronized static char[] getStaticAutoActivationCharacters() {
        if (!listenerToClearAutoActivationAlreadySetup) {
            PyDevUiPrefs.getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent event) {
                    autoActivationCharsCache = null;
                }
            });
            listenerToClearAutoActivationAlreadySetup = true;
        }

        if (autoActivationCharsCache == null) {
            char[] defaultAutoActivationCharacters = PythonCompletionProcessor
                    .getStaticCompletionProposalAutoActivationCharacters();

            useAutocompleteOnAllAsciiCharsCache = PyCodeCompletionPreferences.useAutocompleteOnAllAsciiChars()
                    && PyCodeCompletionPreferences.useAutocomplete();

            char[] c2;
            //just use the extension for the simple if we do have it
            c2 = new char[ALL_ASCII_CHARS.length + defaultAutoActivationCharacters.length];
            System.arraycopy(ALL_ASCII_CHARS, 0, c2, 0, ALL_ASCII_CHARS.length);
            System.arraycopy(defaultAutoActivationCharacters, 0, c2, ALL_ASCII_CHARS.length,
                    defaultAutoActivationCharacters.length);
            autoActivationCharsCache = c2;
        }
        return autoActivationCharsCache;
    }

    /**
     * @return chars that are used for context information auto-activation
     */
    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    /**
     * @return some error that might have happened in the completion
     */
    @Override
    public String getErrorMessage() {
        String ret = this.lastError;
        if (ret == null && showDefault()) {
            ret = defaultPythonProcessor.getErrorMessage();
        }
        this.lastError = null;
        return ret;
    }

    /**
     * @return the validator we should use
     */
    @Override
    public IContextInformationValidator getContextInformationValidator() {
        final IContextInformationValidator defaultContextInformationValidator = defaultPythonProcessor
                .getContextInformationValidator();
        return new ContextInformationDelegator(defaultContextInformationValidator);
    }

}
