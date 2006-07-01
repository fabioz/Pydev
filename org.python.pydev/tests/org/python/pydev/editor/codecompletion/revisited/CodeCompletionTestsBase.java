/*
 * Created on Mar 9, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.BundleInfoStub;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;
import org.python.pydev.utils.PrintProgressMonitor;

/**
 * @author Fabio Zadrozny
 */
public class CodeCompletionTestsBase extends TestCase {

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
	
	public static Class restored;
	public static Class restored2;
	
	public static Class restoredSystem;
	public Preferences preferences;
    protected static boolean DEBUG_TESTS_BASE = false;

	/*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        PydevPlugin.setBundleInfo(new BundleInfoStub());
        preferences = new Preferences();
        ProjectModulesManager.IN_TESTS = true;
    }
    
    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        PydevPlugin.setBundleInfo(null);
        ProjectModulesManager.IN_TESTS = false;
    }
    
    protected boolean restoreProjectPythonPath(boolean force, String path){
        if(restored == null || restored != this.getClass() || force){
            //cache
            restored = this.getClass();
            nature = createNature();
            ProjectStub projectStub = new ProjectStub("testProjectStub", path, new IProject[0], new IProject[0]);
			nature.setProject(projectStub);
			projectStub.setNature(nature);
    	    nature.setAstManager(new ASTManager());
    	    
    	    ASTManager astManager = ((ASTManager)nature.getAstManager());
            astManager.setNature(nature);
            astManager.setProject(projectStub, false);
            astManager.changePythonPath(path, projectStub, getProgressMonitor(),null);
            return true;
        }
        return false;
    }
    
    protected boolean restoreProjectPythonPath2(boolean force, String path){
    	if(restored2 == null || restored2 != this.getClass() || force){
    		//cache
    		restored2 = this.getClass();
    		nature2 = createNature();
            
    		ProjectStub natureProject = (ProjectStub) nature.getProject();
            ProjectStub projectStub = new ProjectStub("testProjectStub2", path, new IProject[]{natureProject}, new IProject[0]);
            
            //as we're adding a reference, we also have to set the referencing...
            natureProject.referencingProjects = new IProject[]{projectStub};
            
			nature2.setProject(projectStub); //references the project 1
			projectStub.setNature(nature2);
    		nature2.setAstManager(new ASTManager());
    		
    		ASTManager astManager = ((ASTManager)nature2.getAstManager());
    		astManager.setNature(nature2);
    		astManager.setProject(projectStub, false);
    		astManager.changePythonPath(path, projectStub, getProgressMonitor(),null);
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
            info.forcedLibs.add("mx");
            info.restorePythonpath(path, getProgressMonitor()); //here

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
        InterpreterInfo info = (InterpreterInfo) iMan.getDefaultInterpreterInfo(getProgressMonitor());
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
        InterpreterInfo info2 = (InterpreterInfo) iMan2.getDefaultInterpreterInfo(getProgressMonitor());
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
        restoreProjectPythonPath2(force, TestDependent.TEST_PYSRC_LOC2);
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
        restoreProjectPythonPath2(force, TestDependent.TEST_PYSRC_LOC2);
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
        restoreProjectPythonPath2(force, TestDependent.TEST_PYSRC_LOC2);
        if(DEBUG_TESTS_BASE){
            System.out.println("-------------- Checking size (for proj1 and proj2)");
        }
        
        checkSize();
    }
    
    /**
     * checks if the size of the system modules manager and the project moule manager are coherent
     * (we must have more modules in the system than in the project)
     */
    protected void checkSize() {
        IInterpreterManager iMan = getInterpreterManager();
        InterpreterInfo info = (InterpreterInfo) iMan.getDefaultInterpreterInfo(getProgressMonitor());
        assertTrue(info.modulesManager.getSize() > 0);
        
        int size = ((ASTManager)nature.getAstManager()).getSize();
        assertTrue("Interpreter size:"+info.modulesManager.getSize()+" should be smaller than project size:"+size+" " +
        		"(because it contains system+project info)" , info.modulesManager.getSize() < size );
        
        size = ((ASTManager)nature2.getAstManager()).getSize();
        assertTrue("Interpreter size:"+info.modulesManager.getSize()+" should be smaller than project size:"+size+" " +
        		"(because it contains system+project info)" , info.modulesManager.getSize() < size );
    }
   


    
    
    
    
    public void testEmpty() {
        //just so that we don't get 'no tests found' warning
    }
    
    
    
    
    
    // ================================================================= helpers for doing code completion requests
    
    protected PyCodeCompletion codeCompletion;
    
    public ICompletionProposal[] requestCompl(String strDoc, int documentOffset, int returned, String []retCompl) throws CoreException, BadLocationException{
    	return requestCompl(strDoc, documentOffset, returned, retCompl, nature);
    }
    public ICompletionProposal[] requestCompl(String strDoc, int documentOffset, int returned, String []retCompl, PythonNature nature) throws CoreException, BadLocationException{
    	return requestCompl(null, strDoc, documentOffset, returned, retCompl, nature);
    }
    
    public ICompletionProposal[] requestCompl(File file, int documentOffset, int returned, String []retCompl) throws CoreException, BadLocationException{
        String strDoc = REF.getFileContents(file);
        return requestCompl(file, strDoc, documentOffset, returned, retCompl);
    }
    
    public ICompletionProposal[] requestCompl(File file, String strDoc, int documentOffset, int returned, String []retCompl) throws CoreException, BadLocationException{
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
     */
    public ICompletionProposal[] requestCompl(File file, String strDoc, int documentOffset, int returned, String []retCompl, PythonNature nature) throws CoreException, BadLocationException{
        if(documentOffset == -1)
            documentOffset = strDoc.length();
        
        IDocument doc = new Document(strDoc);
        CompletionRequest request = new CompletionRequest(file, nature, doc, documentOffset, codeCompletion);

        List props = codeCompletion.getCodeCompletionProposals(null, request);
        ICompletionProposal[] codeCompletionProposals = codeCompletion.onlyValidSorted(props, request.qualifier, request.isInCalltip);
        
        
        for (int i = 0; i < retCompl.length; i++) {
            assertContains(retCompl[i], codeCompletionProposals);
        }

        if(returned > -1){
            StringBuffer buffer = getAvailableAsStr(codeCompletionProposals);
            assertEquals("Expected "+returned+" received: "+codeCompletionProposals.length+"\n"+buffer, returned, codeCompletionProposals.length);
        }
        return codeCompletionProposals;
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
     * @param string
     * @param codeCompletionProposals
     */
    protected void assertNotContains(String string, ICompletionProposal[] codeCompletionProposals) {
    	for (int i = 0; i < codeCompletionProposals.length; i++) {
    		ICompletionProposal completionProposal = codeCompletionProposals[i];
    		if(checkIfEquals(string, completionProposal)){
    			fail("The string "+string+" was found in the returned completions (was not expected to be found).");
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

    public ICompletionProposal[] requestCompl(String strDoc, String []retCompl) throws CoreException, BadLocationException{
        return requestCompl(strDoc, -1, retCompl.length, retCompl);
    }
    
    public ICompletionProposal[] requestCompl(String strDoc, String retCompl) throws CoreException, BadLocationException{
        return requestCompl(strDoc, new String[]{retCompl});
    }

    public static void assertContains(List<String> found, String toFind) {
        for (String str : found) {
            if (str.equals(toFind)){
                return;
            }
        }
        fail("The string "+toFind+" was not found amongst the passed strings.");
    }

}
