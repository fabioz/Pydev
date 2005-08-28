/*
 * Created on Sep 23, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codefolding;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.PyEditConfiguration;
import org.python.pydev.editor.correctionassist.PyCorrectionAssistant;


public class PySourceViewer extends ProjectionViewer {

    private PyEditProjection projection;
    private PyCorrectionAssistant fCorrectionAssistant;

    public PySourceViewer(Composite parent, IVerticalRuler ruler, IOverviewRuler overviewRuler, boolean showsAnnotationOverview, int styles, PyEditProjection projection) {
        super(parent, ruler, overviewRuler, showsAnnotationOverview, styles);
        this.projection = projection;
        
    }
    
    public void configure(SourceViewerConfiguration configuration) {
        super.configure(configuration);
        if (configuration instanceof PyEditConfiguration) {
            PyEditConfiguration pyConfiguration = (PyEditConfiguration) configuration;
            fCorrectionAssistant = pyConfiguration.getCorrectionAssistant(this);
            fCorrectionAssistant.install(this);
        }
    }
        
    
    /**
     * @return a class that iterates through the markers available in this source viewer
     */
    public Iterable<IMarker> getMarkerIteratable(){
        final IAnnotationModel annotationModel = getAnnotationModel();
        //it may be null on external files, because I simply cannot make it get the org.python.copiedfromeclipsesrc.PydevFileEditorInput
        //(if it did, I could enhance it...). Instead, it returns a org.eclipse.ui.internal.editors.text.JavaFileEditorInput
        //that never has an annotation model. (shortly, eclipse bug).
        if(annotationModel != null){
            final Iterator annotationIterator = annotationModel.getAnnotationIterator();
    
            return new Iterable<IMarker>(){
    
                public Iterator<IMarker> iterator() {
                    return new Iterator<IMarker>(){
    
                        private IMarker marker;
    
                        public boolean hasNext() {
                            while(annotationIterator.hasNext()){
                                if(marker != null){
                                    return true;
                                }
                                
                                while(annotationIterator.hasNext()){
                                    Object object = annotationIterator.next();
                                    if(object instanceof MarkerAnnotation){
                                        MarkerAnnotation m = (MarkerAnnotation) object;
                                        marker = m.getMarker();
                                        return true;
                                    }
                                }
                            }
                            return false;
                        }
    
                        public IMarker next() {
                            hasNext();
                            
                            IMarker m = marker;
                            marker = null;
                            return m;
                        }
    
                        public void remove() {
                            throw new RuntimeException("not implemented");
                        }
                        
                    };
                }
                
            };
        }
        return new Iterable<IMarker>(){
            
            public Iterator<IMarker> iterator() {
                return new Iterator<IMarker>(){
                    public boolean hasNext() {
                        return false;
                    }

                    public IMarker next() {
                        return null;
                    }

                    public void remove() {
                        throw new RuntimeException("not implemented");
                    }
                };
            }
        };
    }
    
    /* (non-Javadoc)
    }
     * @see org.eclipse.jface.text.source.projection.ProjectionViewer#canDoOperation(int)
     */
    public boolean canDoOperation(int operation) {
        
        if(operation == PyEdit.CORRECTIONASSIST_PROPOSALS){
            return true;
        }
        
        return super.canDoOperation(operation);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.text.source.projection.ProjectionViewer#doOperation(int)
     */
    public void doOperation(int operation) {
        super.doOperation(operation);
		if (getTextWidget() == null)
			return;
		
		switch (operation) {
			case PyEdit.CORRECTIONASSIST_PROPOSALS:
				String msg= fCorrectionAssistant.showPossibleCompletions();
				projection.setStatusLineErrorMessage(msg);
				return;
		}
    }
}