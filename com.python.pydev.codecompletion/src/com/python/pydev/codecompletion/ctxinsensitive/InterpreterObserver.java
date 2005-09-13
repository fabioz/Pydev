/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.Document;
import org.python.parser.SimpleNode;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.revisited.ModulesManager;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.SystemModulesManager;
import org.python.pydev.editor.codecompletion.revisited.modules.ModulesKey;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.NotConfiguredInterpreterException;
import org.python.pydev.ui.interpreters.IInterpreterManager;
import org.python.pydev.ui.interpreters.IInterpreterObserver;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

public class InterpreterObserver implements IInterpreterObserver {

    /**
     * @see org.python.pydev.ui.interpreters.IInterpreterObserver#notifyDefaultPythonpathRestored(org.python.pydev.ui.interpreters.AbstractInterpreterManager, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void notifyDefaultPythonpathRestored(IInterpreterManager manager, IProgressMonitor monitor) {
        try {
            InterpreterInfo defaultInterpreterInfo = manager.getDefaultInterpreterInfo(monitor);
            SystemModulesManager m = defaultInterpreterInfo.modulesManager;
            AdditionalInterpreterInfo additionalSystemInfo = restoreInfoForModuleManager(monitor, m);

            //ok, set it and save it
            AdditionalInterpreterInfo.setAdditionalSystemInfo(manager, additionalSystemInfo);
            AdditionalInterpreterInfo.saveAdditionalSystemInfo(manager);
        } catch (NotConfiguredInterpreterException e) {
            //ok, nothing configured, nothing to do...
        }
    }

    /**
     * Restores the info for a module manager
     * 
     * @param monitor a monitor to keep track of the progress
     * @param m the module manager
     * @return the info generated from the module manager
     */
    private AdditionalInterpreterInfo restoreInfoForModuleManager(IProgressMonitor monitor, ModulesManager m) {
        AdditionalInterpreterInfo additionalSystemInfo = new AdditionalInterpreterInfo();

        ModulesKey[] allModules = m.getAllModules();
        int i = 0;
        for (ModulesKey key : allModules) {
            i++;

            if (key.file != null) { //otherwise it should be treated as a compiled module (no ast generation)

                if (key.file.exists()) {

                    if (PythonPathHelper.isValidSourceFile(REF.getFileAbsolutePath(key.file))) {
                        monitor.setTaskName("Creating additional info (" + i + " of " + allModules.length + ") for " + key.file.getName());
                        monitor.worked(1);

                        try {
                            //  the code below works with the default parser (that has much more info... and is much slower)
                            PyParser.ParserInfo parserInfo = new PyParser.ParserInfo(new Document(REF.getFileContents(key.file)), false, null);
                            Object[] obj = PyParser.reparseDocument(parserInfo);
                            SimpleNode node = (SimpleNode) obj[0];

                            //maybe later we can change by this, if it becomes faster and more consistent
                            //SimpleNode node = FastParser.reparseDocument(REF.getFileContents(key.file));

                            if (node != null) {
                                EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
                                node.accept(visitor);
                                Iterator<ASTEntry> classesAndMethods = visitor.getClassesAndMethodsIterator();

                                while (classesAndMethods.hasNext()) {
                                    SimpleNode classOrFunc = classesAndMethods.next().node;
                                    additionalSystemInfo.addClassOrFunc(classOrFunc, key.name);
                                }
                            }

                        } catch (Exception e) {
                            PydevPlugin.log(IStatus.ERROR, "Problem parsing the file :" + key.file + ".", e);
                        }
                    }
                } else {
                    PydevPlugin.log("The file :" + key.file + " does not exist, but is marked as existing in the pydev code completion.");
                }
            }
        }
        return additionalSystemInfo;
    }

    /**
     * received when the interpreter manager is restored
     *  
     * this means that we have to restore the additional interpreter information we stored
     *  
     * @see org.python.pydev.ui.interpreters.IInterpreterObserver#notifyInterpreterManagerRecreated(org.python.pydev.ui.interpreters.AbstractInterpreterManager)
     */
    public void notifyInterpreterManagerRecreated(final IInterpreterManager manager) {
        if(!AdditionalInterpreterInfo.loadAdditionalSystemInfo(manager)){
            //not successfully loaded
            new Job("Pydev... Restoring additional info"){

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    notifyDefaultPythonpathRestored(manager, monitor);
                    return Status.OK_STATUS;
                }
                
            }.schedule();
        }
    }

    public void notifyProjectPythonpathRestored(final PythonNature nature, IProgressMonitor monitor) {
        ModulesManager m = nature.getAstManager().getProjectModulesManager();
        AdditionalInterpreterInfo additionalSystemInfo = restoreInfoForModuleManager(monitor, m);
        
        //ok, set it and save it
        AdditionalInterpreterInfo.setAdditionalInfoForProject(nature.getProject(), additionalSystemInfo);
        AdditionalInterpreterInfo.saveAdditionalInfoForProject(nature.getProject());
    }

    public void notifyNatureRecreated(final PythonNature nature, IProgressMonitor monitor) {
        if(!AdditionalInterpreterInfo.loadAdditionalInfoForProject(nature.getProject())){
            notifyProjectPythonpathRestored(nature, monitor);
        }
    }

}
