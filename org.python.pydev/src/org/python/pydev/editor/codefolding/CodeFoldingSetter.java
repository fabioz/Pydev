/*
 * Created on Jul 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codefolding;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.IPropertyListener;
import org.python.parser.SimpleNode;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.editor.model.ClassNode;
import org.python.pydev.editor.model.FunctionNode;
import org.python.pydev.editor.model.IModelListener;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;

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
    public synchronized void modelChanged(AbstractNode root, final SimpleNode root2) {
        ProjectionAnnotationModel model = (ProjectionAnnotationModel) editor.getAdapter(ProjectionAnnotationModel.class);

        if (model == null) {
            //we have to get the model to do it... so, start a thread and try until get it...
            //this had to be done because sometimes we get here and we still are unable to get the
            //projection annotation model. (there should be a better way, but this solves it...
            //even if it looks like a hack...)
            new Thread() {
                public void run() {
                    ProjectionAnnotationModel modelT = null;
                    for (int i = 0; i < 10 && modelT == null; i++) {
                        modelT = (ProjectionAnnotationModel) editor.getAdapter(ProjectionAnnotationModel.class);
                        try {
                            sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (modelT != null) {
                        addMarksToModel(root2, modelT);
                    }
                }
            }.start();
        } else {
            addMarksToModel(root2, model);
        }

    }

    /**
     * @param root2
     * @param model
     */
    private void addMarksToModel(SimpleNode root2, ProjectionAnnotationModel model) {
        try {
            if (model != null) {
                ArrayList collapsed = new ArrayList();

                //put annotations in array list.
                Iterator iter = model.getAnnotationIterator();
                while (iter != null && iter.hasNext()) {
                    PyProjectionAnnotation element = (PyProjectionAnnotation) iter.next();
                    collapsed.add(element);
                }

                EasyASTIteratorVisitor visitor = EasyASTIteratorVisitor.create(root2);
                //(re) insert annotations.
                List nodes = visitor.getClassesAndMethodsList();

                addMarks(nodes, model, collapsed);

                //remove the annotations that have not been reinserted.
                for (Iterator it = collapsed.iterator(); it.hasNext();) {
                    PyProjectionAnnotation element = (PyProjectionAnnotation) it.next();
                    model.removeAnnotation(element);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * To add a mark, we have to do the following:
     * 
     * Get the current node to add and find the next that is on the same indentation or on an indentation that is lower than the current (this will mark the end of the selection).
     * 
     * If we don't find that, the end of the selection is the end of the file.
     * 
     * @param nodes
     * @param collapsed
     * @param model
     */
    private void addMarks(List nodes, ProjectionAnnotationModel model, ArrayList collapsed) {
        int i = 0;

        try {
            IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());

            for (Iterator iter = nodes.iterator(); iter.hasNext(); ++i) {

                ASTEntry element = (ASTEntry) iter.next();
                int end = element.endLine;
                int start = element.node.beginLine-1;
                if (end == -1) {
                    end = start;
                }
                try {
                    addFoldingMark(element, start, end, model, collapsed);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        } catch (NullPointerException e) {
        }
    }

    /**
     * @param element
     */
    private int getStartColumn(AbstractNode element) {
        int start = element.getStart().column;
        if (element instanceof FunctionNode) {
            return start - 4; //this is the 'def ' token
        } else if (element instanceof ClassNode) {
            return start - 6; //this is the 'class ' token
        } else {
            throw new RuntimeException("Invalid class");
        }

    }

    /**
     * @param node
     * @param model
     * @throws BadLocationException
     */
    private void addFoldingMark(ASTEntry node, int start, int end, ProjectionAnnotationModel model, ArrayList collapsed) throws BadLocationException {

        try {
            IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            int offset = document.getLineOffset(start);
            int endOffset = document.getLineOffset(end);
            Position position = new Position(offset, endOffset - offset);

            Annotation anottation = getAnnotationToAdd(position, node, model, collapsed);
            if (model.getPosition(anottation) != null && model.getPosition(anottation).equals(position) == false) {
                model.modifyAnnotationPosition(anottation, position);
            } else {
                model.addAnnotation(anottation, position);
            }

        } catch (BadLocationException x) {
        }
    }

    /**
     * We have to be careful not to remove collapsed annotations because if this happens, previous code folding is not correct.
     * 
     * @param position
     * @param node
     * @param model
     * @param collapsed
     * @return
     */
    private ProjectionAnnotation getAnnotationToAdd(Position position, ASTEntry node, ProjectionAnnotationModel model, ArrayList collapsed) {
        for (Iterator iter = collapsed.iterator(); iter.hasNext();) {
            PyProjectionAnnotation element = (PyProjectionAnnotation) iter.next();
            if (element.appearsSame(node)) {
                collapsed.remove(element); //after getting it, remove it, so we don't accidentally get it again.
                return element;
            }
        }
        return new PyProjectionAnnotation(node);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IPropertyListener#propertyChanged(java.lang.Object, int)
     */
    public void propertyChanged(Object source, int propId) {
        if (propId == PyEditProjection.PROP_FOLDING_CHANGED) {
            modelChanged(null, editor.getAST());
        }
    }

}