/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;
import org.python.pydev.core.ICompletionRequest;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.ImportsSelection;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQual;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.PyDebugTarget;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.editor.codecompletion.IPyCodeCompletion;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant2;
import org.python.pydev.editor.codecompletion.PyLinkedModeCompletionProposal;
import org.python.pydev.editor.codecompletion.templates.PyTemplateCompletionProcessor;
import org.python.pydev.editor.simpleassist.ISimpleAssistParticipant2;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_interactive_console.console.IScriptConsoleCommunication;
import org.python.pydev.shared_interactive_console.console.IScriptConsoleInterpreter;
import org.python.pydev.shared_interactive_console.console.InterpreterResponse;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleViewer;
import org.python.pydev.shared_ui.content_assist.AbstractCompletionProcessorWithCycling;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;
import org.python.pydev.shared_ui.proposals.PyCompletionProposal;

/**
 * Default implementation for the console interpreter.
 *
 * Will ask things to the IScriptConsoleCommunication
 */
public class PydevConsoleInterpreter implements IScriptConsoleInterpreter {

    private IScriptConsoleCommunication consoleCommunication;

    private List<Runnable> closeRunnables = new ArrayList<Runnable>();

    private List<ISimpleAssistParticipant2> simpleParticipants;

    private List<IPythonNature> naturesUsed = new ArrayList<IPythonNature>();

    private IInterpreterInfo interpreterInfo;

    private PyStackFrame frame;

    private ILaunch launch;

    private Process process;

    @SuppressWarnings("unchecked")
    public PydevConsoleInterpreter() {
        List<Object> p = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_SIMPLE_ASSIST);
        ArrayList<ISimpleAssistParticipant2> list = new ArrayList<ISimpleAssistParticipant2>();
        for (Object o : p) {
            if (o instanceof ISimpleAssistParticipant2) {
                list.add((ISimpleAssistParticipant2) o);
            }
        }
        this.simpleParticipants = list;
    }

    @Override
    public void setOnContentsReceivedCallback(ICallback<Object, Tuple<String, String>> onContentsReceived) {
        consoleCommunication.setOnContentsReceivedCallback(onContentsReceived);
    }

    /*
     * (non-Javadoc)
     * @see com.aptana.interactive_console.console.IScriptConsoleInterpreter#exec(java.lang.String)
     */
    public void exec(String command, final ICallback<Object, InterpreterResponse> onResponseReceived) {
        consoleCommunication.execInterpreter(command, onResponseReceived);
    }

    @Override
    public void interrupt() {
        consoleCommunication.interrupt();
    }

    /**
     * Set frame context for the new pydev console interpreter
     *
     * @param frame
     */
    public void setFrame(PyStackFrame frame) throws Exception {
        this.frame = frame;
    }

    @SuppressWarnings("unchecked")
    public ICompletionProposal[] getCompletions(IScriptConsoleViewer viewer, String commandLine, int position,
            int offset, int whatToShow) throws Exception {

        final String text = commandLine.substring(0, position);
        ActivationTokenAndQual tokenAndQual = PySelection.getActivationTokenAndQual(new Document(text), text.length(),
                true, false);
        String textForCompletionInConsole = PySelection
                .getTextForCompletionInConsole(new Document(text), text.length());

        //Code-completion for imports
        ImportInfo importsTipper = ImportsSelection.getImportsTipperStr(text, false);
        if (importsTipper.importsTipperStr.length() != 0) {
            importsTipper.importsTipperStr = importsTipper.importsTipperStr.trim();
            Set<IToken> tokens = new TreeSet<IToken>();
            boolean onlyGetDirectModules = false;

            //Check all the natures.
            for (final IPythonNature nature : naturesUsed) {
                ICodeCompletionASTManager astManager = nature.getAstManager();
                IToken[] importTokens = astManager.getCompletionsForImport(importsTipper, new ICompletionRequest() {

                    public IPythonNature getNature() {
                        return nature;
                    }

                    public File getEditorFile() {
                        return null;
                    }

                    public IModule getModule() throws MisconfigurationException {
                        return null;
                    }
                }, onlyGetDirectModules);

                //only get all modules for the 1st one we analyze (no need to get on the others)
                onlyGetDirectModules = true;
                tokens.addAll(Arrays.asList(importTokens));
            }

            int qlen = tokenAndQual.qualifier.length();
            List<ICompletionProposal> ret = new ArrayList<ICompletionProposal>(tokens.size());
            Iterator<IToken> it = tokens.iterator();
            for (int i = 0; i < tokens.size(); i++) {
                IToken t = it.next();
                int replacementOffset = offset - qlen;
                String representation = t.getRepresentation();
                if (representation.startsWith(tokenAndQual.qualifier)) {
                    ret.add(new PyLinkedModeCompletionProposal(representation, replacementOffset, qlen, representation
                            .length(), t, null, null, IPyCompletionProposal.PRIORITY_DEFAULT,
                            PyCompletionProposal.ON_APPLY_DEFAULT, ""));
                }
            }
            return ret.toArray(new ICompletionProposal[ret.size()]);
        }
        //END Code-completion for imports

        String actTok = tokenAndQual.activationToken;
        if (tokenAndQual.qualifier != null && tokenAndQual.qualifier.length() > 0) {
            if (actTok.length() > 0 && actTok.charAt(actTok.length() - 1) != '.') {
                actTok += '.';
            }
            actTok += tokenAndQual.qualifier;
        }

        boolean showOnlyTemplates = whatToShow == AbstractCompletionProcessorWithCycling.SHOW_ONLY_TEMPLATES;
        boolean showForTabCompletion = whatToShow == AbstractCompletionProcessorWithCycling.SHOW_FOR_TAB_COMPLETIONS;

        //simple completions (clients)
        ArrayList<ICompletionProposal> results = new ArrayList<ICompletionProposal>();

        if (!showForTabCompletion) {
            for (ISimpleAssistParticipant2 participant : simpleParticipants) {
                results.addAll(participant.computeConsoleProposals(tokenAndQual.activationToken,
                        tokenAndQual.qualifier,
                        offset));
            }
        }

        ArrayList<ICompletionProposal> results2 = new ArrayList<ICompletionProposal>();

        if (!showOnlyTemplates) {
            //shell completions
            if (consoleCommunication != null) {
                ICompletionProposal[] consoleCompletions = consoleCommunication.getCompletions(text,
                        textForCompletionInConsole, offset,
                        showForTabCompletion);
                // If we're only showing ipython completions, then short-circuit the rest
                if (showForTabCompletion) {
                    return consoleCompletions;
                }
                results2.addAll(Arrays.asList(consoleCompletions));
            }
        }

        if (tokenAndQual.activationToken.length() == 0) {
            //templates (only if we have no activation token)
            PyTemplateCompletionProcessor pyTemplateCompletionProcessor = new PyTemplateCompletionProcessor();
            pyTemplateCompletionProcessor.addTemplateProposals(viewer, offset, results2);
        }

        Collections.sort(results2, IPyCodeCompletion.PROPOSAL_COMPARATOR);

        ArrayList<ICompletionProposal> results3 = new ArrayList<ICompletionProposal>();
        if (!showOnlyTemplates) {
            //other participants
            List<Object> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_COMPLETION);
            for (Object participant : participants) {
                if (participant instanceof IPyDevCompletionParticipant2) {
                    IPyDevCompletionParticipant2 participant2 = (IPyDevCompletionParticipant2) participant;
                    results3.addAll(participant2.computeConsoleCompletions(tokenAndQual, this.naturesUsed, viewer,
                            offset));
                }
            }
            Collections.sort(results3, IPyCodeCompletion.PROPOSAL_COMPARATOR);
        }
        results.addAll(results2);
        results.addAll(results3);

        return results.toArray(new ICompletionProposal[results.size()]);
    }

    /*
     * (non-Javadoc)
     * @see com.aptana.interactive_console.console.IScriptConsoleShell#getDescription(org.eclipse.jface.text.IDocument, int)
     */
    public String getDescription(IDocument doc, int position) throws Exception {
        String actTok = PySelection.getTextForCompletionInConsole(doc, position);
        return consoleCommunication.getDescription(actTok);
    }

    /*
     * (non-Javadoc)
     * @see com.aptana.interactive_console.console.IScriptConsoleShell#close()
     */
    public void close() {
        if (consoleCommunication != null) {
            try {
                consoleCommunication.close();
            } catch (Exception e) {
                //ignore
            }
            consoleCommunication = null;
        }
        // run all close runnables.
        for (Runnable r : this.closeRunnables) {
            r.run();
        }

        //we can close just once!
        this.closeRunnables = null;
    }

    /*
     * (non-Javadoc)
     * @see com.aptana.interactive_console.console.IConsoleRequest#setConsoleCommunication(com.aptana.interactive_console.console.IScriptConsoleCommunication)
     */
    public void setConsoleCommunication(IScriptConsoleCommunication protocol) {
        this.consoleCommunication = protocol;
    }

    public IScriptConsoleCommunication getConsoleCommunication() {
        return consoleCommunication;
    }

    public void addCloseOperation(Runnable runnable) {
        this.closeRunnables.add(runnable);
    }

    public void setNaturesUsed(List<IPythonNature> naturesUsed) {
        if (naturesUsed == null) {
            naturesUsed = new ArrayList<IPythonNature>();
        }
        this.naturesUsed = naturesUsed;
    }

    public void setInterpreterInfo(IInterpreterInfo interpreterInfo) {
        this.interpreterInfo = interpreterInfo;
    }

    public IInterpreterInfo getInterpreterInfo() {
        return this.interpreterInfo;
    }

    public void setLaunch(ILaunch launch) {
        this.launch = launch;
    }

    public ILaunch getLaunch() {
        return launch;
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public Process getProcess() {
        return process;
    }

    public PyStackFrame getFrame() {
        return frame;
    }

    /**
     * Enable/Disable linking of the debug console with the suspended frame.
     */
    public void linkWithDebugSelection(boolean isLinkedWithDebug) {
        this.consoleCommunication.linkWithDebugSelection(isLinkedWithDebug);
    }

    public void setLaunchAndRelatedInfo(ILaunch launch) {
        this.setLaunch(launch);
        if (launch != null) {
            IDebugTarget debugTarget = launch.getDebugTarget();
            IInterpreterInfo projectInterpreter = null;
            if (debugTarget instanceof PyDebugTarget) {
                PyDebugTarget pyDebugTarget = (PyDebugTarget) debugTarget;
                PythonNature nature = PythonNature.getPythonNature(pyDebugTarget.project);
                if (nature != null) {
                    ArrayList<IPythonNature> natures = new ArrayList<>(1);
                    this.setNaturesUsed(natures);
                    try {
                        projectInterpreter = nature.getProjectInterpreter();
                        this.setInterpreterInfo(projectInterpreter);
                    } catch (Throwable e1) {
                        Log.log(e1);
                    }
                }
            }
        }

    }

}
