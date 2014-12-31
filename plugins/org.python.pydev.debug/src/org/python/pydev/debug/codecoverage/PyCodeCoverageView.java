/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.codecoverage;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.ui.launching.PythonRunnerCallbacks;
import org.python.pydev.debug.ui.launching.PythonRunnerCallbacks.CreatedCommandLineParams;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editorinput.PyOpenEditor;
import org.python.pydev.editorinput.PySourceLocatorBase;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_core.callbacks.ICallbackWithListeners;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.FontUtils;
import org.python.pydev.shared_ui.IFontUsage;
import org.python.pydev.shared_ui.tooltips.presenter.StyleRangeWithCustomData;
import org.python.pydev.shared_ui.tree.PyFilteredTree;
import org.python.pydev.shared_ui.utils.IViewWithControls;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.tree.AllowValidPathsFilter;
import org.python.pydev.tree.FileTreeLabelProvider;
import org.python.pydev.tree.FileTreePyFilesProvider;
import org.python.pydev.ui.NotifyViewCreated;
import org.python.pydev.ui.ViewPartWithOrientation;
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

@SuppressWarnings({ "rawtypes", "unchecked" })
public class PyCodeCoverageView extends ViewPartWithOrientation implements IViewWithControls {

    public static final String PYCOVERAGE_VIEW_ORIENTATION = "PYCOVERAGE_VIEW_ORIENTATION";

    @Override
    public String getOrientationPreferencesKey() {
        return PYCOVERAGE_VIEW_ORIENTATION;
    }

    public static String PY_COVERAGE_VIEW_ID = "org.python.pydev.views.PyCodeCoverageView";

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
     * Opens the coverage folder action
     */
    protected Action openCoverageFolderAction = new OpenCoverageFolderAction();

    /**
     * clear the results (and erase .coverage file)
     */
    protected ProgressAction clearAction = new ClearAction();

    protected Action selectColumnsAction = new SelectColumnsAction();

    /**
     * get the new results from the .coverage file
     */
    protected RefreshAction refreshAction = new RefreshAction();

    private Button chooseButton;

    //write the results here
    private StyledText text;

    //tree so that user can browse results.
    private TreeViewer viewer;

    public TreeViewer getTreeViewer() {
        return viewer;
    }

    private SashForm sash;

    /*default for testing */Button allRunsGoThroughCoverage;
    /*default for testing */Button clearCoverageInfoOnNextLaunch;
    /*default for testing */Button refreshCoverageInfoOnNextLaunch;

    private Label labelErrorFolderNotSelected;

    //Actions ------------------------------
    /**
     * In this action we have to go and refresh all the info based on the chosen dir.
     *
     * @author Fabio Zadrozny
     */
    private final class OpenCoverageFolderAction extends Action {

        public OpenCoverageFolderAction() {
            this.setText("Open folder with .coverage files.");
        }

        @Override
        public void run() {
            try {
                FileUtils.openDirectory(PyCoverage.getCoverageDirLocation());
            } catch (Exception e) {
                Log.log(e);
            }
        }

    }

    /**
     * In this action we have to go and refresh all the info based on the chosen dir.
     *
     * @author Fabio Zadrozny
     */
    private final class RefreshAction extends ProgressAction {

        public RefreshAction() {
            this.setText("Refresh coverage information");
        }

        @Override
        public void run() {
            try {
                executeRefreshAction(this.monitor);
            } catch (Exception e) {
                Log.log(e);
            }
        }

    }

    /**
     * Note that this method should never be directly called.
     *
     * For a proper refresh do:
     *      ProgressOperation.startAction(getSite().getShell(), action, true);
     */
    /*default for tests*/void executeRefreshAction(IProgressMonitor monitor) {
        if (viewer == null) { //Safeguard: if the view containing this one was removed and for some reason not properly disposed, this would occur.
            return;
        }
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        IContainer lastChosenDir = PyCoveragePreferences.getLastChosenDir();
        if (lastChosenDir == null) {
            return;
        }
        PyCoverage.getPyCoverage().refreshCoverageInfo(lastChosenDir, monitor);

        File input = lastChosenDir.getLocation().toFile();
        viewer.refresh();
        ITreeContentProvider contentProvider = (ITreeContentProvider) viewer.getContentProvider();
        ISelection selection = viewer.getSelection();
        if (selection instanceof StructuredSelection) {
            StructuredSelection current = (StructuredSelection) selection;
            Object firstElement = current.getFirstElement();
            if (firstElement != null) {
                onSelectedFileInTree(firstElement);
                return;
            }
        }
        //If the current selection wasn't valid, select something or notify that nothing is selected.
        Object[] children = contentProvider.getChildren(input);
        if (children.length > 0) {
            viewer.setSelection(new StructuredSelection(children[0]));
        } else {
            onSelectedFileInTree(null);
        }
    }

    private final ICallbackListener<Process> afterCreatedProcessListener = new ICallbackListener<Process>() {

        public Object call(final Process obj) {
            if (viewer == null) { //Safeguard: if the view containing this one was removed and for some reason not properly disposed, this would occur.
                return null;
            }
            new Thread() {
                @Override
                public void run() {
                    boolean finished = false;
                    while (!finished) {
                        try {
                            obj.waitFor();
                            finished = true;
                        } catch (InterruptedException e) {
                            //ignore
                        }
                    }
                    //If it got here, the process was finished (so, check the setting on refresh and do it if
                    //needed).
                    if (PyCoveragePreferences.getRefreshAfterNextLaunch()) {
                        RunInUiThread.async(new Runnable() {

                            public void run() {
                                ProgressOperation.startAction(getSite().getShell(), refreshAction, true);
                            }
                        });
                    }
                }
            }.start();
            return null;
        }
    };

    private final ICallbackListener<PythonRunnerCallbacks.CreatedCommandLineParams> onCreatedCommandLineListener = new ICallbackListener<PythonRunnerCallbacks.CreatedCommandLineParams>() {
        public Object call(CreatedCommandLineParams arg) {
            if (viewer == null) { //Safeguard: if the view containing this one was removed and for some reason not properly disposed, this would occur.
                return null;
            }
            if (arg.coverageRun) {
                if (PyCoveragePreferences.getClearCoverageInfoOnNextLaunch()) {
                    try {
                        PyCoverage.getPyCoverage().clearInfo();
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
            }
            return null;
        }
    };

    public static IContainer getChosenDir() {
        return PyCoveragePreferences.getLastChosenDir();
    }

    /**
     *
     * @author Fabio Zadrozny
     */
    private final class ClearAction extends ProgressAction {

        public ClearAction() {
            this.setText("Clear coverage information");
        }

        @Override
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
    private final class SelectColumnsAction extends Action {

        public SelectColumnsAction() {
            this.setText("Select the number of columns for the name.");
        }

        @Override
        public void run() {
            InputDialog d = new InputDialog(EditorUtils.getShell(), "Enter number of columns",
                    "Enter the number of columns to be used for the name.", ""
                            + PyCoveragePreferences.getNameNumberOfColumns(), new IInputValidator() {

                        public String isValid(String newText) {
                            if (newText.trim().length() == 0) {
                                return "Please enter a number > 5";
                            }
                            try {
                                int i = Integer.parseInt(newText);
                                if (i < 6) {
                                    return "Please enter a number > 5";
                                }
                                if (i > 256) {
                                    return "Please enter a number <= 256";
                                }
                            } catch (NumberFormatException e) {
                                return "Please enter a number > 5";
                            }
                            return null;
                        }
                    });
            int retCode = d.open();
            if (retCode == InputDialog.OK) {
                PyCoveragePreferences.setNameNumberOfColumns(Integer.parseInt(d.getValue()));
                onSelectedFileInTree(lastSelectedFile);
            }

        }
    }

    /**
     *
     * @author Fabio Zadrozny
     */
    private final class SelectionChangedTreeAction extends Action {
        @Override
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

            if (obj == null) {
                return;
            }

            onSelectedFileInTree(obj);
        }
    }

    private File lastSelectedFile;

    private void onSelectedFileInTree(Object obj) {
        if (obj == null) {
            text.setText("");
        } else {
            File realFile = new File(obj.toString());
            if (realFile.exists()) {
                lastSelectedFile = realFile;
                Tuple<String, List<StyleRange>> statistics = PyCoverage.getPyCoverage().cache.getStatistics(
                        realFile.toString(), realFile);

                text.setText(statistics.o1);
                text.setStyleRanges(statistics.o2.toArray(new StyleRange[statistics.o2.size()]));
            } else {
                text.setText("Selection no longer exists in disk: " + obj.toString());
            }
        }
    }

    /**
     *
     * @author Fabio Zadrozny
     */
    private final class DoubleClickTreeAction extends ProgressAction {

        @Override
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
                    openFileWithCoverageMarkers(realFile);
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    /**
     *
     * @author Fabio Zadrozny
     */
    private final class ChooseAction extends ProgressAction {
        @Override
        public void run() {
            ContainerSelectionDialog dialog = new ContainerSelectionDialog(getSite().getShell(), null, false,
                    "Choose folder to be analyzed in the code-coverage");
            dialog.showClosedProjects(false);
            if (dialog.open() != Window.OK) {
                return;
            }
            Object[] objects = dialog.getResult();
            if (objects.length == 1) { //only one folder can be selected
                if (objects[0] instanceof IPath) {
                    IPath p = (IPath) objects[0];

                    IWorkspace w = ResourcesPlugin.getWorkspace();
                    IContainer folderForLocation = (IContainer) w.getRoot().findMember(p);
                    setSelectedContainer(folderForLocation);
                }
            }
        }
    }

    public void setSelectedContainer(IContainer container) {
        lastSelectedFile = null;
        PyCoveragePreferences.setLastChosenDir(container);
        updateErrorMessages();

        File input = container.getLocation().toFile();
        viewer.setInput(input);

        ITreeContentProvider contentProvider = (ITreeContentProvider) viewer.getContentProvider();
        Object[] children = contentProvider.getChildren(input);
        if (children.length > 0) {
            viewer.setSelection(new StructuredSelection(children[0]));
        } else {
            viewer.setSelection(new StructuredSelection());
        }

        ProgressOperation.startAction(getSite().getShell(), refreshAction, true);
    }

    // Class -------------------------------------------------------------------

    /**
     * The constructor.
     */
    public PyCodeCoverageView() {
        NotifyViewCreated.notifyViewCreated(this);

    }

    public void refresh() {
        viewer.refresh();
        getSite().getPage().bringToTop(this);
    }

    @Override
    protected void setNewOrientation(int orientation) {
        if (sash != null && !sash.isDisposed() && fParent != null && !fParent.isDisposed()) {
            GridLayout layout = (GridLayout) fParent.getLayout();
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

    /**
     * This is a callback that will allow us to create the viewer and initialize it.
     */
    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.verticalSpacing = 2;
        layout.marginWidth = 0;
        layout.marginHeight = 2;
        parent.setLayout(layout);

        sash = new SashForm(parent, SWT.HORIZONTAL);
        GridData layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        sash.setLayoutData(layoutData);

        parent = sash;

        leftComposite = new Composite(parent, SWT.MULTI);
        layout = new GridLayout();
        layout.numColumns = 3;
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

        text = new StyledText(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        onControlCreated.call(text);
        try {
            text.setFont(new Font(null, FontUtils.getFontData(IFontUsage.WIDGET, false)));
        } catch (Exception e) {
            //ok, might mot be available.
        }

        text.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseDown(MouseEvent e) {
                int offset;
                try {
                    offset = text.getOffsetAtLocation(new Point(e.x, e.y));
                } catch (IllegalArgumentException e1) {
                    return; //Yes, in this case we clicked out of the possible range (i.e.: if we had no contents).
                }
                StyleRange r = text.getStyleRangeAtOffset(offset);
                if (r instanceof StyleRangeWithCustomData) {
                    StyleRangeWithCustomData styleRangeWithCustomData = (StyleRangeWithCustomData) r;
                    Object o = styleRangeWithCustomData.customData;
                    if (o instanceof FileNode) {
                        FileNode fileNode = (FileNode) o;
                        if (fileNode.node != null && fileNode.node.exists()) {
                            openFileWithCoverageMarkers(fileNode.node);
                        }
                    }
                }
            }
        });

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        text.setLayoutData(layoutData);

        parent = leftComposite;

        //all the runs from now on go through coverage?
        Label label = new Label(parent, SWT.None);
        label.setText("Enable code coverage for new launches?");
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        label.setLayoutData(layoutData);

        allRunsGoThroughCoverage = new Button(parent, SWT.CHECK);
        allRunsGoThroughCoverage.setSelection(PyCoveragePreferences.getInternalAllRunsDoCoverage());
        allRunsGoThroughCoverage.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PyCoveragePreferences.setInternalAllRunsDoCoverage(allRunsGoThroughCoverage.getSelection());
                updateErrorMessages();
            }
        });
        layoutData = new GridData();
        layoutData.horizontalSpan = 2;
        layoutData.grabExcessHorizontalSpace = false;
        allRunsGoThroughCoverage.setLayoutData(layoutData);
        //end all runs go through coverage

        //Clear the coverage info on each launch?
        label = new Label(parent, SWT.None);
        label.setText("Auto clear on a new launch?");
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        label.setLayoutData(layoutData);

        clearCoverageInfoOnNextLaunch = new Button(parent, SWT.CHECK);
        clearCoverageInfoOnNextLaunch.setSelection(PyCoveragePreferences.getClearCoverageInfoOnNextLaunch());
        clearCoverageInfoOnNextLaunch.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PyCoveragePreferences.setClearCoverageInfoOnNextLaunch(clearCoverageInfoOnNextLaunch.getSelection());
            }
        });

        PythonRunnerCallbacks.onCreatedCommandLine.registerListener(onCreatedCommandLineListener);
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = false;
        layoutData.horizontalAlignment = GridData.FILL;
        clearCoverageInfoOnNextLaunch.setLayoutData(layoutData);

        Button button = new Button(parent, SWT.PUSH);
        button.setText("Clear");
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ProgressOperation.startAction(getSite().getShell(), clearAction, true);
            }
        });
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = false;
        layoutData.widthHint = 50;
        layoutData.horizontalAlignment = GridData.END;
        button.setLayoutData(layoutData);
        //end all runs go through coverage

        //Refresh the coverage info on each launch?
        label = new Label(parent, SWT.None);
        label.setText("Auto refresh on new launch?");
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        label.setLayoutData(layoutData);

        refreshCoverageInfoOnNextLaunch = new Button(parent, SWT.CHECK);
        refreshCoverageInfoOnNextLaunch.setSelection(PyCoveragePreferences.getRefreshAfterNextLaunch());
        refreshCoverageInfoOnNextLaunch.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PyCoveragePreferences.setRefreshAfterNextLaunch(refreshCoverageInfoOnNextLaunch.getSelection());
            }
        });

        PythonRunnerCallbacks.afterCreatedProcess.registerListener(afterCreatedProcessListener);
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        refreshCoverageInfoOnNextLaunch.setLayoutData(layoutData);

        button = new Button(parent, SWT.PUSH);
        button.setText("Refresh");
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ProgressOperation.startAction(getSite().getShell(), refreshAction, true);
            }
        });
        layoutData = new GridData();
        layoutData.widthHint = 50;
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.horizontalAlignment = GridData.END;
        button.setLayoutData(layoutData);
        //end refresh

        //choose button
        chooseButton = new Button(parent, SWT.PUSH);
        createButton(parent, chooseButton, "Choose folder to analyze", chooseAction);
        //end choose button

        PatternFilter patternFilter = new PatternFilter();

        FilteredTree filter = PyFilteredTree.create(parent, patternFilter, true);
        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.grabExcessVerticalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.verticalAlignment = GridData.FILL;
        layoutData.horizontalSpan = 3;
        filter.setLayoutData(layoutData);

        viewer = filter.getViewer();
        onControlCreated.call(viewer);
        viewer.setContentProvider(new FileTreePyFilesProvider());
        viewer.setLabelProvider(new FileTreeLabelProvider());
        viewer.addFilter(new AllowValidPathsFilter());

        hookViewerActions();

        Tree tree = (Tree) viewer.getControl();

        TreeItem item = new TreeItem(tree, SWT.NONE);
        item.setText("Altenatively, to select a folder, drag it to this area.");

        item = new TreeItem(tree, SWT.NONE);

        item = new TreeItem(tree, SWT.NONE);
        item.setText("Note: Only the sources under the folder selected");
        item = new TreeItem(tree, SWT.NONE);
        item.setText("will have coverage information collected.");

        // Allow data to be copied or moved to the drop target
        int operations = DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT;
        DropTarget target = new DropTarget(tree, operations);

        // Receive data in Text or File format
        final FileTransfer fileTransfer = FileTransfer.getInstance();
        Transfer[] types = new Transfer[] { fileTransfer };
        target.setTransfer(types);

        target.addDropListener(new DropTargetListener() {
            public void dragEnter(DropTargetEvent event) {
                if (event.detail == DND.DROP_DEFAULT) {
                    if ((event.operations & DND.DROP_COPY) != 0) {
                        event.detail = DND.DROP_COPY;
                    } else {
                        event.detail = DND.DROP_NONE;
                    }
                }
                // will accept text but prefer to have files dropped
                for (int i = 0; i < event.dataTypes.length; i++) {
                    if (fileTransfer.isSupportedType(event.dataTypes[i])) {
                        event.currentDataType = event.dataTypes[i];
                        // files should only be copied
                        if (event.detail != DND.DROP_COPY) {
                            event.detail = DND.DROP_NONE;
                        }
                        break;
                    }
                }
            }

            public void dragOver(DropTargetEvent event) {
            }

            public void dragOperationChanged(DropTargetEvent event) {
                if (event.detail == DND.DROP_DEFAULT) {
                    if ((event.operations & DND.DROP_COPY) != 0) {
                        event.detail = DND.DROP_COPY;
                    } else {
                        event.detail = DND.DROP_NONE;
                    }
                }
                // allow text to be moved but files should only be copied
                if (fileTransfer.isSupportedType(event.currentDataType)) {
                    if (event.detail != DND.DROP_COPY) {
                        event.detail = DND.DROP_NONE;
                    }
                }
            }

            public void dragLeave(DropTargetEvent event) {
            }

            public void dropAccept(DropTargetEvent event) {
            }

            public void drop(DropTargetEvent event) {
                if (fileTransfer.isSupportedType(event.currentDataType)) {
                    String[] files = (String[]) event.data;
                    if (files.length == 1) {
                        File file = new File(files[0]);
                        if (file.isDirectory()) {
                            PySourceLocatorBase locator = new PySourceLocatorBase();
                            IContainer container = locator.getContainerForLocation(
                                    Path.fromOSString(file.getAbsolutePath()), null);
                            if (container != null && container.exists()) {
                                setSelectedContainer(container);
                            }
                        }
                    }
                }
            }
        });

        configureToolbar();

        updateErrorMessages();

    }

    private void configureToolbar() {
        IActionBars actionBars = getViewSite().getActionBars();
        //IToolBarManager toolbarManager = actionBars.getToolBarManager();
        IMenuManager menuManager = actionBars.getMenuManager();

        menuManager.add(selectColumnsAction);
        //menuManager.add(clearAction);
        //menuManager.add(refreshAction);
        menuManager.add(openCoverageFolderAction);

        addOrientationPreferences(menuManager);
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
                ProgressOperation.startAction(getSite().getShell(), action, true);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });

        layoutData = new GridData();
        layoutData.grabExcessHorizontalSpace = true;
        layoutData.horizontalAlignment = GridData.FILL;
        layoutData.horizontalSpan = 3;
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
    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.part.WorkbenchPart#dispose()
     */
    @Override
    public void dispose() {
        try {
            PythonRunnerCallbacks.afterCreatedProcess.unregisterListener(afterCreatedProcessListener);
            PythonRunnerCallbacks.onCreatedCommandLine.unregisterListener(onCreatedCommandLineListener);
            PyCoveragePreferences.setInternalAllRunsDoCoverage(false);
            PyCoveragePreferences.setLastChosenDir(null);
            if (text != null) {
                onControlDisposed.call(text);
                text.dispose();
                text = null;
            }

            if (viewer != null) {
                onControlDisposed.call(viewer);
                viewer.getTree().dispose();
                viewer = null;
            }
        } catch (Throwable e) {
            Log.log(e);
        }
        super.dispose();

    }

    private void updateErrorMessages() {

        boolean showError = false;

        if (PyCoveragePreferences.getInternalAllRunsDoCoverage()) {
            if (PyCoveragePreferences.getLastChosenDir() == null) {
                showError = true;
            }
        }
        if (showError) {
            if (labelErrorFolderNotSelected == null) {
                labelErrorFolderNotSelected = new Label(leftComposite, SWT.NONE);
                labelErrorFolderNotSelected.setForeground(PydevPlugin.getColorCache().getColor("RED"));
                labelErrorFolderNotSelected.setText("Folder must be selected for launching with coverage.");
                GridData layoutData = new GridData();
                layoutData.grabExcessHorizontalSpace = true;
                layoutData.horizontalSpan = 2;
                layoutData.horizontalAlignment = GridData.FILL;
                labelErrorFolderNotSelected.setLayoutData(layoutData);
            }
        } else {
            if (labelErrorFolderNotSelected != null) {
                this.labelErrorFolderNotSelected.dispose();
                this.labelErrorFolderNotSelected = null;
            }
        }
        this.leftComposite.layout();
    }

    /**
     * Gets the py code coverage view. May only be called in the UI thread. If the view is not visible, if createIfNotThere
     * is true, it's made visible.
     *
     * Note that it may return null if createIfNotThere == false and the view is not currently shown or if not in the
     * UI thread.
     */
    public static PyCodeCoverageView getView(boolean createIfNotThere) {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        try {
            if (workbenchWindow == null) {
                return null;
            }
            IWorkbenchPage page = workbenchWindow.getActivePage();
            if (createIfNotThere) {
                return (PyCodeCoverageView) page.showView(PY_COVERAGE_VIEW_ID, null, IWorkbenchPage.VIEW_ACTIVATE);
            } else {
                IViewReference viewReference = page.findViewReference(PY_COVERAGE_VIEW_ID);
                if (viewReference != null) {
                    //if it's there, return it (but don't restore it if it's still not there).
                    //when made visible, it'll handle things properly later on.
                    return (PyCodeCoverageView) viewReference.getView(false);
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }

    public String getCoverageText() {
        return this.text.getText();
    }

    /**
     * this is the type of the marker
     */
    public static final String PYDEV_COVERAGE_MARKER = "org.python.pydev.debug.pydev_coverage_marker";

    private void openFileWithCoverageMarkers(File realFile) {
        IEditorPart editor = PyOpenEditor.doOpenEditor(realFile);
        if (editor instanceof PyEdit) {
            PyEdit e = (PyEdit) editor;
            IEditorInput input = e.getEditorInput();
            final IFile original = (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
            if (original == null) {
                return;
            }
            final IDocument document = e.getDocumentProvider().getDocument(e.getEditorInput());
            //When creating it, it'll already start to listen for changes to remove the marker when needed.
            new RemoveCoverageMarkersListener(document, e, original);

            final FileNode cache = (FileNode) PyCoverage.getPyCoverage().cache.getFile(realFile);
            if (cache != null) {

                IWorkspaceRunnable r = new IWorkspaceRunnable() {
                    public void run(IProgressMonitor monitor) throws CoreException {

                        final String type = PYDEV_COVERAGE_MARKER;
                        try {
                            original.deleteMarkers(type, false, 1);
                        } catch (CoreException e1) {
                            Log.log(e1);
                        }

                        final String message = "Not Executed";

                        for (Iterator<Tuple<Integer, Integer>> it = cache.notExecutedIterator(); it.hasNext();) {
                            try {
                                Map<String, Object> map = new HashMap<String, Object>();
                                Tuple<Integer, Integer> startEnd = it.next();

                                IRegion region = document.getLineInformation(startEnd.o1 - 1);
                                int errorStart = region.getOffset();

                                region = document.getLineInformation(startEnd.o2 - 1);
                                int errorEnd = region.getOffset() + region.getLength();

                                map.put(IMarker.MESSAGE, message);
                                map.put(IMarker.SEVERITY, new Integer(IMarker.SEVERITY_ERROR));
                                map.put(IMarker.CHAR_START, errorStart);
                                map.put(IMarker.CHAR_END, errorEnd);
                                map.put(IMarker.TRANSIENT, Boolean.valueOf(true));
                                map.put(IMarker.PRIORITY, new Integer(IMarker.PRIORITY_HIGH));

                                MarkerUtilities.createMarker(original, map, type);
                            } catch (Exception e1) {
                                Log.log(e1);
                            }
                        }
                    }
                };

                try {
                    original.getWorkspace().run(r, null, IWorkspace.AVOID_UPDATE, null);
                } catch (CoreException e1) {
                    Log.log(e1);
                }
            }
        }
    }

    public ICallbackWithListeners getOnControlCreated() {
        return onControlCreated;
    }

    public ICallbackWithListeners getOnControlDisposed() {
        return onControlDisposed;
    }

}
