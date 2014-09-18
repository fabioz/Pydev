/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 9, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyCodeCompletion;
import org.python.pydev.editor.codecompletion.PyCodeCompletionUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevTestUtils;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.ui.BundleInfoStub;
import org.python.pydev.ui.interpreters.PythonInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;
import org.python.pydev.utils.PrintProgressMonitor;

/**
 * @author Fabio Zadrozny
 */
public class CodeCompletionTestsBase extends TestCase {

    public CodeCompletionTestsBase() {

    }

    public CodeCompletionTestsBase(String name) {
        super(name);
    }

    public static void main(String[] args) {
        //for single setup / teardown, check http://www.beust.com/weblog/archives/000082.html
        //(may be useful to get rid of the ThreadStreamReader threads)
        junit.textui.TestRunner.run(CodeCompletionTestsBase.class);
    }

    /**
     * We want to keep it initialized among runs from the same class.
     * Check the restorePythonPath function.
     */
    public static PythonNature nature;

    /**
     * Nature for the second project. 
     * 
     * This nature has the other nature as a dependency.
     */
    public static PythonNature nature2;

    /**
     * A map with the name of the project pointing to the last class that restored the
     * nature. This is done in this way because we don't want the nature to be recreated
     * all the time among tests from the same test case.
     */
    public static Map<String, Class<?>> restoredClass = new HashMap<String, Class<?>>();

    /**
     * Serves the same purpose that the restoredClass serves, but for the system 
     * python nature.
     */
    public static Class<?> restoredSystem;
    private PreferenceStore preferences;

    public PreferenceStore getPreferences() {
        if (this.preferences == null) {
            this.preferences = new PreferenceStore();
        }
        return this.preferences;
    }

    protected boolean ADD_MX_TO_FORCED_BUILTINS = true;
    protected boolean ADD_NUMPY_TO_FORCED_BUILTINS = true;

    /**
     * Whether we want to debug problems in this class.
     */
    protected static boolean DEBUG_TESTS_BASE = false;

    /*
     * @see TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        PydevPlugin.setBundleInfo(new BundleInfoStub());
        ProjectModulesManager.IN_TESTS = true;
        FileUtils.IN_TESTS = true;
        PydevTestUtils.setTestPlatformStateLocation();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        PydevPlugin.setBundleInfo(null);
        ProjectModulesManager.IN_TESTS = false;
        FileUtils.IN_TESTS = false;
    }

    /**
     * Backwards-compatibility interface
     */
    protected boolean restoreProjectPythonPath(boolean force, String path) {
        return restoreProjectPythonPath(force, path, getNameToCacheNature());
    }

    /**
     * Backwards-compatibility interface
     */
    protected boolean restoreProjectPythonPath2(boolean force, String path) {
        return restoreProjectPythonPath2(force, path, getNameToCacheNature2());
    }

    protected String getNameToCacheNature() {
        return "testProjectStub";
    }

    protected String getNameToCacheNature2() {
        return "testProjectStub2";
    }

    /**
     * A method that creates the default nature
     * 
     * @param force whether the creation of the new nature should be forced
     * @param path the pythonpath for the new nature
     * @param name the name for the project
     * @return true if the creation was needed and false if it wasn't
     */
    protected boolean restoreProjectPythonPath(boolean force, String path, String name) {
        PythonNature n = checkNewNature(name, force);
        if (n != null) {
            nature = n;
            ProjectStub projectStub = new ProjectStub(name, path, new IProject[0], new IProject[0]);

            setAstManager(path, projectStub, nature);
            return true;
        }
        return false;
    }

    /**
     * A method that creates a project that references the project from the 'default' nature
     * (and adds itself as a reference in the other project). 
     * 
     * @param force whether the creation of the new nature should be forced
     * @param path the pythonpath for the new nature
     * @param name the name for the project
     * @return true if the creation was needed and false if it wasn't
     */
    protected boolean restoreProjectPythonPath2(boolean force, String path, String name) {
        PythonNature n = checkNewNature(name, force);
        if (n != null) {
            nature2 = n;

            ProjectStub projectFromNature1 = (ProjectStub) nature.getProject();
            //create a new project referencing the first one
            ProjectStub projectFromNature2 = new ProjectStub(name, path, new IProject[] { projectFromNature1 },
                    new IProject[0]);

            //as we're adding a reference, we also have to set the referencing...
            projectFromNature1.referencingProjects = new IProject[] { projectFromNature2 };

            setAstManager(path, projectFromNature2, nature2);
            return true;
        }
        return false;
    }

    /**
     * Checks if we have to create a new nature for the given name
     * 
     * @param name the name of the project to be checked for the creation of the nature
     * @param force whether the creation of the new nature should be forced
     * @return the PythonNature created (if needed) or null if the creation was not needed
     */
    protected PythonNature checkNewNature(String name, boolean force) {
        Class<?> restored = CodeCompletionTestsBase.restoredClass.get(name);
        if (restored == null || restored != this.getClass() || force) {
            //cache
            CodeCompletionTestsBase.restoredClass.put(name, this.getClass());
            return createNature();
        }
        return null;
    }

    /**
     * This method sets the ast manager for a nature and restores the pythonpath
     * with the path passed
     * @param path the pythonpath that shoulb be set for this nature
     * @param projectStub the project where the nature should be set
     * @param pNature the nature we're interested in
     */
    protected void setAstManager(String path, ProjectStub projectStub, PythonNature pNature) {
        pNature.setProject(projectStub); //references the project 1
        projectStub.setNature(pNature);
        pNature.setAstManager(new ASTManager());

        ASTManager astManager = ((ASTManager) pNature.getAstManager());
        astManager.setNature(pNature);
        astManager.setProject(projectStub, pNature, false);
        astManager.changePythonPath(path, projectStub, getProgressMonitor());
    }

    /**
     * @return the pydev interpreter manager we are testing
     */
    protected IInterpreterManager getInterpreterManager() {
        return PydevPlugin.getPythonInterpreterManager();
    }

    protected static int GRAMMAR_TO_USE_FOR_PARSING = IPythonNature.LATEST_GRAMMAR_VERSION;

    /**
     * @return a PythonNature that is regarded as a python nature with the latest grammar.
     */
    public static PythonNature createStaticNature() {
        return new PythonNature() {
            @Override
            public int getInterpreterType() throws CoreException {
                return IInterpreterManager.INTERPRETER_TYPE_PYTHON;
            }

            @Override
            public int getGrammarVersion() {
                return GRAMMAR_TO_USE_FOR_PARSING;
            }
        };
    }

    /**
     * @return a nature that is python-specific
     */
    protected PythonNature createNature() {
        return createStaticNature();
    }

    /**
     * @return whether is was actually restored (given the force parameter)
     */
    protected boolean restoreSystemPythonPath(boolean force, String path) {
        if (restoredSystem == null || restoredSystem != this.getClass() || force) {
            //restore manager and cache
            setInterpreterManager(path);
            restoredSystem = this.getClass();

            //get default and restore the pythonpath
            InterpreterInfo info = getDefaultInterpreterInfo();
            this.beforeRestore(info);
            info.restoreCompiledLibs(getProgressMonitor());
            if (ADD_MX_TO_FORCED_BUILTINS) {
                info.addForcedLib("mx");
            }
            if (ADD_NUMPY_TO_FORCED_BUILTINS) {
                info.addForcedLib("numpy");
            }

            //postconditions
            afterRestorSystemPythonPath(info);
            return true;
        }
        return false;
    }

    /**
     * Give subclasses a chance to configure the interpreter info.
     */
    protected void beforeRestore(InterpreterInfo info) {

    }

    /**
     * @return the default interpreter info for the current manager
     */
    protected InterpreterInfo getDefaultInterpreterInfo() {
        IInterpreterManager iMan = getInterpreterManager();
        InterpreterInfo info;
        try {
            info = (InterpreterInfo) iMan.getDefaultInterpreterInfo(false);
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
        return info;
    }

    /**
     * @return a progress monitor
     */
    protected IProgressMonitor getProgressMonitor() {
        if (DEBUG_TESTS_BASE) {
            return new PrintProgressMonitor();
        }
        return new NullProgressMonitor();
    }

    /**
     * Sets the interpreter manager we should use
     * @param path 
     */
    protected void setInterpreterManager(String path) {
        PythonInterpreterManager interpreterManager = new PythonInterpreterManager(this.getPreferences());

        InterpreterInfo info;
        info = (InterpreterInfo) interpreterManager.createInterpreterInfo(TestDependent.PYTHON_EXE,
                new NullProgressMonitor(), false);
        TestDependent.PYTHON_EXE = info.executableOrJar;
        if (path != null) {
            info = new InterpreterInfo(info.getVersion(), info.executableOrJar,
                    PythonPathHelper.parsePythonPathFromStr(path, new ArrayList<String>()));
        }

        interpreterManager.setInfos(new IInterpreterInfo[] { info }, null, null);
        PydevPlugin.setPythonInterpreterManager(interpreterManager);
    }

    /**
     * @param info the information for the system manager that we just restored
     */
    protected void afterRestorSystemPythonPath(InterpreterInfo info) {
        nature = null; //has to be restored for the project, as we just restored the system pythonpath

        //ok, the system manager must be there
        assertTrue(info.getModulesManager().getSize(true) > 0);

        //and it must be registered as the pydev interpreter manager
        IInterpreterManager iMan2 = getInterpreterManager();
        InterpreterInfo info2;
        try {
            info2 = (InterpreterInfo) iMan2.getDefaultInterpreterInfo(false);
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
        assertTrue(info2 == info);

        //does it have the loaded modules?
        assertTrue(info2.getModulesManager().getSize(true) > 0);
        assertTrue(info2.getModulesManager().getBuiltins().length > 0);

    }

    /**
     * @see #restorePythonPath(boolean)
     * 
     * same as the restorePythonPath function but also includes the site packages in the distribution
     */
    public void restorePythonPath(String path, boolean force) {
        restoreSystemPythonPath(force, path);
        restoreProjectPythonPath(force, getProjectPythonpath());
        restoreProjectPythonPath2(force, getProjectPythonpathNature2());
        checkSize();
    }

    public String getProjectPythonpathNature2() {
        return TestDependent.TEST_PYSRC_LOC2;
    }

    /**
     * Note: subclasses may return a string with '|' as a separator. That way the source folder will be the first and the
     * remainders will be set as external source folders.
     */
    public String getProjectPythonpath() {
        return TestDependent.TEST_PYSRC_LOC;
    }

    /**
     * @see #restorePythonPath(boolean)
     * 
     * same as the restorePythonPath function but also includes the site packages in the distribution
     */
    public void restorePythonPathWithSitePackages(boolean force) {
        restoreSystemPythonPath(force, TestDependent.GetCompletePythonLib(true));
        restoreProjectPythonPath(force, getProjectPythonpath());
        restoreProjectPythonPath2(force, getProjectPythonpathNature2());
        checkSize();
    }

    /**
     * restores the pythonpath with the source library (system manager) and the source location for the tests (project manager)
     * 
     * @param force whether this should be forced, even if it was previously created for this class
     */
    public void restorePythonPath(boolean force) {
        if (DEBUG_TESTS_BASE) {
            System.out.println("-------------- Restoring system pythonpath");
        }
        restoreSystemPythonPath(force, TestDependent.GetCompletePythonLib(false));
        if (DEBUG_TESTS_BASE) {
            System.out.println("-------------- Restoring project pythonpath");
        }
        restoreProjectPythonPath(force, getProjectPythonpath());
        restoreProjectPythonPath2(force, getProjectPythonpathNature2());
        if (DEBUG_TESTS_BASE) {
            System.out.println("-------------- Checking size (for proj1 and proj2)");
        }

        checkSize();
    }

    public void restorePythonPathWithCustomSystemPath(boolean force, String systemPath) {
        if (DEBUG_TESTS_BASE) {
            System.out.println("-------------- Restoring system pythonpath");
        }
        restoreSystemPythonPath(force, systemPath);
        if (DEBUG_TESTS_BASE) {
            System.out.println("-------------- Restoring project pythonpath");
        }
        restoreProjectPythonPath(force, getProjectPythonpath());
        restoreProjectPythonPath2(force, getProjectPythonpathNature2());
        if (DEBUG_TESTS_BASE) {
            System.out.println("-------------- Checking size (for proj1 and proj2)");
        }

        checkSize();
    }

    /**
     * checks if the size of the system modules manager and the project moule manager are coherent
     * (we must have more modules in the system than in the project)
     */
    protected void checkSize() {
        try {
            IInterpreterManager iMan = getInterpreterManager();
            InterpreterInfo info = (InterpreterInfo) iMan.getDefaultInterpreterInfo(false);
            assertTrue(info.getModulesManager().getSize(true) > 0);

            int size = ((ASTManager) nature.getAstManager()).getSize();
            assertTrue("Interpreter size:" + info.getModulesManager().getSize(true)
                    + " should be smaller than project size:" + size + " "
                    + "(because it contains system+project info)", info.getModulesManager().getSize(true) < size);

            size = ((ASTManager) nature2.getAstManager()).getSize();
            assertTrue("Interpreter size:" + info.getModulesManager().getSize(true)
                    + " should be smaller than project size:" + size + " "
                    + "(because it contains system+project info)", info.getModulesManager().getSize(true) < size);
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public void testEmpty() {
        //just so that we don't get 'no tests found' warning
    }

    // ================================================================= helpers for doing code completion requests

    public IPyCodeCompletion codeCompletion;

    public ICompletionProposal[] requestCompl(String strDoc, int documentOffset, int returned, String[] retCompl)
            throws Exception {
        return requestCompl(strDoc, documentOffset, returned, retCompl, nature);
    }

    public ICompletionProposal[] requestCompl(String strDoc, int documentOffset, int returned, String[] retCompl,
            PythonNature nature) throws Exception {
        return requestCompl(null, strDoc, documentOffset, returned, retCompl, nature);
    }

    public ICompletionProposal[] requestCompl(File file, int documentOffset, int returned, String[] retCompl)
            throws Exception {
        String strDoc = FileUtils.getFileContents(file);
        return requestCompl(file, strDoc, documentOffset, returned, retCompl);
    }

    public ICompletionProposal[] requestCompl(File file, String strDoc, int documentOffset, int returned,
            String[] retCompl) throws Exception {
        return requestCompl(file, strDoc, documentOffset, returned, retCompl, nature);
    }

    /**
     * make a request for a code completion
     * 
     * @param the file where we are doing the completion
     * @param strDoc the document requesting the code completion
     * @param documentOffset the offset of the document (if -1, the doc length is used)
     * @param returned the number of completions expected (if -1 not tested)
     * @param retCompl a string array specifying the expected completions that should be contained (may only be a 
     * subset of all completions.
     * @return 
     * 
     * @throws CoreException
     * @throws BadLocationException
     * @throws MisconfigurationException 
     */
    public ICompletionProposal[] requestCompl(File file, String strDoc, int documentOffset, int returned,
            String[] retCompl, PythonNature nature) throws Exception, MisconfigurationException {
        if (documentOffset == -1) {
            documentOffset = strDoc.length();
        }

        IDocument doc = new Document(strDoc);
        CompletionRequest request = new CompletionRequest(file, nature, doc, documentOffset, codeCompletion);

        List<Object> props = codeCompletion.getCodeCompletionProposals(null, request);
        ICompletionProposal[] codeCompletionProposals = PyCodeCompletionUtils.onlyValidSorted(props, request.qualifier,
                request.isInCalltip);

        for (int i = 0; i < retCompl.length; i++) {
            assertContains(retCompl[i], codeCompletionProposals);
        }

        if (returned > -1) {
            StringBuffer buffer = getAvailableAsStr(codeCompletionProposals);
            assertEquals("Expected " + returned + " received: " + codeCompletionProposals.length + "\n" + buffer,
                    returned, codeCompletionProposals.length);
        }
        return codeCompletionProposals;
    }

    /**
     * If this method does not find the completion we're looking for, it throws
     * a failure exception.
     * 
     * @param string the string we're looking for 
     * @param codeCompletionProposals the proposals found
     */
    public static ICompletionProposal assertContains(String string, ICompletionProposal[] codeCompletionProposals) {
        for (int i = 0; i < codeCompletionProposals.length; i++) {
            ICompletionProposal completionProposal = codeCompletionProposals[i];
            if (checkIfEquals(string, completionProposal)) {
                return completionProposal;
            }
        }
        StringBuffer buffer = getAvailableAsStr(codeCompletionProposals);

        fail("The string >>" + string + "<< was not found in the returned completions.\nAvailable:\n" + buffer);
        return null;
    }

    /**
     * If this method does not find the completion we're looking for, it throws
     * a failure exception.
     * 
     * @param string the string we're looking for 
     * @param codeCompletionProposals the proposals found
     */
    protected void assertNotContains(String string, ICompletionProposal[] codeCompletionProposals) {
        for (int i = 0; i < codeCompletionProposals.length; i++) {
            ICompletionProposal completionProposal = codeCompletionProposals[i];
            if (checkIfEquals(string, completionProposal)) {
                fail("The string >>" + string
                        + "<< was found in the returned completions (was not expected to be found).");
            }
        }
    }

    /**
     * Checks if the completion we're looking for is the same completion we're analyzing.
     * 
     * @param lookingFor this is the completion we are looking for
     * @param completionProposal this is the completion proposal
     * @return if the completion we're looking for is the same completion we're checking
     */
    protected static boolean checkIfEquals(String lookingFor, ICompletionProposal completionProposal) {
        return completionProposal.getDisplayString().equals(lookingFor);
    }

    /**
     * @return StringBuffer with a string representing the array of proposals found.
     */
    protected static StringBuffer getAvailableAsStr(ICompletionProposal[] codeCompletionProposals) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < codeCompletionProposals.length; i++) {
            buffer.append(codeCompletionProposals[i].getDisplayString());
            buffer.append("\n");
        }
        return buffer;
    }

    public ICompletionProposal[] requestCompl(String strDoc, String[] retCompl) throws Exception {
        return requestCompl(strDoc, -1, retCompl.length, retCompl);
    }

    public ICompletionProposal[] requestCompl(String strDoc, int expectedCompletions, String[] retCompl)
            throws Exception {
        return requestCompl(strDoc, -1, expectedCompletions, retCompl);
    }

    public ICompletionProposal[] requestCompl(String strDoc, String retCompl) throws Exception {
        return requestCompl(strDoc, new String[] { retCompl });
    }

    public static void assertContains(List<String> found, String toFind) {
        for (String str : found) {
            if (str.equals(toFind)) {
                return;
            }
        }
        fail("The string " + toFind + " was not found amongst the passed strings.");
    }

    public static void assertContains(Map found, Object toFind) {
        if (found.containsKey(toFind)) {
            return;
        }

        FastStringBuffer available = new FastStringBuffer();
        for (Object o : found.keySet()) {
            available.append(o.toString());
            available.append('\n');
        }
        fail(StringUtils.format("Object: %s not found. Available:\n%s", toFind,
                available));
    }

}
