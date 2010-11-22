package org.python.pydev.debug.pyunit;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.callbacks.CallbackWithListeners;
import org.python.pydev.core.callbacks.ICallbackWithListeners;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.core.uiutils.RunInUiThread;
import org.python.pydev.debug.newconsole.prefs.ColorManager;
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
public class PyUnitView extends ViewPartWithOrientation implements SelectionListener, MouseListener{
    
    public static int MAX_RUNS_TO_KEEP = 10;
    public final ICallbackWithListeners onControlCreated = new CallbackWithListeners();
    public final ICallbackWithListeners onDispose = new CallbackWithListeners();
    private List<PyUnitTestRun> allRuns = new ArrayList<PyUnitTestRun>();
    private PyUnitTestRun currentRun;
    
    private ColorAndStyleCache colorAndStyleCache;

    @SuppressWarnings("unchecked")
    public PyUnitView() {
        List<IViewCreatedObserver> participants = ExtensionHelper.getParticipants(
                ExtensionHelper.PYDEV_VIEW_CREATED_OBSERVER);
        for (IViewCreatedObserver iViewCreatedObserver : participants) {
            iViewCreatedObserver.notifyViewCreated(this);
        }
        colorAndStyleCache= new ColorAndStyleCache(PydevPrefs.getChainedPrefStore());
        IPropertyChangeListener prefListener= new IPropertyChangeListener() {
            
            public void propertyChange(PropertyChangeEvent event) {
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
        };
        PydevPrefs.getChainedPrefStore().addPropertyChangeListener(prefListener);
    }

    private SashForm sash;
    private Tree tree;
    private StyledText text;
    private CounterPanel fCounterPanel;
    private PyUnitProgressBar fProgressBar;
    private Composite fCounterComposite;
    
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
        super.createPartControl(parent);
        final ToolTipHandler tooltip = new ToolTipHandler(parent.getShell());

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
        tooltip.activateHoverHelp(tree);
        tree.setHeaderVisible(true);
        createColumn("Result", 70);
        createColumn("File", 180);
        createColumn("Test", 180);
        createColumn("Time (s)", 80);
        onControlCreated.call(tree);
        
        tree.addMouseListener(this);
        tree.addSelectionListener(this);


        text = new StyledText(sash, SWT.MULTI);
        onControlCreated.call(text);
    }
    
    private void configureToolBar() {
        IActionBars actionBars= getViewSite().getActionBars();
        IToolBarManager toolBar= actionBars.getToolBarManager();
        toolBar.add(new RelaunchErrorsAction(this));
        toolBar.add(new RelaunchAction(this));
        toolBar.add(new StopAction(this));
        toolBar.add(new ShowOnlyFailuresAction(this));
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
    
    @Override
    public void dispose() {
        if(this.tree != null){
            Tree t = this.tree;
            this.tree = null;
            t.dispose();
            onDispose.call(t);
        }
        if(this.text != null){
            StyledText t = this.text;
            this.text = null;
            t.dispose();
            onDispose.call(t);
        }
        super.dispose();
    }

    public static PyUnitViewServerListener registerPyUnitServer(final IPyUnitServer pyUnitServer) {
        return registerPyUnitServer(pyUnitServer, true);
    }
    
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
    
    protected void addTestRunAndMakecurrent(PyUnitTestRun testRun) {
        if(allRuns.size() +1 > MAX_RUNS_TO_KEEP){
            allRuns.remove(0);
        }
        allRuns.add(testRun);
        setCurrentRun(testRun);
    }

    /*default */ void notifyFinished(PyUnitTestRun testRun) {
        if(testRun != currentRun){
            return;
        }
        updateCountersAndBar();
    }

    
    /*default*/ void notifyTest(PyUnitTestResult result) {
        notifyTest(result, true);
    }
    

    /*default*/ void notifyTestsCollected() {
        RunInUiThread.async(new Runnable() {
            
            public void run() {
                updateCountersAndBar();
            }
        });
    }


    
    private void notifyTest(PyUnitTestResult result, boolean updateBar) {
        if(result.getTestRun() != currentRun){
            return;
        }
        if(!showOnlyErrors || (showOnlyErrors && !result.status.equals("ok"))){
            TreeItem treeItem = new TreeItem(tree, 0);
            File file = new File(result.location);
            treeItem.setText(new String[]{result.status, file.getName(), result.test, result.time});
            if(!result.isOk()){
                Color errorColor = getErrorColor();
                treeItem.setForeground(errorColor);
            }
            treeItem.setData ("TIP_TEXT", result.location);
            treeItem.setData("RESULT", result);
        }
        
        if(updateBar){
            updateCountersAndBar();
        }
    }

    public Color getErrorColor() {
        TextAttribute attribute = ColorManager.getDefault().getConsoleErrorTextAttribute();
        Color errorColor = attribute.getForeground();
        return errorColor;
    }

    private void updateCountersAndBar() {
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
    

    private void setShowBarWithError(boolean hasError, boolean hasRuns, boolean finished) {
        fProgressBar.reset(hasError, false, hasRuns?1:0, finished?1:2);
    }

    private FastStringBuffer temp = new FastStringBuffer();
    private boolean showOnlyErrors;
    public void widgetSelected(SelectionEvent e) {
        if(e.item != null){
            PyUnitTestResult result = (PyUnitTestResult) e.item.getData("RESULT");
            temp.clear();
            if(result.capturedOutput != null){
                temp.append(result.capturedOutput);
            }
            if(result.errorContents != null){
                temp.append(result.errorContents);
            }
            text.setText(temp.toString());
        }
        
    }

    public void widgetDefaultSelected(SelectionEvent e) {
    }

    public void mouseDoubleClick(MouseEvent e) {
        if(e.widget == tree){
            System.out.println("Double click tree.");
        }
    }

    public void mouseDown(MouseEvent e) {
    }

    public void mouseUp(MouseEvent e) {
    }

    public PyUnitTestRun getCurrentTestRun() {
        return this.currentRun;
    }

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
    }

    public List<PyUnitTestRun> getAllTestRuns() {
        return new ArrayList<PyUnitTestRun>(allRuns);
    }

    public void setShowOnlyErrors(boolean b) {
        this.showOnlyErrors = b;
        this.setCurrentRun(currentRun); //update all!
    }

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

}
