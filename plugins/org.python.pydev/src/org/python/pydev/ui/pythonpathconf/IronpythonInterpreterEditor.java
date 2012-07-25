/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;

public class IronpythonInterpreterEditor extends AbstractInterpreterEditor {

    public IronpythonInterpreterEditor(String labelText, Composite parent, IInterpreterManager interpreterManager) {
        super(IInterpreterManager.IRONPYTHON_INTERPRETER_PATH, labelText, parent, interpreterManager);
    }

    @Override
    public String[] getInterpreterFilterExtensions() {
        if (REF.isWindowsPlatform()) {
            return new String[] { "*.exe", "*.*" };
        }
        return null;
    }

    protected Tuple<String, String> getAutoNewInput() {
        return new Tuple<String, String>(getUniqueInterpreterName("ipy"), "ipy"); //This should be enough to find it from the PATH or any other way it's defined.
    }

    protected void doFillIntoGrid(Composite parent, int numColumns) {
        super.doFillIntoGrid(parent, numColumns);
        this.autoConfigButton.setToolTipText("Will try to find Iron Python on the PATH (will fail if not available)");
    }
}
