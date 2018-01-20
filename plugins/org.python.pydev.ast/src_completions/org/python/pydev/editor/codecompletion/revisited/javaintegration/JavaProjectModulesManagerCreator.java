/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.javaintegration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.log.Log;


/**
 * Creator for modules manager.
 * 
 * @author fabioz
 */
public class JavaProjectModulesManagerCreator {

    /**
     * Flag indicating whether JDT is supported in this installation.
     */
    private static boolean JDTSupported = true;

    /**
     * This method will check the given project and if it's a java project, will create a project modules manager
     * that can be used to get things from it as we need in pydev.
     */
    public static IModulesManager createJavaProjectModulesManagerIfPossible(IProject project) {
        if (JDTSupported == false) {
            return null;
        }

        try {
            if (project.isOpen()) {
                IProjectNature nature = project.getNature(JavaCore.NATURE_ID);
                if (nature instanceof IJavaProject) {
                    IJavaProject javaProject = (IJavaProject) nature;
                    return new JavaProjectModulesManager(javaProject);
                }
            }
        } catch (Throwable e) {
            if (JythonModulesManagerUtils.isOptionalJDTClassNotFound(e)) {
                //ignore it at this point: we don't have JDT... set the static variable to it and don't even
                //try to get to this point again (no need to log it or anything).
                JDTSupported = false;
                return null;
            } else {
                Log.log(e);
            }
        }

        return null;
    }

}
