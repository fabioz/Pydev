/*
 * Created on Jul 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codefolding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.IPropertyListener;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.DocIterator;
import org.python.pydev.editor.ErrorDescription;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.model.IModelListener;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;

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
    public synchronized void modelChanged(final SimpleNode root2) {
        ProjectionAnnotationModel model = (ProjectionAnnotationModel) editor.getAdapter(ProjectionAnnotationModel.class);

        if (model == null) {
            //we have to get the model to do it... so, start a thread and try until get it...
            //this had to be done because sometimes we get here and we still are unable to get the
            //projection annotation model. (there should be a better way, but this solves it...
            //even if it looks like a hack...)
            Thread t = new Thread() {
                public void run() {
                    ProjectionAnnotationModel modelT = null;
                    for (int i = 0; i < 10 && modelT == null; i++) { //we will try it for 10 secs...
                        try {
                            sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        modelT = (ProjectionAnnotationModel) editor.getAdapter(ProjectionAnnotationModel.class);
                        if (modelT != null) {
                        	addMarksToModel(root2, modelT);
                        	break;
                        }
                    }
                }
            };
            t.setPriority(Thread.MIN_PRIORITY);
            t.setName("CodeFolding - get annotation model");
            t.start();
        } else {
            addMarksToModel(root2, model);
        }

    }

    /**
     * Given the ast, create the needed marks and set them in the passed model.
     */
    @SuppressWarnings("unchecked")
    private synchronized void addMarksToModel(SimpleNode root2, ProjectionAnnotationModel model) {
        try {
            if (model != null) {
                ArrayList<PyProjectionAnnotation> existing = new ArrayList<PyProjectionAnnotation>();

                //get the existing annotations
                Iterator<PyProjectionAnnotation> iter = model.getAnnotationIterator();
                while (iter != null && iter.hasNext()) {
                    PyProjectionAnnotation element = iter.next();
                    existing.add(element);
                }

                //now, remove the annotations not used and add the new ones needed
                IDocument doc = editor.getDocument();
                if(doc != null){ //this can happen if we change the input of the editor very quickly.
                    List<FoldingEntry> marks = getMarks(doc, root2);
                    Map<ProjectionAnnotation, Position> annotationsToAdd = getAnnotationsToAdd(marks, model, existing);
                    
                    model.replaceAnnotations(existing.toArray(new Annotation[existing.size()]), annotationsToAdd);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * To add a mark, we have to do the following:
     * 
     * Get the current node to add and find the next that is on the same indentation or on an indentation that is lower 
     * than the current (this will mark the end of the selection).
     * 
     * If we don't find that, the end of the selection is the end of the file.
     */
    private Map<ProjectionAnnotation, Position> getAnnotationsToAdd(List<FoldingEntry> nodes, ProjectionAnnotationModel model, 
            List<PyProjectionAnnotation> existing) {
        
        Map<ProjectionAnnotation, Position> annotationsToAdd = new HashMap<ProjectionAnnotation, Position>();
        try {
            for (FoldingEntry element:nodes) {
                if(element.startLine < element.endLine-1){
                    Tuple<ProjectionAnnotation, Position> tup = getAnnotationToAdd(element, element.startLine, element.endLine, model, existing);
                    if(tup != null){
                        annotationsToAdd.put(tup.o1, tup.o2);
                    }
                }
            }
        } catch (BadLocationException e) {
        } catch (NullPointerException e) {
        }
        return annotationsToAdd;
    }


    /**
     * @return an annotation that should be added (or null if that entry already has an annotation
     * added for it).
     */
    private Tuple<ProjectionAnnotation, Position> getAnnotationToAdd(FoldingEntry node, int start, int end, 
            ProjectionAnnotationModel model, List<PyProjectionAnnotation> existing) throws BadLocationException {
        try {
            IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            int offset = document.getLineOffset(start);
            int endOffset = offset; 
            try {
				endOffset = document.getLineOffset(end);
			} catch (Exception e) {
				//sometimes when we are at the last line, the command above will not work very well
				IRegion lineInformation = document.getLineInformation(end);
				endOffset = lineInformation.getOffset()+lineInformation.getLength();
			}
            Position position = new Position(offset, endOffset - offset);

            return getAnnotationToAdd(position, node, model, existing);

        } catch (BadLocationException x) {
        	//this could happen
        }
        return null;
    }

    /**
     * We have to be careful not to remove existing annotations because if this happens, previous code folding is not correct.
     */
    private Tuple<ProjectionAnnotation, Position> getAnnotationToAdd(Position position, FoldingEntry node, 
            ProjectionAnnotationModel model, List<PyProjectionAnnotation> existing) {
        for (Iterator<PyProjectionAnnotation> iter = existing.iterator(); iter.hasNext();) {
            PyProjectionAnnotation element = iter.next();
            Position existingPosition = model.getPosition(element);
            if (existingPosition.equals(position)) {
                //ok, do nothing to this annotation (neither remove nor add, as it already exists in the correct place).
                existing.remove(element); 
                return null;
            }
        }
        return new Tuple<ProjectionAnnotation, Position>(new PyProjectionAnnotation(node.getAstEntry()), position);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IPropertyListener#propertyChanged(java.lang.Object, int)
     */
    public void propertyChanged(Object source, int propId) {
        if (propId == PyEditProjection.PROP_FOLDING_CHANGED) {
            modelChanged(editor.getAST());
        }
    }
    
    

    /**
     * To get the marks, we work a little with the ast and a little with the doc... the ast is good to give us all things but the comments,
     * and the doc will give us the comments.
     * 
     * @return a list of entries, ordered by their appearance in the document.
     * 
     * Also, there should be no overlap for any of the entries
     */
    public static List<FoldingEntry> getMarks(IDocument doc, SimpleNode ast) {

        List<FoldingEntry> ret = new ArrayList<FoldingEntry>();
        
        SequencialASTIteratorVisitor visitor = SequencialASTIteratorVisitor.create(ast);
        //(re) insert annotations.
        List<ASTEntry> nodes = visitor.getAsList(new Class[]{Import.class, ImportFrom.class, ClassDef.class, FunctionDef.class, Str.class});            
        for (ASTEntry entry : nodes) {
            FoldingEntry foldingEntry = null;
            if(entry.node instanceof Import || entry.node instanceof ImportFrom){
                foldingEntry = new FoldingEntry(FoldingEntry.TYPE_IMPORT, entry.node.beginLine-1, entry.endLine, entry);
                
            }else if(entry.node instanceof ClassDef){
                ClassDef def = (ClassDef) entry.node;
                foldingEntry = new FoldingEntry(FoldingEntry.TYPE_DEF, def.name.beginLine-1, entry.endLine, entry);
                
            }else if(entry.node instanceof FunctionDef){
                FunctionDef def = (FunctionDef) entry.node;
                foldingEntry = new FoldingEntry(FoldingEntry.TYPE_DEF, def.name.beginLine-1, entry.endLine, entry);
                
            }else if(entry.node instanceof Str){
                foldingEntry = new FoldingEntry(FoldingEntry.TYPE_STR, entry.node.beginLine-1, entry.endLine, entry);
            }
            if(foldingEntry != null){
                addFoldingEntry(ret, foldingEntry);
            }
        }
        
        //and at last, get the comments
        DocIterator it = new PySelection.DocIterator(true, new PySelection(doc,0));
        while(it.hasNext()){
            String string = it.next();
            if(string.trim().startsWith("#")){
                int l = it.getCurrentLine()-1;
                addFoldingEntry(ret, new FoldingEntry(FoldingEntry.TYPE_COMMENT, l, l+1, new ASTEntry(null, new commentType(string))));
            }
        }
        Collections.sort(ret, new Comparator<FoldingEntry>(){

            public int compare(FoldingEntry o1, FoldingEntry o2) {
                if (o1.startLine < o2.startLine){
                    return -1;
                }
                if (o1.startLine > o2.startLine){
                    return 1;
                }
                return 0;
            }});

        return ret;
    }

    private static void addFoldingEntry(List<FoldingEntry> ret, FoldingEntry foldingEntry) {
        //we only group comments and imports
        if(ret.size() > 0 && (foldingEntry.type == FoldingEntry.TYPE_COMMENT || foldingEntry.type == FoldingEntry.TYPE_IMPORT)){
            FoldingEntry prev = ret.get(ret.size()-1);
            if(prev.type == foldingEntry.type && prev.startLine < foldingEntry.startLine && prev.endLine == foldingEntry.startLine){
                prev.endLine = foldingEntry.endLine;
            }else{
                ret.add(foldingEntry);
            }
        }else{
            ret.add(foldingEntry);
        }
    }

    public void errorChanged(ErrorDescription errorDesc) {
        //ignore the errors (we're just interested in the ast in this class)
    }

}











