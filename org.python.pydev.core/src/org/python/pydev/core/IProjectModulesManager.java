/*
 * Created on Jan 14, 2006
 */
package org.python.pydev.core;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;

public interface IProjectModulesManager extends IModulesManager {


    /**
     * Set the project this modules manager works with.
     * 
     * @param project the project related to this manager
     * @param restoreDeltas says whether deltas should be restored (if they are not, they should be discarded)
     */
    public abstract void setProject(IProject project, boolean restoreDeltas);

    public abstract void processUpdate(ModulesKey data);

    public abstract void processDelete(ModulesKey key);

    public abstract void processInsert(ModulesKey key);

    public abstract void endProcessing();

	public abstract void rebuildModule(File f, IDocument doc, IProject project, IProgressMonitor monitor, IPythonNature nature);

	public abstract void removeModule(File file, IProject project, IProgressMonitor monitor);

	public abstract void validatePathInfo(String pythonpath, IProject project, IProgressMonitor monitor);



}