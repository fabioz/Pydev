package org.python.pydev.editor.codecompletion.revisited.jython;

import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;

/**
 * Base class for code-completion on a workbench test.
 *
 * @author Fabio
 */
public abstract class AbstractJythonWorkbenchTests extends JythonCodeCompletionTestsBase{

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion();

    }
    
}
