package org.python.pydev.debug.pyunit;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IToolBarManager;
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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.callbacks.CallbackWithListeners;
import org.python.pydev.core.callbacks.ICallbackWithListeners;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.core.tooltips.presenter.ToolTipPresenterHandler;
import org.python.pydev.core.uiutils.RunInUiThread;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.prefs.ColorManager;
import org.python.pydev.debug.ui.ILinkContainer;
import org.python.pydev.debug.ui.PythonConsoleLineTracker;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.ui.ColorAndStyleCache;
import org.python.pydev.ui.IViewCreatedObserver;


/**
 * ViewPart that'll listen to the PyUnitServer and show what'sash happening (with a green/red bar).
 * 
 * Other features should include:
 * 
 * - Relaunching the tests
 * - Relaunching only the tests that failed
 * - Show stack traces of errors (when selected)
 * - Show output of test cases (when selected)
 * - If a string was different, show a diff
 * - Show tests ran
 * - Show only failed tests ran
 * - Stop execution of the tests
 * - Show the number of sucesses, failures and errors
 * - Double-click to go to test
 * - Show time of test (and allow reorderig based on it)
 * - Auto-show on test run should be an option
 * 
 * 
 * References:
 * 
 * http://www.eclipse.org/swt/snippets/
 * 
 * Notes on tree/table: http://www.eclipse.org/swt/R3_2/new_and_noteworthy.html (see links below)
 * 
 * Sort table by column (applicable to tree: http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet2.java?view=markup&content-type=text%2Fvnd.viewcvs-markup&revision=HEAD )
 * Reorder columns by drag ( http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet193.java?view=markup&content-type=text%2Fvnd.viewcvs-markup&revision=HEAD )
 * Sort indicator in column header ( http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet192.java?view=markup&content-type=text%2Fvnd.viewcvs-markup&revision=HEAD )
 * 
 * 
 * org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart (but it'sash really not meant to be reused)
 * 
 */
@SuppressWarnings("rawtypes")
public class PyUnitView extends ViewPartWithOrientation{
    
    public static final String PYUNIT_VIEW_SHOW_ONLY_ERRORS = "PYUNIT_VIEW_SHOW_ONLY_ERRORS";
    public static final boolean PYUNIT_VIEW_DEFAULT_SHOW_ONLY_ERRORS = true;
    
    public static int MAX_RUNS_TO_KEEP = 15;
    public final ICallbackWithListeners onControlCreated = new CallbackWithListeners();
    public final ICallbackWithListeners onDispose = new CallbackWithListeners();
    private List<PyUnitTestRun> allRuns = new ArrayList<PyUnitTestRun>();
    private PyUnitTestRun currentRun;
    private PythonConsoleLineTracker lineTracker = new PythonConsoleLineTracker();
    ActivateLinkmouseListener activateLinkmouseListener = new ActivateLinkmouseListener();
    
    /*default*/ PythonConsoleLineTracker getLineTracker() {
        return lineTracker;
    }
    
    private ColorAndStyleCache colorAndStyleCache;

    @SuppressWarnings("unchecked")
    public PyUnitView() {
        PydevDebugPlugin plugin = PydevDebugPlugin.getDefault();
        
        if(plugin != null){
            this.showOnlyErrors = plugin.getPreferenceStore().getBoolean(PYUNIT_VIEW_SHOW_ONLY_ERRORS);
        }
        
        List<IViewCreatedObserver> participants = ExtensionHelper.getParticipants(
                ExtensionHelper.PYDEV_VIEW_CREATED_OBSERVER);
        for (IViewCreatedObserver iViewCreatedObserver : participants) {
            iViewCreatedObserver.notifyViewCreated(this);
        }
        
        lineTracker.init(new ILinkContainer() {
            
            public void addLink(IHyperlink link, int offset, int length) {
                if(testOutputText == null){
                    return;
                }
                StyleRange range = new StyleRange();
                range.underline = true;
                try{
                    range.underlineStyle = SWT.UNDERLINE_LINK;
                }catch(Throwable e){
                    //Ignore (not available on earlier versions of eclipse)
                }
                
                //Set the proper color if it's available.
                TextAttribute textAttribute = ColorManager.getDefault().getHyperlinkTextAttribute();
                if(textAttribute != null){
                    range.foreground = textAttribute.getForeground();
                }
                range.start = offset;
                range.length = length+1;
                range.data = link;
                testOutputText.setStyleRange(range);
            }

            
            public String getContents(int lineOffset, int lineLength) throws BadLocationException {
                if(testOutputText == null){
                    return "";
                }
                if(lineLength <= 0){
                    return "";
                }
                return testOutputText.getText(lineOffset, lineOffset+lineLength);
            }
        });
    }

    private SashForm sash;
    private Tree tree;
    private StyledText testOutputText;
    private CounterPanel fCounterPanel;
    private PyUnitProgressBar fProgressBar;
    private Composite fCounterComposite;
    private IPropertyChangeListener prefListener;
    
    /**
     * Whether we should show only errors or not.
     */
    private boolean showOnlyErrors;
    
    public PyUnitProgressBar getProgressBar() {
        return fProgressBar;
    }
    
    public CounterPanel getCounterPanel() {
        return fCounterPanel;
    }
    
    public Tree getTree() {
        return tree;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void createPartControl(Composite parent) {
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
        
        fCounterComposite= new Composite(parent, SWT.NONE);
        layout= new GridLayout();
        fCounterComposite.setLayout(layout);
        fCounterComposite.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

        fCounterPanel = new CounterPanel(fCounterComposite);
        fCounterPanel.setLayoutData(
            new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
        fProgressBar = new PyUnitProgressBar(fCounterComposite);
        fProgressBar.setLayoutData(
                new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

        sash = new SashForm(parent, SWT.HORIZONTAL);
        GridData layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        sash.setLayoutData(layoutData);
                
        tree = new Tree(sash, SWT.FULL_SELECTION|SWT.SINGLE);
        tooltip.install(tree);
        tree.setHeaderVisible(true);
        createColumn("Result", 70);
        createColumn("Test", 180);
        createColumn("File", 180);
        createColumn("Time (s)", 80);
        onControlCreated.call(tree);
        
        tree.addMouseListener(new DoubleClickTreeItemMouseListener());
        tree.addKeyListener(new EnterProssedTreeItemKeyListener());
        tree.addSelectionListener(new SelectResultSelectionListener());
        
        if(PydevPlugin.getDefault() != null){
            colorAndStyleCache= new ColorAndStyleCache(PydevPrefs.getChainedPrefStore());
            prefListener= new IPropertyChangeListener() {
                
                public void propertyChange(PropertyChangeEvent event) {
                    if(tree != null){
                        String property = event.getProperty();
                        if(ColorAndStyleCache.isColorOrStyleProperty(property)){
                            colorAndStyleCache.reloadNamedColor(property);
                            Color errorColor = getErrorColor();
                            TreeItem[] items = tree.getItems();
                            for(TreeItem item:items){
                                PyUnitTestResult result = (PyUnitTestResult) item.getData("RESULT");
                                if(result!= null && !result.isOk()){
                                    item.setForeground(errorColor);
                                }
                            }
                            
                            if(fProgressBar != null){
                                fProgressBar.updateErrorColor(true);
                            }
                        }
                    }
                }
            };
            PydevPrefs.getChainedPrefStore().addPropertyChangeListener(prefListener);
        }

        StyledText text = new StyledText(sash, SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
        this.setTextComponent(text);
    }
    
    private void configureToolBar() {
        IActionBars actionBars= getViewSite().getActionBars();
        IToolBarManager toolBar= actionBars.getToolBarManager();
        toolBar.add(new RelaunchErrorsAction(this));
        toolBar.add(new RelaunchAction(this));
        toolBar.add(new StopAction(this));
        ShowOnlyFailuresAction action = new ShowOnlyFailuresAction(this);
        toolBar.add(action);
        action.setChecked(this.showOnlyErrors);
        toolBar.add(new HistoryAction(this));
        
    }

    @Override
    protected void setNewOrientation(int orientation) {
        if(sash != null && !sash.isDisposed() && fCounterComposite != null && !fCounterComposite.isDisposed()){
            GridLayout layout= (GridLayout) fCounterComposite.getLayout();
            if(orientation == VIEW_ORIENTATION_HORIZONTAL){
                sash.setOrientation(SWT.HORIZONTAL);
                layout.numColumns = 2;
                
            }else{
                sash.setOrientation(SWT.VERTICAL);
                layout.numColumns = 1;
            }
            fParent.layout();
        }
    }
    
    
    private void createColumn(String text, int width) {
        TreeColumn column1;
        column1 = new TreeColumn(tree, SWT.LEFT);
        column1.setText(text);
        column1.setWidth(width);
        column1.setMoveable(true);
    }

    @Override
    public void setFocus() {
        
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void dispose() {
        if(this.tree != null){
            Tree t = this.tree;
            this.tree = null;
            t.dispose();
            onDispose.call(t);
        }
        if(this.testOutputText != null){
            StyledText t = this.testOutputText;
            this.testOutputText = null;
            t.dispose();
            onDispose.call(t);
        }
        if(this.fCounterPanel != null){
            this.fCounterPanel.dispose();
            this.fCounterPanel = null;
        }
        if(this.prefListener != null){
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
        final PyUnitViewServerListener serverListener = new PyUnitViewServerListener(pyUnitServer, pyUnitServer.getPyUnitLaunch());
        
        Runnable r = new Runnable() {
            public void run() {
                try {
                    PyUnitView view = getView();
                    if(view != null){
                        serverListener.setView(view);
                        view.addTestRunAndMakecurrent(serverListener.getTestRun());
                    }else{
                        Log.log("Could not get pyunit view");
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        };
        
        if(async){
            Display.getDefault().asyncExec(r);
        }else{
            Display.getDefault().syncExec(r);
        }
        return serverListener;
    }

    
    /**
     * Gets the py unit view. May only be called in the UI thread. If the view is not visible, shows it.
     */
    public static PyUnitView getView() {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        try {
            if(workbenchWindow == null){
                return null;
            }
            IWorkbenchPage page= workbenchWindow.getActivePage();
            return (PyUnitView) page.showView("org.python.pydev.debug.pyunit.pyUnitView", null, IWorkbenchPage.VIEW_VISIBLE);
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }
    
    /**
     * Adds a given test run to our list of test runs and makes it current. If there are too many test runs, removes 
     * the oldest one before adding another one.
     */
    protected void addTestRunAndMakecurrent(PyUnitTestRun testRun) {
        if(allRuns.size() +1 > MAX_RUNS_TO_KEEP){
            allRuns.remove(0);
        }
        allRuns.add(testRun);
        setCurrentRun(testRun);
    }

    /**
     * Notifies that the test run has finished.
     */
    /*default */ void notifyFinished(PyUnitTestRun testRun) {
        if(testRun != currentRun){
            return;
        }
        asyncUpdateCountersAndBar();
    }

    
    /**
     * Notifies that a test result has been added.
     */
    /*default*/ void notifyTest(PyUnitTestResult result) {
        notifyTest(result, true);
    }
    

    /**
     * Used to update the number of tests available. 
     */
    /*default*/ void notifyTestsCollected(PyUnitTestRun testRun) {
        if(testRun != currentRun){
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
        if(result.getTestRun() != currentRun){
            return;
        }
        if(!showOnlyErrors || (showOnlyErrors && !result.status.equals("ok"))){
            TreeItem treeItem = new TreeItem(tree, 0);
            File file = new File(result.location);
            treeItem.setText(new String[]{result.status, result.test, file.getName(), result.time});
            if(!result.isOk()){
                Color errorColor = getErrorColor();
                treeItem.setForeground(errorColor);
            }
            
            treeItem.setData (ToolTipPresenterHandler.TIP_DATA, result);
            treeItem.setData("RESULT", result);
        }
        
        if(updateBar){
            updateCountersAndBar();
        }
    }

    /**
     * @return the color that should be used for errors.
     */
    public Color getErrorColor() {
        TextAttribute attribute = ColorManager.getDefault().getConsoleErrorTextAttribute();
        Color errorColor = attribute.getForeground();
        return errorColor;
    }

    /**
     * Updates the number of test runs and the bar with the current progress.
     */
    private void updateCountersAndBar() {
        if(fCounterPanel == null){
            return;
        }
        if(currentRun != null){
            String totalNumberOfRuns = currentRun.getTotalNumberOfRuns();
            int numberOfRuns = currentRun.getNumberOfRuns();
            int numberOfErrors = currentRun.getNumberOfErrors();
            int numberOfFailures = currentRun.getNumberOfFailures();
            
            fCounterPanel.setRunValue(numberOfRuns, totalNumberOfRuns);
            fCounterPanel.setErrorValue(numberOfErrors);
            fCounterPanel.setFailureValue(numberOfFailures);
            
            try {
                int totalAsInt;
                if(currentRun.getFinished()){
                    totalAsInt = numberOfRuns;
                }else{
                    totalAsInt = Integer.parseInt(totalNumberOfRuns);
                }
                fProgressBar.reset(numberOfErrors + numberOfFailures > 0, false, numberOfRuns, totalAsInt);
            } catch (NumberFormatException e) {
                //use this if we're unable to collect the number of runs as a string.
                setShowBarWithError(numberOfErrors + numberOfFailures > 0, numberOfRuns > 0, currentRun.getFinished());
            }
        }else{
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
        fProgressBar.reset(hasError, false, hasRuns?1:0, finished?1:2);
    }


    
    /**
     * Selection listener added to the tree so that the text output is updated when the selection changes.
     */
    private final class SelectResultSelectionListener extends SelectionAdapter{
        public void widgetSelected(SelectionEvent e) {
            if(e.item != null){
                PyUnitTestResult result = (PyUnitTestResult) e.item.getData("RESULT");
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
    /*default*/ void onSelectResult(PyUnitTestResult result) {
        tempOnSelectResult.clear();
        
        boolean addedErrors = false;
        if(result != null){
            if(result.errorContents != null && result.errorContents.length() > 0){
                addedErrors = true;
                tempOnSelectResult.append(ERRORS_HEADER);
                tempOnSelectResult.append(result.errorContents);
            }
            
            if(result.capturedOutput != null && result.capturedOutput.length() > 0){
                if(tempOnSelectResult.length() > 0){
                    tempOnSelectResult.append("\n");
                }
                tempOnSelectResult.append(CAPTURED_OUTPUT_HEADER);
                tempOnSelectResult.append(result.capturedOutput);
            }
        }
        String string = tempOnSelectResult.toString();
        testOutputText.setText(string);
        testOutputText.setStyleRange(new StyleRange());
        
        if(addedErrors){
            StyleRange range = new StyleRange();
            //Set the proper color if it's available.
            TextAttribute errorTextAttribute = ColorManager.getDefault().getConsoleErrorTextAttribute();
            if(errorTextAttribute != null){
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
        
        public void mouseUp(MouseEvent e) {
            Widget w = e.widget;
            if(w instanceof StyledText){
                StyledText styledText = (StyledText) w;
                int offset = styledText.getCaretOffset();
                if(offset >= 0 && offset < styledText.getCharCount()){
                    StyleRange styleRangeAtOffset = styledText.getStyleRangeAtOffset(offset);
                    if(styleRangeAtOffset != null){
                        Object l = styleRangeAtOffset.data;
                        if(l instanceof IHyperlink){
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
        public void mouseDoubleClick(MouseEvent e) {
            if(e.widget == tree){
                onTriggerGoToTest();
            }
        }
    }
    
    /**
     * Makes the test with the enter pressed in the tree active in the editor.
     */
    private final class EnterProssedTreeItemKeyListener extends KeyAdapter {
        public void keyReleased(KeyEvent e) {
            if(e.widget == tree && (e.keyCode == SWT.LF || e.keyCode == SWT.CR)){
                onTriggerGoToTest();
            }
        }
    }
    

    /**
     * Makes the test currently selected in the tree the active test in the editor.
     */
    public void onTriggerGoToTest() {
        TreeItem[] selection = tree.getSelection();
        if(selection.length >= 1){
            PyUnitTestResult result = (PyUnitTestResult) selection[0].getData("RESULT");
            result.open();
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
        tree.removeAll();
        if(testRun != null){
            List<PyUnitTestResult> sharedResultsList = testRun.getSharedResultsList();
            for (PyUnitTestResult result : sharedResultsList) {
                notifyTest(result, false);
            }
        }
        updateCountersAndBar();
        testOutputText.setText(""); //leave no result selected
    }

    /**
     * @return returns a copy with the test runs available.
     */
    public List<PyUnitTestRun> getAllTestRuns() {
        return new ArrayList<PyUnitTestRun>(allRuns);
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
        boolean removedCurrent = false;
        
        for(Iterator<PyUnitTestRun> it=this.allRuns.iterator();it.hasNext();){
            PyUnitTestRun next = it.next();
            if(next.getFinished()){
                if(next == this.currentRun){
                    removedCurrent = true;
                }
                it.remove();
            }
        }
        if(removedCurrent){
            if(this.allRuns.size() > 0){
                this.setCurrentRun(this.allRuns.get(0));
            }else{
                this.setCurrentRun(null);
            }
        }
    }

    /**
     * Sets the text component to be used (in tests we want to set it externally)
     */
    @SuppressWarnings("unchecked")
    /*default*/ void setTextComponent(StyledText text) {
        this.testOutputText = text;
        onControlCreated.call(text);
        text.addMouseListener(this.activateLinkmouseListener);
    }


}
