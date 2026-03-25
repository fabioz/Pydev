package org.python.pydev.ui.actions.container;

import java.util.Objects;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.package_manager.UVPackageManager;

public abstract class PyUVAction extends PyContainerAction {
    protected UVPackageManager pm;

    @Override
    protected boolean confirmRun() {
        return true;
    }

    @Override
    protected void afterRun(int resourcesAffected) {
    }

    @Override
    protected boolean needsUIThread() {
        return true;
    }

    abstract protected String getTaskName();

    @Override
    protected int doActionOnContainer(IContainer container, IProgressMonitor monitor) {
        if (monitor.isCanceled()) {
            return 0;
        }
        monitor.beginTask(getTaskName(), 100);
        monitor.worked(1);
        IProject project = container.getProject();
        pm = new UVPackageManager(project.getLocation().toString());
        String result = runCommand();
        int affected = 1;
        if (monitor.isCanceled()) {
            return 0;
        }
        monitor.worked(50);
        if (!Objects.equals(result, "")) {
            try {
                project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            } catch (CoreException e) {
                Log.log(e);
            }
            affected = 0;
        }
        monitor.worked(100);
        monitor.done();
        return affected;
    }

    abstract protected String runCommand();
}
