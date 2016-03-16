/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.customizations.app_engine.launching;

import java.util.Map;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.customizations.common.CustomizationCommons;


/**
 * Test to check if a given container can be run from google app engine.
 * 
 * @author Fabio
 */
public class AppEnginePropertyTester extends PropertyTester {

    /**
     * Expected value is ignored.
     * 
     * Considers as available for the run a container of a project with the GOOGLE_APP_ENGINE variable
     * declared in it and has a app.yaml or app.yml under it.
     */
    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        IContainer container = CustomizationCommons.getContainerFromObject(receiver);
        if (container == null) {
            return false;
        }

        IPythonPathNature nature = CustomizationCommons.getPythonPathNatureFromObject(receiver);
        if (nature == null) {
            return false;
        }

        //dev_appserver.py [options] <application root>
        //
        //Application root must be the path to the application to run in this server.
        //Must contain a valid app.yaml or app.yml file.
        IFile file = container.getFile(new Path("app.yaml"));
        if (file == null || !file.exists()) {
            file = container.getFile(new Path("app.yml"));
            if (file == null || !file.exists()) {
                return false;
            }
        }

        try {
            Map<String, String> variableSubstitution = nature.getVariableSubstitution();
            //Only consider a google app engine a project that has a google app engine variable!
            if (variableSubstitution != null
                    && variableSubstitution.containsKey(AppEngineConstants.GOOGLE_APP_ENGINE_VARIABLE)) {
                return true;
            }
        } catch (Exception e) {
            Log.log(e);
        }

        return false;
    }

}
