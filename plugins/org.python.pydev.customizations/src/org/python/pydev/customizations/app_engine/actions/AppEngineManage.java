/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.customizations.app_engine.actions;

import java.io.File;

import org.eclipse.core.resources.IContainer;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.customizations.app_engine.util.AppEngineProcessWindow;
import org.python.pydev.shared_ui.EditorUtils;

/**
 * Just opens a dialog where the user can manage things.
 */
public class AppEngineManage extends AbstractAppEngineHandler {

    @Override
    protected void handleExecution(IContainer container, IPythonPathNature pythonPathNature, File appcfg,
            File appEngineLocation) {
        AppEngineProcessWindow processWindow = new AppEngineProcessWindow(EditorUtils.getShell());
        processWindow.setParameters(container, pythonPathNature, appcfg, appEngineLocation);
        processWindow.open();
    }

}