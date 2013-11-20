/**
 * Copyright (c) 2013 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.refactoring;

import java.io.File;
import java.io.IOException;

import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;

public class ModuleRenameRefactoringRequest extends RefactoringRequest {

    public ModuleRenameRefactoringRequest(File file, IPythonNature nature) throws IOException {
        super(file, new PySelection(FileUtilsFileBuffer.getDocFromFile(file)), nature);
    }

    @Override
    public void fillInitialNameAndOffset() {
        try {
            initialName = nature.resolveModule(file);
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
    }

}