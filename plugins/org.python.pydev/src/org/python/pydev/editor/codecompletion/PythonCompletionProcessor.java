/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 29, 2004
 *
 */
package org.python.pydev.editor.codecompletion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.python.pydev.ast.codecompletion.CompletionRequest;
import org.python.pydev.ast.codecompletion.IPyCodeCompletion;
import org.python.pydev.ast.codecompletion.PyCodeCompletion;
import org.python.pydev.ast.codecompletion.PyCodeCompletionPreferences;
import org.python.pydev.ast.codecompletion.PyCodeCompletionUtils;
import org.python.pydev.ast.interpreter_managers.ChooseInterpreterManager;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPySyntaxHighlightingAndCodeCompletionEditor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.TokensOrProposalsList;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PyDevUiPrefs;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.content_assist.AbstractCompletionProcessorWithCycling;

/**
 * @author Dmoore
 * @author Fabio Zadrozny
 *
 * This class is responsible for code completion / template completion.
 */
public class PythonCompletionProcessor extends AbstractCompletionProcessorWithCycling {

    /**
     * This makes the templates completion
     */
    private IPyTemplateCompletionProcessor templatesCompletion;

    /**
     * This makes python code completion
     */
    private IPyCodeCompletion codeCompletion;

    /**
     * Edit.
     */
    private IPySyntaxHighlightingAndCodeCompletionEditor edit;

    /**
     * Some error...
     */
    private String error;

    /**
     * These are the activation chars (cache)
     */
    private volatile static char[] activationChars = null;

    /**
     * This is the class that manages the context information (validates it and
     * changes its presentation).
     */
    private PyContextInformationValidator contextInformationValidator;

    /**
     * @param edit the editor that works with this processor
     * @param pyContentAssistant the content assistant that will invoke this completion
     */
    public PythonCompletionProcessor(IPySyntaxHighlightingAndCodeCompletionEditor edit,
            PyContentAssistant pyContentAssistant) {
        super(pyContentAssistant);
        this.templatesCompletion = new PyTemplateCompletionProcessor();
        this.edit = edit;
        this.contentAssistant = pyContentAssistant;
        this.codeCompletion = getCodeCompletionEngine();

        contextInformationValidator = new PyContextInformationValidator();

        pyContentAssistant.addCompletionListener(new ICompletionListener() {

            @Override
            public void assistSessionEnded(ContentAssistEvent event) {
            }

            @Override
            public void assistSessionStarted(ContentAssistEvent event) {
                startCycle();
            }

            @Override
            public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
                //ignore
            }

        });

    }

    protected IPyCodeCompletion getCodeCompletionEngine() {
        return new PyCodeCompletion();
    }

    /**
     * This is the interface implemented to get the completions.
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
     */
    @Override
    public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
        updateStatus();
        ICompletionProposalHandle[] proposals;

        try {
            //FIRST: discover activation token and qualifier.
            IDocument doc = viewer.getDocument();

            //list for storing the proposals
            TokensOrProposalsList pythonAndTemplateProposals = new TokensOrProposalsList();

            IPythonNature nature = edit.getPythonNature();

            if (nature == null) {
                IInterpreterManager manager = ChooseInterpreterManager.chooseInterpreterManager();
                if (manager != null) {
                    nature = new SystemPythonNature(manager);
                } else {
                    CompletionError completionError = new CompletionError(new RuntimeException(
                            "No interpreter configured."));
                    this.error = completionError.getErrorMessage();
                    return new ICompletionProposal[] { completionError };
                }
            }

            if (nature == null || !nature.startRequests()) {
                return new ICompletionProposal[0];
            }
            try {
                CompletionRequest request = new CompletionRequest(edit.getEditorFile(), nature, doc, documentOffset,
                        codeCompletion, PyCodeCompletionPreferences.getUseSubstringMatchInCodeCompletion());

                //SECOND: getting code completions and deciding if templates should be shown too.
                //Get code completion proposals
                if (PyCodeCompletionPreferences.useCodeCompletion()) {
                    if (whatToShow == SHOW_ALL) {
                        try {
                            pythonAndTemplateProposals.addAll(getPythonProposals(documentOffset, doc, request));
                        } catch (Throwable e) {
                            Log.log(e);
                            CompletionError completionError = new CompletionError(e);
                            this.error = completionError.getErrorMessage();
                            //Make the error visible to the user!
                            return new ICompletionProposal[] { completionError };
                        }
                    }

                }

                String[] strs = PySelection.getActivationTokenAndQualifier(doc, documentOffset, false);

                String activationToken = strs[0];
                String qualifier = strs[1];

                //THIRD: Get template proposals (if asked for)
                if (request.showTemplates && (activationToken == null || activationToken.trim().length() == 0)) {
                    TokensOrProposalsList templateProposals = getTemplateProposals(viewer, documentOffset,
                            activationToken, qualifier);
                    pythonAndTemplateProposals.addAll(templateProposals);
                }

                //to show the valid ones, we'll get the qualifier from the initial request
                proposals = PyCodeCompletionUtils.onlyValid(pythonAndTemplateProposals, request.qualifier,
                        request.isInCalltip, request.useSubstringMatchInCodeCompletion, nature.getProject());

                // Note: sorting happens later on.
            } finally {
                nature.endRequests();
            }
        } catch (Exception e) {
            Log.log(e);
            CompletionError completionError = new CompletionError(e);
            this.error = completionError.getErrorMessage();
            //Make the error visible to the user!
            return new ICompletionProposal[] { completionError };
        }

        doCycle();
        return ConvertCompletionProposals.convertHandlesToProposals(proposals);
    }

    /**
     * Returns the python proposals as a list.
     * First parameter of tuple is a list and second is a Boolean object indicating whether the templates
     * should be also shown or not.
     * @param viewer
     * @throws CoreException
     * @throws BadLocationException
     * @throws MisconfigurationException
     * @throws IOException
     * @throws PythonNatureWithoutProjectException
     */
    private TokensOrProposalsList getPythonProposals(int documentOffset, IDocument doc,
            CompletionRequest request)
            throws CoreException, BadLocationException, IOException, MisconfigurationException,
            PythonNatureWithoutProjectException {
        //if non empty string, we're in imports section.
        String importsTipperStr = request.codeCompletion.getImportsTipperStr(request).importsTipperStr;

        if (importsTipperStr.length() != 0 || request.isInCalltip) {
            request.showTemplates = false; //don't show templates if we are in the imports section or inside a calltip.
        }

        TokensOrProposalsList allProposals = request.codeCompletion.getCodeCompletionProposals(request);
        return allProposals;
    }

    /**
     * Returns the template proposals as a list.
     */
    private TokensOrProposalsList getTemplateProposals(ITextViewer viewer, int documentOffset,
            String activationToken, java.lang.String qualifier) {
        List<ICompletionProposalHandle> propList = new ArrayList<ICompletionProposalHandle>();
        if (this.templatesCompletion != null) {
            this.templatesCompletion.addTemplateProposals(viewer, documentOffset, propList);
        }
        return new TokensOrProposalsList(propList);
    }

    /**
     * Ok, if we have
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
     */
    @Override
    public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
        //System.out.println("computeContextInformation");
        if (viewer.getDocument() != this.contextInformationValidator.doc) {
            return null;
        }
        //System.out.println("this.contextInformationValidator.returnedFalseOnce:"+this.contextInformationValidator.returnedFalseOnce);
        //if we didn't return false at least once, it is already installed.
        if (this.contextInformationValidator.returnedFalseOnce
                && this.contextInformationValidator.isContextInformationValid(documentOffset)) {
            return new IContextInformation[] { this.contextInformationValidator.fInformation };
        }
        return null;
    }

    /**
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
     */
    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        return getStaticCompletionProposalAutoActivationCharacters();
    }

    /**
     * Attribute that determines if the listener that'll clear the auto activation chars is already in place.
     */
    private volatile static boolean listenerToClearAutoActivationAlreadySetup = false;

    /**
     * @return the auto-activation chars that should be used.
     */
    public static char[] getStaticCompletionProposalAutoActivationCharacters() {
        if (!listenerToClearAutoActivationAlreadySetup) {
            //clears the cache when the preferences are changed.
            IPreferenceStore preferenceStore = PyDevUiPrefs.getPreferenceStore();
            preferenceStore.addPropertyChangeListener(new IPropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent event) {
                    activationChars = null; //clear the cache when it changes
                }

            });
            listenerToClearAutoActivationAlreadySetup = true;
        }

        if (activationChars == null) { //let's cache this

            if (!PyCodeCompletionPreferences.useAutocomplete()) {
                activationChars = new char[0];

            } else {
                char[] c = new char[0];
                if (PyCodeCompletionPreferences.isToAutocompleteOnDot()) {
                    c = StringUtils.addChar(c, '.');
                }
                if (PyCodeCompletionPreferences.isToAutocompleteOnPar()) {
                    c = StringUtils.addChar(c, '(');
                }
                activationChars = c;
            }
        }
        return activationChars;
    }

    /**
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
     */
    @Override
    public char[] getContextInformationAutoActivationCharacters() {
        return null;
    }

    /**
     * If completion fails for some reason, we could give it here...
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
     */
    @Override
    public java.lang.String getErrorMessage() {
        String ret = this.error;
        this.error = null;
        return ret;
    }

    /**
     *
     * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
     */
    @Override
    public IContextInformationValidator getContextInformationValidator() {
        return this.contextInformationValidator;
    }

}