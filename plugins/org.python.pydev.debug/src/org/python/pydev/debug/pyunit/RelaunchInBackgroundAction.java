/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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
import org.python.pydev.core.log.Log;
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
            currentTestRun.relaunch();
            return Status.OK_STATUS;
        }

        public void setTestToRun(PyUnitTestRun currentTestRun) {
            this.currentTestRun = currentTestRun;
        }
    }

    private WeakReference<PyUnitView> view;
    
    RelaunchJob relaunchJob = new RelaunchJob();

    public RelaunchInBackgroundAction(PyUnitView pyUnitView) {
        this.view = new WeakReference<PyUnitView>(pyUnitView);
        this.setChecked(false);
        this.setImageDescriptor(PydevPlugin.getImageCache().getDescriptor("icons/relaunch_background.png"));
        setInitialTooltipText();
    }

    private void setInitialTooltipText() {
        this.setToolTipText(
                "Click to rerun the current test suite whenever any Python file changes.\n" +
        		"\nNote that the test being currently run will be terminated if it still hasn't finished.");
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
        if (this.isChecked()) {
            startListening();
        } else {
            stopListening();
        }
    }

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

        final boolean[] run = new boolean[]{false};
        try {
            event.getDelta().accept(new IResourceDeltaVisitor() {

                public boolean visit(IResourceDelta delta) {
                    switch (delta.getKind()) {
                        case IResourceDelta.CHANGED:
                            IResource resource = delta.getResource();
                            if(resource instanceof IFile){
                                
                                //Check if a source file was changed (i.e.: don't get .pyc, .class, etc).
                                if(PythonPathHelper.isValidSourceFile((IFile)resource)){
                                    int flags = delta.getFlags();
                                    if((flags & IResourceDelta.CONTENT) != 0){
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

        if(run[0]){
            //Ok, we've the view, let's relaunch the current launch (if any)
            PyUnitTestRun currentTestRun = pyUnitView.getCurrentTestRun();
            if (currentTestRun != null) {
                relaunchJob.setTestToRun(currentTestRun);
                relaunchJob.schedule(200); //Give some time to handle more changes...
            }
        }
    }
}
