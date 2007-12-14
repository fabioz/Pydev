package org.python.pydev.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
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
import org.python.pydev.plugin.PydevPlugin;

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
        @SuppressWarnings("unchecked")
        private Map fAddAnnotations;

        /** Lock object for modifying the annotations. */
        private Object fLockObject;

        /**
         * Initializes this collector with the given annotation model.
         *
         * @param annotationModel the annotation model
         */
        public SpellingProblemCollector(IAnnotationModel annotationModel) {
            Assert.isLegal(annotationModel != null);
            fAnnotationModel = annotationModel;
            if (fAnnotationModel instanceof ISynchronizable)
                fLockObject = ((ISynchronizable) fAnnotationModel).getLockObject();
            else
                fLockObject = fAnnotationModel;
        }

        /*
         * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#accept(org.eclipse.ui.texteditor.spelling.SpellingProblem)
         */
        @SuppressWarnings("unchecked")
        public void accept(SpellingProblem problem) {
            fAddAnnotations.put(new SpellingAnnotation(problem), new Position(problem.getOffset(), problem.getLength()));
        }

        /*
         * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#beginCollecting()
         */
        @SuppressWarnings("unchecked")
        public void beginCollecting() {
            fAddAnnotations = new HashMap();
        }

        /*
         * @see org.eclipse.ui.texteditor.spelling.ISpellingProblemCollector#endCollecting()
         */
        @SuppressWarnings("unchecked")
        public void endCollecting() {

            List toRemove = new ArrayList();

            synchronized (fLockObject) {
                Iterator iter = fAnnotationModel.getAnnotationIterator();
                while (iter.hasNext()) {
                    Object n = iter.next();
                    if (n instanceof SpellingAnnotation) {
                        toRemove.add(n);
                    }
                }
                Annotation[] annotationsToRemove = (Annotation[]) toRemove.toArray(new Annotation[toRemove.size()]);

                if (fAnnotationModel instanceof IAnnotationModelExtension) {
                    ((IAnnotationModelExtension) fAnnotationModel).replaceAnnotations(annotationsToRemove, fAddAnnotations);
                } else {
                    for (int i = 0; i < annotationsToRemove.length; i++) {
                        fAnnotationModel.removeAnnotation(annotationsToRemove[i]);
                    }
                    for (iter = fAddAnnotations.keySet().iterator(); iter.hasNext();) {
                        Annotation annotation = (Annotation) iter.next();
                        fAnnotationModel.addAnnotation(annotation, (Position) fAddAnnotations.get(annotation));
                    }
                }
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

    private ISpellingProblemCollector fSpellingProblemCollector;

    /** The spelling context containing the Java source content type. */
    private SpellingContext fSpellingContext;

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
    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
        reconcile(subRegion);
    }

    /*
     * @see org.eclipse.jface.text.reconciler.IReconcilingStrategy#reconcile(org.eclipse.jface.text.IRegion)
     */
    public void reconcile(IRegion region) {
        if (fViewer.getAnnotationModel() == null || fSpellingProblemCollector == null) {
            return;
        }

        try {
            //we're not using incremental updates!!! -- that's why the region is ignored and fDocument.len is used.
            //PyEditConfiguration#getReconciler should also be configured for that if needed.
            ITypedRegion[] partitions = TextUtilities.computePartitioning(fDocument, IPythonPartitions.PYTHON_PARTITION_TYPE, 0, fDocument
                    .getLength(), false);

            ArrayList<IRegion> regions = new ArrayList<IRegion>();
            for (ITypedRegion partition : partitions) {
                if (fProgressMonitor != null && fProgressMonitor.isCanceled())
                    return;

                String type = partition.getType();

                if (!type.equals(IPythonPartitions.PY_DEFAULT) && !type.equals(IPythonPartitions.PY_BACKQUOTES)) {
                    //only calculate for regions that are strings and comments.
                    regions.add(new Region(partition.getOffset(), partition.getLength()));
                }
            }

            int size = regions.size();
            if (size > 0) {
                fSpellingService.check(fDocument, regions.toArray(new IRegion[size]), fSpellingContext, fSpellingProblemCollector,
                        fProgressMonitor);
            }

        } catch (Exception e) {
            PydevPlugin.log(e);
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
    public void setDocument(IDocument document) {
        fDocument = document;

        IAnnotationModel model = fViewer.getAnnotationModel();
        if (model == null) {
            fSpellingProblemCollector = null;
        } else {
            fSpellingProblemCollector = new SpellingProblemCollector(model);
        }
    }

    /*
     * @see org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
     */
    public final void setProgressMonitor(IProgressMonitor monitor) {
        fProgressMonitor = monitor;
    }

}
