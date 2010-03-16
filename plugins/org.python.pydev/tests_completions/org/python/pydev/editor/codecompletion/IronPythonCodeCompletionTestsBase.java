package org.python.pydev.editor.codecompletion;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.IronpythonInterpreterManagerStub;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

public class IronPythonCodeCompletionTestsBase extends CodeCompletionTestsBase{

    protected boolean isInTestFindDefinition = false;

    @Override
    protected IInterpreterManager getInterpreterManager() {
        return PydevPlugin.getIronpythonInterpreterManager();
    }
    
    @Override
    protected void setInterpreterManager() {
        PydevPlugin.setIronpythonInterpreterManager(new IronpythonInterpreterManagerStub(this.getPreferences()));
    }
    
    
    @Override
    protected PythonNature createNature() {
        return new PythonNature(){
            @Override
            public int getInterpreterType() throws CoreException {
                return IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON;
            }
            @Override
            public int getGrammarVersion() {
                return IPythonNature.LATEST_GRAMMAR_VERSION;
            }
            
            @Override
            public String resolveModule(File file) throws MisconfigurationException {
                if(isInTestFindDefinition){
                    return null;
                }
                return super.resolveModule(file);
            }
        };
    }
    
    @Override
    public void setUp() throws Exception {
    	super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(TestDependent.IRONPYTHON_LIB, false);
    }

    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        super.tearDown();
    }
}
