package org.python.pydev.plugin.nature;

import org.eclipse.core.resources.IProject;

/**
 * Plugins can contribute one of these if they wish to add implicit entries
 * to a project's classpath.
 */
public interface IPythonPathContributor
{
    /**
     * Returns the additional python path entries (for the given project)
     * separated by a | character.
     * 
     * @param aProject
     */
    public String getAdditionalPythonPath(IProject aProject);
}
