package org.python.pydev.debug.pyunit;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Tree;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.debug.pyunit.HistoryAction.HistoryMenuCreator;
import org.python.pydev.debug.pyunit.HistoryAction.IActionsMenu;
import org.python.pydev.debug.ui.launching.UnitTestLaunchShortcut;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;

/**
 * This test uses actual launches!
 */
public class PyUnitView2TestTestWorkbench extends AbstractWorkbenchTestCase implements ILaunchListener{

    public static Test suite() {
        TestSuite suite = new TestSuite(PyUnitView2TestTestWorkbench.class.getName());
        
        suite.addTestSuite(PyUnitView2TestTestWorkbench.class); 
        
        if (suite.countTestCases() == 0) {
            throw new Error("There are no test cases to run");
        } else {
            return suite;
        }
    }


    private ILaunch launchAdded;
    private List<ILaunch> launchesRemoved = new ArrayList<ILaunch>();

    
    protected void setUp() throws Exception {
        //no need for default setup
        closeWelcomeView();
        super.setUp();
        String testCaseContents = "" +
        		"import unittest\n" +
        		"\n" +
        		"class TestCase(unittest.TestCase):\n" +
        		"    \n" +
        		"    def testMet1(self):\n" +
        		"        print 'ok'\n" +
        		"\n" +
        		"    def testMet2(self):\n" +
        		"        self.fail('failed')\n" +
        		"        \n" +
        		"    def testMet2__todo(self):\n" +
        		"        raise RuntimeError('error')\n" +
        		"        \n" +
        		"";
        setFileContents(testCaseContents);
    }
    
    
    public void testPyUnitView2() throws Exception {
        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        launchManager.addLaunchListener(this);
        UnitTestLaunchShortcut unitTestLaunchShortcut = new UnitTestLaunchShortcut();
        unitTestLaunchShortcut.launch(editor, "run");
        
        //1 minute for the launch to complete should be enough
        goToManual(60*1000, new ICallback<Boolean, Object>() {
            
            public Boolean call(Object arg) {
                PyUnitView view = PyUnitView.getView();
                PyUnitTestRun currentTestRun = view.getCurrentTestRun();
                if(currentTestRun == null){
                    return false;
                }
                return launchesRemoved.size() == 1;
            }
        });
        
        goToManual(60*1000, getPyUnitViewOkCallback(0, 3));
        
        executePyUnitViewAction(PyUnitView.getView(), RelaunchAction.class);
        
        goToManual(60*1000, getPyUnitViewOkCallback(1, 3));
        
        executePyUnitViewAction(PyUnitView.getView(), RelaunchErrorsAction.class);
        
        goToManual(60*1000, getPyUnitViewOkCallback(2, 2));
    }


    private ICallback<Boolean, Object> getPyUnitViewOkCallback(final int historySize, final int methodsRun) {
        return new ICallback<Boolean, Object>() {

            public Boolean call(Object arg) {
                PyUnitView view = PyUnitView.getView();
                PyUnitTestRun currentTestRun = view.getCurrentTestRun();
                if(currentTestRun == null){
                    return false;
                }
                if(!currentTestRun.getFinished()){
                    return false;
                }
                Tree tree = view.getTree();
                if(tree.getItemCount() != methodsRun){
                    return false;
                }
                CounterPanel counterPanel = view.getCounterPanel();
                if(!counterPanel.fNumberOfErrors.getText().equals("1")){
                    return false;
                }
                if(!counterPanel.fNumberOfFailures.getText().equals("1")){
                    return false;
                }
                HistoryAction historyAction = (HistoryAction) getPyUnitViewAction(view, HistoryAction.class);
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
                if(historySize + 1 != actions.size()){ //+1 to count for the current!
                    return false;
                }
                
                return true;
            }
        };
    }


    public void launchRemoved(ILaunch launch) {
        Assert.isTrue(this.launchAdded == launch);
        this.launchesRemoved.add(launch);
        this.launchAdded = null;
    }


    public void launchAdded(ILaunch launch) {
        Assert.isTrue(this.launchAdded == null);
        this.launchAdded = launch;
    }


    public void launchChanged(ILaunch launch) {
        
    }


}
