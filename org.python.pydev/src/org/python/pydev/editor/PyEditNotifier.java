package org.python.pydev.editor;

import java.lang.ref.WeakReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.PyEdit.MyResources;
import org.python.pydev.plugin.PydevPlugin;

public class PyEditNotifier {
	
	private WeakReference<PyEdit> pyEdit;

	public static interface INotifierRunnable{
		public void run(IProgressMonitor monitor);
	}
	
	public PyEditNotifier(PyEdit edit){
		this.pyEdit = new WeakReference<PyEdit>(edit);
	}
	
    public void notifyOnCreateActions(final MyResources resources) {
    	final PyEdit edit = pyEdit.get();
    	if(edit == null){
    		return;
    	}
    	INotifierRunnable runnable = new INotifierRunnable(){
    		public void run(final IProgressMonitor monitor){
		        for(IPyEditListener listener : edit.getAllListeners()){
		            try {
		            	if(!monitor.isCanceled()){
		            		listener.onCreateActions(resources, edit, monitor);
		            	}
		            } catch (Exception e) {
		                //must not fail
		                PydevPlugin.log(e);
		            }
		        }
	        }
    	};
    	runIt(runnable);
    }

    public void notifyOnSave() {
    	final PyEdit edit = pyEdit.get();
    	if(edit == null){
    		return;
    	}
    	INotifierRunnable runnable = new INotifierRunnable(){
    		public void run(IProgressMonitor monitor){
    			for(IPyEditListener listener : edit.getAllListeners()){
    				try {
		            	if(!monitor.isCanceled()){
		            		listener.onSave(edit, monitor);
		            	}
    				} catch (Throwable e) {
    					//must not fail
    					PydevPlugin.log(e);
    				}
    			}
	        }
    	};
    	runIt(runnable);

    }

	private void runIt(final INotifierRunnable runnable) {
		Job job = new Job("PyEditNotifier"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				runnable.run(monitor);
				return Status.OK_STATUS;
			}
			
		};
		job.setPriority(Job.BUILD);
		job.setSystem(true);
		job.schedule();
	}

    public void notifyOnDispose() {
    	final PyEdit edit = pyEdit.get();
    	if(edit == null){
    		return;
    	}
    	
    	INotifierRunnable runnable = new INotifierRunnable(){
    		public void run(IProgressMonitor monitor){
    			for(IPyEditListener listener : edit.getAllListeners()){
    				try {
		            	if(!monitor.isCanceled()){
		            		listener.onDispose(edit, monitor);
		            	}
    				} catch (Throwable e) {
    					//no need to worry... as we're disposing, in shutdown, we may not have access to some classes anymore
    				}
    			}
	        }
    	};
    	runIt(runnable);
    }

    /**
     * @param document the document just set
     */
    public void notifyOnSetDocument(final IDocument document) {
    	final PyEdit edit = pyEdit.get();
    	if(edit == null){
    		return;
    	}
    	INotifierRunnable runnable = new INotifierRunnable(){
    		public void run(IProgressMonitor monitor){
    			for(IPyEditListener listener : edit.getAllListeners()){
    				try {
		            	if(!monitor.isCanceled()){
		            		listener.onSetDocument(document, edit, monitor);
		            	}
    				} catch (Exception e) {
    					//must not fail
    					PydevPlugin.log(e);
    				}
    			}
	        }
    	};
    	runIt(runnable);
    }

}
