/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 14, 2006
 */
package org.python.pydev.core;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.callbacks.ICallback0;

public interface IProjectModulesManager extends IModulesManager {

    /**
     * Set the project this modules manager works with.
     * 
     * @param project the project related to this manager
     * @param restoreDeltas says whether deltas should be restored (if they are not, they should be discarded)
     */
    public abstract void setProject(IProject project, IPythonNature nature, boolean restoreDeltas);

    public abstract void processUpdate(ModulesKey data);

    public abstract void processDelete(ModulesKey key);

    public abstract void processInsert(ModulesKey key);

    public abstract void endProcessing();

    public abstract void rebuildModule(File f, ICallback0<IDocument> doc, IProject project, IProgressMonitor monitor,
            IPythonNature nature);

    public abstract void removeModule(File file, IProject project, IProgressMonitor monitor);

    /**
     * @return a given module only if it's actually controlled in the given modules manager (not considering any dependencies)
     */
    public abstract IModule getModuleInDirectManager(String name, IPythonNature nature, boolean dontSearchInit);

    /**
     * @return the name of a given module or null if it can't resolve it for this modules manager.
     */
    public abstract String resolveModuleInDirectManager(IFile file);

    /**
     * @return the name of a given module or null if it can't resolve it for this modules manager.
     */
    public abstract String resolveModuleInDirectManager(String full);

}