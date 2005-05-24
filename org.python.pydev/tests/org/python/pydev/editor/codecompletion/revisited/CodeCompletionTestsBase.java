/*
 * Created on Mar 9, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.python.pydev.plugin.BundleInfo;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PythonNature;
import org.python.pydev.ui.BundleInfoStub;
import org.python.pydev.ui.IInterpreterManager;
import org.python.pydev.ui.InterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * @author Fabio Zadrozny
 */
public class CodeCompletionTestsBase extends TestCase {

    private static class InterpreterManagerStub extends InterpreterManager implements IInterpreterManager {

        public InterpreterManagerStub(Preferences prefs) {
            super(prefs);
        }

        public String getDefaultInterpreter() {
            return PYTHON_EXE;
        }

        public String[] getInterpreters() {
            return new String[]{PYTHON_EXE};
        }

        public String addInterpreter(String executable, IProgressMonitor monitor) {
            throw new RuntimeException("not impl");
        }

        public String[] getInterpretersFromPersistedString(String persisted) {
            throw new RuntimeException("not impl");
        }

        public String getStringToPersist(String[] executables) {
            throw new RuntimeException("not impl");
        }
        
        /**
         * @see org.python.pydev.ui.IInterpreterManager#getInterpreterInfo(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
         */
        public InterpreterInfo getInterpreterInfo(String executable, IProgressMonitor monitor) {
            InterpreterInfo info = super.getInterpreterInfo(executable, monitor);
            PYTHON_EXE = info.executable;
            return info;
        }
    }


    //NOTE: this should be gotten from some variable to point to the python lib (less system dependence, but still, some).
    public static String PYTHON_EXE="C:/bin/Python24/python.exe";
    public static final String PYTHON_INSTALL="C:/bin/Python24/";
    public static final String PYTHON_LIB="C:/bin/Python24/lib/";
    public static final String PYTHON_SITE_PACKAGES="C:/bin/Python24/Lib/site-packages/";
    
    //NOTE: this should set to the tests pysrc location, so that it can be added to the pythonpath.
    public static final String TEST_PYSRC_LOC="D:/dev_programs/eclipse_3/eclipse/workspace/org.python.pydev/tests/pysrc/";

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
            restored = this.getClass();
    	    nature = new PythonNature();
    	    nature.setAstManager(new ASTManager());
    	    ((ASTManager)nature.getAstManager()).changePythonPath(path, null, new NullProgressMonitor());
        }
    }
    
    private void restoreSystemPythonPath(boolean force, String path){
        if(restoredSystem == null || restoredSystem != this.getClass() || force){
            PydevPlugin.setInterpreterManager(new InterpreterManagerStub(preferences));
            restoredSystem = this.getClass();
            
            IInterpreterManager iMan = PydevPlugin.getInterpreterManager();
            InterpreterInfo info = iMan.getDefaultInterpreterInfo(new NullProgressMonitor());
            info.restorePythonpath(path);
            nature = null; //has to be restored
            assertTrue(info.modulesManager.getSize() > 0);

            IInterpreterManager iMan2 = PydevPlugin.getInterpreterManager();
            InterpreterInfo info2 = iMan2.getDefaultInterpreterInfo(new NullProgressMonitor());
	        assertTrue(info2 == info);
	        assertTrue(info2.modulesManager.getSize() > 0);

        }
    }
    
    public void restorePythonPathWithSitePackages(boolean force){
        restoreSystemPythonPath(force, PYTHON_LIB+"|"+PYTHON_SITE_PACKAGES);
        restoreProjectPythonPath(force, TEST_PYSRC_LOC);
        checkSize();
    }


    public void restorePythonPath(boolean force){
        restoreSystemPythonPath(force, PYTHON_LIB);
        restoreProjectPythonPath(force, TEST_PYSRC_LOC);
        checkSize();
    }
    
    /**
     * 
     */
    private void checkSize() {
        IInterpreterManager iMan = PydevPlugin.getInterpreterManager();
        InterpreterInfo info = iMan.getDefaultInterpreterInfo(new NullProgressMonitor());
        int size = ((ASTManager)nature.getAstManager()).getSize();
        assertTrue(info.modulesManager.getSize() > 0);
        assertTrue(info.modulesManager.getSize() < size);
    }
   


}
