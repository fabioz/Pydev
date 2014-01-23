package org.python.pydev.shared_ui.mark_occurrences;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.AssertionFailedException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.ProgressMonitorWrapper;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_ui.editor.BaseEditor;

/**
 * This is a 'low-priority' thread. It acts as a singleton. Requests to mark the occurrences
 * will be forwarded to it, so, it should sleep for a while and then check for a request.
 * 
 * If the request actually happened, it will go on to process it, otherwise it will sleep some more.
 * 
 * @author Fabio
 */
public abstract class BaseMarkOccurrencesJob extends Job {

    protected static class MarkOccurrencesRequest {

        public final boolean proceedWithMarkOccurrences;

        public MarkOccurrencesRequest(boolean proceedWithMarkOccurrences) {
            this.proceedWithMarkOccurrences = proceedWithMarkOccurrences;
        }

    }

    public static final boolean DEBUG = false;

    public BaseMarkOccurrencesJob(String string) {
        super(string);
    }

    /**
     * This is the editor to be analyzed
     */
    protected WeakReference<BaseEditor> editor;

    /**
     * This is the request time for this job
     */
    private long currRequestTime = -1;

    /**
     * Make it thread safe.
     * 
     * Note: it's static because we only want 1 mark occurrences job running at a time!
     */
    private static volatile long lastRequestTime = -1;

    private static BaseMarkOccurrencesJob currRunningInstance;
    private static final Object lock = new Object();

    public static synchronized void scheduleRequest(BaseMarkOccurrencesJob newJob) {
        scheduleRequest(newJob, 700);
    }

    /**
     * This is the function that should be called when we want to schedule a request for 
     * a mark occurrences job.
     */
    public static synchronized void scheduleRequest(BaseMarkOccurrencesJob newJob, int scheduleTime) {
        synchronized (lock) {
            BaseMarkOccurrencesJob j = currRunningInstance;
            if (j != null) {
                //I.e.: we only want to have one job running at a time!
                j.cancel();
                currRunningInstance = null;
            }
            currRunningInstance = newJob;
            currRunningInstance.schedule(scheduleTime);
        }
    }

    /**
     * The selection when the occurrences job was requested
     */
    protected TextSelectionUtils ps;

    protected BaseMarkOccurrencesJob(WeakReference<BaseEditor> editor, TextSelectionUtils ps) {
        super("MarkOccurrencesJob");
        setPriority(Job.BUILD);
        setSystem(true);
        this.editor = editor;
        this.ps = ps;
        currRequestTime = System.currentTimeMillis();
    }

    protected abstract MarkOccurrencesRequest createRequest(BaseEditor baseEditor,
            IDocumentProvider documentProvider, IProgressMonitor monitor) throws Exception;

    @Override
    public IStatus run(IProgressMonitor monitor) {
        if (currRequestTime == -1) {
            return Status.OK_STATUS;
        }
        if (currRequestTime == lastRequestTime) {
            return Status.OK_STATUS;
        }
        lastRequestTime = currRequestTime;

        monitor = new ProgressMonitorWrapper(monitor) {
            @Override
            public boolean isCanceled() {
                return super.isCanceled() || currRequestTime != lastRequestTime;
            }
        };

        BaseEditor baseEditor = editor.get();
        try {
            try {

                if (baseEditor == null || monitor.isCanceled()) {
                    return Status.OK_STATUS;
                }

                IEditorInput editorInput = baseEditor.getEditorInput();
                if (editorInput == null) {
                    return Status.OK_STATUS;
                }

                IDocumentProvider documentProvider = baseEditor.getDocumentProvider();
                if (documentProvider == null || monitor.isCanceled()) {
                    return Status.OK_STATUS;
                }

                IAnnotationModel annotationModel = documentProvider.getAnnotationModel(baseEditor.getEditorInput());
                if (annotationModel == null || monitor.isCanceled()) {
                    return Status.OK_STATUS;
                }

                //now, let's see if the editor still has a document (so that we still can add stuff to it)

                if (documentProvider.getDocument(editorInput) == null) {
                    return Status.OK_STATUS;
                }

                if (baseEditor.getSelectionProvider() == null) {
                    return Status.OK_STATUS;
                }

                //to see if a new request was not created in the meantime (in which case this one will be cancelled)
                if (monitor.isCanceled()) {
                    return Status.OK_STATUS;
                }

                MarkOccurrencesRequest ret = createRequest(baseEditor, documentProvider, monitor);
                if (baseEditor.cache == null || monitor.isCanceled()) { //disposed (cannot add or remove annotations)
                    return Status.OK_STATUS;
                }

                if (ret != null && ret.proceedWithMarkOccurrences) {
                    Map<String, Object> cache = baseEditor.cache;
                    if (cache == null) {
                        return Status.OK_STATUS;
                    }

                    Map<Annotation, Position> annotationsToAddAsMap = getAnnotationsToAddAsMap(baseEditor,
                            annotationModel,
                            ret, monitor);
                    if (annotationsToAddAsMap == null) {
                        //something went wrong, so, let's remove the occurrences
                        removeOccurenceAnnotations(annotationModel, baseEditor);
                    } else {
                        //get the ones to remove
                        List<Annotation> toRemove = getOccurrenceAnnotationsInEditor(baseEditor);

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
                                ext.replaceAnnotations(toRemove.toArray(new Annotation[0]), annotationsToAddAsMap);
                            }

                        } finally {
                            thread.setPriority(initiaThreadlPriority);
                        }

                        //put them in the pyEdit
                        cache.put(getOccurrenceAnnotationsCacheKey(),
                                new ArrayList<Annotation>(annotationsToAddAsMap.keySet()));

                    }
                } else {
                    removeOccurenceAnnotations(annotationModel, baseEditor);
                }
            } catch (OperationCanceledException e) {
                throw e;//rethrow this error...
            } catch (AssertionFailedException e) {
                String message = e.getMessage();
                if (message != null && message.indexOf("The file:") != -1 && message.indexOf("does not exist.") != -1) {
                    //don't even report it (the file was probably removed while we were doing the analysis)
                } else {
                    Log.log(e);
                    Log.log("Error while analyzing the file:" + baseEditor.getIFile());
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

    protected abstract Map<Annotation, Position> getAnnotationsToAddAsMap(BaseEditor baseEditor,
            IAnnotationModel annotationModel, MarkOccurrencesRequest ret, IProgressMonitor monitor)
            throws BadLocationException;

    /**
     * Gotten from JavaEditor#getLockObject
     */
    protected Object getLockObject(IAnnotationModel annotationModel) {
        if (annotationModel instanceof ISynchronizable) {
            return ((ISynchronizable) annotationModel).getLockObject();
        } else {
            return annotationModel;
        }
    }

    protected abstract String getOccurrenceAnnotationsCacheKey();

    protected abstract String getOccurrenceAnnotationsType();

    /**
     * @return the list of occurrence annotations in the pyedit
     */
    public final List<Annotation> getOccurrenceAnnotationsInEditor(final BaseEditor baseEditor) {
        List<Annotation> toRemove = new ArrayList<Annotation>();
        final Map<String, Object> cache = baseEditor.cache;

        if (cache == null) {
            return toRemove;
        }

        @SuppressWarnings("unchecked")
        List<Annotation> inEdit = (List<Annotation>) cache.get(getOccurrenceAnnotationsCacheKey());
        if (inEdit != null) {
            Iterator<Annotation> annotationIterator = inEdit.iterator();
            while (annotationIterator.hasNext()) {
                Annotation annotation = annotationIterator.next();
                if (annotation.getType().equals(getOccurrenceAnnotationsType())) {
                    toRemove.add(annotation);
                }
            }
        }
        return toRemove;
    }

    /**
     * @param annotationModel
     */
    protected synchronized void removeOccurenceAnnotations(IAnnotationModel annotationModel, BaseEditor pyEdit) {
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
                List<Annotation> annotationsToRemove = getOccurrenceAnnotationsInEditor(pyEdit);

                if (annotationModel instanceof IAnnotationModelExtension) {
                    //replace those 
                    ((IAnnotationModelExtension) annotationModel).replaceAnnotations(
                            annotationsToRemove.toArray(new Annotation[annotationsToRemove.size()]),
                            new HashMap<Annotation, Position>());
                } else {
                    Iterator<Annotation> annotationIterator = annotationsToRemove.iterator();

                    while (annotationIterator.hasNext()) {
                        annotationModel.removeAnnotation(annotationIterator.next());
                    }
                }
                cache.put(getOccurrenceAnnotationsCacheKey(), null);
            }
            //end remove the annotations
        } finally {
            thread.setPriority(initiaThreadlPriority);
        }
    }
}
