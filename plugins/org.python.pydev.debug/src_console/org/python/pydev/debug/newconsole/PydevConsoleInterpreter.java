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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.python.pydev.ast.codecompletion.IPyDevCompletionParticipant2;
import org.python.pydev.ast.codecompletion.ProposalsComparator;
import org.python.pydev.ast.simpleassist.ISimpleAssistParticipant2;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;
import org.python.pydev.core.ICompletionRequest;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.IterTokenEntry;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.docutils.ImportsSelection;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQualifier;
import org.python.pydev.core.interactive_console.IScriptConsoleViewer;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.debug.model.PyDebugTarget;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.editor.codecompletion.IPyTemplateCompletionProcessor;
import org.python.pydev.editor.codecompletion.PyTemplateCompletionProcessor;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_interactive_console.console.IScriptConsoleCommunication;
import org.python.pydev.shared_interactive_console.console.IScriptConsoleInterpreter;
import org.python.pydev.shared_interactive_console.console.InterpreterResponse;
import org.python.pydev.shared_ui.content_assist.AbstractCompletionProcessorWithCycling;

/**
 * Default implementation for the console interpreter.
 *
 * Will ask things to the IScriptConsoleCommunication
 */
public class PydevConsoleInterpreter implements IScriptConsoleInterpreter {

    private IScriptConsoleCommunication consoleCommunication;

    private List<Runnable> closeRunnables = new ArrayList<Runnable>();

    private List<ISimpleAssistParticipant2> simpleParticipants;

    /**
     * Note: contains all the natures (if it was a single one selected, it's expanded to the nature + references + system nature).
     */
    private Set<IPythonNature> initialNatures = new HashSet<IPythonNature>();

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
    @Override
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

    @Override
    @SuppressWarnings("unchecked")
    public ICompletionProposalHandle[] getCompletions(IScriptConsoleViewer viewer, String commandLine, int position,
            int offset, int whatToShow) throws Exception {

        final String text = commandLine.substring(0, position);
        ActivationTokenAndQualifier tokenAndQual = PySelection.getActivationTokenAndQualifier(new Document(text),
                text.length(),
                true, false);
        String textForCompletionInConsole = PySelection
                .getTextForCompletionInConsole(new Document(text), text.length());

        if (PySelection.isCompletionForLiteralNumber(tokenAndQual.activationToken)) {
            // suppress completions that would be invalid
            return new ICompletionProposalHandle[0];
        }

        //Code-completion for imports
        ImportInfo importsTipper = ImportsSelection.getImportsTipperStr(text, false);
        Set<IPythonNature> natureAndRelatedNatures = getNatureAndRelatedNatures();
        if (importsTipper.importsTipperStr.length() != 0) {
            importsTipper.importsTipperStr = importsTipper.importsTipperStr.trim();
            Set<IToken> tokens = new TreeSet<IToken>();
            final boolean onlyGetDirectModules = true;

            //Check all the natures.
            for (final IPythonNature nature : natureAndRelatedNatures) {
                ICodeCompletionASTManager astManager = nature.getAstManager();
                TokensList importTokens = astManager.getCompletionsForImport(importsTipper, new ICompletionRequest() {

                    @Override
                    public IPythonNature getNature() {
                        return nature;
                    }

                    @Override
                    public File getEditorFile() {
                        return null;
                    }

                    @Override
                    public IModule getModule() throws MisconfigurationException {
                        return null;
                    }
                }, onlyGetDirectModules);

                //only get all modules for the 1st one we analyze (no need to get on the others)
                for (IterTokenEntry entry : importTokens) {
                    IToken iToken = entry.getToken();
                    tokens.add(iToken);
                }
            }

            int qlen = tokenAndQual.qualifier.length();
            List<ICompletionProposalHandle> ret = new ArrayList<ICompletionProposalHandle>(tokens.size());
            Iterator<IToken> it = tokens.iterator();
            for (int i = 0; i < tokens.size(); i++) {
                IToken t = it.next();
                int replacementOffset = offset - qlen;
                String representation = t.getRepresentation();
                if (representation.startsWith(tokenAndQual.qualifier)) {
                    ret.add(CompletionProposalFactory.get().createPyLinkedModeCompletionProposal(representation,
                            replacementOffset, qlen, representation
                                    .length(),
                            t, null, null, IPyCompletionProposal.PRIORITY_DEFAULT,
                            IPyCompletionProposal.ON_APPLY_DEFAULT, "", null));
                }
            }
            return ret.toArray(new ICompletionProposalHandle[ret.size()]);
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
        ArrayList<ICompletionProposalHandle> results = new ArrayList<ICompletionProposalHandle>();

        if (!showForTabCompletion) {
            for (ISimpleAssistParticipant2 participant : simpleParticipants) {
                results.addAll(participant.computeConsoleProposals(tokenAndQual.activationToken,
                        tokenAndQual.qualifier,
                        offset));
            }
        }

        ProposalsComparator proposalsComparator = new ProposalsComparator(tokenAndQual.qualifier, null);
        ArrayList<ICompletionProposalHandle> results2 = new ArrayList<ICompletionProposalHandle>();

        if (!showOnlyTemplates) {
            //shell completions
            if (consoleCommunication != null) {
                ICompletionProposalHandle[] consoleCompletions = consoleCommunication.getCompletions(text,
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
            IPyTemplateCompletionProcessor pyTemplateCompletionProcessor = new PyTemplateCompletionProcessor();
            pyTemplateCompletionProcessor.addTemplateProposals((ITextViewer) viewer, offset, results2);
        }

        Collections.sort(results2, proposalsComparator);

        ArrayList<ICompletionProposalHandle> results3 = new ArrayList<ICompletionProposalHandle>();
        if (!showOnlyTemplates) {
            //other participants
            List<Object> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_COMPLETION);
            for (Object participant : participants) {
                if (participant instanceof IPyDevCompletionParticipant2) {
                    IPyDevCompletionParticipant2 participant2 = (IPyDevCompletionParticipant2) participant;
                    results3.addAll(
                            participant2.computeConsoleCompletions(tokenAndQual, natureAndRelatedNatures, viewer,
                                    offset));
                }
            }
            Collections.sort(results3, proposalsComparator);
        }
        results.addAll(results2);
        results.addAll(results3);

        return results.toArray(new ICompletionProposalHandle[results.size()]);
    }

    /*
     * (non-Javadoc)
     * @see com.aptana.interactive_console.console.IScriptConsoleShell#getDescription(org.eclipse.jface.text.IDocument, int)
     */
    @Override
    public String getDescription(IDocument doc, int position) throws Exception {
        String actTok = PySelection.getTextForCompletionInConsole(doc, position);
        return consoleCommunication.getDescription(actTok);
    }

    /*
     * (non-Javadoc)
     * @see com.aptana.interactive_console.console.IScriptConsoleShell#close()
     */
    @Override
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
    @Override
    public void setConsoleCommunication(IScriptConsoleCommunication protocol) {
        this.consoleCommunication = protocol;
    }

    @Override
    public IScriptConsoleCommunication getConsoleCommunication() {
        return consoleCommunication;
    }

    public void addCloseOperation(Runnable runnable) {
        this.closeRunnables.add(runnable);
    }

    public void setNaturesUsed(List<IPythonNature> localNaturesUsed) {
        if (localNaturesUsed == null) {
            localNaturesUsed = new ArrayList<IPythonNature>();
        }
        this.initialNatures = new HashSet<>(localNaturesUsed);
    }

    private Set<IPythonNature> getNatureAndRelatedNatures() {
        Set<IPythonNature> ret = new HashSet<IPythonNature>();
        for (IPythonNature iPythonNature : this.initialNatures) {
            try {
                ICodeCompletionASTManager astManager = iPythonNature.getAstManager();
                if (astManager == null) {
                    continue;
                }
                IModulesManager modulesManager = astManager.getModulesManager();
                if (modulesManager == null) {
                    continue;
                }
                IModulesManager[] managersInvolved = modulesManager
                        .getManagersInvolved(true);
                for (IModulesManager iModulesManager : managersInvolved) {
                    ret.add(iModulesManager.getNature());
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
        return ret;
    }

    public void setInterpreterInfo(IInterpreterInfo interpreterInfo) {
        this.interpreterInfo = interpreterInfo;
    }

    @Override
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
    @Override
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
                    natures.add(nature);
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
