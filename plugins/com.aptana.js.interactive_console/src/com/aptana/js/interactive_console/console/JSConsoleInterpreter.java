/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.js.interactive_console.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.ActivationTokenAndQual;
import org.python.pydev.editor.codecompletion.IPyCodeCompletion;

import com.aptana.interactive_console.console.IScriptConsoleCommunication;
import com.aptana.interactive_console.console.IScriptConsoleInterpreter;
import com.aptana.interactive_console.console.InterpreterResponse;
import com.aptana.interactive_console.console.ui.IScriptConsoleViewer;
import com.aptana.shared_core.callbacks.ICallback;
import com.aptana.shared_core.structure.Tuple;

/**
 * Default implementation for the console interpreter. 
 * 
 * Will ask things to the IScriptConsoleCommunication
 */
public class JSConsoleInterpreter implements IScriptConsoleInterpreter {

    private IScriptConsoleCommunication consoleCommunication;

    private List<Runnable> closeRunnables = new ArrayList<Runnable>();

    private ILaunch launch;

    private Process process;

    public JSConsoleInterpreter() {
    }

    public Object getInterpreterInfo() {
        return null;
    }

    /*
     * (non-Javadoc)
     * @see com.aptana.interactive_console.console.IScriptConsoleInterpreter#exec(java.lang.String)
     */
    public void exec(String command, final ICallback<Object, InterpreterResponse> onResponseReceived,
            final ICallback<Object, Tuple<String, String>> onContentsReceived) {
        consoleCommunication.execInterpreter(command, onResponseReceived, onContentsReceived);
    }

    public ICompletionProposal[] getCompletions(IScriptConsoleViewer viewer, String commandLine, int position,
            int offset, int whatToShow) throws Exception {

        final String text = commandLine.substring(0, position);
        ActivationTokenAndQual tokenAndQual = PySelection.getActivationTokenAndQual(new Document(text), text.length(),
                true, false);

        String actTok = tokenAndQual.activationToken;
        if (tokenAndQual.qualifier != null && tokenAndQual.qualifier.length() > 0) {
            if (actTok.length() > 0 && actTok.charAt(actTok.length() - 1) != '.') {
                actTok += '.';
            }
            actTok += tokenAndQual.qualifier;
        }

        ArrayList<ICompletionProposal> results = new ArrayList<ICompletionProposal>();

        //shell completions 
        if (consoleCommunication != null) {
            ICompletionProposal[] consoleCompletions = consoleCommunication.getCompletions(text, actTok, offset);
            results.addAll(Arrays.asList(consoleCompletions));
        }

        Collections.sort(results, IPyCodeCompletion.PROPOSAL_COMPARATOR);

        return results.toArray(new ICompletionProposal[results.size()]);
    }

    /*
     * (non-Javadoc)
     * @see com.aptana.interactive_console.console.IScriptConsoleShell#getDescription(org.eclipse.jface.text.IDocument, int)
     */
    public String getDescription(IDocument doc, int position) throws Exception {
        ActivationTokenAndQual tokenAndQual = PySelection.getActivationTokenAndQual(doc, position, true, false);
        String actTok = tokenAndQual.activationToken;
        if (tokenAndQual.qualifier != null && tokenAndQual.qualifier.length() > 0) {
            if (actTok.length() > 0 && actTok.charAt(actTok.length() - 1) != '.') {
                actTok += '.';
            }
            actTok += tokenAndQual.qualifier;
        }
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

    /**
     * Enable/Disable linking of the debug console with the suspended frame.
     */
    public void linkWithDebugSelection(boolean isLinkedWithDebug) {
        this.consoleCommunication.linkWithDebugSelection(isLinkedWithDebug);
    }

}
