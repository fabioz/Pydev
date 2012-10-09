/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 29, 2006
 */
package com.python.pydev.refactoring.markoccurrences;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.PydevPlugin;
import com.python.pydev.refactoring.refactorer.AstEntryRefactorerRequestConstants;
import com.python.pydev.refactoring.ui.MarkOccurrencesPreferencesPage;
import com.python.pydev.refactoring.wizards.rename.PyRenameEntryPoint;

/**
 * This is a 'low-priority' thread. It acts as a singleton. Requests to mark the occurrences
 * will be forwarded to it, so, it should sleep for a while and then check for a request.
 * 
 * If the request actually happened, it will go on to process it, otherwise it will sleep some more.
 * 
 * @author Fabio
 */
public class MarkOccurrencesJob extends Job {

    private static final boolean DEBUG = false;
    private static MarkOccurrencesJob singleton;

    /**
     * Make it thread safe
     */
    private static volatile long lastRequestTime = -1;

    /**
     * This is the editor to be analyzed
     */
    private WeakReference<PyEdit> editor;

    /**
     * This is the request time for this job
     */
    private long currRequestTime = -1;

    /**
     * The selection when the occurrences job was requested
     */
    private PySelection ps;

    private MarkOccurrencesJob(WeakReference<PyEdit> editor, PySelection ps) {
        super("MarkOccurrencesJob");
        setPriority(Job.BUILD);
        setSystem(true);
        this.editor = editor;
        this.ps = ps;
        currRequestTime = System.currentTimeMillis();
    }

    /**
     * Mark if we are still abel to do it by the time we get to the run.
     */
    public IStatus run(IProgressMonitor monitor) {
        if (currRequestTime == -1) {
            return Status.OK_STATUS;
        }
        if (currRequestTime == lastRequestTime) {
            return Status.OK_STATUS;
        }
        lastRequestTime = currRequestTime;

        try {
            final PyEdit pyEdit = editor.get();

            if (pyEdit == null || monitor.isCanceled()) {
                return Status.OK_STATUS;
            }
            try {
                IDocumentProvider documentProvider = pyEdit.getDocumentProvider();
                if (documentProvider == null || monitor.isCanceled()) {
                    return Status.OK_STATUS;
                }

                IAnnotationModel annotationModel = documentProvider.getAnnotationModel(pyEdit.getEditorInput());
                if (annotationModel == null || monitor.isCanceled()) {
                    return Status.OK_STATUS;
                }

                Tuple3<RefactoringRequest, PyRenameEntryPoint, Boolean> ret = checkAnnotations(pyEdit,
                        documentProvider, monitor);
                if (pyEdit.cache == null || monitor.isCanceled()) { //disposed (cannot add or remove annotations)
                    return Status.OK_STATUS;
                }

                PySourceViewer viewer = pyEdit.getPySourceViewer();
                if (viewer == null || monitor.isCanceled()) {
                    return Status.OK_STATUS;
                }
                if (viewer.getIsInToggleCompletionStyle() || monitor.isCanceled()) {
                    return Status.OK_STATUS;
                }

                if (ret.o3) {
                    if (!addAnnotations(pyEdit, annotationModel, ret.o1, ret.o2)) {
                        //something went wrong, so, let's remove the occurrences
                        removeOccurenceAnnotations(annotationModel, pyEdit);
                    }
                } else {
                    removeOccurenceAnnotations(annotationModel, pyEdit);
                }
            } catch (OperationCanceledException e) {
                throw e;//rethrow this error...
            } catch (AssertionFailedException e) {
                String message = e.getMessage();
                if (message != null && message.indexOf("The file:") != -1 && message.indexOf("does not exist.") != -1) {
                    //don't even report it (the file was probably removed while we were doing the analysis)
                } else {
                    Log.log(e);
                    Log.log("Error while analyzing the file:" + pyEdit.getIFile());
                }
            } catch (Throwable initialE) {
                //Totally ignore this one
                //                Throwable e = initialE;
                //                int i = 0;
                //                while(e.getCause() != null && e.getCause() != e && i < 30){
                //                    e = e.getCause();
                //                    i++;//safeguard for recursion
                //                }
                //                if(e instanceof BadLocationException){
                //                    //ignore (may have changed during the analysis)
                //                }else{
                //                    Log.log(initialE);
                //                    Log.log("Error while analyzing the file:"+pyEdit.getIFile());
                //                }
            }

        } catch (Throwable e) {
            //            Log.log(e); -- ok, remove this log, as things can happen if the user starts editing after the analysis is requested
        }
        return Status.OK_STATUS;
    }

    /**
     * @return a tuple with the refactoring request, the processor and a boolean indicating if all pre-conditions succedded.
     * @throws MisconfigurationException 
     */
    private Tuple3<RefactoringRequest, PyRenameEntryPoint, Boolean> checkAnnotations(PyEdit pyEdit,
            IDocumentProvider documentProvider, IProgressMonitor monitor) throws BadLocationException,
            OperationCanceledException, CoreException, MisconfigurationException {
        if (!MarkOccurrencesPreferencesPage.useMarkOccurrences()) {
            return new Tuple3<RefactoringRequest, PyRenameEntryPoint, Boolean>(null, null, false);
        }

        //now, let's see if the editor still has a document (so that we still can add stuff to it)
        IEditorInput editorInput = pyEdit.getEditorInput();
        if (editorInput == null) {
            return new Tuple3<RefactoringRequest, PyRenameEntryPoint, Boolean>(null, null, false);
        }

        if (documentProvider.getDocument(editorInput) == null) {
            return new Tuple3<RefactoringRequest, PyRenameEntryPoint, Boolean>(null, null, false);
        }

        if (pyEdit.getSelectionProvider() == null) {
            return new Tuple3<RefactoringRequest, PyRenameEntryPoint, Boolean>(null, null, false);
        }

        //ok, the editor is still there wit ha document... move on
        PyRefactorAction pyRefactorAction = getRefactorAction(pyEdit);

        final RefactoringRequest req = getRefactoringRequest(pyEdit, pyRefactorAction, this.ps);

        if (req == null || !req.nature.getRelatedInterpreterManager().isConfigured()) { //we check if it's configured because it may still be a stub...
            return new Tuple3<RefactoringRequest, PyRenameEntryPoint, Boolean>(null, null, false);
        }

        PyRenameEntryPoint processor = new PyRenameEntryPoint(req);
        //to see if a new request was not created in the meantime (in which case this one will be cancelled)
        if (currRequestTime != lastRequestTime || monitor.isCanceled()) {
            return new Tuple3<RefactoringRequest, PyRenameEntryPoint, Boolean>(null, null, false);
        }

        try {
            processor.checkInitialConditions(monitor);
            if (currRequestTime != lastRequestTime || monitor.isCanceled()) {
                return new Tuple3<RefactoringRequest, PyRenameEntryPoint, Boolean>(null, null, false);
            }

            processor.checkFinalConditions(monitor, null);
            if (currRequestTime != lastRequestTime || monitor.isCanceled()) {
                return new Tuple3<RefactoringRequest, PyRenameEntryPoint, Boolean>(null, null, false);
            }

            //ok, pre-conditions suceeded
            return new Tuple3<RefactoringRequest, PyRenameEntryPoint, Boolean>(req, processor, true);
        } catch (Throwable e) {
            throw new RuntimeException("Error in occurrences while analyzing modName:" + req.moduleName
                    + " initialName:" + req.initialName + " line (start at 0):" + req.ps.getCursorLine(), e);
        }
    }

    /**
     * @return true if the annotations were removed and added without any problems and false otherwise
     */
    private synchronized boolean addAnnotations(final PyEdit pyEdit, IAnnotationModel annotationModel,
            final RefactoringRequest req, PyRenameEntryPoint processor) throws BadLocationException {
        HashSet<ASTEntry> occurrences = processor.getOccurrences();
        if (occurrences == null) {
            if (DEBUG) {
                System.out.println("Occurrences == null");
            }
            return false;
        }

        Map<String, Object> cache = pyEdit.cache;
        if (cache == null) {
            return false;
        }

        IDocument doc = pyEdit.getDocument();
        ArrayList<Annotation> annotations = new ArrayList<Annotation>();
        Map<Annotation, Position> toAddAsMap = new HashMap<Annotation, Position>();
        boolean markOccurrencesInStrings = MarkOccurrencesPreferencesPage.useMarkOccurrencesInStrings();

        //get the annotations to add
        for (ASTEntry entry : occurrences) {
            if (!markOccurrencesInStrings) {
                if (entry.node instanceof Name) {
                    Name name = (Name) entry.node;
                    if (name.ctx == Name.Artificial) {
                        continue;
                    }
                }
            }

            SimpleNode node = entry.getNameNode();
            IRegion lineInformation = doc.getLineInformation(node.beginLine - 1);

            try {
                Annotation annotation = new Annotation(PydevPlugin.OCCURRENCE_ANNOTATION_TYPE, false, "occurrence");
                Position position = new Position(lineInformation.getOffset() + node.beginColumn - 1,
                        req.initialName.length());
                toAddAsMap.put(annotation, position);
                annotations.add(annotation);

            } catch (Exception e) {
                Log.log(e);
            }
        }

        //get the ones to remove
        List<Annotation> toRemove = PydevPlugin.getOccurrenceAnnotationsInPyEdit(pyEdit);

        //let other threads execute before getting the lock on the annotation model
        Thread.yield();

        Thread thread = Thread.currentThread();
        int initiaThreadlPriority = thread.getPriority();
        try {
            //before getting the lock, let's execute with normal priority, to optimize the time that we'll 
            //retain that object locked (the annotation model is used on lots of places, so, retaining the lock
            //on it on a minimum priority thread is not a good thing.
            thread.setPriority(Thread.NORM_PRIORITY);

            synchronized (getLockObject(annotationModel)) {
                //replace them
                IAnnotationModelExtension ext = (IAnnotationModelExtension) annotationModel;
                ext.replaceAnnotations(toRemove.toArray(new Annotation[0]), toAddAsMap);
            }

        } finally {
            thread.setPriority(initiaThreadlPriority);
        }

        //put them in the pyEdit
        cache.put(PydevPlugin.ANNOTATIONS_CACHE_KEY, annotations);
        return true;
    }

    /**
     * @param pyEdit the editor where we should look for the occurrences
     * @param pyRefactorAction the action that will return the initial refactoring request
     * @param ps the pyselection used (if null it will be created in this method)
     * @return a refactoring request suitable for finding the locals in the file
     * @throws BadLocationException
     * @throws MisconfigurationException 
     */
    public static RefactoringRequest getRefactoringRequest(final PyEdit pyEdit, PyRefactorAction pyRefactorAction,
            PySelection ps) throws BadLocationException, MisconfigurationException {
        final RefactoringRequest req = pyRefactorAction.getRefactoringRequest();
        req.ps = ps;
        req.fillInitialNameAndOffset();
        req.inputName = "foo";
        req.setAdditionalInfo(AstEntryRefactorerRequestConstants.FIND_DEFINITION_IN_ADDITIONAL_INFO, false);
        req.setAdditionalInfo(AstEntryRefactorerRequestConstants.FIND_REFERENCES_ONLY_IN_LOCAL_SCOPE, true);
        return req;
    }

    /**
     * @param pyEdit the editor that will have this action
     * @return the action (with the pyedit attached to it)
     */
    public static PyRefactorAction getRefactorAction(PyEdit pyEdit) {
        PyRefactorAction pyRefactorAction = new PyRefactorAction() {

            @Override
            protected String perform(IAction action, IProgressMonitor monitor) throws Exception {
                throw new RuntimeException("Perform should not be called in this case.");
            }
        };
        pyRefactorAction.setEditor(pyEdit);
        return pyRefactorAction;
    }

    /**
     * @param annotationModel
     */
    private synchronized void removeOccurenceAnnotations(IAnnotationModel annotationModel, PyEdit pyEdit) {
        //remove the annotations
        Map<String, Object> cache = pyEdit.cache;
        if (cache == null) {
            return;
        }

        //let other threads execute before getting the lock on the annotation model
        Thread.yield();

        Thread thread = Thread.currentThread();
        int initiaThreadlPriority = thread.getPriority();
        //before getting the lock, let's execute with normal priority, to optimize the time that we'll 
        //retain that object locked (the annotation model is used on lots of places, so, retaining the lock
        //on it on a minimum priority thread is not a good thing.
        thread.setPriority(Thread.NORM_PRIORITY);

        try {
            synchronized (getLockObject(annotationModel)) {
                List<Annotation> annotationsToRemove = PydevPlugin.getOccurrenceAnnotationsInPyEdit(pyEdit);

                if (annotationModel instanceof IAnnotationModelExtension) {
                    //replace those 
                    ((IAnnotationModelExtension) annotationModel).replaceAnnotations(
                            annotationsToRemove.toArray(new Annotation[annotationsToRemove.size()]), new HashMap());
                } else {
                    Iterator<Annotation> annotationIterator = annotationsToRemove.iterator();

                    while (annotationIterator.hasNext()) {
                        annotationModel.removeAnnotation(annotationIterator.next());
                    }
                }
                cache.put(PydevPlugin.ANNOTATIONS_CACHE_KEY, null);
            }
            //end remove the annotations
        } finally {
            thread.setPriority(initiaThreadlPriority);
        }
    }

    /**
     * Gotten from JavaEditor#getLockObject
     */
    private Object getLockObject(IAnnotationModel annotationModel) {
        if (annotationModel instanceof ISynchronizable)
            return ((ISynchronizable) annotationModel).getLockObject();
        else
            return annotationModel;
    }

    /**
     * This is the function that should be called when we want to schedule a request for 
     * a mark occurrences job.
     */
    public static synchronized void scheduleRequest(WeakReference<PyEdit> editor2, PySelection ps) {
        MarkOccurrencesJob j = singleton;
        if (j != null) {
            synchronized (j) {
                j.cancel();
                singleton = null;
            }
        }
        singleton = new MarkOccurrencesJob(editor2, ps);
        singleton.schedule(750);
    }

}
