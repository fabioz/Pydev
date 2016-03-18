/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Tree;
import org.python.pydev.debug.pyunit.HistoryAction.HistoryMenuCreator;
import org.python.pydev.debug.pyunit.HistoryAction.IActionsMenu;
import org.python.pydev.debug.ui.launching.UnitTestLaunchShortcut;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.AbstractWorkbenchTestCase;
import org.python.pydev.shared_core.callbacks.ICallback;

/**
 * This test uses actual launches!
 */
public class PyUnitView2TestTestWorkbench extends AbstractWorkbenchTestCase implements ILaunchListener {

    private ILaunch launchAdded;
    private List<ILaunch> launchesRemoved = new ArrayList<ILaunch>();

    @Override
    protected void setUp() throws Exception {
        //no need for default setup
        closeWelcomeView();
        super.setUp();
        String testCaseContents = "" +
                "import unittest\n" +
                "\n" +
                "class TestCase(unittest.TestCase):\n" +
                "    \n"
                +
                "    def testMet1(self):\n" +
                "        print 'ok'\n" +
                "\n" +
                "    def testMet2(self):\n"
                +
                "        self.fail('failed')\n" +
                "        \n" +
                "    def testMet2__todo(self):\n"
                +
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
        goToManual(60 * 1000, new ICallback<Boolean, Object>() {

            @Override
            public Boolean call(Object arg) {
                PyUnitView view = PyUnitView.getView();
                PyUnitTestRun currentTestRun = view.getCurrentTestRun();
                if (currentTestRun == null) {
                    return false;
                }
                return launchesRemoved.size() == 1;
            }
        });

        PyUnitView view = PyUnitView.getView();
        ShowOnlyFailuresAction action = (ShowOnlyFailuresAction) getPyUnitViewAction(view, ShowOnlyFailuresAction.class);
        action.setChecked(false);//clicking it should do this.
        action.run();
        assertTrue(!action.isChecked());

        //note that only 3 methods appear in the tree because we've selected to show all methods (not only errors/failures)
        ICallback<Boolean, Object> callback = getPyUnitViewOkCallback(0, 3);
        goToManual(15 * 1000, callback);
        assertTrue(callback.call(THROW_ERROR));

        executePyUnitViewAction(PyUnitView.getView(), RelaunchAction.class);

        callback = getPyUnitViewOkCallback(1, 3);
        goToManual(15 * 1000, callback);
        assertTrue(callback.call(THROW_ERROR));

        executePyUnitViewAction(PyUnitView.getView(), RelaunchErrorsAction.class);

        action = (ShowOnlyFailuresAction) getPyUnitViewAction(view, ShowOnlyFailuresAction.class);
        action.setChecked(true);//clicking it should do this.
        action.run();
        assertTrue(action.isChecked());

        //note that only 2 methods appear in the tree because we've selected to show only errors/failures
        callback = getPyUnitViewOkCallback(2, 2);
        goToManual(15 * 1000, callback);
        assertTrue(callback.call(THROW_ERROR));

    }

    private static final String THROW_ERROR = "THROW_ERROR";

    private ICallback<Boolean, Object> getPyUnitViewOkCallback(final int historySize, final int methodsAppearingInTree) {
        return new ICallback<Boolean, Object>() {

            @Override
            public Boolean call(Object arg) {
                PyUnitView view = PyUnitView.getView();
                PyUnitTestRun currentTestRun = view.getCurrentTestRun();
                if (currentTestRun == null) {
                    if (arg == THROW_ERROR) {
                        throw new AssertionError("currentTestRun == null");
                    }
                    return false;
                }
                if (!currentTestRun.getFinished()) {
                    if (arg == THROW_ERROR) {
                        throw new AssertionError("!currentTestRun.getFinished()");
                    }
                    return false;
                }
                Tree tree = view.getTree();
                if (tree.getItemCount() != methodsAppearingInTree) {
                    if (arg == THROW_ERROR) {
                        throw new AssertionError("tree.getItemCount() " + tree.getItemCount() +
                                "!= methodsRun "
                                + methodsAppearingInTree);
                    }
                    return false;
                }
                CounterPanel counterPanel = view.getCounterPanel();
                if (!counterPanel.fNumberOfErrors.getText().equals("1")) {
                    if (arg == THROW_ERROR) {
                        throw new AssertionError("!counterPanel.fNumberOfErrors.getText().equals(\"1\")");
                    }
                    return false;
                }
                if (!counterPanel.fNumberOfFailures.getText().equals("1")) {
                    if (arg == THROW_ERROR) {
                        throw new AssertionError("!counterPanel.fNumberOfFailures.getText().equals(\"1\")");
                    }
                    return false;
                }
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
                if (historySize + 1 != actions.size()) { //+1 to count for the current!
                    if (arg == THROW_ERROR) {
                        throw new AssertionError("historySize + 1 != actions.size()");
                    }
                    return false;
                }

                return true;
            }
        };
    }

    @Override
    public void launchRemoved(ILaunch launch) {
        Assert.isTrue(this.launchAdded == launch);
        this.launchesRemoved.add(launch);
        this.launchAdded = null;
    }

    @Override
    public void launchAdded(ILaunch launch) {
        Assert.isTrue(this.launchAdded == null);
        this.launchAdded = launch;
    }

    @Override
    public void launchChanged(ILaunch launch) {

    }

}
