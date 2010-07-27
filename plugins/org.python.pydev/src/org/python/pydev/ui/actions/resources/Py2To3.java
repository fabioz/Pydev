package org.python.pydev.ui.actions.resources;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.python.pydev.consoles.MessageConsoles;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.runners.UniversalRunner;
import org.python.pydev.runners.UniversalRunner.AbstractRunner;
import org.python.pydev.ui.UIConstants;


/**
 * Applies 2to3.py in the selected folder(s)/file(s)
 */
public class Py2To3 extends PyResourceAction implements IObjectActionDelegate{

    public static final String RUN_2_TO_3_CODE = "" +
    "from lib2to3.main import main\n" +
    "import sys\n" +
    "import os\n" +
    "\n" +
    "sys.exit(main('lib2to3.fixes'))\n" +
    "";

    
    PythonNature natureUsed;
    List<String> parameters;
    List<IContainer> refresh;
    
    protected boolean confirmRun() {
        clearRunInput();
        PythonNature nature = null;
        for(IResource c:selectedResources){
            PythonNature n2 = PythonNature.getPythonNature(c);
            if(n2 != null){
                if(nature == null){
                    nature = n2;
                    
                } else{
                    if(n2 != nature){
                        MessageBox message = new MessageBox(PyAction.getShell(), SWT.OK | SWT.ICON_ERROR);
                        message.setText("Multiple python natures");
                        message.setMessage("This action can only be applied in one project at a time.");
                        message.open();
                        return false;
                    }
                }
            }
        }
        
        if(nature == null){
            MessageBox message = new MessageBox(PyAction.getShell(), SWT.OK | SWT.ICON_ERROR);
            message.setText("No nature found");
            message.setMessage("This action can only be applied in a project that is configured as a Pydev project.");
            message.open();
            return false;
        }
        
        
        AbstractRunner runner = UniversalRunner.getRunner(nature);
        
        Tuple<String, String> tup = 
            runner.runCodeAndGetOutput(RUN_2_TO_3_CODE, new String[]{"--help"}, null, new NullProgressMonitor());
        if(tup.o1.indexOf("ImportError") != -1 || tup.o2.indexOf("ImportError") != -1){
            MessageBox message = new MessageBox(PyAction.getShell(), SWT.OK | SWT.ICON_ERROR);
            message.setText("Unable to run 2to3");
            message.setMessage("Unable to run 2to3. Details: \n"+tup.o1+"\n"+tup.o2+"\n\nNotes: check if lib2to3 is properly installed in your Python install.");
            message.open();
            return false;
        }
        
        String msg = "Please enter the parameters to be passed for 2to3.py\n\n"+tup.o1+"\n\n" +
        		"E.g.: \n" +
        		"Leave empty for preview\n" +
        		"-w to apply with backup\n" +
        		"-w -n to apply without backup.";
        if(tup.o2 != null && tup.o2.length() > 0){
            msg += "\n";
            msg += tup.o2;
        }
        
        InputDialog d = new InputDialog(
                PyAction.getShell(), 
                "Parameters for 2to3.py", 
                msg, 
                "", 
                null){
            protected Control createDialogArea(Composite parent) {
//                try {
//                    FontData labelFontData = new FontData("Courier New", 8, SWT.NONE);
//                    parent.setFont(new Font(parent.getDisplay(), labelFontData));
//                } catch (Throwable e) {
//                    //ignore
//                }
                return super.createDialogArea(parent);
            }
        };

        
        int retCode = d.open();
        if (retCode != InputDialog.OK) {
            return false;
        }
        
        MessageConsole console = MessageConsoles.getConsole("2To3", UIConstants.PY_INTERPRETER_ICON);
        console.clearConsole();
        parameters = StringUtils.split(d.getValue(), " ");
        natureUsed = nature;
        return true;
    }

    protected void afterRun(int resourcesAffected) {
        for(IContainer c:this.refresh){
            try {
                c.refreshLocal(IResource.DEPTH_INFINITE, null);
            } catch (CoreException e) {
                PydevPlugin.log(e);
            }
        }
        clearRunInput();
    }

    private void clearRunInput() {
        natureUsed = null;
        refresh = null;
        parameters = null;
    }

    
    protected int doActionOnResource(IResource next, IProgressMonitor monitor) {
        this.refresh = new ArrayList<IContainer>();
        AbstractRunner runner = UniversalRunner.getRunner(natureUsed);
        if(next instanceof IContainer){
            this.refresh.add((IContainer) next);
        }else{
            this.refresh.add(next.getParent());
        }
        
        String dir = next.getLocation().toOSString();
        File workingDir = new File(dir);
        if(!workingDir.exists()){
            PydevPlugin.log("Received file that does not exist for 2to3: "+workingDir);
            return 0;
        }
        if(!workingDir.isDirectory()){
            workingDir = workingDir.getParentFile();
            if(!workingDir.isDirectory()){
                PydevPlugin.log("Unable to find working dir for 2to3. Found invalid: "+workingDir);
                return 0;
            }
        }
        ArrayList<String> parametersWithResource = new ArrayList<String>(parameters);
        parametersWithResource.add(0, dir);
        Tuple<String, String> tup = 
            runner.runCodeAndGetOutput(RUN_2_TO_3_CODE, parametersWithResource.toArray(new String[0]), workingDir, monitor);
        IOConsoleOutputStream out = MessageConsoles.getConsoleOutputStream("2To3", UIConstants.PY_INTERPRETER_ICON);
        try {
            out.write(tup.o1);
            out.write("\n");
            out.write(tup.o2);
        } catch (IOException e) {
            PydevPlugin.log(e);
        }
        return 1;
    }

}
