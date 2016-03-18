/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.customizations.common;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.runners.UniversalRunner;
import org.python.pydev.runners.UniversalRunner.AbstractRunner;
import org.python.pydev.shared_core.io.ThreadStreamReader;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * This is the window used to handle a process. Currently specific to google app engine (could be more customizable
 * if needed).
 */
public abstract class ProcessWindow extends Dialog {

    //Labels
    private static final String SEND_TO_PROMPT_LABEL = "Send to &prompt: ";
    private static final String SEND_LABEL = "&Send";

    private static final String EXECUTING_COMMAND_LABEL = "E&xecuting: ";
    private static final String COMMAND_TO_EXECUTE_LABEL = "Command to e&xecute: ";

    private static final String CLOSE_LABEL = "C&lose";
    private static final String CANCEL_LABEL = "&Cancel";
    private static final String RUN_LABEL = "&Run";

    //Input
    protected Text output;
    protected IContainer container;
    protected IPythonPathNature pythonPathNature;
    protected File appcfg;
    protected File appEngineLocation;

    //If not null, this command should be run when the interface is opened.
    protected String initialCommand;

    //lock and state
    private Object lock = new Object();

    private static final int STATE_NOT_RUNNING = 0;
    private static final int STATE_RUNNING = 1;
    private volatile int state = STATE_NOT_RUNNING;

    //only while running
    private ThreadStreamReader err;
    private ThreadStreamReader std;
    private OutputStream outputStream;
    private ProcessHandler processHandler;
    private Process process;

    //UI
    private Button cancelButton;
    private Button okButton;
    private Combo commandToExecute;
    private Text sendToText;

    private final int NUMBER_OF_COLUMNS = 6;
    private Label commandToExecuteLabel;

    /**
     * This thread is responsible for reading from the process and writing to it asynchronously.
     */
    class ProcessHandler extends Thread {

        /**
         * List of commands that still need to be sent to the process.
         */
        private List<String> commandTexts = new ArrayList<String>();

        /**
         * Lock for accessing commandTexts.
         */
        private Object commandTextsLock = new Object();

        /**
         * Whether we should force the process to quit
         */
        private boolean forceQuit = false;

        /**
         * A buffer with the contents found. We analyze that buffer to know if we should
         * change the command Text to have an echo char (when password is requested)
         */
        private FastStringBuffer buffer = new FastStringBuffer();

        /**
         * Keep here until process is finished (naturally or we finish it).
         */
        @Override
        public void run() {
            try {
                try {
                    while (process != null && forceQuit == false) {
                        boolean hasExited = true;
                        try {
                            process.exitValue();
                        } catch (IllegalThreadStateException e) {
                            //that's ok, still running!
                            hasExited = false;
                        }
                        try {
                            Thread.sleep(75);
                        } catch (InterruptedException e1) {
                            //ignore
                        }
                        try {
                            String errContents = err.getAndClearContents();
                            String stdContents = std.getAndClearContents();
                            append(errContents);
                            append(stdContents);

                            if (hasExited) {
                                process = null;
                            } else {

                                synchronized (commandTextsLock) {
                                    if (this.commandTexts.size() > 0) {
                                        String txt = this.commandTexts.remove(0);
                                        try {
                                            append("\n");
                                            outputStream.write(txt.getBytes());
                                            outputStream.flush();
                                        } catch (IOException e) {
                                            Log.log(e);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            append(e.getMessage());
                            Log.log(e);
                            break;
                        }
                    }
                } finally {

                    //Gotten out of the loop.
                    if (process != null) {
                        append("Forcing the process to quit.\n");
                        try {
                            process.destroy();
                        } catch (Exception e) {
                        }
                        process = null;
                    }
                    //liberate all.
                    err = null;
                    std = null;
                    outputStream = null;
                    processHandler = null;

                    append("FINISHED\n\n");
                }
            } finally {
                onEndRun();
            }
        }

        /**
         * This function is called synchronously, but adds the contents to the output window the user
         * is seeing asynchronously.
         * 
         * It also sets the echo char by analyzing the available contents.
         * 
         * Nothing is done if the contents is an empty string.
         */
        private void append(final String contents) {
            if (contents == null || contents.length() == 0) {
                return;
            }
            buffer.append(contents);
            if (buffer.length() > 2000) {
                //Let it always close to 2000.
                try {
                    buffer.delete(0, 2000 - buffer.length());
                } catch (Exception e) {
                }
            }
            final List<String> split = StringUtils.splitInLines(buffer.toString());

            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    if (split.size() > 0) {
                        String last = split.get(split.size() - 1);
                        if (last.toLowerCase().indexOf("password for") != -1) {
                            ProcessWindow.this.sendToText.setEchoChar('*');
                        } else {
                            ProcessWindow.this.sendToText.setEchoChar('\0');
                        }
                    }

                    output.append(contents);
                }
            });
        }

        /**
         * Adds a command to be executed (asynchronously)
         */
        public void addCommandText(String text) {
            synchronized (commandTextsLock) {
                this.commandTexts.add(text);
            }
        }
    }

    /**
     * We need to set the shell style to be resizable.
     */
    public ProcessWindow(Shell parentShell) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected void configureShell(final Shell shell) {
        super.configureShell(shell);
        shell.setText("Manage Google App Engine");
    }

    /**
     * Create the dialog contents
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite top = (Composite) super.createDialogArea(parent);

        Composite composite = new Composite(top, SWT.None);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        composite.setLayout(new GridLayout(NUMBER_OF_COLUMNS, false));

        createLabel(composite, "Arguments to pass to: " + appcfg.getAbsolutePath());
        createLabel(composite, "The command line can be changed as needed.");

        Link link = new Link(composite, SWT.None);
        link.setText("See <a>http://code.google.com/appengine/docs/python/tools/uploadinganapp.html</a>");
        link.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                Program.launch("http://code.google.com/appengine/docs/python/tools/uploadinganapp.html");
            }
        });
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = NUMBER_OF_COLUMNS;
        link.setLayoutData(gridData);

        //--- Command to execute
        commandToExecuteLabel = createLabel(composite, COMMAND_TO_EXECUTE_LABEL, 1);
        commandToExecute = new Combo(composite, SWT.SINGLE | SWT.BORDER);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = NUMBER_OF_COLUMNS - 2; //1 from the label and 1 from the button
        gridData.grabExcessHorizontalSpace = true;
        commandToExecute.setLayoutData(gridData);
        String[] availableCommands = getAvailableCommands();
        commandToExecute.setItems(availableCommands);
        commandToExecute.setText(availableCommands[0]);

        okButton = createButton(composite, RUN_LABEL, 1, SWT.PUSH);
        okButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                buttonPressed(IDialogConstants.OK_ID);
            }
        });
        okButton.setData(IDialogConstants.OK_ID);

        //--- main output
        output = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = NUMBER_OF_COLUMNS;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        output.setLayoutData(gridData);

        //--- Send any command to the shell
        createLabel(composite, SEND_TO_PROMPT_LABEL, 1);
        sendToText = createText(composite, NUMBER_OF_COLUMNS - 2); //1 from the label and 1 from the button
        sendToText.addKeyListener(new KeyListener() {

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.character == '\r' || e.character == '\n') {
                    addCurrentCommand();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }
        });
        Button button = createButton(composite, SEND_LABEL, 1, SWT.PUSH);
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addCurrentCommand();
            }
        });

        return top;
    }

    /**
     * Subclasses should override to provide the commands available to be executed.
     */
    protected abstract String[] getAvailableCommands();

    private Label createLabel(Composite composite, String message) {
        return createLabel(composite, message, NUMBER_OF_COLUMNS);
    }

    private Label createLabel(Composite composite, String message, int horizontalSpan) {
        Label label = new Label(composite, SWT.None);
        label.setText(message);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = horizontalSpan;
        label.setLayoutData(gridData);
        return label;
    }

    @Override
    protected void constrainShellSize() {
        getShell().setSize(640, 480);
        super.constrainShellSize();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        cancelButton = createButton(parent, IDialogConstants.CANCEL_ID, CLOSE_LABEL, false);
    }

    /**
     * Overridden to execute the initial command if we had one set.
     */
    @Override
    public void create() {
        super.create();
        //After creating things, execute the initial command if it was set.
        this.okButton.setFocus();
        if (this.initialCommand != null) {
            commandToExecute.setText(this.initialCommand);
            this.okPressed();
        }
    }

    /**
     * The Ok is used for Run/Cancel.
     */
    @Override
    protected void okPressed() {
        boolean doRun = false;

        synchronized (lock) {
            if (state == STATE_NOT_RUNNING) {
                state = STATE_RUNNING;
                doRun = true;
            }
        }

        if (doRun) {
            run();

            commandToExecuteLabel.setText(EXECUTING_COMMAND_LABEL);
            commandToExecute.setEnabled(false);
            cancelButton.setEnabled(false);
            okButton.setText(CANCEL_LABEL);
        } else {
            //We're running... this means it meant a cancel.
            cancelRun();
        }
    }

    /**
     * Requests the process to be canceled (when it's possible to do so). onEndRun() is called after
     * it's successfully canceled.
     */
    private void cancelRun() {
        //Running: cancel it.
        ProcessHandler handler = this.processHandler;
        if (handler != null) {
            handler.forceQuit = true;
        }
    }

    private void onEndRun() {
        synchronized (lock) {
            state = STATE_NOT_RUNNING;
        }

        Display.getDefault().asyncExec(new Runnable() {

            @Override
            public void run() {
                commandToExecuteLabel.setText(COMMAND_TO_EXECUTE_LABEL);
                commandToExecute.setEnabled(true);
                cancelButton.setEnabled(true);
                okButton.setText(RUN_LABEL);
            }
        });
    }

    @Override
    public boolean close() {
        if (state == STATE_NOT_RUNNING) {
            return super.close();
        } else {
            cancelRun();
            return false;
        }
    }

    @Override
    protected void cancelPressed() {
        if (state == STATE_NOT_RUNNING) {
            super.cancelPressed();
        } else {
            cancelRun();
        }
    }

    public void setParameters(IContainer container, IPythonPathNature pythonPathNature, File appcfg,
            File appEngineLocation) {
        this.container = container;
        this.pythonPathNature = pythonPathNature;
        this.appcfg = appcfg;
        this.appEngineLocation = appEngineLocation;
    }

    private void run() {
        if (processHandler != null) {
            return; //Still running.
        }
        try {
            String cmdLineArguments = commandToExecute.getText().trim();
            String[] arguments = new String[0];
            if (cmdLineArguments.length() > 0) {
                arguments = PythonRunnerConfig.parseStringIntoList(cmdLineArguments);
            }

            AbstractRunner universalRunner = UniversalRunner.getRunner(pythonPathNature.getNature());
            Tuple<Process, String> run = universalRunner.createProcess(appcfg.getAbsolutePath(), arguments,
                    appEngineLocation, new NullProgressMonitor());

            process = run.o1;
            if (process != null) {

                std = new ThreadStreamReader(process.getInputStream());
                err = new ThreadStreamReader(process.getErrorStream());

                std.start();
                err.start();

                outputStream = process.getOutputStream();

                processHandler = new ProcessHandler();
                processHandler.start();
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    private Button createButton(Composite composite, String label, int colSpan, int style) {
        Button button = new Button(composite, style);
        button.setText(label);
        setButtonLayout(button, colSpan);
        return button;
    }

    private void setButtonLayout(Button button, int colSpan) {
        GridData gridData;
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = colSpan;
        gridData.grabExcessHorizontalSpace = true;
        button.setLayoutData(gridData);
    }

    private Text createText(Composite composite, int colSpan) {
        Text text = new Text(composite, SWT.SINGLE | SWT.BORDER);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = colSpan;
        text.setLayoutData(gridData);
        return text;
    }

    private void addCurrentCommand() {
        ProcessHandler p = processHandler;
        if (p != null) {
            String text = sendToText.getText();
            sendToText.setText("");
            p.addCommandText(text + "\n");
        }
    }

    public void setInitialCommandToRun(String initialCommand) {
        this.initialCommand = initialCommand;
    }

}
