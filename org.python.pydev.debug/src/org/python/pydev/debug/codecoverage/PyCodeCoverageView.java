package org.python.pydev.debug.codecoverage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.part.ViewPart;
import org.python.pydev.editor.refactoring.PyRefactoring;
import org.python.pydev.tree.AllowValidPathsFilter;
import org.python.pydev.tree.FileTreeLabelProvider;
import org.python.pydev.tree.FileTreePyFilesProvider;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class PyCodeCoverageView extends ViewPart implements IPropertyListener, IStructuredContentProvider {

    private TreeViewer viewer;

    private Action doubleClickAction;

    private Action chooseAction;

    protected Action clearAction;

    private Button clearButton;

    private List elements = new ArrayList();

    private Button chooseButton;

    private Composite rComposite;

    private Text text;

    protected String currentDir;

    /**
     * The constructor.
     */
    public PyCodeCoverageView() {
    }

    public void refresh() {
        viewer.refresh();
        getSite().getPage().bringToTop(this);
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    public void createPartControl(Composite parent) {
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.verticalSpacing = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 2;
        parent.setLayout(layout);

        rComposite = new Composite(parent, SWT.MULTI);
        layout = new GridLayout();
        layout.numColumns = 1;
        layout.verticalSpacing = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 2;
        GridData layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        rComposite.setLayoutData(layoutData);
        rComposite.setLayout(layout);

        text = new Text(parent, SWT.MULTI);
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        text.setLayoutData(layoutData);
        text.setEditable(false);

        parent = rComposite;

        //choose button
        chooseButton = new Button(parent, SWT.PUSH);
        chooseAction = new Action() {
            public void run() {
                DirectoryDialog dialog = new DirectoryDialog(getSite().getShell());
                String string = dialog.open();
                if (string != null) {
                    text.setText("Chosen dir:" + string);
                    notifyDirChanged(string);
                }
            }
        };
        createButton(parent, chooseButton, "Choose dir!", chooseAction);
        //end choose button

        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(new FileTreePyFilesProvider());
        viewer.setLabelProvider(new FileTreeLabelProvider());
        viewer.addFilter(new AllowValidPathsFilter());

        doubleClickAction = new Action() {
            public void run() {
                ISelection selection = viewer.getSelection();
                Object obj = ((IStructuredSelection) selection).getFirstElement();

                File realFile = new File(obj.toString());
                if (realFile.exists()) {
                    System.out.println("opening file:" + obj.toString());
                    //                    ItemPointer p = new ItemPointer(realFile, new
                    // Location(-1, -1), null);
                    //                    new PyOpenAction().run(p);
                }
            }
        };
        hookViewerActions();

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        viewer.getControl().setLayoutData(layoutData);

        //clear results button
        clearButton = new Button(parent, SWT.PUSH);
        clearAction = new Action() {
            public void run() {

                PyCoverage.getPyCoverage().clearInfo();

                MessageDialog.openInformation(getSite().getShell(), "Cleared",
                        "All the coverage data has been cleared!");

                text.setText("");
            }
        };
        createButton(parent, clearButton, "Clear coverage information!", clearAction);
        //end choose button

        this.refresh();

    }

    /**
     * @param string
     */
    protected void notifyDirChanged(String newDir) {
        File file = new File(newDir);
        PyCoverage.getPyCoverage().refreshCoverageInfo(file);
        viewer.setInput(file);

    }

    /**
     * @param parent
     * @param button
     * @param string
     */
    private void createButton(Composite parent, Button button, String txt, final Action action) {
        GridData layoutData;
        button.setText(txt);
        button.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                action.run();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        button.setLayoutData(layoutData);
    }

    private void hookViewerActions() {
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                doubleClickAction.run();
            }
        });

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = (IStructuredSelection) event.getSelection();

                Object selected_file = selection.getFirstElement();
                System.out.println("Number of items selected is " + selection.size());
                System.out.println("selected_file = " + selected_file);
            }

        });
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    public void propertyChanged(Object source, int propId) {
        if (source == null) {
            return;
        }

        Object[] sources = (Object[]) source;

        if (sources[0] == null || sources[1] == null) {
            return;
        }

        if (sources[0] == PyRefactoring.getPyRefactoring() && propId == PyRefactoring.REFACTOR_RESULT) {

            elements.clear();
            elements.addAll((Collection) sources[1]);
        }
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public Object[] getElements(Object parent) {
        return elements.toArray();
    }

}