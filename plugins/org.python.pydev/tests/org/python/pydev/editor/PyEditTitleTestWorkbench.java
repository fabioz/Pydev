/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor;

import java.io.ByteArrayInputStream;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.editorinput.PyOpenEditor;
import org.python.pydev.shared_core.callbacks.ICallback;

public class PyEditTitleTestWorkbench extends AbstractWorkbenchTestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite(PyEditTitleTestWorkbench.class.getName());

        suite.addTestSuite(PyEditTitleTestWorkbench.class);

        if (suite.countTestCases() == 0) {
            throw new Error("There are no test cases to run");
        } else {
            return suite;
        }
    }

    @Override
    protected void setUp() throws Exception {
        //no need for default setup
        closeWelcomeView();
    }

    public void testEditorTitle() throws Exception {
        NullProgressMonitor monitor = new NullProgressMonitor();
        IProject project = createProject(monitor, "pydev_title_project");

        IFile myFile = project.getFile("my_file.py");
        myFile.create(new ByteArrayInputStream("".getBytes()), true, monitor);

        IFolder folder = project.getFolder("folder");
        folder.create(true, true, null);

        IFile file2 = folder.getFile("my_file.py");
        file2.create(new ByteArrayInputStream("".getBytes()), true, monitor);

        project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

        PyEdit editor2 = null;
        PyEdit editor = null;
        try {
            editor = (PyEdit) PyOpenEditor.doOpenEditor(myFile);
            final PyEdit editorRef = editor;
            String partName = editor.getPartName();
            assertEquals("my_file", partName);
            editor2 = (PyEdit) PyOpenEditor.doOpenEditor(file2);
            final PyEdit editor2final = editor2;
            //We may wait until 10 seconds for the condition to happen (we must not keep the ui-thread
            //otherwise it won't work).
            goToManual(10000, new ICallback<Boolean, Object>() {

                @Override
                public Boolean call(Object arg) {
                    return "my_file (pydev_title_project)".equals(editorRef.getPartName())
                            && "my_file (folder)".equals(editor2final.getPartName());
                }
            });
        } finally {
            if (editor2 != null) {
                editor2.close(false);
            }
            if (editor != null) {
                editor.close(false);
            }
        }

    }

}
