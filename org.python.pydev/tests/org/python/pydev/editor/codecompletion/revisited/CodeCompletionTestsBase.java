/*
 * Created on Mar 9, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.plugin.BundleInfo;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.BundleInfoStub;
import org.python.pydev.ui.interpreters.IInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;
import org.python.pydev.utils.PrintProgressMonitor;

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
    protected static boolean DEBUG_TESTS_BASE = false;

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
    
    protected boolean restoreProjectPythonPath(boolean force, String path){
        if(restored == null || restored != this.getClass() || force){
            //cache
            restored = this.getClass();
            nature = createNature();
    	    nature.setAstManager(new ASTManager());
    	    
    	    ASTManager astManager = ((ASTManager)nature.getAstManager());
            astManager.setNature(nature);
            astManager.changePythonPath(path, null, getProgressMonitor());
            return true;
        }
        return false;
    }

    /**
     * @return the pydev interpreter manager we are testing
     */
    protected IInterpreterManager getInterpreterManager() {
        return PydevPlugin.getPythonInterpreterManager();
    }

    /**
     * @return a nature that is python-specific
     */
    protected PythonNature createNature() {
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
    
    /**
     * @return whether is was actually restored (given the force parameter)
     */
    protected boolean restoreSystemPythonPath(boolean force, String path){
        if(restoredSystem == null || restoredSystem != this.getClass() || force){
            //restore manager and cache
            setInterpreterManager();
            restoredSystem = this.getClass();
            
            //get default and restore the pythonpath
            InterpreterInfo info = getDefaultInterpreterInfo();
            info.restoreCompiledLibs(getProgressMonitor());
            info.restorePythonpath(path, getProgressMonitor());

            //postconditions
            afterRestorSystemPythonPath(info);
            return true;
        }
        return false;
    }

    /**
     * @return the default interpreter info for the current manager
     */
    protected InterpreterInfo getDefaultInterpreterInfo() {
        IInterpreterManager iMan = getInterpreterManager();
        InterpreterInfo info = iMan.getDefaultInterpreterInfo(getProgressMonitor());
        return info;
    }

    /**
     * @return a progress monitor
     */
    private IProgressMonitor getProgressMonitor() {
        if (DEBUG_TESTS_BASE){
            return new PrintProgressMonitor();
        }
        return new NullProgressMonitor();
    }

    /**
     * Sets the interpreter manager we should use
     */
    protected void setInterpreterManager() {
        PydevPlugin.setPythonInterpreterManager(new PythonInterpreterManagerStub(preferences));
    }
    
    
    /**
     * @param info the information for the system manager that we just restored
     */
    protected void afterRestorSystemPythonPath(InterpreterInfo info) {
        nature = null; //has to be restored for the project, as we just restored the system pythonpath
        
        //ok, the system manager must be there
        assertTrue(info.modulesManager.getSize() > 0);

        //and it must be registered as the pydev interpreter manager
        IInterpreterManager iMan2 = getInterpreterManager();
        InterpreterInfo info2 = iMan2.getDefaultInterpreterInfo(getProgressMonitor());
        assertTrue(info2 == info);
        
        //does it have the loaded modules?
        assertTrue(info2.modulesManager.getSize() > 0);
        assertTrue(info2.modulesManager.getBuiltins().length > 0);
        
    }

    /**
     * @see #restorePythonPath(boolean)
     * 
     * same as the restorePythonPath function but also includes the site packages in the distribution
     */
    public void restorePythonPath(String path, boolean force){
        restoreSystemPythonPath(force, path);
        restoreProjectPythonPath(force, TestDependent.TEST_PYSRC_LOC);
        checkSize();
    }
    
    /**
     * @see #restorePythonPath(boolean)
     * 
     * same as the restorePythonPath function but also includes the site packages in the distribution
     */
    public void restorePythonPathWithSitePackages(boolean force){
        restoreSystemPythonPath(force, TestDependent.PYTHON_LIB+"|"+TestDependent.PYTHON_SITE_PACKAGES);
        restoreProjectPythonPath(force, TestDependent.TEST_PYSRC_LOC);
        checkSize();
    }


    /**
     * restores the pythonpath with the source library (system manager) and the source location for the tests (project manager)
     * 
     * @param force whether this should be forced, even if it was previously created for this class
     */
    public void restorePythonPath(boolean force){
        if(DEBUG_TESTS_BASE){
            System.out.println("-------------- Restoring system pythonpath");
        }
        restoreSystemPythonPath(force, TestDependent.PYTHON_LIB);
        if(DEBUG_TESTS_BASE){
            System.out.println("-------------- Restoring project pythonpath");
        }
        restoreProjectPythonPath(force, TestDependent.TEST_PYSRC_LOC);
        if(DEBUG_TESTS_BASE){
            System.out.println("-------------- Checking size");
        }
        checkSize();
    }
    
    /**
     * checks if the size of the system modules manager and the project moule manager are coherent
     * (we must have more modules in the system than in the project)
     */
    protected void checkSize() {
        IInterpreterManager iMan = getInterpreterManager();
        InterpreterInfo info = iMan.getDefaultInterpreterInfo(getProgressMonitor());
        int size = ((ASTManager)nature.getAstManager()).getSize();
        assertTrue(info.modulesManager.getSize() > 0);
        assertTrue(""+info.modulesManager.getSize()+" "+size , info.modulesManager.getSize() < size );
    }
   


    
    
    
    
    public void testEmpty() {
        //just so that we don't get 'no tests found' warning
    }
    
    
    
    
    
    // ================================================================= helpers for doing code completion requests
    
    protected PyCodeCompletion codeCompletion;
    
    public void requestCompl(String strDoc, int documentOffset, int returned, String []retCompl) throws CoreException, BadLocationException{
        requestCompl(null, strDoc, documentOffset, returned, retCompl);
    }
    
    public void requestCompl(File file, int documentOffset, int returned, String []retCompl) throws CoreException, BadLocationException{
        String strDoc = REF.getFileContents(file);
        requestCompl(file, strDoc, documentOffset, returned, retCompl);
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
     * 
     * @throws CoreException
     * @throws BadLocationException
     */
    public void requestCompl(File file, String strDoc, int documentOffset, int returned, String []retCompl) throws CoreException, BadLocationException{
        if(documentOffset == -1)
            documentOffset = strDoc.length();
        
        IDocument doc = new Document(strDoc);
        CompletionRequest request = new CompletionRequest(file, nature, doc, documentOffset, codeCompletion);

        List props = codeCompletion.getCodeCompletionProposals(null, request);
        ICompletionProposal[] codeCompletionProposals = codeCompletion.onlyValidSorted(props, request.qualifier);
        
        
        for (int i = 0; i < retCompl.length; i++) {
            assertContains(retCompl[i], codeCompletionProposals);
        }

        if(returned > -1){
            StringBuffer buffer = getAvailableAsStr(codeCompletionProposals);
            assertEquals("Expected "+returned+" received: "+codeCompletionProposals.length+"\n"+buffer, returned, codeCompletionProposals.length);
        }
    }
    
    /**
     * @param string
     * @param codeCompletionProposals
     */
    protected void assertContains(String string, ICompletionProposal[] codeCompletionProposals) {
        for (int i = 0; i < codeCompletionProposals.length; i++) {
            ICompletionProposal completionProposal = codeCompletionProposals[i];
            if(checkIfEquals(string, completionProposal)){
                return ;
            }
        }
        StringBuffer buffer = getAvailableAsStr(codeCompletionProposals);
        
        fail("The string "+string+" was not found in the returned completions.\nAvailable:\n"+buffer);
    }

    /**
     * Checks if the completion we're looking for is the same completion we're analyzing.
     * 
     * @param lookingFor this is the completion we are looking for
     * @param completionProposal this is the completion proposal
     * @return if the completion we're looking for is the same completion we're checking
     */
    protected boolean checkIfEquals(String lookingFor, ICompletionProposal completionProposal) {
        return completionProposal.getDisplayString().equals(lookingFor);
    }

    /**
     * @param codeCompletionProposals
     * @return
     */
    protected StringBuffer getAvailableAsStr(ICompletionProposal[] codeCompletionProposals) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < codeCompletionProposals.length; i++) {
            buffer.append(codeCompletionProposals[i].getDisplayString());
            buffer.append("\n");
        }
        return buffer;
    }

    public void requestCompl(String strDoc, String []retCompl) throws CoreException, BadLocationException{
        requestCompl(strDoc, -1, retCompl.length, retCompl);
    }
    
    public void requestCompl(String strDoc, String retCompl) throws CoreException, BadLocationException{
        requestCompl(strDoc, new String[]{retCompl});
    }

    
}
