package org.python.pydev.debug.pyunit;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;

public class PyUnitViewTestTestWorkbench extends AbstractWorkbenchTestCase{

    public static Test suite() {
        TestSuite suite = new TestSuite(PyUnitViewTestTestWorkbench.class.getName());
        
        suite.addTestSuite(PyUnitViewTestTestWorkbench.class); 
        
        if (suite.countTestCases() == 0) {
            throw new Error("There are no test cases to run");
        } else {
            return suite;
        }
    }
    
    protected void setUp() throws Exception {
        //no need for default setup
        closeWelcomeView();
    }
    
    IPyUnitServerListener pyUnitViewServerListener;
    
    public void testPyUnitView() throws Exception {
        PyUnitViewServerListener.TIMEOUT = 0;
        PyUnitViewServerListener.JOBS_PRIORITY = Job.INTERACTIVE;
        
        
        IPyUnitServer pyUnitServer = new IPyUnitServer() {
            
            public void registerOnNotifyTest(IPyUnitServerListener pyUnitViewServerListener) {
                PyUnitViewTestTestWorkbench.this.pyUnitViewServerListener = pyUnitViewServerListener;
            }
        };
        
        PyUnitViewServerListener serverListener1 = PyUnitView.registerPyUnitServer(pyUnitServer, false);
        PyUnitView view = PyUnitView.getView();
        assertSame(pyUnitViewServerListener, serverListener1);
        
        notifyTest("ok", "d:/temp/a.py", "TestCase.testMet1", "", "");
        assertSame(view.getCurrentTestRun(), serverListener1.getTestRun());
        assertEquals(1, serverListener1.getTestRun().getSharedResultsList().size());
        CounterPanel counterPanel = view.getCounterPanel();
        assertEquals("Runs: 1", counterPanel.fNumberOfRuns.getText());
        assertEquals("0", counterPanel.fNumberOfErrors.getText());
        assertEquals("0", counterPanel.fNumberOfFailures.getText());
        PyUnitProgressBar progressBar = view.getProgressBar();
        assertEquals(false, progressBar.getHasErrors());
        assertEquals(false, progressBar.getHasFinished());
        
        notifyTest("fail", "d:/temp/a.py", "TestCase.testMet2", "", "");
        assertEquals("Runs: 2", counterPanel.fNumberOfRuns.getText());
        assertEquals("0", counterPanel.fNumberOfErrors.getText());
        assertEquals("1", counterPanel.fNumberOfFailures.getText());
        assertEquals(true, progressBar.getHasErrors());
        assertEquals(false, progressBar.getHasFinished());
        
        notifyTest("error", "d:/temp/a.py", "TestCase.testMet2", "", "");
        assertEquals("Runs: 3", counterPanel.fNumberOfRuns.getText());
        assertEquals("1", counterPanel.fNumberOfErrors.getText());
        assertEquals("1", counterPanel.fNumberOfFailures.getText());
        assertEquals(true, progressBar.getHasErrors());
        assertEquals(false, progressBar.getHasFinished());
        
        notifyFinished();
        assertEquals(true, progressBar.getHasErrors());
        assertEquals(true, progressBar.getHasFinished());
        
        
        PyUnitViewServerListener serverListener2 = PyUnitView.registerPyUnitServer(pyUnitServer, false);
        assertSame(pyUnitViewServerListener, serverListener2);
        assertNotSame(pyUnitViewServerListener, serverListener1);
        
        assertSame(serverListener2.getTestRun(), view.getCurrentTestRun());
        assertEquals(0, serverListener2.getTestRun().getSharedResultsList().size());
        assertEquals("Runs: 0", counterPanel.fNumberOfRuns.getText());
        assertEquals("0", counterPanel.fNumberOfErrors.getText());
        assertEquals("0", counterPanel.fNumberOfFailures.getText());
        assertEquals(false, progressBar.getHasErrors());
        assertEquals(false, progressBar.getHasFinished());
        
        view.setCurrentRun(serverListener1.getTestRun());
        assertEquals(true, progressBar.getHasErrors());
        assertEquals(true, progressBar.getHasFinished());
        
        view.setCurrentRun(serverListener2.getTestRun());
        assertEquals(false, progressBar.getHasErrors());
        assertEquals(false, progressBar.getHasFinished());
//        goToManual();
    }

    
    private void notifyFinished() {
        pyUnitViewServerListener.notifyFinished();
        goToManual(50); //should be enough for it to execute
    }

    private void notifyTest(String status, String location, String test, String capturedOutput, String errorContents) {
        pyUnitViewServerListener.notifyTest(status, location, test, capturedOutput, errorContents);
        goToManual(50); //should be enough for it to execute
    }
}
