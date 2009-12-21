package org.python.pydev.customizations.app_engine.actions;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.customizations.app_engine.util.AppEngineProcessWindow;
import org.python.pydev.editor.actions.PyAction;

/**
 * Just opens a dialog where the user can manage things.
 */
public class AppEngineManage extends AbstractAppEngineHandler{

    protected void handleExecution(IContainer container, IPythonPathNature pythonPathNature, File appcfg, File appEngineLocation){
        AppEngineProcessWindow processWindow = new AppEngineProcessWindow(PyAction.getShell());
        processWindow.setParameters(container, pythonPathNature, appcfg, appEngineLocation);
        processWindow.open();
    }
    
}