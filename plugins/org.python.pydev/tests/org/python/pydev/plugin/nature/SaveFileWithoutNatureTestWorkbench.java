/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.nature;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.editorinput.PyOpenEditor;

public class SaveFileWithoutNatureTestWorkbench extends AbstractWorkbenchTestCase {

    @Override
    protected void setUp() throws Exception {
        //no setup (because we won't have the nature in this test)
        closeWelcomeView();
    }

    public void testEditWithNoNature() throws Exception {
        NullProgressMonitor monitor = new NullProgressMonitor();
        IProject project = createProject(monitor, "pydev_no_nature_project");

        IFile myFile = project.getFile("my_file.py");

        String contents = "";
        String newContents = "class Foo(object):\n    pass";

        myFile.create(new ByteArrayInputStream(contents.getBytes()), true, monitor);
        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        try {
            editor = (PyEdit) PyOpenEditor.doOpenEditor(myFile);
            editor.getDocument().set(newContents);
            editor.doSave(monitor);
        } finally {
            editor.close(true);
            editor = null;
        }
        assertEquals(newContents, FileUtilsFileBuffer.getDocFromResource(myFile).get());

    }

}
