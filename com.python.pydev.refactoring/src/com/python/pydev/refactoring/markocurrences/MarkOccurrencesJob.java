/*
 * Created on Apr 29, 2006
 */
package com.python.pydev.refactoring.markocurrences;

import java.lang.ref.WeakReference;
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
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;

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
                sleep(750);
            
                //there was another request in the meantime (so, let's restart the process)
                if(wokenUpAt != currRequestTime){
                    waitOnNext = false;
                    continue;
                }
            } catch (Exception e) {
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
            
            final PyEdit pyEdit = editor.get();
            
            if(pyEdit == null){
                continue;
            }
            
            IDocumentProvider documentProvider = pyEdit.getDocumentProvider();
            IAnnotationModel annotationModel= documentProvider.getAnnotationModel(pyEdit.getEditorInput());
            
            removeOccurenceAnnotations(annotationModel);

            PyRefactorAction pyRefactorAction = getRefactorAction();
            try {
                pyRefactorAction.setEditor(pyEdit);
                final RefactoringRequest req = getRefactoringRequest(pyEdit, pyRefactorAction);
                
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
            } catch (Exception e) {
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
                for (ASTEntry entry : ocurrences) {
                    IRegion lineInformation = doc.getLineInformation(entry.node.beginLine-1);
                    
                    try {
                        annotationModel.addAnnotation(
                            new Annotation("org.eclipse.jdt.ui.occurrences", false, "occurrence"), 
                            new Position(lineInformation.getOffset() + entry.node.beginColumn - 1, req.duringProcessInfo.initialName.length()));
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
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
                req.ps = new PySelection(pyEdit);
            }});
        
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
    private void removeOccurenceAnnotations(IAnnotationModel annotationModel) {
        //remove the annotations
        synchronized(getLockObject(annotationModel)){
            
            //ok, let's re-use the ocurrences marker from jdt (jdt is a pre-requisite anyway).
            Iterator<Annotation> annotationIterator = annotationModel.getAnnotationIterator();
            while(annotationIterator.hasNext()){
                Annotation annotation = annotationIterator.next();
                if(annotation.getType().equals("org.eclipse.jdt.ui.occurrences")){
                    annotationModel.removeAnnotation(annotation);
                }
            }
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
