/*
 * Created on 08/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.plugin.PydevPlugin;

public class StuctureCreationTest extends CodeCompletionTestsBase{
    
    private InterpreterObserver observer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.restorePythonPath(false);
        observer = new InterpreterObserver();
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    @Override
    protected void restoreSystemPythonPath(boolean force, String path) {
        super.restoreSystemPythonPath(force, path);
        IProgressMonitor monitor = new NullProgressMonitor();
        observer.notifySystemPythonpathRestored(PydevPlugin.getPythonInterpreterManager().getDefaultInterpreterInfo(monitor), path, monitor);
    }
    
    @Override
    protected void restoreProjectPythonPath(boolean force, String path) {
        super.restoreProjectPythonPath(force, path);
        observer.notifyProjectPythonpathRestored(nature);
    }
    
    public void testSetup() {
        
    }

}
