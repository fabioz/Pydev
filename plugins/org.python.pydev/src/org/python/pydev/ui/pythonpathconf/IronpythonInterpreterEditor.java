/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.shared_core.utils.PlatformUtils;
import org.python.pydev.ui.pythonpathconf.IInterpreterProviderFactory.InterpreterType;

public class IronpythonInterpreterEditor extends AbstractInterpreterEditor {

    public IronpythonInterpreterEditor(String labelText, Composite parent, IInterpreterManager interpreterManager) {
        super(IInterpreterManager.IRONPYTHON_INTERPRETER_PATH, labelText, parent, interpreterManager);
    }

    @Override
    public String[] getInterpreterFilterExtensions() {
        if (PlatformUtils.isWindowsPlatform()) {
            return new String[] { "*.exe", "*.*" };
        }
        return null;
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        super.doFillIntoGrid(parent, numColumns);
        this.autoConfigButton.setToolTipText("Will try to find IronPython on the PATH (will fail if not available)");
    }

    @Override
    public InterpreterType getInterpreterType() {
        return InterpreterType.IRONPYTHON;
    }

}
