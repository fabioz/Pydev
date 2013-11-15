package org.python.pydev.debug.referrers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.XMLUtils;
import org.python.pydev.debug.model.XMLUtils.XMLToReferrersInfo;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;
import org.python.pydev.debug.model.remote.RunCustomOperationCommand;
import org.python.pydev.shared_ui.tree.PyFilteredTree;
import org.python.pydev.shared_ui.utils.UIUtils;

public class ReferrersView extends ViewPart {

    private static final class ReferrerCommandResponseListener implements ICommandResponseListener {
        private final String locator;
        private final AbstractDebugTarget debugTarget;
        private ReferrersView referrersView;

        private ReferrerCommandResponseListener(ReferrersView referrersView, String locator,
                AbstractDebugTarget debugTarget) {
            this.locator = locator;
            this.debugTarget = debugTarget;
            this.referrersView = referrersView;
        }

        @Override
        public void commandComplete(AbstractDebuggerCommand cmd) {
            try {
                if (cmd instanceof RunCustomOperationCommand) {
                    RunCustomOperationCommand c = (RunCustomOperationCommand) cmd;
                    XMLToReferrersInfo xmlToReferrers = XMLUtils.XMLToReferrers(debugTarget, locator,
                            c.getResponsePayload());
                    if (xmlToReferrers != null) {
                        referrersView.addReferrersInfo(xmlToReferrers);
                    }
                }
            } finally {
                this.finish();
            }
        }

        private void finish() {
            boolean removedNow;
            synchronized (referrersView.listenersLock) {
                removedNow = referrersView.listeners.remove(this);
            }

            if (removedNow) {
                referrersView.endProgress();
            }
        }
    }

    private PyFilteredTree filter;

    private TreeViewer viewer;

    private ProgressBar progressBar;

    private Composite parent;

    private ReferrersViewContentProvider provider;

    private static final String REFERRERS_VIEW_ID = "org.python.pydev.views.ReferrersView";

    public ReferrersView() {
    }

    private void configureToolBar() {
        IViewSite viewSite = getViewSite();
        if (viewSite == null) {
            return;
        }
        IActionBars actionBars = viewSite.getActionBars();
        IToolBarManager toolBar = actionBars.getToolBarManager();
        //IMenuManager menuManager = actionBars.getMenuManager(); -- not adding anything to the menu for now.

        IAction showTestRunnerPreferencesAction = new ClearCurrentReferrers(this);
        toolBar.add(showTestRunnerPreferencesAction);

    }

    @SuppressWarnings("restriction")
    @Override
    public void createPartControl(Composite parent) {
        configureToolBar();
        parent.setLayout(new GridLayout(1, true));

        PatternFilter patternFilter = new PatternFilter();

        filter = PyFilteredTree.create(parent, patternFilter, true);

        viewer = filter.getViewer();
        provider = new ReferrersViewContentProvider();
        viewer.setContentProvider(provider);
        viewer.setLabelProvider(new org.eclipse.debug.internal.ui.DefaultLabelProvider()); //Internal for the debug, but let's rely on it for now...

        GridDataFactory.fillDefaults().grab(true, true).applyTo(filter);
        this.parent = parent;
    }

    @Override
    public void setFocus() {
        filter.setFocus();
    }

    private final Set<ReferrerCommandResponseListener> listeners = new HashSet<>();
    private final Object listenersLock = new Object();

    /**
     * Gets the py unit view. May only be called in the UI thread. If the view is not visible, shows it if the
     * preference to do that is set to true.
     * 
     * Note that it may return null if the preference to show it is false and the view is not currently shown.
     */
    public static ReferrersView getView(boolean forceVisible) {
        return (ReferrersView) UIUtils.getView(REFERRERS_VIEW_ID, forceVisible);
    }

    public void showReferrersFor(final AbstractDebugTarget debugTarget, final String locator) {
        RunCustomOperationCommand cmd = new RunCustomOperationCommand(debugTarget, locator,
                "from pydevd_referrers import get_referrer_info",
                "get_referrer_info");

        ReferrerCommandResponseListener listener = new ReferrerCommandResponseListener(this, locator, debugTarget);

        synchronized (listenersLock) {
            startProgress();
            listeners.add(listener);
        }
        cmd.setCompletionListener(listener);

        debugTarget.postCommand(cmd);
    }

    // Information to add to the tree and updating it ------------------------------------------------------------------

    private List<XMLToReferrersInfo> xmlToReferrers = new ArrayList<>();
    private final Object xmlToReferrersLock = new Object();

    protected void addReferrersInfo(XMLToReferrersInfo xmlToReferrers) {
        synchronized (xmlToReferrersLock) {
            this.xmlToReferrers.add(xmlToReferrers);
        }
        updateTreeJob.schedule();
    }

    public void clear() {
        synchronized (xmlToReferrersLock) {
            this.xmlToReferrers.clear();
        }

        //Any registered pending command should be stopped now!
        synchronized (listenersLock) {
            for (ReferrerCommandResponseListener referrerCommandResponseListener : listeners) {
                referrerCommandResponseListener.finish();
            }
            listeners.clear();
        }

        updateTreeJob.schedule();
    }

    Job updateTreeJob = new UIJob("Update Referrers view") {

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            if (viewer.getTree().isDisposed()) {
                return Status.OK_STATUS;
            }
            XMLToReferrersInfo[] array;
            synchronized (xmlToReferrersLock) {
                array = xmlToReferrers.toArray(new XMLToReferrersInfo[xmlToReferrers.size()]);
            }
            viewer.setInput(array);
            return Status.OK_STATUS;
        }
    };

    // Progress bar handling -------------------------------------------------------------------------------------------

    private int inProgress = 0;
    private Object progressLock = new Object();

    private void endProgress() {
        synchronized (progressLock) {
            inProgress -= 1;
        }
        updateProgressBarJob.schedule(); //Dispose ASAP
    }

    private void startProgress() {
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
