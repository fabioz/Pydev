/*
 * Created on Sep 23, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codefolding;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.python.pydev.editor.PyEdit;


public class PySourceViewer extends ProjectionViewer {


    private WeakReference<PyEdit> projection;

	public PySourceViewer(Composite parent, IVerticalRuler ruler, IOverviewRuler overviewRuler, boolean showsAnnotationOverview, int styles, PyEditProjection projection) {
        super(parent, ruler, overviewRuler, showsAnnotationOverview, styles);
        this.projection = new WeakReference<PyEdit>((PyEdit) projection);
    }
    
    private boolean isInToggleCompletionStyle;
    
    public void setInToggleCompletionStyle(boolean b){
        this.isInToggleCompletionStyle = b;
    }
    
    public boolean getIsInToggleCompletionStyle(){
        return this.isInToggleCompletionStyle;
    }
    
    public PyEdit getEdit(){
    	return projection.get();
    }
    
        
    /**
     * @param markerLine the line we want markers on
     * @param markerType the type of the marker (if null, it is not used)
     * @return a list of markers at the given line
     */
    public List<IMarker> getMarkersAtLine(int markerLine, String markerType){
        ArrayList<IMarker> markers = new ArrayList<IMarker>();
        
        Iterable<IMarker> markerIteratable = getMarkerIteratable();
        for (IMarker marker : markerIteratable) {
            try {
                //check the line
                Integer line = (Integer) marker.getAttribute(IMarker.LINE_NUMBER);
                if(line != null && line.intValue() == markerLine){
                    
                    //and the marker type
                    if(markerType == null || markerType.equals(marker.getType())){
                        markers.add(marker);
                    }
                }
            } catch (CoreException e) {
                //ok - no line ?
            }
        }
        
        return markers;
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
    

}