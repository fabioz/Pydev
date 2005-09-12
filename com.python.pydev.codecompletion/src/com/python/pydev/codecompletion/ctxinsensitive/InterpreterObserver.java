/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.parser.SimpleNode;
import org.python.pydev.core.REF;
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
        InterpreterInfo defaultInterpreterInfo = manager.getDefaultInterpreterInfo(monitor);
        SystemModulesManager m = defaultInterpreterInfo.modulesManager;
        AdditionalInterpreterInfo additionalSystemInfo = AdditionalInterpreterInfo.getAdditionalSystemInfo(m);

        ModulesKey[] allModules = m.getAllModules();
        int i = 0;
        for (ModulesKey key : allModules) {
            i++;

            if (key.file != null) { //otherwise it should be treated as a compiled module (no ast generation)
            
                if (key.file.exists()) {

                    if (PythonPathHelper.isValidSourceFile(REF.getFileAbsolutePath(key.file))) {
                        monitor.setTaskName("Creating additional info (" + i + " of "+ allModules.length + ") for " + key.file.getName());
                        monitor.worked(i);
                    
                        try {
                            //  the code below would work with the default parser (that has much more info... and is much slower)
                            PyParser.ParserInfo parserInfo = new PyParser.ParserInfo(new Document(REF.getFileContents(key.file)), false, null);
                            Object[] obj = PyParser.reparseDocument(parserInfo);
                            SimpleNode node = (SimpleNode) obj[0];
                            
                            if (node != null) {
                                //SimpleNode node = FastParser.reparseDocument(REF.getFileContents(key.file));

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
    }

    /**
     * received when the interpreter manager is restored
     *  
     * this means that we have to restore the additional interpreter information we stored
     *  
     * @see org.python.pydev.ui.interpreters.IInterpreterObserver#notifyInterpreterManagerRecreated(org.python.pydev.ui.interpreters.AbstractInterpreterManager)
     */
    public void notifyInterpreterManagerRecreated(IInterpreterManager manager) {
        try {
            IProgressMonitor monitor = new NullProgressMonitor();
            InterpreterInfo defaultInterpreterInfo = manager.getDefaultInterpreterInfo(monitor);
            SystemModulesManager m = defaultInterpreterInfo.modulesManager;

        } catch (NotConfiguredInterpreterException e) {
            //we should ignore that because there is no interpreter configured for us to get additional information.
        }
    }

    public void notifyProjectPythonpathRestored(PythonNature nature) {
    }

    public void notifyNatureRecreated(PythonNature nature) {
    }

}
