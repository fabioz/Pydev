/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.pyunit.HistoryAction.IActionsMenu;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.plugin.PydevPlugin;


/**
 * This action will take care of relaunching the current test suite whenever any file changes.
 * 
 * @author fabioz
 */
public class RelaunchInBackgroundAction extends Action implements IResourceChangeListener {

    /**
     * @author fabioz
     *
     */
    private final class RelaunchJob extends Job {

        private PyUnitTestRun currentTestRun;

        private RelaunchJob() {
            super("Relaunch test suite");
            this.setPriority(Job.SHORT);
            this.setSystem(true);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            if (!currentTestRun.getFinished()) { //Wait for the current run to finish.
                this.schedule(300);
                return Status.OK_STATUS;
            }
            if (PydevDebugPlugin.getDefault().getPreferenceStore()
                    .getBoolean(PyUnitView.PYUNIT_VIEW_BACKGROUND_RELAUNCH_SHOW_ONLY_ERRORS)) {
                currentTestRun.relaunchOnlyErrors();
            } else {
                currentTestRun.relaunch();
            }
            return Status.OK_STATUS;
        }

        public void setTestToRun(PyUnitTestRun currentTestRun) {
            this.currentTestRun = currentTestRun;
        }
    }

    public class RelaunchInBackgroundOptionsMenuCreator implements IMenuCreator {

        private Menu fMenu;

        public RelaunchInBackgroundOptionsMenuCreator() {

        }

        @Override
        public void dispose() {
            if (fMenu != null) {
                fMenu.dispose();
                fMenu = null;
            }
        }

        @Override
        public Menu getMenu(Control parent) {
            if (fMenu != null) {
                fMenu.dispose();
            }

            final MenuManager manager = new MenuManager();
            manager.setRemoveAllWhenShown(true);
            manager.addMenuListener(new IMenuListener() {
                @Override
                public void menuAboutToShow(final IMenuManager manager2) {
                    fillMenuManager(new IActionsMenu() {

                        @Override
                        public void add(IAction action) {
                            manager2.add(action);
                        }
                    });
                }
            });
            fMenu = manager.createContextMenu(parent);

            return fMenu;
        }

        @Override
        public Menu getMenu(Menu parent) {
            return null; //yes, return null here (no sub children)
        }

        public void fillMenuManager(IActionsMenu actionsMenu) {
            actionsMenu.add(new RelaunchOnlyErrorsOnBackgroundRelaunch());
        }
    }

    private class RelaunchOnlyErrorsOnBackgroundRelaunch extends Action {

        public RelaunchOnlyErrorsOnBackgroundRelaunch() {
            this.setText("Run only failed tests when relaunching due to file changes?");
            this.setToolTipText("If checked, a relaunch will relaunch only the errors in the current test run.\n"
                    + "\n" + "If no errors are found, the full test suite is run again.");
            this.setChecked(PydevDebugPlugin.getDefault().getPreferenceStore()
                    .getBoolean(PyUnitView.PYUNIT_VIEW_BACKGROUND_RELAUNCH_SHOW_ONLY_ERRORS));
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.action.Action#run()
         */
        @Override
        public void run() {
            PydevDebugPlugin.getDefault().getPreferenceStore()
                    .setValue(PyUnitView.PYUNIT_VIEW_BACKGROUND_RELAUNCH_SHOW_ONLY_ERRORS, this.isChecked());
        }
    }

    private WeakReference<PyUnitView> view;

    RelaunchJob relaunchJob = new RelaunchJob();

    private boolean listeningChanges;

    public RelaunchInBackgroundAction(PyUnitView pyUnitView) {
        this.view = new WeakReference<PyUnitView>(pyUnitView);
        setMenuCreator(new RelaunchInBackgroundOptionsMenuCreator()); //Options for user
        this.listeningChanges = false;
        this.setImageDescriptor(PydevPlugin.getImageCache().getDescriptor("icons/relaunch_background_disabled.png"));
        setInitialTooltipText();
    }

    private void setInitialTooltipText() {
        this.setToolTipText("Click to rerun the current test suite whenever any Python file changes.\n"
                + "\nNote that a new run will only be done after the current test run finishes.");
    }

    private void stopListening() {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    }

    private void startListening() {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        this.listeningChanges = !this.listeningChanges;

        if (this.listeningChanges) {
            this.setImageDescriptor(PydevPlugin.getImageCache().getDescriptor("icons/relaunch_background_enabled.png"));
            startListening();
        } else {
            this.setImageDescriptor(PydevPlugin.getImageCache().getDescriptor("icons/relaunch_background_disabled.png"));
            stopListening();
        }
    }

    @Override
    public void resourceChanged(final IResourceChangeEvent event) {
        //Handle cases where the view was closed...
        if (view == null) {
            stopListening();
            return;
        }

        PyUnitView pyUnitView = view.get();
        if (pyUnitView == null) {
            stopListening();
            return;
        }

        final boolean[] run = new boolean[] { false };
        try {
            event.getDelta().accept(new IResourceDeltaVisitor() {

                @Override
                public boolean visit(IResourceDelta delta) {
                    switch (delta.getKind()) {
                        case IResourceDelta.CHANGED:
                            IResource resource = delta.getResource();
                            if (resource instanceof IFile) {

                                //Check if a source file was changed (i.e.: don't get .pyc, .class, etc).
                                if (PythonPathHelper.isValidSourceFile((IFile) resource)) {
                                    int flags = delta.getFlags();
                                    if ((flags & IResourceDelta.CONTENT) != 0) {
                                        //Uncomment to debug...
                                        //System.out.println("----------------------");
                                        //System.out.println("----------------------");
                                        //System.out.println("----------------------");
                                        //System.out.println("----------------------");
                                        //System.out.println(event.getResource());
                                        //System.out.println(event.getBuildKind());
                                        //System.out.println(event.getSource());
                                        //System.out.println(resource+" "+resource.getModificationStamp());
                                        run[0] = true;
                                    }
                                }
                            }
                            break;
                    }
                    return true;
                }
            });
        } catch (CoreException e) {
            Log.log(e);
        }

        if (run[0]) {
            //Ok, we've the view, let's relaunch the current launch (if any)
            PyUnitTestRun currentTestRun = pyUnitView.getCurrentTestRun();
            if (currentTestRun != null) {
                relaunchJob.setTestToRun(currentTestRun);
                relaunchJob.schedule(200); //Give some time to handle more changes...
            }
        }
    }
}
