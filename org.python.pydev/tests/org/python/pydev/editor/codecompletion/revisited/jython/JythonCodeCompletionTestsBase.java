/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion.revisited.jython;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.python.copiedfromeclipsesrc.JavaVmLocationFinder;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.PythonInterpreterManagerStub;
import org.python.pydev.editor.codecompletion.revisited.TestDependent;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.interpreters.IInterpreterManager;
import org.python.pydev.utils.ICallback;

public class JythonCodeCompletionTestsBase extends CodeCompletionTestsBase{
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        //we also need to set from where the info on the java env
        JavaVmLocationFinder.callbackJavaExecutable = new ICallback(){
            public Object call(Object args) {
                return new File(TestDependent.JAVA_LOCATION);
            }
        };
        
        //and on the associated jars to the java runtime
        JavaVmLocationFinder.callbackJavaJars = new ICallback(){
            public Object call(Object args) {
                ArrayList<File> jars = new ArrayList<File>();
                jars.add(new File(TestDependent.JAVA_RT_JAR_LOCATION));
                return jars;
            }
        };
    }

    @Override
    protected PythonNature createNature() {
        return new PythonNature(){
            @Override
            public boolean isJython() throws CoreException {
                return true;
            }
            @Override
            public boolean isPython() throws CoreException {
                return false;
            }
        };
    }
    
    @Override
    protected IInterpreterManager getInterpreterManager() {
        return PydevPlugin.getJythonInterpreterManager();
    }
    
    @Override
    protected void setInterpreterManager() {
        PydevPlugin.setJythonInterpreterManager(new PythonInterpreterManagerStub(preferences));
    }
    
}
