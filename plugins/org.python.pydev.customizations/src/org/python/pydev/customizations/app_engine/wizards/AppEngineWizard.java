package org.python.pydev.customizations.app_engine.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.ICallback;
import org.python.pydev.plugin.PyStructureConfigHelpers;
import org.python.pydev.ui.wizards.project.PythonProjectWizard;

/**
 * Wizard that helps in the creation of a Pydev project configured for Google App Engine. 
 * 
 * @author Fabio
 */
public class AppEngineWizard extends PythonProjectWizard{

    private AppEngineConfigWizardPage appEngineConfigWizardPage;

    /**
     * Add wizard pages to the instance
     * 
     * @see org.eclipse.jface.wizard.IWizard#addPages()
     */
    public void addPages(){
        addPage(projectPage);

        appEngineConfigWizardPage = new AppEngineConfigWizardPage("Goole App Engine Page");
        appEngineConfigWizardPage.setTitle("Google App Engine");
        appEngineConfigWizardPage.setDescription("Set Google App Engine Configuration");
        addPage(appEngineConfigWizardPage);

        addProjectReferencePage();
    }

    /**
     * Overridden to add the external source folders from google app engine. 
     */
    protected void createAndConfigProject(final IProject newProjectHandle, final IProjectDescription description,
            final String projectType, final String projectInterpreter, IProgressMonitor monitor) throws CoreException{
        ICallback<List<IFolder>, IProject> getSourceFolderHandlesCallback = new ICallback<List<IFolder>, IProject>(){

            public List<IFolder> call(IProject projectHandle){
                if(projectPage.shouldCreatSourceFolder()){
                    IFolder folder = projectHandle.getFolder("src");
                    List<IFolder> ret = new ArrayList<IFolder>();
                    ret.add(folder);
                    return ret;
                }
                return null;
            }
        };

        ICallback<List<String>, IProject> getExternalSourceFolderHandlesCallback = new ICallback<List<String>, IProject>(){
        
            public List<String> call(IProject projectHandle){
                return appEngineConfigWizardPage.getExternalSourceFolders();
            }
        };
        
        PyStructureConfigHelpers.createPydevProject(description, newProjectHandle, monitor, projectType,
                projectInterpreter, getSourceFolderHandlesCallback, getExternalSourceFolderHandlesCallback);
    }

}
