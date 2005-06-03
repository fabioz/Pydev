/*
 * License: Common Public License v1.0
 * Created on Jun 2, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.plugin.nature;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.utils.REF;

/**
 * @author Fabio Zadrozny
 */
public class PythonPathNature implements IPythonPathNature {

    /**
     * This is the property that has the python path - associated with the project.
     */
    private static final QualifiedName PROJECT_SOURCE_PATH = new QualifiedName(PydevPlugin.getPluginID(), "PROJECT_SOURCE_PATH");
    private static final QualifiedName PROJECT_EXTERNAL_SOURCE_PATH = new QualifiedName(PydevPlugin.getPluginID(), "PROJECT_EXTERNAL_SOURCE_PATH");
    private IProject project;


    public void setProject(IProject project){
        this.project = project;
    }
    

    public List getCompleteProjectPythonPath() {
        if(project == null){
            return null;
        }
        
        PythonNature nature = PythonNature.getPythonNature(project);
        ProjectModulesManager projectModulesManager = nature.getAstManager().getProjectModulesManager();
        return projectModulesManager.getCompletePythonPath();
    }

    /**
     * @param project
     * @return
     * @throws CoreException
     */
    public String getOnlyProjectPythonPathStr() throws CoreException {
        String source = project.getPersistentProperty(PROJECT_SOURCE_PATH);
        String external = project.getPersistentProperty(PROJECT_EXTERNAL_SOURCE_PATH);
        
        if(source == null){
            source = "";
        }
        //we have to work on this one to resolve to full files, as what is stored is the position
        //relative to the project location
        String[] strings = source.split("\\|");
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < strings.length; i++) {
            if(strings[i].trim().length()>0){
                IPath p = new Path(strings[i]);
                p = PydevPlugin.getLocation(p, project);
                buf.append(REF.getFileAbsolutePath(p.toFile()));
                buf.append("|");
            }
        }
        
        
        if(external == null){
            external = "";
        }
        return buf.toString()+"|"+external;
    }

    

    public void setProjectSourcePath(String newSourcePath) throws CoreException {
        project.setPersistentProperty(PythonPathNature.PROJECT_SOURCE_PATH, newSourcePath);
    }

    public void setProjectExternalSourcePath(String newExternalSourcePath) throws CoreException {
        project.setPersistentProperty(PythonPathNature.PROJECT_EXTERNAL_SOURCE_PATH, newExternalSourcePath);
    }

    public String getProjectSourcePath() throws CoreException {
        return project.getPersistentProperty(PythonPathNature.PROJECT_SOURCE_PATH);
    }

    public String getProjectExternalSourcePath() throws CoreException {
        return project.getPersistentProperty(PythonPathNature.PROJECT_EXTERNAL_SOURCE_PATH);
    }

}
