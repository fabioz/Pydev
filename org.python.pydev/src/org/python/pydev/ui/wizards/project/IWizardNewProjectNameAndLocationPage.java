/**
 * 
 */
package org.python.pydev.ui.wizards.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.wizard.IWizardPage;

/**
 * The first page in the New Project wizard must implement this interface.
 */
public interface IWizardNewProjectNameAndLocationPage extends IWizardPage
{
    /**
     * Returns a flag indicating whether the default python src folder
     * should be created.
     */
    public boolean shouldCreatSourceFolder();

    /**
     * Returns the project type.
     */
    public String getProjectType();

    /**
     * Returns a handle to the new project.
     */
    public IProject getProjectHandle();

    /**
     * Gets the location path for the new project.
     */
    public IPath getLocationPath();

    /**
     * @return "Default" to mean that the default interpreter should be used or the complete path to an interpreter
     * configured.
     * 
     * Note that this changes from the python nature, where only the path is returned (because at this point, we
     * want to give the user a visual indication that it's the Default interpreter if that's the one selected)
     */
    public String getProjectInterpreter();

}
