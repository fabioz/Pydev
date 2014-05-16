package org.python.pydev.ui.pythonpathconf;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.editor.codecompletion.IPythonModuleResolver;

/**
 * A {@link IPythonModuleResolver} that delegates its behavior to instances that
 * are provided by unit tests.
 */
public class MockPythonModuleResolver implements IPythonModuleResolver {
    private static IPythonModuleResolver testDelegate;

    public static void setTestDelegate(IPythonModuleResolver testDelegate) {
        MockPythonModuleResolver.testDelegate = testDelegate;
    }

    @Override
    public String resolveModule(IProject project, IPath moduleLocation, List<IPath> baseLocations) {
        if (testDelegate != null) {
            return testDelegate.resolveModule(project, moduleLocation, baseLocations);
        }
        return null;
    }

    @Override
    public Collection<IPath> findAllModules(IProject project, IProgressMonitor monitor) {
        if (testDelegate != null) {
            return testDelegate.findAllModules(project, monitor);
        }
        return null;
    }
}
