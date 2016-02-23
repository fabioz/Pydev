/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.shared_core.callbacks.ICallback0;

public class SystemASTManager extends AbstractASTManager {

    public SystemASTManager(IInterpreterManager manager, IPythonNature nature, IInterpreterInfo info) {
        this.modulesManager = info.getModulesManager();
        setNature(nature);
    }

    @Override
    public void setProject(IProject project, IPythonNature nature, boolean restoreDeltas) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void rebuildModule(File file, ICallback0<IDocument> doc, IProject project, IProgressMonitor monitor,
            IPythonNature nature) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void removeModule(File file, IProject project, IProgressMonitor monitor) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void saveToFile(File astOutputFile) {
        throw new RuntimeException("Not implemented");
    }

}
