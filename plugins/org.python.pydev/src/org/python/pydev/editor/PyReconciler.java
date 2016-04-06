/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISynchronizable;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingContext;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;
import org.eclipse.ui.texteditor.spelling.SpellingService;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.log.Log;

/**
 * Based on SpellingReconcileStrategy.
 * 
 * Reconcile strategy used for spell checking.
 */
public class PyReconciler implements IReconcilingStrategy, IReconcilingStrategyExtension {

    /**
     * Spelling problem collector.
     */
    private class SpellingProblemCollector implements ISpellingProblemCollector {

        /** Annotation model. */
        private IAnnotationModel fAnnotationModel;

        /** Annotations to add. */
        private Map<Annotation, Position> fAddAnnotations;

        /**
         * Initializes this collector with the given annotation model.
         *
         * @param annotationModel the annotation model
         */
        public SpellingProblemCollector(IAnnotationModel annotationModel) {
            Assert.isLegal(annotationModel != null);
            fAnnotationModel = annotationModel;
        }

        /*
         * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#accept(org.eclipse.ui.texteditor.spelling.SpellingProblem)
         */
        @Override
        public void accept(SpellingProblem problem) {
            fAddAnnotations
                    .put(new SpellingAnnotation(problem), new Position(problem.getOffset(), problem.getLength()));
        }

        /*
         * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#beginCollecting()
         */
        @Override
        public void beginCollecting() {
            fAddAnnotations = new HashMap<Annotation, Position>();
        }

        /*
         * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#endCollecting()
         */
        @Override
        public void endCollecting() {

            List<Object> toRemove = new ArrayList<Object>();

            Object fLockObject;
            if (fAnnotationModel instanceof ISynchronizable) {
                fLockObject = ((ISynchronizable) fAnnotationModel).getLockObject();
            } else {
                fLockObject = new Object();
            }

            //let other threads execute before getting the lock on the annotation model
            Thread.yield();

            Thread thread = Thread.currentThread();
            int initiaThreadlPriority = thread.getPriority();
            try {
                //before getting the lock, let's execute with normal priority, to optimize the time that we'll 
                //retain that object locked (the annotation model is used on lots of places, so, retaining the lock
                //on it on a minimum priority thread is not a good thing.
                thread.setPriority(Thread.NORM_PRIORITY);
                Iterator<Annotation> iter;

                synchronized (fLockObject) {
                    iter = fAnnotationModel.getAnnotationIterator();
                    while (iter.hasNext()) {
                        Object n = iter.next();
                        if (n instanceof SpellingAnnotation) {
                            toRemove.add(n);
                        }
                    }
                    iter = null;
                }

                Annotation[] annotationsToRemove = toRemove.toArray(new Annotation[toRemove.size()]);

                //let other threads execute before getting the lock (again) on the annotation model
                Thread.yield();
                synchronized (fLockObject) {
                    if (fAnnotationModel instanceof IAnnotationModelExtension) {
                        ((IAnnotationModelExtension) fAnnotationModel).replaceAnnotations(annotationsToRemove,
                                fAddAnnotations);
                    } else {
                        for (int i = 0; i < annotationsToRemove.length; i++) {
                            fAnnotationModel.removeAnnotation(annotationsToRemove[i]);
                        }
                        for (iter = fAddAnnotations.keySet().iterator(); iter.hasNext();) {
                            Annotation annotation = iter.next();
                            fAnnotationModel.addAnnotation(annotation, fAddAnnotations.get(annotation));
                        }
                    }
                }

            } finally {
                thread.setPriority(initiaThreadlPriority);
            }
            fAddAnnotations = null;
        }
    }

    /** The text editor to operate on. */
    private ISourceViewer fViewer;

    /** The document to operate on. */
    private IDocument fDocument;

    /** The progress monitor. */
    private IProgressMonitor fProgressMonitor;

    private SpellingService fSpellingService;

    /** The spelling context containing the Java source content type. */
    private SpellingContext fSpellingContext;

    /**
     * Set containing the models that are being checked at the current moment (this is used
     * so that when there are multiple editors binded to the same model, we only make the check in one of those
     * models -- the others don't need to do anything, as it's based on the annotation model that's shared
     * among them)
     * 
     * It's static so that we can share it among threads.
     */
    private static HashSet<IAnnotationModel> modelBeingChecked = new HashSet<IAnnotationModel>();

    /**
     * Creates a new comment reconcile strategy.
     * 
     * @param viewer the source viewer
     * @param spellingService the spelling service to use
     */
    public PyReconciler(ISourceViewer viewer, SpellingService spellingService) {
        Assert.isNotNull(viewer);
        Assert.isNotNull(spellingService);
        fViewer = viewer;
        fSpellingService = spellingService;
        fSpellingContext = new SpellingContext();
        fSpellingContext.setContentType(getContentType());
    }

    /*
     * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#initialReconcile()
     */
    @Override
    public void initialReconcile() {
        reconcile(new Region(0, fDocument.getLength()));
    }

    /*
     * Currently only calls the 'whole doc reconcile'.
     * 
     * To make it work we'd need to get the dirtyRegion + region of the text next to it, remove annotations matching and
     * do a new reconcile in that region.
     * 
     * Note that the dirty region marks whether it was a removal or insertion.
     * 
     * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.reconciler.DirtyRegion,org.eclipse.jface.text.IRegion)
     */
    @Override
    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
        reconcile(subRegion);
    }

    /*
     * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.IRegion)
     */
    @Override
    public void reconcile(IRegion region) {
        IAnnotationModel annotationModel = fViewer.getAnnotationModel();

        if (annotationModel == null) {
            return;
        }

        //Bug: https://sourceforge.net/tracker/index.php?func=detail&aid=2013310&group_id=85796&atid=577329
        //When having multiple editors for the same document, only one of the reconcilers actually needs to
        //work (because the others are binded to the same annotation model, so, having one do the work is enough)
        synchronized (modelBeingChecked) {
            if (modelBeingChecked.contains(annotationModel)) {
                return;
            }
            modelBeingChecked.add(annotationModel);
        }

        try {
            //we're not using incremental updates!!! -- that's why the region is ignored and fDocument.len is used.
            //PyEditConfiguration#getReconciler should also be configured for that if needed.
            ITypedRegion[] partitions = TextUtilities.computePartitioning(fDocument,
                    IPythonPartitions.PYTHON_PARTITION_TYPE, 0, fDocument.getLength(), false);

            ArrayList<IRegion> regions = new ArrayList<IRegion>();
            for (ITypedRegion partition : partitions) {
                if (fProgressMonitor != null && fProgressMonitor.isCanceled()) {
                    return;
                }

                String type = partition.getType();

                if (!type.equals(IPythonPartitions.PY_DEFAULT) && !type.equals(IPythonPartitions.PY_BACKQUOTES)) {
                    //only calculate for regions that are strings and comments.
                    regions.add(new Region(partition.getOffset(), partition.getLength()));
                }
            }

            int size = regions.size();
            if (size > 0) {
                //only create the collector when actually needed for the current model
                ISpellingProblemCollector spellingProblemCollector = new SpellingProblemCollector(annotationModel);
                fSpellingService.check(fDocument, regions.toArray(new IRegion[size]), fSpellingContext,
                        spellingProblemCollector, fProgressMonitor);
            }

        } catch (BadLocationException e) {
            //Ignore: can happen if the document changes during the reconciling.

        } catch (Exception e) {
            Log.log(e);

        } finally {
            synchronized (modelBeingChecked) {
                modelBeingChecked.remove(annotationModel);
            }
        }
    }

    /**
     * Returns the content type of the underlying editor input.
     *
     * @return the content type of the underlying editor input or
     *         <code>null</code> if none could be determined
     */
    protected IContentType getContentType() {
        return Platform.getContentTypeManager().getContentType("org.python.pydev.pythonfile");
    }

    /*
     * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#setDocument(org.eclipse.jface.text.IDocument)
     */
    @Override
    public void setDocument(IDocument document) {
        fDocument = document;

        //Note: if we have multiple editors for the same doc, the document and the annotation model will be the same 
        //for multiple reconcilers
    }

    /*
     * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public final void setProgressMonitor(IProgressMonitor monitor) {
        fProgressMonitor = monitor;
    }

}
