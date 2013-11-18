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
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.plugin.nature.PythonNature;

public class ModuleRenameRefactoringRequest extends RefactoringRequest {

    public ModuleRenameRefactoringRequest(File file, PythonNature nature) throws IOException {
        super(file, new PySelection(FileUtilsFileBuffer.getDocFromFile(file)), nature);
    }

    @Override
    public void fillInitialNameAndOffset() {
        initialName = FullRepIterable.getFirstPart(file.getName());
    }

}