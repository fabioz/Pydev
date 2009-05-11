package org.python.pydev.customizations.app_engine.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
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
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.runners.ThreadStreamReader;

/**
 * This is the window used to handle a process. Currently specific to google app engine (could be more customizable
 * if needed).
 */
public class ProcessWindow extends Dialog{
    
    //Input
    private Text output;
    private IContainer container;
    private IPythonPathNature pythonPathNature;
    private File appcfg;
    private File appEngineLocation;
    
    
    //lock and state
    private Object lock = new Object();
    
    private static final int STATE_NOT_RUNNING = 0;
    private static final int STATE_RUNNING = 1;
    private int state;
    
    
    //only while running
    private ThreadStreamReader err;
    private ThreadStreamReader std;
    private OutputStream outputStream;
    private ProcessHandler processHandler;
    private Process process;
    
    
    //UI
    private Button cancelButton;
    private Button okButton;
    private Combo commandLineArguments;
    private Text commandText;


    /**
     * This thread is responsible for reading from the process and writing to it asynchronously.
     */
    class ProcessHandler extends Thread{
        
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
         * Keep here until process is finished (natually or we finish it).
         */
        public void run(){
            try{
                try{
                    while(process != null && forceQuit == false){
                        boolean hasExited = true;
                        try{
                            process.exitValue();
                        }catch(IllegalThreadStateException e){
                            //that's ok, still running!
                            hasExited = false;
                        }
                        try{
                            Thread.sleep(75);
                        }catch(InterruptedException e1){
                            //ignore
                        }
                        try{
                            String errContents = err.getAndClearContents();
                            String stdContents = std.getAndClearContents();
                            append(errContents);
                            append(stdContents);
                            
                            
                            if(hasExited){
                                process = null;
                            }else{
                                
                                synchronized(commandTextsLock){
                                    if(this.commandTexts.size() > 0){
                                        String txt = this.commandTexts.remove(0);
                                        try{
                                            append("\n");
                                            outputStream.write(txt.getBytes());
                                            outputStream.flush();
                                        }catch(IOException e){
                                            Log.log(e);
                                        }
                                    }
                                }
                            }
                        }catch(Exception e){
                            append(e.getMessage());
                            Log.log(e);
                            break;
                        }
                    }
                }finally{
                    
                    //Gotten out of the loop.
                    if(process != null){
                        append("Forcing the process to quit.\n");
                        try{
                            process.destroy();
                        }catch(Exception e){
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
            }finally{
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
        private void append(final String contents){
            if(contents.length() == 0){
                return;
            }
            buffer.append(contents);
            if(buffer.length() > 2000){
                //Let it always close to 2000.
                buffer.delete(0, 2000-buffer.length());
            }
            final List<String> split = StringUtils.splitInLines(buffer.toString());
            
            Display.getDefault().asyncExec(new Runnable(){
            
                public void run(){
                    if(split.size() > 0){
                        String last = split.get(split.size()-1);
                        if(last.toLowerCase().indexOf("password for") != -1){
                            ProcessWindow.this.commandText.setEchoChar('*');
                        }else{
                            ProcessWindow.this.commandText.setEchoChar('\0');
                        }
                    }
                    
                    output.append(contents);
                }
            });
        }

        /**
         * Adds a command to be executed (asynchronously)
         */
        public void addCommandText(String text){
            synchronized(commandTextsLock){
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
    

    /**
     * Create the dialog contents
     */
    @Override
    protected Control createDialogArea(Composite parent){
        Composite top = (Composite) super.createDialogArea(parent);

        Composite composite = new Composite(top, SWT.None);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        composite.setLayout(new GridLayout(3, false));

        createLabel(composite, "Arguments to pass to: "+appcfg.getAbsolutePath());
        createLabel(composite, "The command line can be changed as needed.");

        Link link = new Link(composite, SWT.None);
        link.setText("See <a>http://code.google.com/appengine/docs/python/tools/uploadinganapp.html</a>");
        link.addSelectionListener(new SelectionListener(){

            public void widgetDefaultSelected(SelectionEvent e) {
            }

            public void widgetSelected(SelectionEvent e) {
                Program.launch("http://code.google.com/appengine/docs/python/tools/uploadinganapp.html");
            }}
        );
        
        
        commandLineArguments = new Combo(composite, SWT.SINGLE | SWT.BORDER);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        commandLineArguments.setLayoutData(gridData);
        commandLineArguments.setItems(new String[]{
                "update --secure "+container.getLocation().toOSString(),
                "rollback --secure "+container.getLocation().toOSString(),
                "update_indexes --secure "+container.getLocation().toOSString(),
                "vacuum_indexes --secure "+container.getLocation().toOSString(),
                "request_logs --secure "+container.getLocation().toOSString()+ " my_output_file.log",
        });

        commandLineArguments.setText("update --secure "+container.getLocation().toOSString());
        
        
        //--- Send any command to the shell
        commandText = createText(composite, 2);
        Button button = createButton(composite, "Enter command", 1, SWT.PUSH);
        button.addSelectionListener(new SelectionListener(){
        
            public void widgetSelected(SelectionEvent e){
                addCurrentCommand();
            }
        
            public void widgetDefaultSelected(SelectionEvent e){
            }
        });
        commandText.addKeyListener(new KeyListener(){
            
            public void keyReleased(KeyEvent e){
                if(e.character == '\r' || e.character == '\n'){
                    addCurrentCommand();
                }
            }
        
            public void keyPressed(KeyEvent e){
            }
        });

        //--- main output
        output = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
        gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = 3;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        output.setLayoutData(gridData);

        return top;
    }

    private void createLabel(Composite composite, String message){
        Label label = new Label(composite, SWT.None);
        label.setText(message);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = 3;
        label.setLayoutData(gridData);
    }
    
    
    @Override
    protected void constrainShellSize(){
        getShell().setSize(640, 480);
        super.constrainShellSize();
    }

    protected void createButtonsForButtonBar(Composite parent){
        okButton = createButton(parent, IDialogConstants.OK_ID, "&Run", true);
        cancelButton = createButton(parent, IDialogConstants.CANCEL_ID, "&Close", false);
    }

    @Override
    protected void okPressed(){
        if(state == STATE_NOT_RUNNING){
            onStartRun();
            run();
            
            okButton.setEnabled(false);
            cancelButton.setText("&Cancel");
        }
    }

    
    private void onStartRun(){
        state = STATE_RUNNING;
    }
    
    private void onEndRun(){
        state = STATE_NOT_RUNNING;
        Display.getDefault().asyncExec(new Runnable(){
        
            public void run(){
                okButton.setEnabled(true);
                cancelButton.setText("&Close");
            }
        });
    }

    
    public boolean close(){
        if(state == STATE_NOT_RUNNING){
            return super.close();
        }else{
            //Running: cancel it.
            ProcessHandler handler = this.processHandler;
            if(handler != null){
                handler.forceQuit = true;
            }
            return false;
        }
    }

    
    @Override
    protected void cancelPressed(){
        if(state == STATE_NOT_RUNNING){
            super.cancelPressed();
        }else{
            //Running: cancel it.
            ProcessHandler handler = this.processHandler;
            if(handler != null){
                handler.forceQuit = true;
            }
        }
    }

    public void setParameters(IContainer container, IPythonPathNature pythonPathNature, File appcfg,
            File appEngineLocation){
        this.container = container;
        this.pythonPathNature = pythonPathNature;
        this.appcfg = appcfg;
        this.appEngineLocation = appEngineLocation;
    }

    
    private void run(){
        synchronized(lock){
            if(processHandler != null){
                return; //Still running.
            }
            try{
                IProject project = container.getProject();
                IInterpreterInfo interpreterInfo = pythonPathNature.getNature().getProjectInterpreter();
                String executableOrJar = interpreterInfo.getExecutableOrJar();
    
                String cmdLineArguments = commandLineArguments.getText().trim();
                List<String> arguments = new ArrayList<String>();
                if(cmdLineArguments.length() > 0){
                    arguments = StringUtils.split(cmdLineArguments, ' ');
                }
                
                SimplePythonRunner runner = new SimplePythonRunner();
                String[] cmdarray = SimplePythonRunner.preparePythonCallParameters(executableOrJar, appcfg
                        .getAbsolutePath(), arguments.toArray(new String[0]));
    
                Tuple<Process, String> run = runner.run(cmdarray, appEngineLocation, project, new NullProgressMonitor());
                process = run.o1;
                if(process != null){
    
                    std = new ThreadStreamReader(process.getInputStream());
                    err = new ThreadStreamReader(process.getErrorStream());
    
                    std.start();
                    err.start();
    
                    outputStream = process.getOutputStream();
                    
                    processHandler = new ProcessHandler();
                    processHandler.start();
                }
            }catch(MisconfigurationException e){
                Log.log(e);
            }catch(PythonNatureWithoutProjectException e){
                Log.log(e);
            }
        }
    }

    private Button createButton(Composite composite, String label, int colSpan, int style){
        Button button = new Button(composite, style);
        button.setText(label);
        GridData gridData = new GridData();
        gridData.horizontalSpan = colSpan;
        button.setLayoutData(gridData);
        return button;
    }

    private Text createText(Composite composite, int colSpan){
        Text text = new Text(composite, SWT.SINGLE | SWT.BORDER);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.horizontalSpan = colSpan;
        text.setLayoutData(gridData);
        return text;
    }

    private void addCurrentCommand(){
        ProcessHandler p = processHandler;
        if(p != null){
            String text = commandText.getText();
            commandText.setText("");
            p.addCommandText(text+"\n");
        }
    }


}
