package org.python.pydev.shared_core.resource_stubs;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;

public interface IProjectStub extends IProject {

    IContainer getFolder(File parentFile);

    File getProjectRoot();

}
