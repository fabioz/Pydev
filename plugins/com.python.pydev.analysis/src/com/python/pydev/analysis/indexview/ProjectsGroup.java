package com.python.pydev.analysis.indexview;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.python.pydev.plugin.nature.PythonNature;

public class ProjectsGroup extends ElementWithChildren {

    public ProjectsGroup(ITreeElement indexRoot) {
        super(indexRoot);
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    
    @Override
    public String toString() {
        return "Projects";
    }


    @Override
    protected void calculateChildren() {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = root.getProjects();
        for (IProject iProject : projects) {
            PythonNature nature = PythonNature.getPythonNature(iProject);
            if(nature != null){
                addChild(new NatureGroup(this, nature));
            }
        }
    }
    
}
