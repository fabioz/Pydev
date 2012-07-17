package org.python.pydev.core.resource_stubs;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.IPythonNature;

public class ProjectMock extends AbstractIProjectStub {

    private IPythonNature nature;

    public void addMember(FolderMock mod1) {
        mod1.setParent(this);
    }

    public void setNature(IPythonNature pythonNatureStub) {
        this.nature = pythonNatureStub;
    }

    @Override
    public IProjectNature getNature(String natureId) throws CoreException {
        return this.nature;
    }

    @Override
    public IProject getProject() {
        return this;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

}
