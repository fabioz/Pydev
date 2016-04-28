/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Tree;
import org.python.pydev.debug.pyunit.HistoryAction.HistoryMenuCreator;
import org.python.pydev.debug.pyunit.HistoryAction.IActionsMenu;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.Test;
import junit.framework.TestSuite;

public class PyUnitViewTestTestWorkbench extends AbstractWorkbenchTestCase {

    public static Test suite() {
        TestSuite suite = new TestSuite(PyUnitViewTestTestWorkbench.class.getName());

        suite.addTestSuite(PyUnitViewTestTestWorkbench.class);

        if (suite.countTestCases() == 0) {
            throw new Error("There are no test cases to run");
        } else {
            return suite;
        }
    }

    @Override
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

            @Override
            public void registerOnNotifyTest(IPyUnitServerListener pyUnitViewServerListener) {
                PyUnitViewTestTestWorkbench.this.pyUnitViewServerListener = pyUnitViewServerListener;
            }

            @Override
            public IPyUnitLaunch getPyUnitLaunch() {
                return new IPyUnitLaunch() {

                    @Override
                    public void stop() {
                        terminated1 = true;
                    }

                    @Override
                    public void relaunch() {
                        relaunched1 = true;
                    }

                    @Override
                    public void relaunchTestResults(List<PyUnitTestResult> arrayList) {
                    }

                    @Override
                    public void relaunchTestResults(List<PyUnitTestResult> arrayList, String mode) {
                    }

                    @Override
                    public void fillXMLElement(Document document, Element launchElement) {
                    }

                };
            }
        };

        IPyUnitServer pyUnitServer2 = new IPyUnitServer() {

            @Override
            public void registerOnNotifyTest(IPyUnitServerListener pyUnitViewServerListener) {
                PyUnitViewTestTestWorkbench.this.pyUnitViewServerListener = pyUnitViewServerListener;
            }

            @Override
            public IPyUnitLaunch getPyUnitLaunch() {
                return new IPyUnitLaunch() {

                    @Override
                    public void stop() {
                        terminated2 = true;
                    }

                    @Override
                    public void relaunch() {
                        relaunched2 = true;
                    }

                    @Override
                    public void relaunchTestResults(List<PyUnitTestResult> arrayList) {
                    }

                    @Override
                    public void relaunchTestResults(List<PyUnitTestResult> arrayList, String mode) {
                    }

                    @Override
                    public void fillXMLElement(Document document, Element launchElement) {
                    }
                };
            }
        };

        PyUnitViewServerListener serverListener1 = PyUnitView.registerPyUnitServer(pyUnitServer, false);
        PyUnitView view = PyUnitView.getView();
        assertSame(pyUnitViewServerListener, serverListener1);
        CounterPanel counterPanel = view.getCounterPanel();
        PyUnitProgressBar progressBar = view.getProgressBar();

        notifyTestsCollected(9);
        assertEquals("0 / 9", counterPanel.fNumberOfRuns.getText());

        notifyTest("ok", "d:/temp/a.py", "TestCase.testMet1", "", "", "0.1");
        assertSame(view.getCurrentTestRun(), serverListener1.getTestRun());
        assertEquals(1, serverListener1.getTestRun().getSharedResultsList().size());
        assertEquals("1 / 9", counterPanel.fNumberOfRuns.getText());
        assertEquals("0", counterPanel.fNumberOfErrors.getText());
        assertEquals("0", counterPanel.fNumberOfFailures.getText());
        assertEquals(false, progressBar.getHasErrors());

        notifyTest("fail", "d:/temp/a.py", "TestCase.testMet2", "", "", "0.3");
        assertEquals("2 / 9", counterPanel.fNumberOfRuns.getText());
        assertEquals("0", counterPanel.fNumberOfErrors.getText());
        assertEquals("1", counterPanel.fNumberOfFailures.getText());
        assertEquals(true, progressBar.getHasErrors());

        notifyTest("error", "d:/temp/a.py", "TestCase.testMet2", "", "", "0.5");
        assertEquals("3 / 9", counterPanel.fNumberOfRuns.getText());
        assertEquals("1", counterPanel.fNumberOfErrors.getText());
        assertEquals("1", counterPanel.fNumberOfFailures.getText());
        assertEquals(true, progressBar.getHasErrors());

        notifyFinished();

        ShowOnlyFailuresAction action = (ShowOnlyFailuresAction) getPyUnitViewAction(view,
                ShowOnlyFailuresAction.class);
        action.setChecked(false);//clicking it should do this.
        action.run();
        assertTrue(!action.isChecked()); //showing all methods (not only failures/errors)

        checkRun1Active(view, serverListener1);

        PyUnitViewServerListener serverListener2 = PyUnitView.registerPyUnitServer(pyUnitServer2, false);
        assertSame(pyUnitViewServerListener, serverListener2);
        assertNotSame(pyUnitViewServerListener, serverListener1);

        checkRun2Active(view, serverListener2);

        view.setCurrentRun(serverListener1.getTestRun());
        assertEquals(true, progressBar.getHasErrors());

        view.setCurrentRun(serverListener2.getTestRun());
        assertEquals(false, progressBar.getHasErrors());

        executePyUnitViewAction(view, StopAction.class);
        assertTrue(terminated2);
        assertFalse(terminated1);

        HistoryAction historyAction = (HistoryAction) getPyUnitViewAction(view, HistoryAction.class);
        HistoryAction.HistoryMenuCreator menuCreator = (HistoryMenuCreator) historyAction.getMenuCreator();
        final List<SetCurrentRunAction> actions = new ArrayList<SetCurrentRunAction>();
        final List<ClearTerminatedAction> terminatedActions = new ArrayList<ClearTerminatedAction>();
        IActionsMenu actionsMenu = new IActionsMenu() {

            @Override
            public void add(IAction action) {
                if (action instanceof SetCurrentRunAction) {
                    actions.add((SetCurrentRunAction) action);
                } else if (action instanceof ClearTerminatedAction) {
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

        action = (ShowOnlyFailuresAction) getPyUnitViewAction(view, ShowOnlyFailuresAction.class);
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

        executePyUnitViewAction(view, RelaunchAction.class);
        assertTrue(relaunched2);

        //        goToManual();

    }

    private void checkRun1Active(PyUnitView view, PyUnitViewServerListener serverListener1) {
        checkRun1Active(view, serverListener1, false);
    }

    private void checkRun1Active(PyUnitView view, PyUnitViewServerListener serverListener1,
            boolean onlyFailuresInTree) {
        assertSame(view.getCurrentTestRun(), serverListener1.getTestRun());
        assertEquals(3, serverListener1.getTestRun().getSharedResultsList().size());
        CounterPanel counterPanel = view.getCounterPanel();
        PyUnitProgressBar progressBar = view.getProgressBar();
        assertEquals("3 / 9", counterPanel.fNumberOfRuns.getText());
        assertEquals("1", counterPanel.fNumberOfErrors.getText());
        assertEquals("1", counterPanel.fNumberOfFailures.getText());
        assertEquals(true, progressBar.getHasErrors());
        Tree tree = view.getTree();
        if (onlyFailuresInTree) {
            assertEquals(2, tree.getItemCount());
        } else {
            assertEquals(3, tree.getItemCount());
        }

    }

    public void checkRun2Active(PyUnitView view, PyUnitViewServerListener serverListener2) {
        PyUnitProgressBar progressBar = view.getProgressBar();
        CounterPanel counterPanel = view.getCounterPanel();
        assertSame(serverListener2.getTestRun(), view.getCurrentTestRun());
        assertEquals(0, serverListener2.getTestRun().getSharedResultsList().size());
        assertEquals("0 / 0", counterPanel.fNumberOfRuns.getText());
        assertEquals("0", counterPanel.fNumberOfErrors.getText());
        assertEquals("0", counterPanel.fNumberOfFailures.getText());
        assertEquals(false, progressBar.getHasErrors());
        Tree tree = view.getTree();
        assertEquals(0, tree.getItemCount());
    }

    private void notifyFinished() {
        pyUnitViewServerListener.notifyFinished(null);
        goToManual(100); //should be enough for it to execute
    }

    private void notifyTestsCollected(int totalTestsCount) {
        pyUnitViewServerListener.notifyTestsCollected("" + totalTestsCount);
        goToManual(100); //should be enough for it to execute
    }

    private void notifyTest(String status, String location, String test, String capturedOutput, String errorContents,
            String time) {
        pyUnitViewServerListener.notifyTest(status, location, test, capturedOutput, errorContents, time);
        goToManual(100); //should be enough for it to execute
    }
}
