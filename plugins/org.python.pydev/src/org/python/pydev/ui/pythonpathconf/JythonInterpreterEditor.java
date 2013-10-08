/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 04/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.ui.pythonpathconf.IInterpreterProviderFactory.InterpreterType;

public class JythonInterpreterEditor extends AbstractInterpreterEditor {

    public JythonInterpreterEditor(String labelText, Composite parent, IInterpreterManager interpreterManager) {
        super(IInterpreterManager.JYTHON_INTERPRETER_PATH, labelText, parent, interpreterManager);
    }

    @Override
    public String[] getInterpreterFilterExtensions() {
        return new String[] { "*.jar", "*.*" };
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        super.doFillIntoGrid(parent, numColumns);
        this.autoConfigButton.setToolTipText("Will try to find Jython on the PATH (will fail if not available)");
    }

    @Override
    public InterpreterType getInterpreterType() {
        return InterpreterType.JYTHON;
    }

}