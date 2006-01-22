/*
 * License: Common Public License v1.0
 * Created on Jun 2, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.plugin.nature;

import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.REF;
import org.python.pydev.plugin.PydevPlugin;

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
        IModulesManager projectModulesManager = getProjectModulesManager();
        if(projectModulesManager == null){
            return null;
        }
        return projectModulesManager.getCompletePythonPath();
    }
    
    private IModulesManager getProjectModulesManager(){
        if(project == null){
            return null;
        }
        PythonNature nature = PythonNature.getPythonNature(project);
        
        if(nature == null) {
            return null;
        }
        
        if(nature.getAstManager() == null) {
            // AST manager might not be yet available
            // Code completion job is scheduled to be run
            return null;
        }
              
        return nature.getAstManager().getModulesManager();
    }

    /**
     * @return the project pythonpath with complete paths in the filesystem.
     */
    public String getOnlyProjectPythonPathStr() throws CoreException {
        String source = getProjectSourcePath();
        String external = getProjectExternalSourcePath();
        
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
                IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                
                //try to get relative to the workspace 
                IContainer container = null;
                IResource r = null;
                try {
                    r = root.findMember(p);
                } catch (Exception e) {
                    PydevPlugin.log(e);
                }
                if(r instanceof IContainer){
                    container = (IContainer) r;
                }
                
//                this is code to get it relative to the project... I was pursuing this solution, but as too many
//                things already related to it being relative to the workspace, just the getProjectSourcePath was changed
//                if(container == null){
//                    IPath projectPath = project.getFullPath();
//                    projectPath.append(p);
//                    p = projectPath;
//                    //try to get it relative to the project 
//                    r = root.findMember(p);
//                    if(r instanceof IContainer){
//                        container = (IContainer) r;
//                    }
//                }
                
                if(container != null){
                    buf.append(REF.getFileAbsolutePath(container.getLocation().toFile()));
                    buf.append("|");
                }else{ //the location was not found
                    
                    //not in workspace?... maybe it was removed, so, do nothing, but let the user know about it
                    PydevPlugin.log("Unable to find the path "+strings[i]+" in the project were it\n" +
                            "is added as a source folder for pydev (project: "+project.getName()+")");
                }
            }
        }
        
        
        if(external == null){
            external = "";
        }
        return buf.toString()+"|"+external;
    }

    

    public void setProjectSourcePath(String newSourcePath) throws CoreException {
        synchronized(project){
            project.setPersistentProperty(PythonPathNature.PROJECT_SOURCE_PATH, newSourcePath);
        }
    }

    public void setProjectExternalSourcePath(String newExternalSourcePath) throws CoreException {
        synchronized(project){
            project.setPersistentProperty(PythonPathNature.PROJECT_EXTERNAL_SOURCE_PATH, newExternalSourcePath);
        }
    }

    public String getProjectSourcePath() throws CoreException {
        synchronized(project){
            boolean restore = false;
            String projectSourcePath = project.getPersistentProperty(PythonPathNature.PROJECT_SOURCE_PATH);
            //we have to validate it, because as we store the values relative to the workspace, and not to the 
            //project, the path may become invalid (in which case we have to make it compatible again).
            StringBuffer buffer = new StringBuffer();
            String[] paths = projectSourcePath.split("\\|");
            for (String path : paths) {
                if(path.trim().length() > 0){
                    IPath p = new Path(path);
                    if(p.isEmpty()){
                        continue; //go to the next...
                    }
                    IPath projectPath = project.getFullPath();
                    if(!projectPath.isPrefixOf(p)){
                        p = p.removeFirstSegments(1);
                        p = projectPath.append(p);
                        restore = true;
                    }
                    buffer.append(p.toString());
                    buffer.append("|");
                }
            }
            
            //it was wrong and has just been fixed
            if(restore){
                projectSourcePath = buffer.toString();
                setProjectSourcePath(projectSourcePath);
                PythonNature nature = PythonNature.getPythonNature(project);
                if(nature != null){
                    //yeap, everything has to be done from scratch, as all the filesystem paths have just
                    //been turned to dust!
                    nature.rebuildPath(getOnlyProjectPythonPathStr());
                }
            }
            return projectSourcePath;
        }
    }

    public String getProjectExternalSourcePath() throws CoreException {
        synchronized(project){
            //no need to validate because those are always 'file-system' related
            return project.getPersistentProperty(PythonPathNature.PROJECT_EXTERNAL_SOURCE_PATH);
        }
    }

}
