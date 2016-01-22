/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
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
import java.util.ListResourceBundle;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPropertyListener;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.performanceeval.OptimizationRelatedConstants;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.jython.ISpecialStr;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.excepthandlerType;
import org.python.pydev.parser.jython.ast.suiteType;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.ASTEntryWithChildren;
import org.python.pydev.parser.visitors.scope.CodeFoldingVisitor;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.model.ErrorDescription;
import org.python.pydev.shared_core.model.IModelListener;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.string.DocIterator;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;
import org.python.pydev.shared_ui.editor.IPyEditListener3;

/**
 * @author Fabio Zadrozny
 * 
 * This class is used to set the code folding markers.
 * 
 * Changed 15/09/07 to include more folding elements
 */
public class CodeFoldingSetter implements IModelListener, IPropertyListener, IPyEditListener, IPyEditListener3 {

    private PyEdit editor;

    private volatile boolean initialFolding;
    private volatile boolean firstInputChangedCalled = false;

    public CodeFoldingSetter(PyEdit editor) {
        this.editor = editor;
        initialFolding = true;

        editor.addModelListener(this);
        editor.addPropertyListener(this);
        editor.addPyeditListener(this);
    }

    @Override
    public void onInputChanged(BaseEditor edit, IEditorInput oldInput, IEditorInput input, IProgressMonitor monitor) {
        initialFolding = true;
        firstInputChangedCalled = true;
    }

    @Override
    public void onSave(BaseEditor edit, IProgressMonitor monitor) {
    }

    @Override
    public void onCreateActions(ListResourceBundle resources, BaseEditor edit, IProgressMonitor monitor) {
    }

    @Override
    public void onDispose(BaseEditor edit, IProgressMonitor monitor) {
        edit.removeModelListener(this);
        edit.removePropertyListener(this);
        edit.removePyeditListener(this);
    }

    @Override
    public void onSetDocument(IDocument document, BaseEditor edit, IProgressMonitor monitor) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.python.pydev.editor.model.IModelListener#modelChanged(org.python.pydev.editor.model.AbstractNode)
     */
    public synchronized void modelChanged(final ISimpleNode ast) {
        final SimpleNode root2 = (SimpleNode) ast;
        if (!firstInputChangedCalled) {
            asyncUpdateWaitingFormModelAndInputChanged(root2);
            return;
        }

        ProjectionAnnotationModel model = (ProjectionAnnotationModel) editor
                .getAdapter(ProjectionAnnotationModel.class);

        if (model == null) {
            asyncUpdateWaitingFormModelAndInputChanged(root2);
        } else {
            addMarksToModel(root2, model);
        }

    }

    private void asyncUpdateWaitingFormModelAndInputChanged(final SimpleNode ast) {
        //we have to get the model to do it... so, start a thread and try until get it...
        //this had to be done because sometimes we get here and we still are unable to get the
        //projection annotation model. (there should be a better way, but this solves it...
        //even if it looks like a hack...)
        Thread t = new Thread() {
            @Override
            public void run() {
                ProjectionAnnotationModel modelT = null;
                for (int i = 0; i < 10 && modelT == null || !firstInputChangedCalled; i++) { //we will try it for 10 secs...
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        Log.log(e);
                    }
                    modelT = (ProjectionAnnotationModel) editor.getAdapter(ProjectionAnnotationModel.class);
                    if (modelT != null) {
                        addMarksToModel(ast, modelT);
                        break;
                    }
                }
            }
        };
        t.setPriority(Thread.MIN_PRIORITY);
        t.setName("CodeFolding - get annotation model");
        t.start();
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
                if (doc != null) { //this can happen if we change the input of the editor very quickly.
                    boolean foldInitial = initialFolding;
                    initialFolding = false;
                    List<FoldingEntry> marks = getMarks(doc, root2, foldInitial);
                    Map<ProjectionAnnotation, Position> annotationsToAdd;
                    if (marks.size() > OptimizationRelatedConstants.MAXIMUM_NUMBER_OF_CODE_FOLDING_MARKS) {
                        annotationsToAdd = new HashMap<ProjectionAnnotation, Position>();

                    } else {
                        annotationsToAdd = getAnnotationsToAdd(marks, model, existing);
                    }

                    model.replaceAnnotations(existing.toArray(new Annotation[existing.size()]), annotationsToAdd);
                }
            }
        } catch (Exception e) {
            Log.log(e);
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
    private Map<ProjectionAnnotation, Position> getAnnotationsToAdd(List<FoldingEntry> nodes,
            ProjectionAnnotationModel model, List<PyProjectionAnnotation> existing) {

        Map<ProjectionAnnotation, Position> annotationsToAdd = new HashMap<ProjectionAnnotation, Position>();
        try {
            for (FoldingEntry element : nodes) {
                if (element.startLine < element.endLine - 1) {
                    Tuple<ProjectionAnnotation, Position> tup = getAnnotationToAdd(element, element.startLine,
                            element.endLine, model, existing);
                    if (tup != null) {
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
                endOffset = lineInformation.getOffset() + lineInformation.getLength();
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
        return new Tuple<ProjectionAnnotation, Position>(new PyProjectionAnnotation(node.getAstEntry(),
                node.isCollapsed), position);
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
    public static List<FoldingEntry> getMarks(IDocument doc, SimpleNode ast, boolean foldInitial) {

        List<FoldingEntry> ret = new ArrayList<FoldingEntry>();

        CodeFoldingVisitor visitor = CodeFoldingVisitor.create(ast);
        //(re) insert annotations.
        IPreferenceStore prefs = getPreferences();

        if (prefs.getBoolean(PyDevCodeFoldingPrefPage.FOLD_IMPORTS)) {
            createFoldingEntries(ret, visitor,
                    foldInitial ? prefs.getBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_IMPORTS) : false,
                    Import.class,
                    ImportFrom.class);
        }
        if (prefs.getBoolean(PyDevCodeFoldingPrefPage.FOLD_CLASSDEF)) {
            createFoldingEntries(ret, visitor,
                    foldInitial ? prefs.getBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_CLASSDEF) : false,
                    ClassDef.class);
        }
        if (prefs.getBoolean(PyDevCodeFoldingPrefPage.FOLD_FUNCTIONDEF)) {
            createFoldingEntries(ret, visitor,
                    foldInitial ? prefs.getBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_FUNCTIONDEF) : false,
                    FunctionDef.class);
        }
        if (prefs.getBoolean(PyDevCodeFoldingPrefPage.FOLD_STRINGS)) {
            createFoldingEntries(ret, visitor,
                    foldInitial ? prefs.getBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_STRINGS) : false, Str.class);
        }
        if (prefs.getBoolean(PyDevCodeFoldingPrefPage.FOLD_WHILE)) {
            createFoldingEntries(ret, visitor,
                    foldInitial ? prefs.getBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_WHILE) : false, While.class);
        }
        if (prefs.getBoolean(PyDevCodeFoldingPrefPage.FOLD_IF)) {
            createFoldingEntries(ret, visitor,
                    foldInitial ? prefs.getBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_IF) : false, If.class);
        }
        if (prefs.getBoolean(PyDevCodeFoldingPrefPage.FOLD_FOR)) {
            createFoldingEntries(ret, visitor,
                    foldInitial ? prefs.getBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_FOR) : false, For.class);
        }
        if (prefs.getBoolean(PyDevCodeFoldingPrefPage.FOLD_WITH)) {
            createFoldingEntries(ret, visitor,
                    foldInitial ? prefs.getBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_WITH) : false, With.class);
        }
        if (prefs.getBoolean(PyDevCodeFoldingPrefPage.FOLD_TRY)) {
            createFoldingEntries(ret, visitor,
                    foldInitial ? prefs.getBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_TRY) : false,
                    TryExcept.class, TryFinally.class);
        }

        //and at last, get the comments
        if (prefs.getBoolean(PyDevCodeFoldingPrefPage.FOLD_COMMENTS)) {
            boolean collapseComments = foldInitial
                    ? prefs.getBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_COMMENTS) : false;
            DocIterator it = new DocIterator(true, new PySelection(doc, 0));
            while (it.hasNext()) {
                String string = it.next();
                if (string.trim().startsWith("#")) {
                    int l = it.getCurrentLine() - 1;
                    addFoldingEntry(ret, new FoldingEntry(FoldingEntry.TYPE_COMMENT, l, l + 1, new ASTEntry(null,
                            new commentType(string)), collapseComments));
                }
            }
        }

        Collections.sort(ret, new Comparator<FoldingEntry>() {

            public int compare(FoldingEntry o1, FoldingEntry o2) {
                if (o1.startLine < o2.startLine) {
                    return -1;
                }
                if (o1.startLine > o2.startLine) {
                    return 1;
                }
                return 0;
            }
        });

        return ret;
    }

    private static void createFoldingEntries(List<FoldingEntry> ret, CodeFoldingVisitor visitor, boolean collapse,
            Class... elementClasses) {
        List<ASTEntry> nodes = visitor.getAsList(elementClasses);
        for (ASTEntry entry : nodes) {
            createFoldingEntries((ASTEntryWithChildren) entry, ret, collapse);
        }
    }

    /**
     * @param entry the entry that should be added
     * @param ret the list where the folding entry generated should be added
     * @param memo a memo for the nodes that already generated a folding entry (needed
     * for treating if..elif because the elif will be generated when the if is found, and if it's
     * found again later we'll want to ignore it)
     */
    private static void createFoldingEntries(ASTEntryWithChildren entry, List<FoldingEntry> ret, boolean collapse) {
        FoldingEntry foldingEntry = null;
        if (entry.node instanceof Import || entry.node instanceof ImportFrom) {
            foldingEntry = new FoldingEntry(FoldingEntry.TYPE_IMPORT, entry.node.beginLine - 1, entry.endLine, entry);

        } else if (entry.node instanceof ClassDef) {
            ClassDef def = (ClassDef) entry.node;
            foldingEntry = new FoldingEntry(FoldingEntry.TYPE_DEF, def.name.beginLine - 1, entry.endLine, entry);

        } else if (entry.node instanceof FunctionDef) {
            FunctionDef def = (FunctionDef) entry.node;
            foldingEntry = new FoldingEntry(FoldingEntry.TYPE_DEF, def.name.beginLine - 1, entry.endLine, entry);

        } else if (entry.node instanceof TryExcept) {
            foldingEntry = new FoldingEntry(FoldingEntry.TYPE_EXCEPT, entry.node.beginLine - 1, entry.endLine, entry);

            //Removed: we shouldn't have to rely on getting the body 'end' line at this point... that info should already come
            //from the CodeFoldingVisitor (so, this code must be adapted to that... also, a revision on the coding standard
            //must be done)

            TryExcept tryStmt = (TryExcept) entry.node;
            if (tryStmt.handlers != null) {
                for (excepthandlerType except : tryStmt.handlers) {
                    foldingEntry = checkExcept(entry, ret, foldingEntry, entry.endLine, except);
                }
            }

            if (tryStmt.orelse != null) {
                foldingEntry = checkOrElse(entry, ret, foldingEntry, entry.endLine, tryStmt.orelse);
            }

        } else if (entry.node instanceof TryFinally) {
            //entry for the whole try..finally block

            TryFinally tryStmt = (TryFinally) entry.node;
            if (tryStmt.body != null && tryStmt.body.length > 0) {
                if (!(tryStmt.body[0] instanceof TryExcept) || (tryStmt.body[0].beginLine != tryStmt.beginLine)) {
                    //Ignore the try if it is part of a try except block in the format:
                    //try..except..finally (in the same block)
                    foldingEntry = new FoldingEntry(FoldingEntry.TYPE_FINALLY, entry.node.beginLine - 1, entry.endLine,
                            entry);
                }
            }
            if (tryStmt.finalbody != null) {
                if (foldingEntry != null) {
                    //ok, add the current and set the new current to the finally block
                    foldingEntry = checkFinally(entry, ret, foldingEntry, entry.endLine, tryStmt.finalbody, true);
                } else {
                    //the current one shouldn't be added... (just the finally part)
                    foldingEntry = new FoldingEntry(FoldingEntry.TYPE_FINALLY, entry.node.beginLine - 1, entry.endLine,
                            entry);
                    foldingEntry = checkFinally(entry, ret, foldingEntry, entry.endLine, tryStmt.finalbody, false);
                }
            }

        } else if (entry.node instanceof With) {
            foldingEntry = new FoldingEntry(FoldingEntry.TYPE_STATEMENT, entry.node.beginLine - 1, entry.endLine,
                    entry);

        } else if (entry.node instanceof While) {//XXX start test section
            foldingEntry = new FoldingEntry(FoldingEntry.TYPE_STATEMENT, entry.node.beginLine - 1, entry.endLine,
                    entry);
            foldingEntry = checkOrElse(entry, ret, foldingEntry, entry.endLine, ((While) entry.node).orelse);

        } else if (entry.node instanceof For) {
            foldingEntry = new FoldingEntry(FoldingEntry.TYPE_STATEMENT, entry.node.beginLine - 1, entry.endLine,
                    entry);
            foldingEntry = checkOrElse(entry, ret, foldingEntry, entry.endLine, ((For) entry.node).orelse);

        } else if (entry.node instanceof If) {//If comes 'ok' from the CodeFoldingVisitor (no need to check for the else part)
            foldingEntry = new FoldingEntry(FoldingEntry.TYPE_STATEMENT, entry.node.beginLine - 1, entry.endLine,
                    entry);

        } else if (entry.node instanceof Str) {
            if (entry.node.beginLine != entry.endLine) {
                foldingEntry = new FoldingEntry(FoldingEntry.TYPE_STR, entry.node.beginLine - 1, entry.endLine, entry);
            }
        }
        if (foldingEntry != null) {
            foldingEntry.isCollapsed = collapse;
            addFoldingEntry(ret, foldingEntry);
        }

    }

    /**
     * Checks an entry for its 'else' statement. If found, will add a folding entry for the previous block and 
     * return a new entry for the 'else' part (to the end of the previous block).
     * 
     * @param entry the entry that we're analyzing at this point
     * @param ret where the folding entry should be added
     * @param foldingEntry the folding entry that will be added with the contents o the full block (so, if it's a 
     * while...else, it contains the position up to the end of the else block.
     * @param blockEndLine the end line of the whole block (with the else part)
     * @param orelse the suite with the else part
     * @return the same folding entry passed or a new folding entry that should be added in the place of the one passed 
     * as a parameter
     */
    private static FoldingEntry checkOrElse(ASTEntryWithChildren entry, List<FoldingEntry> ret,
            FoldingEntry foldingEntry, int blockEndLine, suiteType orelse) {
        return checkOrElseSuite(entry, ret, foldingEntry, blockEndLine, orelse, FoldingEntry.TYPE_ELSE, "else", true);
    }

    private static FoldingEntry checkFinally(ASTEntryWithChildren entry, List<FoldingEntry> ret,
            FoldingEntry foldingEntry, int blockEndLine, suiteType orelse, boolean addPrevious) {
        return checkOrElseSuite(entry, ret, foldingEntry, blockEndLine, orelse, FoldingEntry.TYPE_FINALLY, "finally",
                addPrevious);
    }

    private static FoldingEntry checkExcept(ASTEntryWithChildren entry, List<FoldingEntry> ret,
            FoldingEntry foldingEntry, int blockEndLine, excepthandlerType orelse) {
        return checkOrElseSuite(entry, ret, foldingEntry, blockEndLine, orelse, FoldingEntry.TYPE_EXCEPT, "except",
                true);
    }

    private static FoldingEntry checkOrElseSuite(ASTEntryWithChildren entry, List<FoldingEntry> ret,
            FoldingEntry foldingEntry, int blockEndLine, SimpleNode orelse, int type, String specialToken,
            boolean addPrevious) {
        if (orelse != null) {
            if (orelse.specialsBefore != null) {
                for (Object o : orelse.specialsBefore) {
                    if (o instanceof ISpecialStr) {
                        ISpecialStr specialStr = (ISpecialStr) o;
                        if (specialStr.toString().equals(specialToken)) {
                            foldingEntry.endLine = specialStr.getBeginLine() - 1;
                            if (addPrevious) {
                                addFoldingEntry(ret, foldingEntry);
                            }
                            foldingEntry = new FoldingEntry(type, specialStr.getBeginLine() - 1, blockEndLine, entry);
                        }
                    }
                }
            }
        }
        return foldingEntry;
    }

    public static IPreferenceStore getPreferences() {
        if (testingPrefs == null) {
            return PydevPrefs.getPreferences();
        } else {
            return testingPrefs;
        }
    }

    private static IPreferenceStore testingPrefs;

    /**
     * Used for tests
     * @return
     */
    public static void setPreferences(IPreferenceStore prefs) {
        CodeFoldingSetter.testingPrefs = prefs;
    }

    private static void addFoldingEntry(List<FoldingEntry> ret, FoldingEntry foldingEntry) {
        //we only group comments and imports
        if (ret.size() > 0
                && (foldingEntry.type == FoldingEntry.TYPE_COMMENT || foldingEntry.type == FoldingEntry.TYPE_IMPORT)) {
            FoldingEntry prev = ret.get(ret.size() - 1);
            if (prev.type == foldingEntry.type && prev.startLine < foldingEntry.startLine
                    && prev.endLine == foldingEntry.startLine) {
                prev.endLine = foldingEntry.endLine;
            } else {
                ret.add(foldingEntry);
            }
        } else {
            ret.add(foldingEntry);
        }
    }

    public void errorChanged(ErrorDescription errorDesc) {
        //ignore the errors (we're just interested in the ast in this class)
    }

}
