/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.console;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.debug.ui.DebugUITools;
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
import org.python.pydev.bindingutils.KeyBindingHelper;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyDebugTarget;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.model.XMLUtils;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.GetCompletionsCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;
import org.python.pydev.debug.newconsole.PydevConsoleCommunication;
import org.python.pydev.debug.newconsole.PydevConsoleCompletionProcessor;
import org.python.pydev.debug.newconsole.PydevConsoleInterpreter;
import org.python.pydev.dltk.console.IScriptConsoleCommunication;
import org.python.pydev.dltk.console.InterpreterResponse;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferencesPage;
import org.python.pydev.editor.codecompletion.PyContentAssistant;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * Will provide code-completion in debug sessions.
 */
@SuppressWarnings("restriction")
public class ConsoleCompletionsPageParticipant implements IConsolePageParticipant {

    /**
     * @return the currently selected / suspended frame. If the console is passed, it will only return
     * a frame that matches the passed console. If no selected / suspended frame is found or the console
     * doesn't match, null is returned.
     */
    protected static PyStackFrame getCurrentSuspendedPyStackFrame(IConsole console) {
        IAdaptable context = DebugUITools.getDebugContext();

        if (context instanceof PyStackFrame) {
            PyStackFrame stackFrame = (PyStackFrame) context;
            if (!stackFrame.isTerminated() && stackFrame.isSuspended()) {
                if (console != null) {
                    //If a console is passed, we must check if it matches the console from the selected frame.
                    AbstractDebugTarget target = (AbstractDebugTarget) stackFrame.getAdapter(IDebugTarget.class);
                    if (DebugUITools.getConsole(target.getProcess()) != console) {
                        return null;
                    }
                }

                return stackFrame;
            }
        }
        return null;
    }

    /**
     * Class to get the completions in debug mode in a suspended frame.
     */
    public static class GetCompletionsInDebug implements IScriptConsoleCommunication, ICommandResponseListener {

        private static final ICompletionProposal[] EMPTY_COMPLETION_PROPOSALS = new ICompletionProposal[0];
        private ICompletionProposal[] receivedCompletions;
        private String actTok;
        private String text;
        private int offset;

        public String getDescription(String text) throws Exception {
            throw new RuntimeException("Not implemented");
        }

        /**
         * Gets the completions at the passed offset.
         */
        public ICompletionProposal[] getCompletions(String text, String actTok, int offset) throws Exception {
            this.text = text;
            this.actTok = actTok;
            this.offset = offset;
            PyStackFrame stackFrame = getCurrentSuspendedPyStackFrame(null);

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
            while (--i > 0 && receivedCompletions == null) {
                try {
                    Thread.sleep(10); //10 millis
                } catch (InterruptedException e) {
                    //ignore
                }
            }

            ICompletionProposal[] temp = receivedCompletions;
            receivedCompletions = null;
            if (temp == null) {
                Log.logInfo("Timeout for waiting for debug completions elapsed (3 seconds).");
                return EMPTY_COMPLETION_PROPOSALS;
            }
            return temp;
        }

        public void execInterpreter(String command, ICallback<Object, InterpreterResponse> onResponseReceived,
                ICallback<Object, Tuple<String, String>> onContentsReceived) {
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
                List<ICompletionProposal> ret = new ArrayList<ICompletionProposal>();
                PydevConsoleCommunication.convertToICompletions(text, actTok, offset, fromServer, ret);
                receivedCompletions = ret.toArray(new ICompletionProposal[ret.size()]);
            } catch (CoreException e) {
                receivedCompletions = EMPTY_COMPLETION_PROPOSALS;
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

            IOConsolePage consolePage = (IOConsolePage) page;
            TextConsoleViewer viewer = consolePage.getViewer();

            contentAssist = new PyContentAssistant() {
                public String showPossibleCompletions() {
                    //Only show completions if we're in a suspended console.
                    if (getCurrentSuspendedPyStackFrame(console) == null) {
                        return null;
                    }
                    return super.showPossibleCompletions();
                };
            };
            contentAssist.setInformationControlCreator(PyContentAssistant.createInformationControlCreator(viewer));
            ILaunch launch = process.getLaunch();
            IDebugTarget debugTarget = launch.getDebugTarget();
            IInterpreterInfo projectInterpreter = null;
            if (debugTarget instanceof PyDebugTarget) {
                PyDebugTarget pyDebugTarget = (PyDebugTarget) debugTarget;
                PythonNature nature = PythonNature.getPythonNature(pyDebugTarget.project);
                if (nature != null) {
                    try {
                        projectInterpreter = nature.getProjectInterpreter();
                    } catch (Throwable e1) {
                        Log.log(e1);
                    }
                }

            }
            contentAssist.install(new ScriptConsoleViewerWrapper(viewer, projectInterpreter));

            PydevConsoleInterpreter interpreter = new PydevConsoleInterpreter();
            interpreter.setConsoleCommunication(new GetCompletionsInDebug());

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
