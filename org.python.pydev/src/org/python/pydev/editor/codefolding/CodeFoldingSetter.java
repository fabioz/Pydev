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
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.editors.text.TextEditor;
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
public class CodeFoldingSetter implements IModelListener {

    private TextEditor editor;

    public CodeFoldingSetter(TextEditor editor) {
        this.editor = editor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.editor.model.IModelListener#modelChanged(org.python.pydev.editor.model.AbstractNode)
     */
    public void modelChanged(AbstractNode root) {
        IAnnotationModel model = (IAnnotationModel) editor
                .getAdapter(ProjectionAnnotationModel.class);
        
        
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
	            while (current != null) {
	                if (current instanceof FunctionNode
	                        || current instanceof ClassNode) {
	                    addFoldingMark(current, model, collapsed);
	                }
	                current = ModelUtils.getNextNode(current);
	            }
	            
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
     * @param node
     * @param model
     * @throws BadLocationException
     */
    private void addFoldingMark(AbstractNode node, IAnnotationModel model, ArrayList collapsed) throws BadLocationException {
        int start = node.getStart().line;
        int end = start;
        int size = node.getChildren().size();
        if(size > 0){
            end = ((AbstractNode)node.getChildren().get(size-1)).getScope().getEnd().line+1;
        }else{
            throw new BadLocationException("Invalid location");
        }

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

}