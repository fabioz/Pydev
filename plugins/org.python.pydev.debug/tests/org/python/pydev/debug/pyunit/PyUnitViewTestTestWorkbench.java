package org.python.pydev.debug.pyunit;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.swt.widgets.Tree;
import org.python.pydev.debug.pyunit.HistoryAction.HistoryMenuCreator;
import org.python.pydev.debug.pyunit.HistoryAction.IActionsMenu;
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
    protected boolean terminated1 = false;
    protected boolean terminated2 = false;
    protected boolean relaunched1 = false;
    protected boolean relaunched2 = false;
    
    public void testPyUnitView() throws Exception {
        PyUnitViewServerListener.TIMEOUT = 0;
        PyUnitViewServerListener.JOBS_PRIORITY = Job.INTERACTIVE;
        
        
        IPyUnitServer pyUnitServer = new IPyUnitServer() {
            
            public void registerOnNotifyTest(IPyUnitServerListener pyUnitViewServerListener) {
                PyUnitViewTestTestWorkbench.this.pyUnitViewServerListener = pyUnitViewServerListener;
            }

            public void stop() {
                terminated1 = true;
            }
            
            public void relaunch() {
                relaunched1 = true;
            }
        };
        
        IPyUnitServer pyUnitServer2 = new IPyUnitServer() {
            
            public void registerOnNotifyTest(IPyUnitServerListener pyUnitViewServerListener) {
                PyUnitViewTestTestWorkbench.this.pyUnitViewServerListener = pyUnitViewServerListener;
            }
            
            public void stop() {
                terminated2 = true;
            }
            
            public void relaunch() {
                relaunched2 = true;
            }
        };
        
        PyUnitViewServerListener serverListener1 = PyUnitView.registerPyUnitServer(pyUnitServer, false);
        PyUnitView view = PyUnitView.getView();
        assertSame(pyUnitViewServerListener, serverListener1);
        CounterPanel counterPanel = view.getCounterPanel();
        PyUnitProgressBar progressBar = view.getProgressBar();

        notifyTest("ok", "d:/temp/a.py", "TestCase.testMet1", "", "");
        assertSame(view.getCurrentTestRun(), serverListener1.getTestRun());
        assertEquals(1, serverListener1.getTestRun().getSharedResultsList().size());
        assertEquals("Runs: 1", counterPanel.fNumberOfRuns.getText());
        assertEquals("0", counterPanel.fNumberOfErrors.getText());
        assertEquals("0", counterPanel.fNumberOfFailures.getText());
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
        checkRun1Active(view, serverListener1);
        
        
        PyUnitViewServerListener serverListener2 = PyUnitView.registerPyUnitServer(pyUnitServer2, false);
        assertSame(pyUnitViewServerListener, serverListener2);
        assertNotSame(pyUnitViewServerListener, serverListener1);
        
        checkRun2Active(view, serverListener2);
        
        view.setCurrentRun(serverListener1.getTestRun());
        assertEquals(true, progressBar.getHasErrors());
        assertEquals(true, progressBar.getHasFinished());
        
        view.setCurrentRun(serverListener2.getTestRun());
        assertEquals(false, progressBar.getHasErrors());
        assertEquals(false, progressBar.getHasFinished());

        execute(view, StopAction.class);
        assertTrue(terminated2);
        assertFalse(terminated1);
        
        HistoryAction historyAction = (HistoryAction) getAction(view, HistoryAction.class);
        HistoryAction.HistoryMenuCreator menuCreator = (HistoryMenuCreator) historyAction.getMenuCreator();
        final List<SetCurrentRunAction> actions = new ArrayList<SetCurrentRunAction>();
        final List<ClearTerminatedAction> terminatedActions = new ArrayList<ClearTerminatedAction>();
        IActionsMenu actionsMenu = new IActionsMenu() {
            
            public void add(IAction action) {
                if(action instanceof SetCurrentRunAction){
                    actions.add((SetCurrentRunAction) action);
                }else if(action instanceof ClearTerminatedAction){
                    terminatedActions.add((ClearTerminatedAction) action);
                }
            }
        };
        menuCreator.fillMenuManager(actionsMenu);
        assertEquals(2, actions.size());
        actions.get(0).run();
        checkRun1Active(view, serverListener1);
        actions.get(1).run();
        checkRun2Active(view, serverListener2);
        actions.get(0).run();
        checkRun1Active(view, serverListener1);
        
        assertEquals(terminatedActions.size(), 1);
        
        ShowOnlyFailuresAction action = (ShowOnlyFailuresAction) getAction(view, ShowOnlyFailuresAction.class);
        assertFalse(action.isChecked());
        action.setChecked(true);//clicking it should do this.
        action.run();
        assertTrue(action.isChecked());
        checkRun1Active(view, serverListener1, true);

        actions.clear();
        terminatedActions.get(0).run();
        terminatedActions.clear();
        
        menuCreator.fillMenuManager(actionsMenu);
        assertEquals(1, actions.size()); //the other was terminated
        assertEquals(1, terminatedActions.size());
        
        
        execute(view, RelaunchAction.class);
        assertTrue(relaunched2);
        
        goToManual();
          
    }

    private void checkRun1Active(PyUnitView view, PyUnitViewServerListener serverListener1) {
        checkRun1Active(view, serverListener1, false);
    }
    
    
    private void checkRun1Active(PyUnitView view, PyUnitViewServerListener serverListener1, boolean onlyFailuresInTree) {
        assertSame(view.getCurrentTestRun(), serverListener1.getTestRun());
        assertEquals(3, serverListener1.getTestRun().getSharedResultsList().size());
        CounterPanel counterPanel = view.getCounterPanel();
        PyUnitProgressBar progressBar = view.getProgressBar();
        assertEquals("Runs: 3", counterPanel.fNumberOfRuns.getText());
        assertEquals("1", counterPanel.fNumberOfErrors.getText());
        assertEquals("1", counterPanel.fNumberOfFailures.getText());
        assertEquals(true, progressBar.getHasErrors());
        assertEquals(true, progressBar.getHasFinished());
        Tree tree = view.getTree();
        if(onlyFailuresInTree){
            assertEquals(2, tree.getItemCount());
        }else{
            assertEquals(3, tree.getItemCount());
        }

    }

    public void checkRun2Active(PyUnitView view, PyUnitViewServerListener serverListener2) {
        PyUnitProgressBar progressBar = view.getProgressBar();
        CounterPanel counterPanel = view.getCounterPanel();
        assertSame(serverListener2.getTestRun(), view.getCurrentTestRun());
        assertEquals(0, serverListener2.getTestRun().getSharedResultsList().size());
        assertEquals("Runs: 0", counterPanel.fNumberOfRuns.getText());
        assertEquals("0", counterPanel.fNumberOfErrors.getText());
        assertEquals("0", counterPanel.fNumberOfFailures.getText());
        assertEquals(false, progressBar.getHasErrors());
        assertEquals(false, progressBar.getHasFinished());
        Tree tree = view.getTree();
        assertEquals(0, tree.getItemCount());
    }

    
    private IAction execute(PyUnitView view, Class<?> class1) {
        IAction action = getAction(view, class1);
        action.run();
        return action;
    }

    private IAction getAction(PyUnitView view, Class<?> class1) {
        IAction action = null;
        IContributionItem[] items = view.getViewSite().getActionBars().getToolBarManager().getItems();
        for (IContributionItem iContributionItem : items) {
            if(iContributionItem instanceof ActionContributionItem){
                ActionContributionItem item = (ActionContributionItem) iContributionItem;
                IAction lAction = item.getAction();
                if(class1.isInstance(lAction)){
                    action = lAction;
                }
            }
        }
        if(action == null){
            fail("Could not find action of class: "+class1);
        }
        return action;
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
