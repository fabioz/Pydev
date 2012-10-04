package org.python.pydev.core.resource_stubs;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;

public class FileMock extends AbstractIFileStub {

    private String name;
    private FolderMock parent;

    public FileMock(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setParent(FolderMock folderMock) {
        this.parent = folderMock;

    }

    @Override
    public IProject getProject() {
        return this.parent.getProject();
    }

    @Override
    public IContainer getParent() {
        return this.parent;
    }

}
