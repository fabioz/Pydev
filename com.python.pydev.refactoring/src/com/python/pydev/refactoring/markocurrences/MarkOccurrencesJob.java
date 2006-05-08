/*
 * Created on Apr 29, 2006
 */
package com.python.pydev.refactoring.markocurrences;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.refactoring.ui.MarkOccurrencesPreferencesPage;
import com.python.pydev.refactoring.wizards.PyRenameProcessor;


/**
 * This is a 'low-priority' thread. It acts as a singleton. Requests to mark the ocurrences
 * will be forwarded to it, so, it should sleep for a while and then check for a request.
 * 
 * If the request actually happened, it will go on to process it, otherwise it will sleep some more.
 * 
 * @author Fabio
 */
public class MarkOccurrencesJob extends Thread{

    private static final String OCCURRENCE_ANNOTATION_TYPE = "org.eclipse.jdt.ui.occurrences";
	private static final String ANNOTATIONS_CACHE_KEY = "MarkOccurrencesJob Annotations";
	private static final boolean DEBUG = false;
    private static MarkOccurrencesJob singleton;
    
    public synchronized static MarkOccurrencesJob get() {
        if(singleton == null){
            singleton = new MarkOccurrencesJob();
            singleton.start();
        }
        return singleton;
    }
    
    private MarkOccurrencesJob() {
        super("MarkOccurrencesJob");
        setPriority(Thread.MIN_PRIORITY);
    }
    
    private WeakReference<PyEdit> editor;
    private long lastRequestTime = -1;
    private long currRequestTime = -1;
    boolean waitOnNext = true;
    private IProgressMonitor monitor = new NullProgressMonitor();

    @SuppressWarnings("unchecked")
    public void run() {
        while(true){
            //synchronization code
            try {
                synchronized(this){
                    if(waitOnNext){
                        wait();
                    }
                    waitOnNext = true;
                }
                long wokenUpAt = currRequestTime;
                
                try {
					sleep(750);
				} catch (Exception e) {
					//ignore
				}
            
                //there was another request in the meantime (so, let's restart the process)
                if(wokenUpAt != currRequestTime){
                    waitOnNext = false;
                    continue;
                }
            } catch (Throwable e) {
                Log.log(e);
            }
            //end sync code
            
            if(currRequestTime == -1){
                continue;
            }
            if(currRequestTime == lastRequestTime){
                continue;
            }
            lastRequestTime = currRequestTime;
            
            try {
	            final PyEdit pyEdit = editor.get();
	            
	            if(pyEdit == null){
	                continue;
	            }
            
	            IDocumentProvider documentProvider = pyEdit.getDocumentProvider();
	            if(documentProvider == null){
	            	continue;
	            }
	            
	            IAnnotationModel annotationModel= documentProvider.getAnnotationModel(pyEdit.getEditorInput());
	            if(annotationModel == null){
	            	continue;
	            }
	            removeOccurenceAnnotations(annotationModel, pyEdit);
	            
	            if(!MarkOccurrencesPreferencesPage.useMarkOccurrences()){
	            	continue;
	            }
	
	            //now, let's see if the editor still has a document (so that we still can add stuff to it)
                IEditorInput editorInput = pyEdit.getEditorInput();
                if(editorInput == null){
                	continue;
                }
                
                if(documentProvider.getDocument(editorInput) == null){
                	continue;
                }
                
                if(pyEdit.getSelectionProvider() == null){
                	continue;
                }
                
                //ok, the editor is still there wit ha document... move on
                PyRefactorAction pyRefactorAction = getRefactorAction();
                pyRefactorAction.setEditor(pyEdit);
                
                final RefactoringRequest req = getRefactoringRequest(pyEdit, pyRefactorAction);
                
                if(req == null){
                	continue;
                }
                
                PyRenameProcessor processor = new PyRenameProcessor(req);
                //to see if a new request was not created in the meantime (in which case this one will be cancelled)
                if (currRequestTime != lastRequestTime) {
                    continue;
                }
                
                monitor.setCanceled(false);
                processor.checkInitialConditions(monitor);
                if (currRequestTime != lastRequestTime) {
                    continue;
                }
                
                processor.checkFinalConditions(monitor, null);
                if (currRequestTime != lastRequestTime) {
                    continue;
                }

                addAnnotations(pyEdit, annotationModel, req, processor);
            } catch (Throwable e) {
                Log.log(e);
            }
        }        
    }

    /**
     * @param pyEdit
     * @param annotationModel
     * @param req
     * @param processor
     * @throws BadLocationException
     */
    private void addAnnotations(final PyEdit pyEdit, IAnnotationModel annotationModel, final RefactoringRequest req, PyRenameProcessor processor) throws BadLocationException {
        //add the annotations
        synchronized (getLockObject(annotationModel)) {
            List<ASTEntry> ocurrences = processor.getOcurrences();
            if(ocurrences != null){
                IDocument doc = pyEdit.getDocument();
                ArrayList<Annotation> annotations = new ArrayList<Annotation>();
                
                for (ASTEntry entry : ocurrences) {
                    IRegion lineInformation = doc.getLineInformation(entry.node.beginLine-1);
                    
                    try {
                        final Annotation annotation = new Annotation(OCCURRENCE_ANNOTATION_TYPE, false, "occurrence");
                        annotations.add(annotation);
						annotationModel.addAnnotation(
                            annotation, 
                            new Position(lineInformation.getOffset() + entry.node.beginColumn - 1, req.duringProcessInfo.initialName.length()));
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
                
                pyEdit.cache.put(ANNOTATIONS_CACHE_KEY, annotations);
            }else{
                if(DEBUG){
                    System.out.println("Occurrences == null");
                }
            }
        }
    }

    /**
     * @param pyEdit
     * @param pyRefactorAction
     * @return
     * @throws BadLocationException
     */
    private RefactoringRequest getRefactoringRequest(final PyEdit pyEdit, PyRefactorAction pyRefactorAction) throws BadLocationException {
        final RefactoringRequest req = pyRefactorAction.getRefactoringRequest();
        Display.getDefault().syncExec(new Runnable(){
            public void run() {
                try {
					req.ps = new PySelection(pyEdit);
				} catch (NullPointerException e) {
					// this can happen if the selection was still not set up
				}
            }});
        if(req.ps == null){
        	return null;
        }
        
        req.fillInitialNameAndOffset();
        req.duringProcessInfo.name = "foo";
        req.findDefinitionInAdditionalInfo = false;
        req.findReferencesOnlyOnLocalScope = true;
        return req;
    }

    /**
     * @return
     */
    private PyRefactorAction getRefactorAction() {
        PyRefactorAction pyRefactorAction = new PyRefactorAction(){
   
            @Override
            protected IPyRefactoring getPyRefactoring() {
                return AbstractPyRefactoring.getPyRefactoring();
            }
   
            @Override
            protected String perform(IAction action, String name, Operation operation) throws Exception {
                throw new RuntimeException("Perform should not be called in this case.");
            }
   
            @Override
            protected String getInputMessage() {
                return null;
            }
            
        };
        return pyRefactorAction;
    }

    /**
     * @param annotationModel
     */
    private void removeOccurenceAnnotations(IAnnotationModel annotationModel, PyEdit pyEdit) {
        //remove the annotations
        synchronized(getLockObject(annotationModel)){
            List<Annotation> annotations = (List<Annotation>) pyEdit.cache.get(ANNOTATIONS_CACHE_KEY);

            if(annotations == null){
            	return;
            }
            
            //ok, let's re-use the ocurrences marker from jdt (jdt is a pre-requisite anyway).
            Iterator<Annotation> annotationIterator = annotations.iterator();
            while(annotationIterator.hasNext()){
                Annotation annotation = annotationIterator.next();
                if(annotation.getType().equals(OCCURRENCE_ANNOTATION_TYPE)){
                    annotationModel.removeAnnotation(annotation);
                }
            }
            pyEdit.cache.put(ANNOTATIONS_CACHE_KEY, null);
        }
        //end remove the annotations
    }

    
    /**
     * Gotten from JavaEditor#getLockObject
     */
    private Object getLockObject(IAnnotationModel annotationModel) {
        if (annotationModel instanceof ISynchronizable)
            return ((ISynchronizable)annotationModel).getLockObject();
        else
            return annotationModel;
    }

    public synchronized void scheduleRequest(WeakReference<PyEdit> editor2) {
        synchronized (this) {
            currRequestTime = System.currentTimeMillis();
            monitor.setCanceled(true);
            this.editor = editor2;
            notify();
        }
    }


}
