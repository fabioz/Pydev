/*
 * Created on Mar 9, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.plugin.PythonNature;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class CodeCompletionTestsBase extends TestCase {

    //NOTE: this should be gotten from some variable to point to the python lib (less system dependence, but still, some).
    public static final String PYTHON_INSTALL="C:/bin/Python23/";
    //NOTE: this should set to the tests pysrc location, so that it can be added to the pythonpath.
    public static final String TEST_PYSRC_LOC="D:/dev_programs/eclipse_3/eclipse/workspace/org.python.pydev/tests/pysrc/";

    public static void main(String[] args) {
        junit.textui.TestRunner.run(CodeCompletionTestsBase.class);
    }

	public PythonNature nature;

	/*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
	    nature = new PythonNature();
	    nature.setAstManager(new ASTManager());
	    ((ASTManager)nature.getAstManager()).changePythonPath(PYTHON_INSTALL+"lib|"+TEST_PYSRC_LOC, null, new NullProgressMonitor());
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
