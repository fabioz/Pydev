package com.python.pydev.analysis.search_index;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.shared_ui.search.ICustomModule;

/**
 * Note: this one doesn't really exist as a match, it's only generated in the tree content provider.
 */
public class PyCustomModule implements IAdaptable, ICustomModule {

    public final IProject project;
    public final ModulesKey modulesKey;
    public final PyModuleLineElement moduleLineElement;
    public final IResource resource;

    public PyCustomModule(PyModuleLineElement moduleLineElement) {
        this.project = moduleLineElement.getProject();
        this.resource = moduleLineElement.getParent();
        this.modulesKey = moduleLineElement.modulesKey;
        this.moduleLineElement = moduleLineElement;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return this.moduleLineElement.getAdapter(adapter);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((modulesKey == null) ? 0 : modulesKey.hashCode());
        result = prime * result + ((project == null) ? 0 : project.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PyCustomModule other = (PyCustomModule) obj;
        if (modulesKey == null) {
            if (other.modulesKey != null) {
                return false;
            }
        } else if (!modulesKey.equals(other.modulesKey)) {
            return false;
        }
        if (project == null) {
            if (other.project != null) {
                return false;
            }
        } else if (!project.equals(other.project)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return this.modulesKey.name;
    }

    @Override
    public Object getModuleLineElement() {
        return moduleLineElement;
    }

}