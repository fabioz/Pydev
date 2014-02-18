/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl.IInformationPresenter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IHyperlink;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.prefs.ColorManager;
import org.python.pydev.debug.ui.ILinkContainer;
import org.python.pydev.debug.ui.PythonConsoleLineTracker;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.ICallbackWithListeners;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_ui.tooltips.presenter.StyleRangeWithCustomData;
import org.python.pydev.shared_ui.tooltips.presenter.ToolTipPresenterHandler;
import org.python.pydev.shared_ui.utils.IViewWithControls;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.shared_ui.utils.UIUtils;
import org.python.pydev.ui.ColorAndStyleCache;
import org.python.pydev.ui.NotifyViewCreated;
import org.python.pydev.ui.ViewPartWithOrientation;

/**
 * ViewPart that'll listen to the PyUnitServer and show what's happening (with a green/red bar).
 * 
 * Features:
 * 
 * - Red/green bar -- OK
 * - Relaunching the tests -- OK
 * - Relaunching only the tests that failed -- OK
 * - Show stack traces of errors (when selected) -- OK
 * - Show output of test cases (when selected) -- OK
 * - Show tests ran -- OK
 * - Show only failed tests ran -- OK
 * - Stop execution of the tests -- OK
 * - Show the number of successes, failures and errors -- OK
 * - Double-click to go to test -- OK
 * - Show time of test (and allow reordering based on it) -- OK
 * - Tooltip on hover for test with links -- OK
 * - Use theme colors -- OK
 * - Auto-show on test run should be an option. -- OK
 * - Check: if it's created after a test suite started running, the results should be properly shown. -- OK
 * - Allow the user to select test runner (Initially at least default and nose. Step 2: py.test) -- OK
 * - Don't show results in the unittest view (only show in the console): in this situation, don't even start the server or use xml-rpc. -- OK
 * - Pydev default test runner distributed -- OK
 * - Show current test(s) being run (handle parallel execution) -- OK
 * - Select some tests and make a new run with them. -- OK
 * - Show total time to run tests. -- OK
 * - Rerun tests on file changes -- OK 
 * 
 * 
 * Nice to have:
 * - Hide or show output pane 
 * - If a string was different, show an improved diff (as JDT)
 * - Save column order (tree.setColumnOrder(order))
 * - Hide columns
 * - Theming bug: when columns order change, the selected text for the last columns is not appearing
 * 
 * 
 * References:
 * 
 * http://www.eclipse.org/swt/snippets/
 * 
 * Notes on tree/table: http://www.eclipse.org/swt/R3_2/new_and_noteworthy.html (see links below)
 * 
 * Working: Sort table by column (applicable to tree: http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet2.java?view=markup&content-type=text%2Fvnd.viewcvs-markup&revision=HEAD )
 * Working: Reorder columns by drag ( http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet193.java?view=markup&content-type=text%2Fvnd.viewcvs-markup&revision=HEAD )
 * Working: Sort indicator in column header ( http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet192.java?view=markup&content-type=text%2Fvnd.viewcvs-markup&revision=HEAD )
 * 
 * Based on org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart (but it's really not meant to be reused)
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class PyUnitView extends ViewPartWithOrientation implements IViewWithControls {

    public static final String PYUNIT_VIEW_ORIENTATION = "PYUNIT_VIEW_ORIENTATION";

    @Override
    public String getOrientationPreferencesKey() {
        return PYUNIT_VIEW_ORIENTATION;
    }

    public static final String PY_UNIT_TEST_RESULT = "RESULT";
    private static final String PY_UNIT_VIEW_ID = "org.python.pydev.debug.pyunit.pyUnitView";
    public static final String PYUNIT_VIEW_SHOW_ONLY_ERRORS = "PYUNIT_VIEW_SHOW_ONLY_ERRORS";
    public static final boolean PYUNIT_VIEW_DEFAULT_SHOW_ONLY_ERRORS = true;

    public static final String PYUNIT_VIEW_SHOW_VIEW_ON_TEST_RUN = "PYUNIT_VIEW_SHOW_VIEW_ON_TEST_RUN";
    public static final boolean PYUNIT_VIEW_DEFAULT_SHOW_VIEW_ON_TEST_RUN = true;

    public static final String PYUNIT_VIEW_BACKGROUND_RELAUNCH_SHOW_ONLY_ERRORS = "PYUNIT_VIEW_BACKGROUND_RELAUNCH_SHOW_ONLY_ERRORS";
    public static final boolean PYUNIT_VIEW_DEFAULT_BACKGROUND_RELAUNCH_SHOW_ONLY_ERRORS = false;

    public static int MAX_RUNS_TO_KEEP = 15;

    private static final Object lockServerListeners = new Object();
    private static final List<PyUnitViewServerListener> serverListeners = new ArrayList<PyUnitViewServerListener>();

    private PyUnitTestRun currentRun;
    private final PythonConsoleLineTracker lineTracker = new PythonConsoleLineTracker();
    private final ActivateLinkmouseListener activateLinkmouseListener = new ActivateLinkmouseListener();

    /*default*/static final int COL_INDEX = 0;
    /*default*/static final int COL_RESULT = 1;
    /*default*/static final int COL_TEST = 2;
    /*default*/static final int COL_FILENAME = 3;
    /*default*/static final int COL_TIME = 4;
    /*default*/static final int NUMBER_OF_COLUMNS = 5;
    private static final NumberFormatException NUMBER_FORMAT_EXCEPTION = new NumberFormatException();

    /*default*/PythonConsoleLineTracker getLineTracker() {
        return lineTracker;
    }

    private ColorAndStyleCache colorAndStyleCache;

    private SashForm sash;
    private Tree tree;
    private StyledText testOutputText;
    private CounterPanel fCounterPanel;
    private PyUnitProgressBar fProgressBar;
    private Label fStatus;
    private Composite fCounterComposite;
    private IPropertyChangeListener prefListener;

    /**
     * Whether we should show only errors or not.
     */
    private boolean showOnlyErrors;

    /*default*/TreeColumn colIndex;
    /*default*/TreeColumn colResult;
    /*default*/TreeColumn colTest;
    /*default*/TreeColumn colFile;
    /*default*/TreeColumn colTime;

    /**
     * Have we disposed of the view?
     */
    private boolean disposed = false;

    public PyUnitView() {
        if (SharedCorePlugin.inTestMode()) {
            // leave showOnlyErrors at default under test
        } else {
            PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
            IPreferenceStore preferenceStore = plugin.getPreferenceStore();
            this.showOnlyErrors = preferenceStore.getBoolean(PYUNIT_VIEW_SHOW_ONLY_ERRORS);
        }

        NotifyViewCreated.notifyViewCreated(this);

        lineTracker.init(new ILinkContainer() {

            public void addLink(IHyperlink link, int offset, int length) {
                if (testOutputText == null) {
                    return;
                }
                StyleRangeWithCustomData range = new StyleRangeWithCustomData();
                range.underline = true;
                try {
                    range.underlineStyle = SWT.UNDERLINE_LINK;
                } catch (Throwable e) {
                    //Ignore (not available on earlier versions of eclipse)
                }

                //Set the proper color if it's available.
                TextAttribute textAttribute = ColorManager.getDefault().getHyperlinkTextAttribute();
                if (textAttribute != null) {
                    range.foreground = textAttribute.getForeground();
                } else {
                    range.foreground = JFaceColors.getHyperlinkText(Display.getDefault());
                }
                range.start = offset;
                range.length = length + 1;
                range.customData = link;
                testOutputText.setStyleRange(range);
            }

            public String getContents(int lineOffset, int lineLength) throws BadLocationException {
                if (testOutputText == null) {
                    return "";
                }
                if (lineLength <= 0) {
                    return "";
                }
                try {
                    return testOutputText.getText(lineOffset, lineOffset + lineLength);
                } catch (IllegalArgumentException e) {
                    return ""; //thrown on invalid range.
                }
            }
        });
    }

    public PyUnitProgressBar getProgressBar() {
        return fProgressBar;
    }

    public CounterPanel getCounterPanel() {
        return fCounterPanel;
    }

    public Tree getTree() {
        return tree;
    }

    @Override
    public void createPartControl(Composite parent) {
        Assert.isTrue(!disposed);
        super.createPartControl(parent);
        IInformationPresenter presenter = new InformationPresenterWithLineTracker();
        final ToolTipPresenterHandler tooltip = new ToolTipPresenterHandler(parent.getShell(), presenter);

        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.verticalSpacing = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 2;
        parent.setLayout(layout);
        configureToolBar();

        fCounterComposite = new Composite(parent, SWT.NONE);
        layout = new GridLayout();
        fCounterComposite.setLayout(layout);
        fCounterComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

        fCounterPanel = new CounterPanel(fCounterComposite);
        fCounterPanel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

        fProgressBar = new PyUnitProgressBar(fCounterComposite);
        fProgressBar.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

        fStatus = new Label(fCounterComposite, 0);
        GridData statusLayoutData = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
        statusLayoutData.grabExcessHorizontalSpace = true;
        fStatus.setLayoutData(statusLayoutData);
        fStatus.setText("Status");

        sash = new SashForm(parent, SWT.HORIZONTAL);
        GridData layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        sash.setLayoutData(layoutData);

        tree = new Tree(sash, SWT.FULL_SELECTION | SWT.MULTI);
        tooltip.install(tree);
        tree.setHeaderVisible(true);

        Listener sortListener = new PyUnitSortListener(this);
        colIndex = createColumn(" ", 50, sortListener);
        colResult = createColumn("Result", 70, sortListener);
        colTest = createColumn("Test", 180, sortListener);
        colFile = createColumn("File", 180, sortListener);
        colTime = createColumn("Time (s)", 80, sortListener);
        onControlCreated.call(tree);

        tree.setSortColumn(colIndex);
        tree.setSortDirection(SWT.DOWN);

        tree.addMouseListener(new DoubleClickTreeItemMouseListener());
        tree.addKeyListener(new EnterProssedTreeItemKeyListener());
        tree.addSelectionListener(new SelectResultSelectionListener());

        Menu menu = new Menu(tree.getShell(), SWT.POP_UP);
        MenuItem runItem = new MenuItem(menu, SWT.PUSH);
        runItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                relaunchSelectedTests(ILaunchManager.RUN_MODE);
            }
        });
        runItem.setText("Run");

        MenuItem debugItem = new MenuItem(menu, SWT.PUSH);
        debugItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                relaunchSelectedTests(ILaunchManager.DEBUG_MODE);
            }
        });
        debugItem.setText("Debug");
        tree.setMenu(menu);

        if (PydevPlugin.getDefault() != null) {
            colorAndStyleCache = new ColorAndStyleCache(PydevPrefs.getChainedPrefStore());
            prefListener = new IPropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent event) {
                    if (tree != null) {
                        String property = event.getProperty();
                        if (ColorAndStyleCache.isColorOrStyleProperty(property)) {
                            colorAndStyleCache.reloadProperty(property);
                            Color errorColor = getErrorColor();
                            TreeItem[] items = tree.getItems();
                            for (TreeItem item : items) {
                                PyUnitTestResult result = (PyUnitTestResult) item.getData(PY_UNIT_TEST_RESULT);
                                if (result != null) {
                                    if (result.isOk()) {

                                    } else if (result.isSkip()) {

                                    } else {
                                        //failure or error.
                                        item.setForeground(errorColor);

                                    }
                                }
                            }

                            if (fProgressBar != null) {
                                fProgressBar.updateErrorColor(true);
                            }
                        }
                    }
                }
            };
            PydevPrefs.getChainedPrefStore().addPropertyChangeListener(prefListener);
        }

        StyledText text = new StyledText(sash, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
        text.setFont(JFaceResources.getFont(IDebugUIConstants.PREF_CONSOLE_FONT));
        this.setTextComponent(text);
        onTestRunAdded();
    }

    private void configureToolBar() {
        IActionBars actionBars = getViewSite().getActionBars();
        IToolBarManager toolBar = actionBars.getToolBarManager();
        IMenuManager menuManager = actionBars.getMenuManager();

        ShowViewOnTestRunAction showViewOnTestRunAction = new ShowViewOnTestRunAction(this);
        menuManager.add(showViewOnTestRunAction);
        IAction showTestRunnerPreferencesAction = new ShowTestRunnerPreferencesAction(this);
        menuManager.add(showTestRunnerPreferencesAction);

        ShowOnlyFailuresAction action = new ShowOnlyFailuresAction(this);
        toolBar.add(action);
        action.setChecked(this.showOnlyErrors);

        toolBar.add(new Separator());
        toolBar.add(new RelaunchAction(this));
        toolBar.add(new RelaunchErrorsAction(this));
        toolBar.add(new StopAction(this));

        toolBar.add(new Separator());
        toolBar.add(new RelaunchInBackgroundAction(this));

        toolBar.add(new Separator());
        toolBar.add(new HistoryAction(this));
        PinHistoryAction pinHistory = new PinHistoryAction(this);
        toolBar.add(pinHistory);
        toolBar.add(new RestorePinHistoryAction(this, pinHistory));

        addOrientationPreferences(menuManager);
    }

    @Override
    protected void setNewOrientation(int orientation) {
        if (sash != null && !sash.isDisposed() && fCounterComposite != null && !fCounterComposite.isDisposed()) {
            GridLayout layout = (GridLayout) fCounterComposite.getLayout();
            if (orientation == VIEW_ORIENTATION_HORIZONTAL) {
                sash.setOrientation(SWT.HORIZONTAL);
                layout.numColumns = 2;

            } else {
                sash.setOrientation(SWT.VERTICAL);
                layout.numColumns = 1;
            }
            fParent.layout();
        }
    }

    private TreeColumn createColumn(String text, int width, Listener sortListener) {
        TreeColumn col;
        col = new TreeColumn(tree, SWT.LEFT);
        col.setText(text);
        col.setWidth(width);
        col.setMoveable(true);

        col.addListener(SWT.Selection, sortListener);

        return col;
    }

    @Override
    public void setFocus() {
        this.tree.setFocus();
    }

    @Override
    public void dispose() {
        if (this.disposed) {
            return;
        }
        this.disposed = true;
        if (this.tree != null) {
            Tree t = this.tree;
            this.tree = null;
            onControlDisposed.call(t);
            t.dispose();
        }
        if (this.testOutputText != null) {
            StyledText t = this.testOutputText;
            this.testOutputText = null;
            onControlDisposed.call(t);
            t.dispose();
        }
        if (this.fCounterPanel != null) {
            this.fCounterPanel.dispose();
            this.fCounterPanel = null;
        }
        if (this.prefListener != null) {
            PydevPrefs.getChainedPrefStore().removePropertyChangeListener(prefListener);
            this.prefListener = null;
        }
        super.dispose();
    }

    public static PyUnitViewServerListener registerPyUnitServer(final IPyUnitServer pyUnitServer) {
        return registerPyUnitServer(pyUnitServer, true);
    }

    /**
     * Registers a pyunit server in the pyunit view (and vice versa), so, the server starts notifying the view
     * about changes in it and the view makes the test run from the server the current one.
     */
    public static PyUnitViewServerListener registerPyUnitServer(final IPyUnitServer pyUnitServer, boolean async) {
        //We create a listener before and later set the view so that we don't run into racing condition errors!
        final PyUnitViewServerListener serverListener = new PyUnitViewServerListener(pyUnitServer,
                pyUnitServer.getPyUnitLaunch());
        PyUnitView.addServerListener(serverListener);

        Runnable r = new Runnable() {
            public void run() {
                try {
                    PyUnitView view = getView();
                    if (view != null) {
                        view.onTestRunAdded();
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        };

        if (async) {
            Display.getDefault().asyncExec(r);
        } else {
            Display.getDefault().syncExec(r);
        }
        return serverListener;
    }

    /**
     * Must be called on the UI thread. Sets the view on all the server listeners (even if it was already set before
     * as it may be that a view was closed and then a new one created).
     */
    private void onTestRunAdded() {
        synchronized (lockServerListeners) {
            for (PyUnitViewServerListener listener : serverListeners) {
                listener.setView(this); //Set in all, as it may be that they have an already disposed view registered.
            }
            if (serverListeners.size() > 0) {
                //make the last one active.
                this.setCurrentRun(serverListeners.get(serverListeners.size() - 1).getTestRun());
            }
        }
    }

    /**
     * Gets the py unit view. May only be called in the UI thread. If the view is not visible, shows it if the
     * preference to do that is set to true.
     * 
     * Note that it may return null if the preference to show it is false and the view is not currently shown.
     */
    public static PyUnitView getView() {
        return (PyUnitView) UIUtils.getView(PY_UNIT_VIEW_ID, ShowViewOnTestRunAction.getShowViewOnTestRun());
    }

    /**
     * Adds a server listener to the static list of available server listeners. This is needed so that we start
     * to listen to it when the view is restored later on (if it's still not visible).
     */
    protected static void addServerListener(PyUnitViewServerListener serverListener) {
        synchronized (lockServerListeners) {

            if (serverListeners.size() + 1 > MAX_RUNS_TO_KEEP) {
                serverListeners.remove(0);
            }
            serverListeners.add(serverListener);
        }
    }

    /**
     * Notifies that the test run has finished.
     */
    /*default */void notifyFinished(PyUnitTestRun testRun) {
        if (this.disposed) {
            return;
        }

        if (testRun != currentRun) {
            return;
        }
        asyncUpdateCountersAndBar();
    }

    /**
     * Notifies that a test result has been added.
     */
    /*default*/void notifyTest(PyUnitTestResult result) {
        if (this.disposed) {
            return;
        }

        notifyTest(result, true);
    }

    /*default*/void notifyTestStarted(PyUnitTestStarted result) {
        if (this.disposed) {
            return;
        }

        if (result.getTestRun() != currentRun) {
            return;
        }

        asyncUpdateCountersAndBar();
    }

    /**
     * Used to update the number of tests available. 
     */
    /*default*/void notifyTestsCollected(PyUnitTestRun testRun) {
        if (this.disposed) {
            return;
        }

        if (testRun != currentRun) {
            return;
        }
        asyncUpdateCountersAndBar();
    }

    /**
     * Calls an update in the counters and progress bar asynchronously (in the UI thread).
     */
    public void asyncUpdateCountersAndBar() {
        RunInUiThread.async(new Runnable() {

            public void run() {
                updateCountersAndBar();
            }
        });
    }

    /**
     * Called after a test has been run (so that we properly update the tree).
     */
    private void notifyTest(PyUnitTestResult result, boolean updateBar) {
        if (this.disposed) {
            return;
        }

        if (result.getTestRun() != currentRun) {
            return;
        }
        if (!showOnlyErrors || (showOnlyErrors && !result.isOk() && !result.isSkip())) {
            TreeItem treeItem = new TreeItem(tree, 0);
            File file = new File(result.location);
            treeItem.setText(new String[] { result.index, result.status, result.test, file.getName(), result.time });
            if (result.isOk()) {

            } else if (result.isSkip()) {

            } else {
                // failure or error
                Color errorColor = getErrorColor();
                treeItem.setForeground(errorColor);
            }

            treeItem.setData(ToolTipPresenterHandler.TIP_DATA, result);
            treeItem.setData(PY_UNIT_TEST_RESULT, result);

            int selectionCount = tree.getSelectionCount();
            if (selectionCount == 0) {
                tree.setSelection(treeItem);
                onSelectResult(result);
            }
        }

        if (updateBar) {
            updateCountersAndBar();
        }
    }

    /**
     * @return the color that should be used for errors.
     */
    public Color getErrorColor() {
        TextAttribute attribute = ColorManager.getDefault().getConsoleErrorTextAttribute();
        if (attribute == null) {
            return null;
        }
        Color errorColor = attribute.getForeground();
        return errorColor;
    }

    /**
     * @return the color that should be used for the foreground.
     */
    public Color getForegroundColor() {
        TextAttribute attribute = ColorManager.getDefault().getForegroundTextAttribute();
        if (attribute == null) {
            return null;
        }
        Color errorColor = attribute.getForeground();
        return errorColor;
    }

    /**
     * Updates the number of test runs and the bar with the current progress.
     */
    private void updateCountersAndBar() {
        if (fCounterPanel == null) {
            return;
        }
        if (currentRun != null) {
            String totalNumberOfRuns = currentRun.getTotalNumberOfRuns();
            int numberOfRuns = currentRun.getNumberOfRuns();
            int numberOfErrors = currentRun.getNumberOfErrors();
            int numberOfFailures = currentRun.getNumberOfFailures();

            try {
                int totalAsInt;
                if (currentRun.getFinished()) {
                    totalAsInt = numberOfRuns;
                    //Leave the number of runs what was set before!
                    //                    totalNumberOfRuns = Integer.toString(totalAsInt);
                } else {
                    totalAsInt = Integer.parseInt(totalNumberOfRuns);
                }
                if (totalAsInt == 0 && numberOfRuns > 0) {
                    totalNumberOfRuns = "?";
                    throw NUMBER_FORMAT_EXCEPTION;
                }
                fProgressBar.reset(numberOfErrors + numberOfFailures > 0, false, numberOfRuns, totalAsInt);
            } catch (NumberFormatException e) {
                //use this if we're unable to collect the number of runs as a string.
                setShowBarWithError(numberOfErrors + numberOfFailures > 0, numberOfRuns > 0, currentRun.getFinished());
            }

            fCounterPanel.setRunValue(numberOfRuns, totalNumberOfRuns);
            fCounterPanel.setErrorValue(numberOfErrors);
            fCounterPanel.setFailureValue(numberOfFailures);

            String totalTime = currentRun.getTotalTime();
            if (totalTime == null) {
                Collection<PyUnitTestStarted> testsRunning = currentRun.getTestsRunning();
                FastStringBuffer bufStatus = new FastStringBuffer("Current: ", 200);
                FastStringBuffer bufTooltip = new FastStringBuffer("Current: ", 200);

                int i = 0;
                for (PyUnitTestStarted pyUnitTestStarted : testsRunning) {
                    if (i > 0) {
                        bufTooltip.append('\n');
                    }
                    bufTooltip.append(pyUnitTestStarted.test);
                    bufTooltip.append("  ");
                    bufTooltip.append('(');
                    bufTooltip.append(pyUnitTestStarted.location);
                    bufTooltip.append(')');

                    if (i > 0) {
                        bufStatus.append(", ");
                    }
                    bufStatus.append(pyUnitTestStarted.test);
                    i++;
                }
                this.fStatus.setText(bufStatus.toString());
                this.fStatus.setToolTipText(bufTooltip.toString());
            } else {
                this.fStatus.setText(totalTime);
                this.fStatus.setToolTipText(totalTime);
            }
        } else {
            this.fStatus.setText("");
            this.fStatus.setToolTipText("");
            fCounterPanel.setRunValue(0, "0");
            fCounterPanel.setErrorValue(0);
            fCounterPanel.setFailureValue(0);

            setShowBarWithError(false, false, false);
        }

    }

    /**
     * Helper method to reset the bar to a state knowing only about if we have errors, runs and whether it's finished.
     * 
     * Only really used if we have no errors or if we don't know how to collect the current number of test runs.
     */
    private void setShowBarWithError(boolean hasError, boolean hasRuns, boolean finished) {
        fProgressBar.reset(hasError, false, hasRuns ? 1 : 0, finished ? 1 : 2);
    }

    /**
     * Selection listener added to the tree so that the text output is updated when the selection changes.
     */
    private final class SelectResultSelectionListener extends SelectionAdapter {
        @Override
        public void widgetSelected(SelectionEvent e) {
            if (e.item != null) {
                PyUnitTestResult result = (PyUnitTestResult) e.item.getData(PY_UNIT_TEST_RESULT);
                onSelectResult(result);
            }
        }

    }

    /**
     * Should only be used in the onSelectResult.
     */
    private final FastStringBuffer tempOnSelectResult = new FastStringBuffer();
    private final String ERRORS_HEADER = "============================= ERRORS =============================\n";
    private final String CAPTURED_OUTPUT_HEADER = "======================== CAPTURED OUTPUT =========================\n";

    /**
     * Called when a test is selected in the tree (shows its results in the text output text component).
     * Makes the line tracker aware of the changes so that links are properly created.
     */
    /*default*/void onSelectResult(PyUnitTestResult result) {
        tempOnSelectResult.clear();

        boolean addedErrors = false;
        if (result != null) {
            if (result.errorContents != null && result.errorContents.length() > 0) {
                addedErrors = true;
                tempOnSelectResult.append(ERRORS_HEADER);
                tempOnSelectResult.append(result.errorContents);
            }

            if (result.capturedOutput != null && result.capturedOutput.length() > 0) {
                if (tempOnSelectResult.length() > 0) {
                    tempOnSelectResult.append("\n");
                }
                tempOnSelectResult.append(CAPTURED_OUTPUT_HEADER);
                tempOnSelectResult.append(result.capturedOutput);
            }
        }
        String string = tempOnSelectResult.toString();
        testOutputText.setFont(JFaceResources.getFont(IDebugUIConstants.PREF_CONSOLE_FONT));

        testOutputText.setText(string);
        testOutputText.setStyleRange(new StyleRange());

        if (addedErrors) {
            StyleRange range = new StyleRange();
            //Set the proper color if it's available.
            TextAttribute errorTextAttribute = ColorManager.getDefault().getConsoleErrorTextAttribute();
            if (errorTextAttribute != null) {
                range.foreground = errorTextAttribute.getForeground();
            }
            range.start = ERRORS_HEADER.length();
            range.length = result.errorContents.length();
            testOutputText.setStyleRange(range);
        }

        lineTracker.splitInLinesAndAppendToLineTracker(string);
    }

    /**
     * Activates the link that was clicked (if the given style range actually has a link).
     */
    private static final class ActivateLinkmouseListener extends MouseAdapter {

        @Override
        public void mouseUp(MouseEvent e) {
            Widget w = e.widget;
            if (w instanceof StyledText) {
                StyledText styledText = (StyledText) w;
                int offset = styledText.getCaretOffset();
                if (offset >= 0 && offset < styledText.getCharCount()) {
                    StyleRange styleRangeAtOffset = styledText.getStyleRangeAtOffset(offset);
                    if (styleRangeAtOffset instanceof StyleRangeWithCustomData) {
                        StyleRangeWithCustomData styleRangeWithCustomData = (StyleRangeWithCustomData) styleRangeAtOffset;
                        Object l = styleRangeWithCustomData.customData;
                        if (l instanceof IHyperlink) {
                            ((IHyperlink) l).linkActivated();
                        }
                    }
                }
            }
        }
    }

    /**
     * Makes the test double clicked in the tree active in the editor.
     */
    private final class DoubleClickTreeItemMouseListener extends MouseAdapter {
        @Override
        public void mouseDoubleClick(MouseEvent e) {
            if (e.widget == tree) {
                onTriggerGoToTest();
            }
        }
    }

    /**
     * Makes the test with the enter pressed in the tree active in the editor.
     */
    private final class EnterProssedTreeItemKeyListener extends KeyAdapter {
        @Override
        public void keyReleased(KeyEvent e) {
            if (e.widget == tree && (e.keyCode == SWT.LF || e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)) {
                onTriggerGoToTest();
            }
        }
    }

    /**
     * Makes the test currently selected in the tree the active test in the editor.
     */
    public void onTriggerGoToTest() {
        TreeItem[] selection = tree.getSelection();
        if (selection.length >= 1) {
            PyUnitTestResult result = (PyUnitTestResult) selection[0].getData(PY_UNIT_TEST_RESULT);
            result.open();
        }
    }

    /**
     * Relaunches the currently selected tests.
     */
    public void relaunchSelectedTests(String mode) {
        TreeItem[] selection = tree.getSelection();
        List<PyUnitTestResult> resultsToRelaunch = new ArrayList<PyUnitTestResult>();
        PyUnitTestRun testRun = null;
        for (TreeItem item : selection) {
            PyUnitTestResult result = (PyUnitTestResult) item.getData(PY_UNIT_TEST_RESULT);
            if (testRun == null) {
                testRun = result.getTestRun();
            } else {
                if (result.getTestRun() != testRun) {
                    continue; //all must be part of the same test run -- this shouldn't really happen.
                }
            }
            resultsToRelaunch.add(result);
        }
        if (resultsToRelaunch.size() > 0 && testRun != null) {
            testRun.relaunch(resultsToRelaunch, mode);
        }
    }

    /**
     * @return the current test run.
     */
    public PyUnitTestRun getCurrentTestRun() {
        return this.currentRun;
    }

    /**
     * Sets the current run (updates the UI)
     * 
     * Note that it can be called to update the current test run when changing whether only errors should be
     * shown or not (so, we don't check if it's the current or not, just go on and update all).
     */
    public void setCurrentRun(PyUnitTestRun testRun) {
        this.currentRun = testRun;
        tree.setRedraw(false);
        try {
            tree.removeAll();
            testOutputText.setText(""); //Clear initial results (the first added will be selected)
            if (testRun != null) {
                List<PyUnitTestResult> sharedResultsList = testRun.getSharedResultsList();
                for (PyUnitTestResult result : sharedResultsList) {
                    notifyTest(result, false);
                }
            }
            updateCountersAndBar();
        } finally {
            tree.setRedraw(true);
        }
    }

    /**
     * @return returns a copy with the test runs available.
     */
    public List<PyUnitTestRun> getAllTestRuns() {
        synchronized (lockServerListeners) {
            ArrayList<PyUnitTestRun> ret = new ArrayList<PyUnitTestRun>();
            for (PyUnitViewServerListener listener : serverListeners) {
                ret.add(listener.getTestRun());
            }
            return ret;
        }
    }

    /**
     * Sets whether only errors should be shown.
     */
    public void setShowOnlyErrors(boolean b) {
        this.showOnlyErrors = b;
        PydevDebugPlugin.getDefault().getPreferenceStore().setValue(PYUNIT_VIEW_SHOW_ONLY_ERRORS, b);
        this.setCurrentRun(currentRun); //update all!
    }

    /**
     * Removes all the terminated test runs.
     */
    public void clearAllTerminated() {
        synchronized (lockServerListeners) {
            boolean removedCurrent = false;

            for (Iterator<PyUnitViewServerListener> it = serverListeners.iterator(); it.hasNext();) {
                PyUnitTestRun next = it.next().getTestRun();
                if (next.getFinished()) {
                    if (next == this.currentRun) {
                        removedCurrent = true;
                    }
                    it.remove();
                }
            }
            if (removedCurrent) {
                if (serverListeners.size() > 0) {
                    this.setCurrentRun(serverListeners.get(0).getTestRun());
                } else {
                    this.setCurrentRun(null);
                }
            }
        }
    }

    /**
     * Sets the text component to be used (in tests we want to set it externally)
     */
    /*default*/void setTextComponent(StyledText text) {
        this.testOutputText = text;
        onControlCreated.call(text);
        text.addMouseListener(this.activateLinkmouseListener);
    }

    public ICallbackWithListeners getOnControlCreated() {
        return onControlCreated;
    }

    public ICallbackWithListeners getOnControlDisposed() {
        return onControlDisposed;
    }

}
