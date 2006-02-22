/*
 * Created on 13/08/2005
 */
package org.python.pydev.editor.codecompletion.revisited.jython;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.python.copiedfromeclipsesrc.JavaVmLocationFinder;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;
import org.python.pydev.utils.ICallback;

public class JythonCodeCompletionTestsBase extends CodeCompletionTestsBase{
    
    protected boolean calledJavaExecutable = false;
    protected boolean calledJavaJars = false;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        //we also need to set from where the info on the java env
        JavaVmLocationFinder.callbackJavaExecutable = new ICallback(){
            public Object call(Object args) {
                calledJavaExecutable = true;
                return new File(TestDependent.JAVA_LOCATION);
            }
        };
        
        //and on the associated jars to the java runtime
        JavaVmLocationFinder.callbackJavaJars = new ICallback(){
            public Object call(Object args) {
                calledJavaJars = true;
                ArrayList<File> jars = new ArrayList<File>();
                jars.add(new File(TestDependent.JAVA_RT_JAR_LOCATION));
                return jars;
            }
        };
    }
    
    @Override
    protected void afterRestorSystemPythonPath(InterpreterInfo info) {
        super.afterRestorSystemPythonPath(info);
        assertTrue(calledJavaExecutable);
        assertTrue(calledJavaJars);
        
        boolean foundRtJar = false;
        for(Object lib: info.libs){
            String s = (String) lib;
            if(s.endsWith("rt.jar")){
                foundRtJar = true;
            }
        }
        assertTrue(foundRtJar);
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
        PydevPlugin.setJythonInterpreterManager(new JythonInterpreterManagerStub(preferences));
    }

    /**
     * @see #restorePythonPath(boolean)
     * 
     * same as the restorePythonPath function but also includes the site packages in the distribution
     */
    public void restorePythonPathWithSitePackages(boolean force){
        throw new RuntimeException("not available for jython");
    }


    /**
     * restores the pythonpath with the source library (system manager) and the source location for the tests (project manager)
     * 
     * @param force whether this should be forced, even if it was previously created for this class
     */
    public void restorePythonPath(boolean force){
        restoreSystemPythonPath(force, TestDependent.JYTHON_LIB_LOCATION+"|"+TestDependent.JAVA_RT_JAR_LOCATION);
        restoreProjectPythonPath(force, TestDependent.TEST_PYSRC_LOC);
        checkSize();
    }

}
