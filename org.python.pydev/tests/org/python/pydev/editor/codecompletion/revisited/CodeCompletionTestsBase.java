/*
 * Created on Mar 9, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.python.pydev.plugin.BundleInfo;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.BundleInfoStub;
import org.python.pydev.ui.interpreters.IInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * @author Fabio Zadrozny
 */
public class CodeCompletionTestsBase extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CodeCompletionTestsBase.class);
    }

    /**
     * We want to keep it initialized among runs from the same class.
     * Check the restorePythonPath function.
     */
	public static PythonNature nature;
	public static Class restored;
	public static Class restoredSystem;
	public Preferences preferences;

	/*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        BundleInfo.setBundleInfo(new BundleInfoStub());
        preferences = new Preferences();
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        BundleInfo.setBundleInfo(null);
    }
    
    private void restoreProjectPythonPath(boolean force, String path){
        if(restored == null || restored != this.getClass() || force){
            //cache
            restored = this.getClass();
            nature = createPythonLikeNature();
    	    nature.setAstManager(new ASTManager());
    	    

            IInterpreterManager iMan = PydevPlugin.getPythonInterpreterManager();
            InterpreterInfo info = iMan.getDefaultInterpreterInfo(new NullProgressMonitor());
    	    ASTManager astManager = ((ASTManager)nature.getAstManager());
            astManager.setNature(nature);
            astManager.changePythonPath(path, null, new NullProgressMonitor());
        }
    }

    /**
     * @return a nature that is python-specific
     */
    protected PythonNature createPythonLikeNature() {
        return new PythonNature(){
            @Override
            public boolean isJython() throws CoreException {
                return false;
            }
            @Override
            public boolean isPython() throws CoreException {
                return true;
            }
        };
    }
    
    private void restoreSystemPythonPath(boolean force, String path){
        if(restoredSystem == null || restoredSystem != this.getClass() || force){
            //restore manager and cache
            PydevPlugin.setPythonInterpreterManager(new InterpreterManagerStub(preferences));
            restoredSystem = this.getClass();
            
            //get default and restore the pythonpath
            IInterpreterManager iMan = PydevPlugin.getPythonInterpreterManager();
            InterpreterInfo info = iMan.getDefaultInterpreterInfo(new NullProgressMonitor());
            info.restoreCompiledLibs(new NullProgressMonitor());
            info.restorePythonpath(path, new NullProgressMonitor());

            //postconditions
            afterRestorSystemPythonPath(info);

        }
    }
    
    
    /**
     * @param info
     */
    private void afterRestorSystemPythonPath(InterpreterInfo info) {
        nature = null; //has to be restored
        assertTrue(info.modulesManager.getSize() > 0);

        IInterpreterManager iMan2 = PydevPlugin.getPythonInterpreterManager();
        InterpreterInfo info2 = iMan2.getDefaultInterpreterInfo(new NullProgressMonitor());
        assertTrue(info2 == info);
        assertTrue(info2.modulesManager.getSize() > 0);
        assertTrue(info2.modulesManager.getBuiltins().length > 0);
        
    }

    public void restorePythonPathWithSitePackages(boolean force){
        restoreSystemPythonPath(force, TestDependent.PYTHON_LIB+"|"+TestDependent.PYTHON_SITE_PACKAGES);
        restoreProjectPythonPath(force, TestDependent.TEST_PYSRC_LOC);
        checkSize();
    }


    public void restorePythonPath(boolean force){
        restoreSystemPythonPath(force, TestDependent.PYTHON_LIB);
        restoreProjectPythonPath(force, TestDependent.TEST_PYSRC_LOC);
        checkSize();
    }
    
    /**
     * 
     */
    private void checkSize() {
        IInterpreterManager iMan = PydevPlugin.getPythonInterpreterManager();
        InterpreterInfo info = iMan.getDefaultInterpreterInfo(new NullProgressMonitor());
        int size = ((ASTManager)nature.getAstManager()).getSize();
        assertTrue(info.modulesManager.getSize() > 0);
        assertTrue(""+info.modulesManager.getSize()+" "+size , info.modulesManager.getSize() < size );
    }
   


}
