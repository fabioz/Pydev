package org.python.pydev.editor;

import java.lang.ref.WeakReference;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.PyEdit.MyResources;
import org.python.pydev.plugin.PydevPlugin;

public class PyEditNotifier {
	
	private WeakReference<PyEdit> pyEdit;

	public PyEditNotifier(PyEdit edit){
		this.pyEdit = new WeakReference<PyEdit>(edit);
	}
	
    public void notifyOnCreateActions(final MyResources resources) {
    	final PyEdit edit = pyEdit.get();
    	if(edit == null){
    		return;
    	}
    	Runnable runnable = new Runnable(){
    		public void run(){
		        for(IPyEditListener listener : edit.getAllListeners()){
		            try {
		                listener.onCreateActions(resources, edit);
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
    	Runnable runnable = new Runnable(){
    		public void run(){
    			for(IPyEditListener listener : edit.getAllListeners()){
    				try {
    					listener.onSave(edit);
    				} catch (Throwable e) {
    					//must not fail
    					PydevPlugin.log(e);
    				}
    			}
	        }
    	};
    	runIt(runnable);

    }

	private void runIt(Runnable runnable) {
		Thread thread = new Thread(runnable);
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

    public void notifyOnDispose() {
    	final PyEdit edit = pyEdit.get();
    	if(edit == null){
    		return;
    	}
    	
    	Runnable runnable = new Runnable(){
    		public void run(){
    			for(IPyEditListener listener : edit.getAllListeners()){
    				try {
    					listener.onDispose(edit);
    				} catch (Exception e) {
    					//must not fail
    					PydevPlugin.log(e);
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
    	Runnable runnable = new Runnable(){
    		public void run(){
    			for(IPyEditListener listener : edit.getAllListeners()){
    				try {
    					listener.onSetDocument(document, edit);
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
