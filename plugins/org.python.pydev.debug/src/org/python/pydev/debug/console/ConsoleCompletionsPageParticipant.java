/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.internal.console.IOConsolePage;
import org.eclipse.ui.internal.console.IOConsolePartition;
import org.eclipse.ui.part.IPageBookViewPage;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.model.XMLUtils;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.GetCompletionsCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;
import org.python.pydev.debug.newconsole.CurrentPyStackFrameForConsole;
import org.python.pydev.debug.newconsole.PydevConsoleCommunication;
import org.python.pydev.debug.newconsole.PydevConsoleCompletionProcessor;
import org.python.pydev.debug.newconsole.PydevConsoleInterpreter;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferencesPage;
import org.python.pydev.editor.codecompletion.PyContentAssistant;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_interactive_console.console.IScriptConsoleCommunication;
import org.python.pydev.shared_interactive_console.console.InterpreterResponse;
import org.python.pydev.shared_ui.bindings.KeyBindingHelper;

/**
 * Will provide code-completion in debug sessions.
 */
@SuppressWarnings("restriction")
public class ConsoleCompletionsPageParticipant implements IConsolePageParticipant {

    /**
     * Class to get the completions in debug mode in a suspended frame.
     */
    public static class GetCompletionsInDebug implements IScriptConsoleCommunication, ICommandResponseListener {

        private static final ICompletionProposal[] EMPTY_COMPLETION_PROPOSALS = new ICompletionProposal[0];
        private String actTok;
        private String text;
        private int offset;
        private volatile List<Object[]> receivedXmlCompletions;
        private CurrentPyStackFrameForConsole currentPyStackFrameForConsole;

        public GetCompletionsInDebug(CurrentPyStackFrameForConsole currentPyStackFrameForConsole) {
            this.currentPyStackFrameForConsole = currentPyStackFrameForConsole;
        }

        public String getDescription(String text) throws Exception {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean isConnected() {
            return this.currentPyStackFrameForConsole.getLastSelectedFrame() != null;
        }

        /**
         * Gets the completions at the passed offset.
         */
        public ICompletionProposal[] getCompletions(String text, String actTok, int offset, boolean showForTabCompletion)
                throws Exception {
            this.text = text;
            this.actTok = actTok;
            this.offset = offset;
            PyStackFrame stackFrame = currentPyStackFrameForConsole.getLastSelectedFrame();

            if (stackFrame != null) {
                AbstractDebugTarget target = (AbstractDebugTarget) stackFrame.getAdapter(IDebugTarget.class);
                if (target != null) {
                    GetCompletionsCommand cmd = new GetCompletionsCommand(target, actTok, stackFrame.getLocalsLocator()
                            .getPyDBLocation());
                    cmd.setCompletionListener(this);
                    target.postCommand(cmd);
                }
                return waitForCommand();
            }
            return EMPTY_COMPLETION_PROPOSALS;
        }

        /**
         * Keeps in a loop for 3 seconds or until the completions are found. If no completions are found in that time,
         * returns an empty array.
         */
        private ICompletionProposal[] waitForCommand() {
            int i = 300; //wait up to 3 seconds
            while (--i > 0 && receivedXmlCompletions == null) {
                try {
                    Thread.sleep(10); //10 millis
                } catch (InterruptedException e) {
                    //ignore
                }
            }

            List<Object[]> fromServer = receivedXmlCompletions;
            receivedXmlCompletions = null;
            if (fromServer == null) {
                Log.logInfo("Timeout for waiting for debug completions elapsed (3 seconds).");
                return EMPTY_COMPLETION_PROPOSALS;
            }
            List<ICompletionProposal> ret = new ArrayList<ICompletionProposal>(fromServer.size());
            PydevConsoleCommunication.convertConsoleCompletionsToICompletions(text, actTok, offset, fromServer, ret, false);
            return ret.toArray(new ICompletionProposal[0]);
        }

        public void execInterpreter(String command, ICallback<Object, InterpreterResponse> onResponseReceived) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void interrupt() {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void setOnContentsReceivedCallback(ICallback<Object, Tuple<String, String>> onContentsReceived) {
            throw new RuntimeException("Not implemented");

        }

        public void close() throws Exception {
            throw new RuntimeException("Not implemented");
        }

        /**
         * Received when the completions command receives a response (ICommandResponseListener)
         * Converts the xml to completions.
         */
        public void commandComplete(AbstractDebuggerCommand cmd) {
            GetCompletionsCommand compCmd = (GetCompletionsCommand) cmd;
            try {
                String response = compCmd.getResponse();
                List<Object[]> fromServer = XMLUtils.convertXMLcompletionsFromConsole(response);
                receivedXmlCompletions = fromServer;
            } catch (CoreException e) {
                receivedXmlCompletions = new ArrayList<>(0);
                Log.log(e);
            }

        }

        public void linkWithDebugSelection(boolean isLinkedWithDebug) {
            throw new RuntimeException("Not implemented");
        }

    }

    /**
     * The content assistant added to the console.
     */
    private PyContentAssistant contentAssist;

    public Object getAdapter(Class adapter) {

        return null;
    }

    /**
     * When a console page is initialized,
     */
    public void init(IPageBookViewPage page, final IConsole console) {
        if (!(console instanceof ProcessConsole)) {
            return;
        }
        ProcessConsole processConsole = (ProcessConsole) console;
        IProcess process = processConsole.getProcess();
        if (process == null) {
            return;
        }
        if (!PyCodeCompletionPreferencesPage.useCodeCompletion()
                || !PyCodeCompletionPreferencesPage.useCodeCompletionOnDebug()) {
            return;
        }
        String attribute = process.getAttribute(Constants.PYDEV_DEBUG_IPROCESS_ATTR);
        if (!Constants.PYDEV_DEBUG_IPROCESS_ATTR_TRUE.equals(attribute)) {
            //Only provide code-completion for pydev debug processes.
            return;
        }
        Control control = page.getControl();
        if (page instanceof IOConsolePage) {

            //Note that completions on "all letters and '_'" are already activated just by installing
            //the content assist, but the completions on the default keybinding is not, so, we have to
            //call it ourselves here.
            control.addKeyListener(new KeyListener() {
                public void keyPressed(KeyEvent e) {

                    if (KeyBindingHelper.matchesContentAssistKeybinding(e)) {
                        contentAssist.showPossibleCompletions();
                    }
                }

                public void keyReleased(KeyEvent e) {
                }
            });

            final CurrentPyStackFrameForConsole currentPyStackFrameForConsole = new CurrentPyStackFrameForConsole(
                    console);
            IOConsolePage consolePage = (IOConsolePage) page;
            TextConsoleViewer viewer = consolePage.getViewer();

            PydevConsoleInterpreter interpreter = new PydevConsoleInterpreter();
            interpreter.setLaunchAndRelatedInfo(process.getLaunch());
            interpreter.setConsoleCommunication(new GetCompletionsInDebug(currentPyStackFrameForConsole));

            contentAssist = new PyContentAssistant() {
                @Override
                public String showPossibleCompletions() {
                    //Only show completions if we're in a suspended console.
                    if (currentPyStackFrameForConsole.getLastSelectedFrame() == null) {
                        return null;
                    }
                    return super.showPossibleCompletions();
                };
            };
            contentAssist.setInformationControlCreator(PyContentAssistant.createInformationControlCreator(viewer));
            contentAssist.install(new ScriptConsoleViewerWrapper(viewer, interpreter.getInterpreterInfo()));

            IContentAssistProcessor processor = new PydevConsoleCompletionProcessor(interpreter, contentAssist);
            contentAssist.setContentAssistProcessor(processor, IOConsolePartition.INPUT_PARTITION_TYPE);
            contentAssist.setContentAssistProcessor(processor, IOConsolePartition.OUTPUT_PARTITION_TYPE);

            contentAssist.enableAutoActivation(true);
            contentAssist.enableAutoInsert(false);
            contentAssist.setAutoActivationDelay(PyCodeCompletionPreferencesPage.getAutocompleteDelay());
        }
    }

    public void dispose() {

    }

    public void activated() {

    }

    public void deactivated() {

    }

}
