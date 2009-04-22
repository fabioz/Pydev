package org.python.pydev.navigator.elements;

import org.eclipse.core.resources.IProject;

public class ProjectConfigError implements ISortedElement{

    private final IProject project;
    private final String label;

    public ProjectConfigError(IProject project, String label) {
        this.project = project;
        this.label = label;
    }

    public IProject getParent() {
        return this.project;
    }

    public int getRank() {
        return ISortedElement.RANK_ERROR;
    }

    public String getLabel() {
        return label;
    }

}
