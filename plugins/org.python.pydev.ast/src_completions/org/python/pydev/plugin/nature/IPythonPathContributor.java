/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.nature;

import org.eclipse.core.resources.IProject;

/**
 * Plugins can contribute one of these if they wish to add implicit entries
 * to a project's classpath.
 */
public interface IPythonPathContributor {
    /**
     * Returns the additional python path entries (for the given project)
     * separated by a | character.
     * 
     * @param aProject
     */
    public String getAdditionalPythonPath(IProject aProject);
}
