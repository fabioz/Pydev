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

public class PyEditTitleTestWorkbench extends AbstractWorkbenchTestCase{
    
	public static Test suite() {
		TestSuite suite = new TestSuite(PyEditTitleTestWorkbench.class.getName());
		
        suite.addTestSuite(PyEditTitleTestWorkbench.class); 
        
        if (suite.countTestCases() == 0) {
            throw new Error("There are no test cases to run");
        } else {
            return suite;
        }
	}
    
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
		try {
            editor = (PyEdit) PyOpenEditor.doOpenEditor(myFile);
            assertEquals("my_file.py", editor.getPartName());
            editor2 = (PyEdit) PyOpenEditor.doOpenEditor(file2);
            assertEquals("my_file.py (pydev_title_project)", editor.getPartName());
            assertEquals("my_file.py (folder)", editor2.getPartName());
        } finally {
            editor2.close(true);
            editor = null;
        }
        
    }

}
