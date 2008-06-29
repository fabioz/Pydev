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

}
