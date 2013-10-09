/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.jython;

import org.eclipse.jface.preference.IPreferenceStore;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.pythonpathconf.InterpreterGeneralPreferencesPage;

/**
 * Base class for code-completion on a workbench test.
 *
 * @author Fabio
 */
public abstract class AbstractJythonWorkbenchTests extends JythonCodeCompletionTestsBase {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Set Interpreter Configuration Auto to DONT_ASK. We can't have the
        // Python not configured dialog open in the tests as that causes the tests to hang
        IPreferenceStore store = PydevPlugin.getDefault().getPreferenceStore();
        store.setValue(InterpreterGeneralPreferencesPage.NOTIFY_NO_INTERPRETER_PY,
                false);
        store.setValue(InterpreterGeneralPreferencesPage.NOTIFY_NO_INTERPRETER_JY,
                false);
        store.setValue(InterpreterGeneralPreferencesPage.NOTIFY_NO_INTERPRETER_IP,
                false);

        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion();

    }

}
