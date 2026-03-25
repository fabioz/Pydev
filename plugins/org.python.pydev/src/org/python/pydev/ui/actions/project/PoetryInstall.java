package org.python.pydev.ui.actions.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.package_manager.PoetryPackageManager;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.ui.actions.container.PyContainerAction;

public class PoetryInstall extends PyContainerAction {
    private IPath absPath;
    private IProject project;
    private String pythonBin;

    @Override
    protected boolean confirmRun() {
        // TODO Figure out why the monitor doesn't progress until a dialog is displayed then use the below.
        //        MessageDialog confirm = new MessageDialog(EditorUtils.getShell(), "Install using poetry", null,
        //                "This will install using...", MessageDialog.QUESTION_WITH_CANCEL, 0, new String[] { "OK", "Cancel" });
        //        confirm.open();
        //        if (confirm.getReturnCode() == 1) {
        //            return false;
        //        }
        return true;
    }

    @Override
    protected void afterRun(int resourcesAffected) {
    }

    @Override
    protected boolean needsUIThread() {
        return true;
    }

    @Override
    protected int doActionOnContainer(IContainer container, IProgressMonitor monitor) {
        monitor.beginTask("Installing project with poetry...", 100);
        MessageDialog confirm = new MessageDialog(EditorUtils.getShell(), "Install using poetry", null,
                "This will install using...", MessageDialog.QUESTION_WITH_CANCEL, 0, new String[] { "OK", "Cancel" });
        confirm.open();
        if (confirm.getReturnCode() == 1) {
            return 0;
        }
        project = container.getProject();
        absPath = project.getLocation();
        PoetryPackageManager pm = new PoetryPackageManager(absPath.toString());
        String result = pm.install();
        monitor.worked(50);
        Log.logInfo("Result: " + result);
        pythonBin = pm.getPython();
        Log.logInfo("Python found at: " + pythonBin);
        PythonNature nature = getNature();
        if (nature == null) {
            // This should never happen
        } else {
            setInterpreter(nature);
        }
        monitor.worked(75);
        try {
            project.refreshLocal(1, monitor);
        } catch (CoreException e) {
            Log.log(e);
        }
        monitor.worked(100);
        monitor.done();
        return 0;
    }

    private PythonNature getNature() {
        IProjectNature nature;
        try {
            nature = project.getNature(PythonNature.PYTHON_NATURE_ID);
            return (PythonNature) nature;
        } catch (CoreException e) {
            return null;
        }
    }

    private void setInterpreter(PythonNature nature) {
        IInterpreterInfo interpreter;
        try {
            interpreter = nature.getProjectInterpreter();
            String name = interpreter.getName();
            Log.logInfo("Name: " + name);
        } catch (MisconfigurationException e) {
            String missingInterpeter = "Interpreter: " + project.getName() + " not found";
            if (Objects.equals("Python not configured.", e.getMessage())) {
                Log.logInfo("Misconfiguration error, no interpreters configured.");
                setDefaultInterpreter(nature);
            } else if (Objects.equals(missingInterpeter, e.getMessage().toString())) {
                Log.logInfo("Misconfiguration error, project interpreter not configured.");
                updateInterpererInfo(nature);
            } else {
                Log.logInfo("Unknown error: Message: " + e.getMessage());
                Log.log(e);
            }
        } catch (PythonNatureWithoutProjectException e1) {
            Log.logInfo("No project nature.");
            Log.log(e1);
        }
    }

    private void setDefaultInterpreter(PythonNature nature) {
        Log.logInfo("Setting default interpreter.");
        IInterpreterManager pythonInterpreterManager = InterpreterManagersAPI.getPythonInterpreterManager();
        IInterpreterInfo projectInterpreter = pythonInterpreterManager.createInterpreterInfo(pythonBin,
                new NullProgressMonitor(), false);
        projectInterpreter.setName(project.getName());
        pythonInterpreterManager.setInfos(new IInterpreterInfo[] { projectInterpreter }, null,
                new NullProgressMonitor());
        Log.logInfo("Configured!");
        try {
            nature.setVersion(PythonNature.PYTHON_VERSION_INTERPRETER, project.getName());
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void updateInterpererInfo(PythonNature nature) {
        IInterpreterManager pythonInterpreterManager = InterpreterManagersAPI.getPythonInterpreterManager();
        IInterpreterInfo[] infos = pythonInterpreterManager.getInterpreterInfos();
        IInterpreterInfo projectInterpreter = pythonInterpreterManager.createInterpreterInfo(pythonBin,
                new NullProgressMonitor(), false);
        projectInterpreter.setName(project.getName());
        Set<String> existing = new HashSet<String>();
        ArrayList<IInterpreterInfo> newInfos = new ArrayList<IInterpreterInfo>();
        for (IInterpreterInfo info : infos) {
            existing.add(info.getName());
            newInfos.add(info);
        }
        newInfos.add(projectInterpreter);
        pythonInterpreterManager.setInfos(newInfos.toArray(new IInterpreterInfo[newInfos.size()]), existing,
                new NullProgressMonitor());

    }

}
