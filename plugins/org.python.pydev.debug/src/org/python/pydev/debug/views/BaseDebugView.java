package org.python.pydev.debug.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import org.python.pydev.debug.model.PyDebugModelPresentation;
import org.python.pydev.shared_ui.utils.RunInUiThread;

/**
 * Base class for some views that are debug-related and have a tree to show contents.
 */
public abstract class BaseDebugView extends ViewPart {

    /**
     * Note: not using a PyFilteredTree because filtering debug views can get recursive as structures in the debugger
     * may be recursive.
     */
    protected TreeViewer viewer;

    protected ProgressBar progressBar;

    protected Composite parent;

    protected ITreeContentProvider provider;

    private ILaunchAndDebugListener listener;

    @Override
    public void createPartControl(Composite parent) {
        IViewSite viewSite = getViewSite();
        if (viewSite != null) {
            configureToolBar(viewSite);
        }

        parent.setLayout(new GridLayout(1, true));

        viewer = new TreeViewer(parent);
        provider = createContentProvider();
        viewer.setContentProvider(provider);
        viewer.setLabelProvider(new PyDebugModelPresentation(false));

        GridDataFactory.fillDefaults().grab(true, true).applyTo(viewer.getTree());

        MenuManager menuManager = new MenuManager();
        Menu menu = menuManager.createContextMenu(viewer.getTree());
        viewer.getTree().setMenu(menu);
        IWorkbenchPartSite site = getSite();
        site.registerContextMenu(menuManager, viewer);
        site.setSelectionProvider(viewer);

        this.parent = parent;

        listener = createListener();
        if (listener != null) {
            DebugPlugin plugin = DebugPlugin.getDefault();

            ILaunchManager launchManager = plugin.getLaunchManager();
            launchManager.addLaunchListener(listener);

            plugin.addDebugEventListener(listener);
        }
    }

    /**
     * The content provider to provide contents for our tree.
     */
    protected abstract ITreeContentProvider createContentProvider();

    /**
     * A listener which will be added when the view is created and will be removed when the view is disposed.
     * 
     * Usually this listener will be responsible for cleaning things up when the debug launch is terminated.
     */
    protected abstract ILaunchAndDebugListener createListener();

    /**
     * Should be overridden to add actions to the toolbar/menubar.
     */
    protected abstract void configureToolBar(IViewSite viewSite);

    @Override
    public void dispose() {
        super.dispose();
        if (listener != null) {
            DebugPlugin plugin = DebugPlugin.getDefault();

            ILaunchManager launchManager = plugin.getLaunchManager();
            launchManager.removeLaunchListener(listener);

            plugin.removeDebugEventListener(listener);
        }
        this.clear();
    }

    /**
     * Set focus to our tree.
     */
    @Override
    public void setFocus() {
        if (viewer == null || viewer.getTree().isDisposed()) {
            return;
        }
        viewer.getTree().setFocus();
    }

    public void clear() {
        updateTreeJob.schedule();
    }

    protected final Job updateTreeJob = new UIJob("Update PyDev Debug view") {

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            if (viewer.getTree().isDisposed()) {
                return Status.OK_STATUS;
            }
            onSetTreeInput();

            RunInUiThread.async(new Runnable() {

                @Override
                public void run() {
                    if (viewer != null && viewer.getTree() != null && !viewer.getTree().isDisposed()) {
                        Object input = viewer.getInput();
                        makeLastVisibleInTree(input);
                    }
                }

            });
            return Status.OK_STATUS;
        }
    };

    /**
     * Subclasses should override to make the last item added in the tree visible. input is the tree input.
     */
    protected abstract void makeLastVisibleInTree(Object input);

    /**
     * Subclasses must override to set the input of the tree.
     */
    protected abstract void onSetTreeInput();

    // Progress bar handling -------------------------------------------------------------------------------------------

    private int inProgress = 0;
    private Object progressLock = new Object();

    /**
     * Finishes showing the progress bar in the view.
     */
    protected void endProgress() {
        synchronized (progressLock) {
            inProgress -= 1;
        }
        updateProgressBarJob.schedule(); //Dispose ASAP
    }

    /**
     * Starts to show an 'unknown' progress bar in the view.
     */
    protected void startProgress() {
        synchronized (progressLock) {
            inProgress += 1;
        }
        updateProgressBarJob.schedule(600); //Wait a bit before creating the progress bar
    }

    Job updateProgressBarJob = new UIJob("Update Referrers view") {

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            synchronized (progressLock) {
                if (inProgress > 0) {
                    if (progressBar == null || progressBar.isDisposed()) {
                        progressBar = new ProgressBar(parent, SWT.INDETERMINATE | SWT.SMOOTH);
                        GridDataFactory.fillDefaults().grab(true, false).applyTo(progressBar);
                        parent.layout(true);
                        final Display display = Display.getCurrent();
                        display.timerExec(100, new Runnable() {
                            int i = 0;

                            public void run() {
                                if (progressBar == null || progressBar.isDisposed()) {
                                    return;
                                }
                                progressBar.setSelection(i++);
                                display.timerExec(100, this);
                            }
                        });
                    }

                } else {
                    if (progressBar != null && !progressBar.isDisposed()) {
                        progressBar.dispose();
                        progressBar = null;
                        parent.layout(true);
                    }
                }
            }
            return Status.OK_STATUS;
        }

    };
}
