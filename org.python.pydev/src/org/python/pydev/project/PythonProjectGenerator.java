package org.python.pydev.project;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.templates.TemplateException;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * Generates a project with Python nature
 * 
 * TODO: Handle IProgressMonitor
 * 
 * @author Mikko Ohtamaa
 */
public class PythonProjectGenerator {
	
	/** Targetted workspace */ 
	IWorkspace workspace;
	
	/** Targetted project */
	IProject project;
	

	/**
	 * Creates a Python project
	 * 
	 * <ul>
	 * <li>Creates a new project
	 * <li>Adds Python nature
	 * </ul>
	 * 
	 * IProgressMonitor has 3 steps.
	 * <ol>
	 * <li>Create a project
	 * <li>Add a nature
	 * <li>Add initial contents
	 * </ol>
	 * 
	 * @param projectName
	 * @param monitor
	 * @return
	 * @throws CoreException
	 * @throws GeneratingException
	 * @throws IOException
	 * @throws TemplateException
	 */
	public IProject createProject(String projectName, IProgressMonitor monitor) 
		    throws CoreException, IOException {
		        
		                
        //bundle = Series60Plugin.getDefault().getBundle();
        
        workspace = ResourcesPlugin.getWorkspace();
        project = workspace.getRoot().getProject(projectName);
                                
        if(!project.exists()) {
        	
        	project.create(null);
        	
            // Create project and set natures
            IProjectDescription projDesc = workspace.newProjectDescription(projectName);
            projDesc.setName(projectName);
            
            
        } else {
        	// TODO: Handle existing conflict gracefully
        	throw new RuntimeException("Project " + projectName + " already exists in the workspace");
        }
                      
        project.open(null);
        
        monitor.worked(1);
        		        
        PythonNature.addNature(project, null);
        
        monitor.worked(2);
        
        // TODO: Create initial Python files here
                        		        
        return project;
    }
	
	
	
	
}
