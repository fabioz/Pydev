/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 08/08/2005
 */
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.shared_core.utils.PlatformUtils;

public class PythonInterpreterEditor extends AbstractInterpreterEditor {

    public PythonInterpreterEditor(String labelText, Composite parent, IInterpreterManager interpreterManager) {
        super(IInterpreterManager.PYTHON_INTERPRETER_PATH, labelText, parent, interpreterManager);
    }

    @Override
    public String[] getInterpreterFilterExtensions() {
        if (PlatformUtils.isWindowsPlatform()) {
            return new String[] { "*.exe", "*.*" };
        }
        return null;
    }

    @Override
    public IInterpreterProviderFactory.InterpreterType getInterpreterType() {
        return IInterpreterProviderFactory.InterpreterType.PYTHON;
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        super.doFillIntoGrid(parent, numColumns);
        this.autoConfigButton.setToolTipText("Will try to find Python on the PATH (will fail if not available)");
    }

}
