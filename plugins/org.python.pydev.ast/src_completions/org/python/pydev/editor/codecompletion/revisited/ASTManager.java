/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IProjectModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.IASTManagerObserver;
import org.python.pydev.shared_core.callbacks.ICallback0;


/**
 * This structure should be in memory, so that it acts very quickly.
 * 
 * Probably an hierarchical structure where modules are the roots and they 'link' to other modules or other definitions, would be what we
 * want.
 * 
 * The ast manager is a part of the python nature (as a field).
 * 
 * It's not meant to be subclassed!
 * 
 * @author Fabio Zadrozny
 */
public final class ASTManager extends AbstractASTManager implements ICodeCompletionASTManager {

    public ASTManager() {
    }

    /**
     * Set the project this ast manager works with.
     */
    @Override
    @SuppressWarnings("unchecked")
    public void setProject(IProject project, IPythonNature nature, boolean restoreDeltas) {
        getProjectModulesManager().setProject(project, nature, restoreDeltas);
        List<IASTManagerObserver> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_MANAGER_OBSERVER);
        for (IASTManagerObserver observer : participants) {
            try {
                observer.notifyASTManagerAttached(this);
            } catch (Exception e) {
                //let's keep it safe
                Log.log(e);
            }
        }
    }

    @Override
    public IModulesManager getModulesManager() {
        return getProjectModulesManager();
    }

    /**
     * @return modules manager wrapped cast to the interface we expect. Creates it if needed.
     */
    private synchronized IProjectModulesManager getProjectModulesManager() {
        if (modulesManager == null) {
            modulesManager = new ProjectModulesManager();
        }
        return (IProjectModulesManager) modulesManager;
    }

    //----------------------- AUXILIARIES

    @Override
    public void changePythonPath(String pythonpath, final IProject project, IProgressMonitor monitor) {
        getProjectModulesManager().changePythonPath(pythonpath, project, monitor);
    }

    @Override
    public void rebuildModule(File f, ICallback0<IDocument> doc, final IProject project, IProgressMonitor monitor,
            IPythonNature nature) {
        getProjectModulesManager().rebuildModule(f, doc, project, monitor, nature);
    }

    @Override
    public void removeModule(File file, IProject project, IProgressMonitor monitor) {
        getProjectModulesManager().removeModule(file, project, monitor);
    }

    /**
     * @return
     */
    public int getSize() {
        return getProjectModulesManager().getSize(true);
    }

    @Override
    public void saveToFile(File astOutputFile) {
        modulesManager.saveToFile(astOutputFile);
    }

    public static ICodeCompletionASTManager loadFromFile(File astOutputFile) throws IOException {
        ASTManager astManager = new ASTManager();
        ProjectModulesManager projectModulesManager = new ProjectModulesManager();
        ProjectModulesManager.loadFromFile(projectModulesManager, astOutputFile);
        astManager.modulesManager = projectModulesManager;
        return astManager;
    }

}
