package com.python.pydev.refactoring.changes;

import java.io.File;

import junit.framework.TestCase;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.Path;
import org.python.pydev.parser.PythonNatureStub;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.resource_stubs.FileStub;
import org.python.pydev.shared_core.resource_stubs.FolderStub;
import org.python.pydev.shared_core.resource_stubs.ProjectStub;

public class PyRenameResourceChangeTest extends TestCase {

    private File tempDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tempDir = FileUtils.getTempFileAt(new File("."), "data_py_rename_resource_change");
        tempDir.mkdirs();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtils.deleteDirectoryTree(tempDir);
    }

    public void testRenameResource() throws Exception {
        ProjectStub project = new ProjectStub(tempDir, new PythonNatureStub());
        File dirFile = new File(tempDir, "dir");
        dirFile.mkdirs();

        File file = new File(dirFile, "file.py");
        file.createNewFile();

        FolderStub folderStub = new FolderStub(project, dirFile);
        FileStub fileStub = new FileStub(project, file);

        String tempName = tempDir.getName();
        IContainer container;

        container = PyRenameResourceChange.getDestination(fileStub,
                "dir.file", "foo.bar.now", null);
        assertEquals(new Path(tempName + "/foo/bar"), container.getFullPath());

        container = PyRenameResourceChange.getDestination(fileStub,
                "dir.file", "foo", null);
        assertEquals(new Path(tempName), container.getFullPath());

        container = PyRenameResourceChange.getDestination(fileStub,
                "dir.file", "my.foo", null);
        assertEquals(new Path(tempName + "/my"), container.getFullPath());

        container = PyRenameResourceChange.getDestination(fileStub,
                "dir.file", "dir.foo", null);
        assertEquals(new Path(tempName + "/dir"), container.getFullPath());

        container = PyRenameResourceChange.getDestination(fileStub,
                "dir.file", "dir", null);
        assertEquals(new Path(tempName + ""), container.getFullPath());

        container = PyRenameResourceChange.getDestination(fileStub,
                "dir.file", "dir.file.now", null);
        assertEquals(new Path(tempName + "/dir/file"), container.getFullPath());
    }
}
