/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.refactoring;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.plugin.nature.PythonNature;

public class ModuleRenameRefactoringRequest extends RefactoringRequest {

    private IContainer target;

    /**
     * @param target: optional (may be null): when null it means we'll move/rename using the same source folder it's currently
     * in as a reference. Otherwise, this is the container it should be moved to.
     */
    public ModuleRenameRefactoringRequest(File file, IPythonNature nature, IContainer target) throws IOException {
        super(file, new PySelection(FileUtilsFileBuffer.getDocFromFile(file)), nature);
        this.target = target;
    }

    public void setTarget(IContainer target) {
        this.target = target;
    }

    public IContainer getTarget() {
        return target;
    }

    @Override
    public void fillInitialNameAndOffset() {
        try {
            initialName = nature.resolveModule(file);
            if (initialName.endsWith(".__init__")) {
                initialName = initialName.substring(0, initialName.length() - 9);
            }
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isModuleRenameRefactoringRequest() {
        return true;
    }

    @Override
    public IPythonNature getTargetNature() {
        if (target != null) {
            return PythonNature.getPythonNature(target);
        }
        return nature;
    }

}