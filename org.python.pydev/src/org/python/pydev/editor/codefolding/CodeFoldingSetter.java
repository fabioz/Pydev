/*
 * Created on Jul 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codefolding;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.IPropertyListener;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.editor.model.ClassNode;
import org.python.pydev.editor.model.FunctionNode;
import org.python.pydev.editor.model.IModelListener;
import org.python.pydev.editor.model.ModelUtils;

/**
 * @author Fabio Zadrozny
 * 
 * This class is used to set the code folding markers.
 */
public class CodeFoldingSetter implements IModelListener, IPropertyListener {

    private PyEdit editor;

    public CodeFoldingSetter(PyEdit editor) {
        this.editor = editor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.editor.model.IModelListener#modelChanged(org.python.pydev.editor.model.AbstractNode)
     */
    public synchronized void modelChanged(AbstractNode root) {
        IAnnotationModel model = (IAnnotationModel) editor
                .getAdapter(ProjectionAnnotationModel.class);
        
        if (model == null){
            //we have to get the model to do it... so, start a thread and try until get it...
            //this had to be done because sometimes we get here and we still are unable to get the 
            //projection annotation model. (there should be a better way, but this solves it...
            //even if it looks like a hack...)
            new Thread(){
                public void run(){
                    IAnnotationModel modelT = null;
                    for(int i=0 ; i < 10 && modelT == null; i++){
	                    modelT = (IAnnotationModel) editor
	                    .getAdapter(ProjectionAnnotationModel.class);
	                    try {
                            sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (modelT != null){
                        addMarksToModel(editor.getPythonModel(), modelT);
                    }
                }
            }.start();
        }else{
            addMarksToModel(root, model);
        }   

    }

    /**
     * @param root
     * @param model
     */
    private void addMarksToModel(AbstractNode root, IAnnotationModel model) {
        try{
	        if (model != null) {
	            ArrayList collapsed = new ArrayList();
	            
	            //put annotations in array list.
	            Iterator iter = model.getAnnotationIterator();
	            while ( iter != null && iter.hasNext()) {
	                PyProjectionAnnotation element = (PyProjectionAnnotation) iter.next();
	                collapsed.add(element);
	            }

	            //(re) insert annotations.
	            AbstractNode current = ModelUtils.getNextNode(root);
	            
	            ArrayList nodes = new ArrayList();
	            
	            while (current != null) {
	                if (current instanceof FunctionNode
	                        || current instanceof ClassNode) {
	                    nodes.add(current);
	                }
	                current = ModelUtils.getNextNode(current);
	            }
	            
	            addMarks(nodes,model, collapsed);
                
	            
	            //remove the annotations that have not been reinserted.
	            for (Iterator it = collapsed.iterator(); it.hasNext();) {
	                PyProjectionAnnotation element = (PyProjectionAnnotation) it.next();
                    model.removeAnnotation(element);
                }
	        }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * To add a mark, we have to do the following:
     * 
     * Get the current node to add and find the next that is on the same indentation
     * or on an indentation that is lower than the current (this will mark the
     * end of the selection).
     * 
     * If we don't find that, the end of the selection is the end of the file. 
     * 
     * @param nodes
     * @param collapsed
     * @param model
     */
    private void addMarks(ArrayList nodes, IAnnotationModel model, ArrayList collapsed) {
        int i=0;
        
        IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        
        for (Iterator iter = nodes.iterator(); iter.hasNext();++i) {
            
            AbstractNode element = (AbstractNode) iter.next();
            int end = findEnd(element, nodes, i, doc);
            int start = element.getStart().line;
            if (end == -1){
                end = start;
            }
            try {
                addFoldingMark(element, start, end, model, collapsed);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param element
     */
    private int getStartColumn(AbstractNode element) {
        int start = element.getStart().column;
        if (element instanceof FunctionNode){
            return start-4; //this is the 'def ' token
        }else if( element instanceof ClassNode){
            return start-6; //this is the 'class ' token
        }else{
            throw new RuntimeException("Invalid class");
        }
        
    }

    /**
     * @param element
     * @param nodes
     * @param i
     * @param doc
     */
    private int findEnd(AbstractNode element, ArrayList nodes, int m, IDocument doc) {
        int end = -1;

        int start = getStartColumn(element);
        
        //we are interested in getting the next code that is not a comment and that
        //starts in the same or lower indentation level.
        int line = element.getStart().line;
        int endDocLine = doc.getNumberOfLines();
        try {
            for(int i=line+1; i<endDocLine; ++i){
                
                
                IRegion region;
                region = doc.getLineInformation(i);
                String src = doc.get(region.getOffset(), region.getLength());
                //we have to ignore comments and whitespaces.
                String trimmed = src.trim();
                if(trimmed.length() == 0 || trimmed.startsWith("#")){
                    continue;
                }
                //TODO: check for multiline strings.
                
                int position = PyAction.getFirstCharRelativePosition(doc, region);
                if (position <= start){
                    return i;
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        
        return end;
    }


    /**
     * @param node
     * @param model
     * @throws BadLocationException
     */
    private void addFoldingMark(AbstractNode node, int start, int end, IAnnotationModel model, ArrayList collapsed) throws BadLocationException {

        try {
            IDocument document = editor.getDocumentProvider().getDocument(
                    editor.getEditorInput());
            int offset = document.getLineOffset(start);
            int endOffset = document.getLineOffset(end);
            Position position = new Position(offset, endOffset - offset);
            
            
            model.addAnnotation(getAnnotationToAdd(position, node, model, collapsed), position);
            
            
        } catch (BadLocationException x) {
            x.printStackTrace();
        }
    }
    
    /**
     * We have to be careful not to remove collapsed annotations because if this happens,
     * previous code folding is not correct. 
     * 
     * @param position
     * @param node
     * @param model
     * @param collapsed
     * @return
     */
    private ProjectionAnnotation getAnnotationToAdd(Position position, AbstractNode node, IAnnotationModel model, ArrayList collapsed){
        for (Iterator iter = collapsed.iterator(); iter.hasNext();) {
            PyProjectionAnnotation element = (PyProjectionAnnotation) iter.next();
            if (element.appearsSame(node)){
                collapsed.remove(element); //after getting it, remove it, so we don't accidentally get it again.
                return element;
            }
        }
        return new PyProjectionAnnotation(node );
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPropertyListener#propertyChanged(java.lang.Object, int)
     */
    public void propertyChanged(Object source, int propId) {
        if(propId == PyEditProjection.PROP_FOLDING_CHANGED){
            System.out.println("PyEditProjection.PROP_FOLDING_CHANGED");
            modelChanged(editor.getPythonModel());
        }
    }

}