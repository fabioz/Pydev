package org.python.pydev.debug.pyunit;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.python.pydev.core.log.Log;

/**
 * ViewPart that'll listen to the PyUnitServer and show what's happening (with a green/red bar).
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
 * org.eclipse.jdt.internal.junit.ui.TestRunnerViewPart (but it's really not meant to be reused)
 * 
 */
public class PyUnitView extends ViewPart{

    private SashForm s;
    Tree tree;
    

    @Override
    public void createPartControl(Composite parent) {
        final ToolTipHandler tooltip = new ToolTipHandler(parent.getShell());

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.verticalSpacing = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 2;
        parent.setLayout(layout);

        s = new SashForm(parent, SWT.HORIZONTAL);
        //s.setOrientation(SWT.VERTICAL); //Update orientation: should be automatic!
        GridData layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        s.setLayoutData(layoutData);
                
        tree = new Tree(s, SWT.FULL_SELECTION|SWT.SINGLE);
        tooltip.activateHoverHelp(tree);
        tree.setHeaderVisible(true);
        createColumn("Result", 50);
        createColumn("File", 200);
        createColumn("Test", 100);
        createColumn("Time (s)", 50);

        parent = s;

        
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

    public static void registerPyUnitServer(final PyUnitServer pyUnitServer) {
        //We create a listener before and later set the view so that we don't run into racing condition errors!
        final PyUnitViewServerListener serverListener = new PyUnitViewServerListener(pyUnitServer);
        
        Runnable r = new Runnable() {
            public void run() {
                IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                try {
                    IWorkbenchPage page= workbenchWindow.getActivePage();
                    PyUnitView view = (PyUnitView) page.showView("org.python.pydev.debug.pyunit.pyUnitView", null, IWorkbenchPage.VIEW_VISIBLE);
                    serverListener.setView(view);
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        };
        
        Display.getDefault().asyncExec(r);
    }

    /*default*/ void notifyTest(String status, String location, String test) {
        TreeItem treeItem = new TreeItem(tree, 0);
        File file = new File(location);
        treeItem.setText(new String[]{status, file.getName(), test});
        treeItem.setData ("TIP_TEXT", location);

    }

}
