/*
 * Created on Mar 20, 2006
 */
package org.python.pydev.debug.newconsole.env;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.Launch;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple3;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.SocketUtil;
import org.python.pydev.runners.SimpleJythonRunner;
import org.python.pydev.runners.SimplePythonRunner;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.ui.UIConstants;

/**
 * This class is used to create the given IProcess and get the console that is attached to that process. 
 */
public class IProcessFactory {

    private List<IPythonNature> naturesUsed;

    public List<IPythonNature> getNaturesUsed() {
        return naturesUsed;
    }
    
    /**
     * @return a shell that we can use.
     */
    public Shell getShell() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    public static final String INTERACTIVE_LAUNCH_PORT = "INTERACTIVE_LAUNCH_PORT";

    /**
     * Creates a launch (and its associated IProcess) for the xml-rpc server to be used in the interactive console.
     * 
     * It'll ask the user how to create it:
     * - editor
     * - python interpreter
     * - jython interpreter
     * 
     * @return the Launch, the Process created and the port that'll be used for the server to call back into
     * this client for requesting input.
     * 
     * @throws UserCanceledException
     * @throws Exception
     */
    public Tuple3<Launch, Process, Integer> createInteractiveLaunch()
            throws UserCanceledException, Exception {
        
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        IWorkbenchPage activePage = workbenchWindow.getActivePage();
        IEditorPart activeEditor = activePage.getActiveEditor();
        PyEdit edit = null;
        Process process = null;
        
        if (activeEditor instanceof PyEdit) {
            edit = (PyEdit) activeEditor;
        }
        
        ChooseProcessTypeDialog dialog = new ChooseProcessTypeDialog(getShell(), edit);
        if(dialog.open() == ChooseProcessTypeDialog.OK){
            
            IInterpreterManager interpreterManager = dialog.getInterpreterManager();
            
            if(interpreterManager != null){
                naturesUsed = dialog.getNatures();
                int port = SocketUtil.findUnusedLocalPort();
                int clientPort = SocketUtil.findUnusedLocalPort();
                
                final Launch launch = new Launch(null, "interactive", null);
                launch.setAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT, "false");
                launch.setAttribute(INTERACTIVE_LAUNCH_PORT, ""+port);
                
        
                File scriptWithinPySrc = PydevPlugin.getScriptWithinPySrc("pydevconsole.py");
                String[] commandLine;
                IInterpreterInfo[] interpreters = interpreterManager.getInterpreterInfos();
                ListDialog listDialog = createChoiceDialog(workbenchWindow, interpreterManager, interpreters);
                int open = listDialog.open();
                if(open != ListDialog.OK || listDialog.getResult().length > 1){
                    return null;
                }
                Object[] result = (Object[]) listDialog.getResult();
                IInterpreterInfo interpreter = null;
                if(result == null || result.length == 0){
                    interpreter = interpreters[0];
                    
                }else{
                    interpreter = ((IInterpreterInfo)result[0]);
                }
                
                if(interpreter == null){
                    return null;
                }
                
                Collection<String> pythonpath = dialog.getPythonpath(interpreter);
                if(pythonpath == null){
                    return null;
                }
                String pythonpathEnv = SimpleRunner.makePythonPathEnvFromPaths(pythonpath);
                String[] env = SimpleRunner.createEnvWithPythonpath(pythonpathEnv, interpreter.getExecutableOrJar(), interpreterManager);
                
                if(interpreterManager.isPython()){
                    commandLine = SimplePythonRunner.makeExecutableCommandStr(interpreter.getExecutableOrJar(), scriptWithinPySrc.getAbsolutePath(), 
                            new String[]{String.valueOf(port), String.valueOf(clientPort)});
                    
                }else if(interpreterManager.isJython()){
                    String vmArgs = PydevDebugPlugin.getDefault().getPreferenceStore().
                        getString(PydevConsoleConstants.INTERACTIVE_CONSOLE_VM_ARGS);
                    
                    commandLine = SimpleJythonRunner.makeExecutableCommandStrWithVMArgs(interpreter.getExecutableOrJar(), scriptWithinPySrc.getAbsolutePath(), 
                            pythonpathEnv, vmArgs, new String[]{String.valueOf(port), String.valueOf(clientPort)});
                    
                }else{
                    throw new RuntimeException("Expected interpreter manager to be python or jython related.");
                }
                
                
                
                process = SimpleRunner.createProcess(commandLine, env, null);
                PydevSpawnedInterpreterProcess spawnedInterpreterProcess = 
                    new PydevSpawnedInterpreterProcess(process, launch);
                
                launch.addProcess(spawnedInterpreterProcess);
                
                return new Tuple3<Launch, Process, Integer>(launch, process, clientPort);
            }
        }
        return null;
    }

    private ListDialog createChoiceDialog(
            IWorkbenchWindow workbenchWindow, IInterpreterManager pythonInterpreterManager, IInterpreterInfo[] interpreters) {
        ListDialog listDialog = new ListDialog(workbenchWindow.getShell());
        listDialog.setContentProvider(new IStructuredContentProvider(){

            public Object[] getElements(Object inputElement) {
                if(inputElement instanceof IInterpreterInfo[]){
                    return (IInterpreterInfo[]) inputElement;
                }
                return new Object[0];
            }

            public void dispose() {
            }

            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
                
            }}
        );
        listDialog.setLabelProvider(new LabelProvider(){
            public Image getImage(Object element) {
                return PydevPlugin.getImageCache().get(UIConstants.PY_INTERPRETER_ICON);
            }
            public String getText(Object element) {
                if(element != null && element instanceof IInterpreterInfo){
                    IInterpreterInfo info = (IInterpreterInfo) element;
                    return info.getNameForUI();
                }
                return super.getText(element);
            }
        });
        listDialog.setInput(interpreters);
        listDialog.setMessage("Select interpreter to be used.");
        return listDialog;
    }


}
