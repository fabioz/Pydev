/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.customizations.app_engine.actions;

import java.io.File;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.jface.dialogs.MessageDialog;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.customizations.app_engine.launching.AppEngineConstants;
import org.python.pydev.customizations.common.CustomizationCommons;
import org.python.pydev.shared_ui.EditorUtils;


/**
 * Abstract class for classes that are executed based on the app engine / target folder.
 */
public abstract class AbstractAppEngineHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        throw new RuntimeException("Not used anymore!");
        //Note: HandlerUtil is not available in eclipse 3.2
        //        ISelection sel = HandlerUtil.getCurrentSelectionChecked(event);
        //        if(sel instanceof IStructuredSelection){
        //            IStructuredSelection selection = (IStructuredSelection) sel;
        //            Object firstElement = selection.getFirstElement();
        //            
        //            return executeInObject(firstElement);
        //
        //        }
        //        return null;
    }

    public Object executeInObject(Object firstElement) {
        IContainer container = CustomizationCommons.getContainerFromObject(firstElement);
        if (container == null) {
            return null;
        }

        IPythonPathNature pythonPathNature = CustomizationCommons.getPythonPathNatureFromObject(firstElement);
        if (pythonPathNature == null) {
            return null;
        }

        Map<String, String> variableSubstitution;
        try {
            variableSubstitution = pythonPathNature.getVariableSubstitution();
            //Only consider a google app engine a project that has a google app engine variable!
            if (variableSubstitution == null
                    || !variableSubstitution.containsKey(AppEngineConstants.GOOGLE_APP_ENGINE_VARIABLE)) {
                return null;
            }

            File appEngineLocation = new File(variableSubstitution.get(AppEngineConstants.GOOGLE_APP_ENGINE_VARIABLE));
            if (!appEngineLocation.isDirectory()) {
                MessageDialog.openError(EditorUtils.getShell(), "Error", "Expected: " + appEngineLocation
                        + " to be a directory.");
                return null;
            }

            File appcfg = new File(appEngineLocation, "appcfg.py");
            if (!appcfg.isFile()) {
                MessageDialog.openError(EditorUtils.getShell(), "Error", "Expected: " + appcfg + " to be a file.");
                return null;
            }

            handleExecution(container, pythonPathNature, appcfg, appEngineLocation);

        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }

    /**
     * Subclasses should override this method to properly handle the execution of the action (this is called when
     * all things are already validated).
     */
    protected abstract void handleExecution(IContainer container, IPythonPathNature pythonPathNature, File appcfg,
            File appEngineLocation);

}
