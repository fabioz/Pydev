package org.python.pydev.debug.codecoverage;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.tree.AllowValidPathsFilter;
import org.python.pydev.tree.FileTreeLabelProvider;
import org.python.pydev.tree.FileTreePyFilesProvider;
import org.python.pydev.utils.ProgressAction;
import org.python.pydev.utils.ProgressOperation;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view shows data obtained from the model. The sample creates a
 * dummy model on the fly, but a real implementation would connect to the model available either in this or another plug-in (e.g. the
 * workspace). The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be presented in the view. Each view can present the same model objects
 * using different labels and icons, if needed. Alternatively, a single label provider can be shared between views in order to ensure that
 * objects of the same type are presented in the same way everywhere.
 * <p>
 */

public class PyCodeCoverageView extends ViewPart {
    //layout stuff
    private Composite leftComposite;

    //actions
    /**
     * double click the tree
     */
    private DoubleClickTreeAction doubleClickAction = new DoubleClickTreeAction();

    /**
     * changed selected element
     */
    private SelectionChangedTreeAction selectionChangedAction = new SelectionChangedTreeAction();

    /**
     * choose new dir
     */
    private ProgressAction chooseAction = new ChooseAction();

    /**
     * clear the results (and erase .coverage file)
     */
    protected ProgressAction clearAction = new ClearAction();

    /**
     * get the new results from the .coverage file
     */
    protected RefreshAction refreshAction = new RefreshAction();

    //buttons
    private Button clearButton;

    private Button refreshButton;

    private Button chooseButton;

    //write the results here
    private Text text;

    //tree som that user can browse results.
    private TreeViewer viewer;

    /**
     *  
     */
    private File lastChosenFile;

    private SashForm s;

    //Actions ------------------------------
    /**
     * In this action we have to go and refresh all the info based on the chosen dir.
     * 
     * @author Fabio Zadrozny
     */
    private final class RefreshAction extends ProgressAction {
        public void run() {
            try {
                PyCoverage.getPyCoverage().refreshCoverageInfo(lastChosenFile, this.monitor);

                viewer.setInput(lastChosenFile); //new files may have been added.
                text.setText("Refreshed info.");
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }
    }

    /**
     * 
     * @author Fabio Zadrozny
     */
    private final class ClearAction extends ProgressAction {
        public void run() {

            PyCoverage.getPyCoverage().clearInfo();

            MessageDialog.openInformation(getSite().getShell(), "Cleared", "All the coverage data has been cleared!");

            text.setText("Data cleared (NOT REFRESHED).");
        }
    }

    /**
     * 
     * @author Fabio Zadrozny
     */
    private final class SelectionChangedTreeAction extends Action {
        public void run() {
            run((IStructuredSelection) viewer.getSelection());
        }

        /**
         * @param event
         */
        public void runWithEvent(SelectionChangedEvent event) {
            run((IStructuredSelection) event.getSelection());
        }

        public void run(IStructuredSelection selection) {
            Object obj = selection.getFirstElement();

            if (obj == null)
                return;

            File realFile = new File(obj.toString());
            if (realFile.exists()) {
                text.setText(PyCoverage.getPyCoverage().cache.getStatistics(realFile));
            }
        }

    }

    /**
     * 
     * @author Fabio Zadrozny
     */
    private final class DoubleClickTreeAction extends ProgressAction {

        public void run() {
            run(viewer.getSelection());
        }

        /**
         * @param event
         */
        public void runWithEvent(DoubleClickEvent event) {
            run(event.getSelection());
        }

        public void run(ISelection selection) {
            try {
                Object obj = ((IStructuredSelection) selection).getFirstElement();

                File realFile = new File(obj.toString());
                if (realFile.exists() && !realFile.isDirectory()) {
                    ItemPointer p = new ItemPointer(realFile, new Location(-1, -1), null);
                    PyOpenAction act = new PyOpenAction();
                    act.run(p);

                    if (act.editor instanceof PyEdit) {
                        PyEdit e = (PyEdit) act.editor;
                        IEditorInput input = e.getEditorInput();
                        IFile original = (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
                        if (original == null)
                            return;
                        IDocument document = e.getDocumentProvider().getDocument(e.getEditorInput());

                        String type = IMarker.PROBLEM;
                        original.deleteMarkers(type, false, 1);

                        String message = "Not Executed";

                        FileNode cache = (FileNode) PyCoverage.getPyCoverage().cache.getFile(realFile);
                        for (Iterator it = cache.notExecutedIterator(); it.hasNext();) {
                            Map map = new HashMap();
                            int errorLine = ((Integer) it.next()).intValue() - 1;

                            IRegion region = document.getLineInformation(errorLine);
                            int errorEnd = region.getOffset();
                            int errorStart = region.getOffset() + region.getLength();

                            map.put(IMarker.MESSAGE, message);
                            map.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));
                            map.put(IMarker.LINE_NUMBER, new Integer(errorLine));
                            map.put(IMarker.CHAR_START, new Integer(errorStart));
                            map.put(IMarker.CHAR_END, new Integer(errorEnd));
                            map.put(IMarker.TRANSIENT, Boolean.valueOf(true));
                            map.put(IMarker.PRIORITY, new Integer(IMarker.PRIORITY_HIGH));

                            MarkerUtilities.createMarker(original, map, type);
                        }

                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * 
     * @author Fabio Zadrozny
     */
    private final class ChooseAction extends ProgressAction {
        public void run() {
            ContainerSelectionDialog dialog = new ContainerSelectionDialog(getSite().getShell(), null, false, "Test");
            dialog.open();
            Object[] objects = dialog.getResult();
            if (objects.length == 1) { //only one folder can be selected
                if (objects[0] instanceof IPath) {
                    IPath p = (IPath) objects[0];

                    
                    //p = PydevPlugin.getLocationFromWorkspace(p);
                    IWorkspace w = ResourcesPlugin.getWorkspace();
                    IContainer folderFolLocation = w.getRoot().getContainerForLocation(p);
                    File file = null;
                    if(folderFolLocation != null){
                    	file = folderFolLocation.getRawLocation().toFile();
                    }else{
                    	file = p.toFile().getAbsoluteFile();
                    }
                    lastChosenFile = file;
                    refreshAction.monitor = this.monitor;
                    refreshAction.run();
                }
            }

            //previous code...
            //DirectoryDialog dialog = new DirectoryDialog(getSite().getShell());
            //if (lastChosenFile != null && lastChosenFile.exists()) {
            //    dialog.setFilterPath(lastChosenFile.getParent());
            //}
            //String string = dialog.open();
            //if (string != null) {
            //    File file = new File(string);
            //    lastChosenFile = file;
            //    refreshAction.monitor = this.monitor;
            //    refreshAction.run();
            //}

        }
    }

    // Class -------------------------------------------------------------------

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
     * This is a callback that will allow us to create the viewer and initialize it.
     */
    public void createPartControl(Composite parent) {

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.verticalSpacing = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 2;
        parent.setLayout(layout);

        s = new SashForm(parent, SWT.HORIZONTAL);
        GridData layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        s.setLayoutData(layoutData);

        parent = s;

        leftComposite = new Composite(parent, SWT.MULTI);
        layout = new GridLayout();
        layout.numColumns = 1;
        layout.verticalSpacing = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 2;
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        leftComposite.setLayoutData(layoutData);
        leftComposite.setLayout(layout);

        text = new Text(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        try {
            text.setFont(new Font(null, "Courier new", 10, 0));
        } catch (Exception e) {
            //ok, might mot be available.
        }
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        text.setLayoutData(layoutData);

        parent = leftComposite;

        //choose button
        chooseButton = new Button(parent, SWT.PUSH);
        createButton(parent, chooseButton, "Choose dir!", chooseAction);
        //end choose button

        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(new FileTreePyFilesProvider());
        viewer.setLabelProvider(new FileTreeLabelProvider());
        viewer.addFilter(new AllowValidPathsFilter());

        hookViewerActions();

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        viewer.getControl().setLayoutData(layoutData);

        //clear results button
        clearButton = new Button(parent, SWT.PUSH);
        createButton(parent, clearButton, "Clear coverage information!", clearAction);
        //end choose button

        //refresh button
        refreshButton = new Button(parent, SWT.PUSH);
        createButton(parent, refreshButton, "Refresh coverage information!", refreshAction);
        //end choose button

        this.refresh();

    }

    /**
     * Create button with hooked action.
     * 
     * @param parent
     * @param button
     * @param string
     */
    private void createButton(Composite parent, Button button, String txt, final ProgressAction action) {
        GridData layoutData;
        button.setText(txt);
        button.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                ProgressOperation.startAction(getSite().getShell(), action);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        button.setLayoutData(layoutData);
    }

    /**
     * 
     * Add the double click and selection changed action
     */
    private void hookViewerActions() {
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                doubleClickAction.runWithEvent(event);
            }
        });

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                selectionChangedAction.runWithEvent(event);
            }

        });
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        viewer.getControl().setFocus();
    }

}