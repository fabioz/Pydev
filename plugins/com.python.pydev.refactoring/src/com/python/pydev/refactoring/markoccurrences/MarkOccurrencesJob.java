/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 29, 2006
 */
package com.python.pydev.refactoring.markoccurrences;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.mark_occurrences.BaseMarkOccurrencesJob;

import com.python.pydev.refactoring.ui.MarkOccurrencesPreferencesPage;
import com.python.pydev.refactoring.wizards.rename.PyReferenceSearcher;

/**
 * This is a 'low-priority' thread. It acts as a singleton. Requests to mark the occurrences
 * will be forwarded to it, so, it should sleep for a while and then check for a request.
 *
 * If the request actually happened, it will go on to process it, otherwise it will sleep some more.
 *
 * @author Fabio
 */
public class MarkOccurrencesJob extends BaseMarkOccurrencesJob {

    private final static class TextBasedLocalMarkOccurrencesRequest extends MarkOccurrencesRequest {

        private String currToken;

        public TextBasedLocalMarkOccurrencesRequest(String currToken) {
            super(true);
            this.currToken = currToken;
        }
    }

    private final static class PyMarkOccurrencesRequest extends MarkOccurrencesRequest {
        private final RefactoringRequest refactoringRequest;
        private final PyReferenceSearcher pyReferenceSearcher;

        public PyMarkOccurrencesRequest(boolean proceedWithMarkOccurrences,
                RefactoringRequest refactoringRequest,
                PyReferenceSearcher pyReferenceSearcher) {
            super(proceedWithMarkOccurrences);
            this.refactoringRequest = refactoringRequest;
            this.pyReferenceSearcher = pyReferenceSearcher;
        }

        public Set<ASTEntry> getOccurrences() {
            return pyReferenceSearcher.getLocalReferences(refactoringRequest);
        }

        public String getInitialName() {
            return refactoringRequest.initialName;
        }

    }

    public MarkOccurrencesJob(WeakReference<BaseEditor> editor, TextSelectionUtils ps) {
        super(editor, ps);
    }

    private static final Set<String> LOCAL_TEXT_SEARCHES_ON = new HashSet<String>();

    static {
        LOCAL_TEXT_SEARCHES_ON.add("assert");
        LOCAL_TEXT_SEARCHES_ON.add("break");
        LOCAL_TEXT_SEARCHES_ON.add("continue");
        LOCAL_TEXT_SEARCHES_ON.add("del");
        LOCAL_TEXT_SEARCHES_ON.add("lambda");
        LOCAL_TEXT_SEARCHES_ON.add("nonlocal");
        LOCAL_TEXT_SEARCHES_ON.add("global");
        LOCAL_TEXT_SEARCHES_ON.add("pass");
        LOCAL_TEXT_SEARCHES_ON.add("print");
        LOCAL_TEXT_SEARCHES_ON.add("raise");
        LOCAL_TEXT_SEARCHES_ON.add("return");
    }

    /**
     * @return a tuple with the refactoring request, the processor and a boolean indicating if all pre-conditions succedded.
     * @throws MisconfigurationException
     */
    @Override
    protected MarkOccurrencesRequest createRequest(BaseEditor baseEditor,
            IDocumentProvider documentProvider, IProgressMonitor monitor) throws BadLocationException,
                    OperationCanceledException, CoreException, MisconfigurationException {
        if (!MarkOccurrencesPreferencesPage.useMarkOccurrences()) {
            return new PyMarkOccurrencesRequest(false, null, null);
        }
        PyEdit pyEdit = (PyEdit) baseEditor;

        //ok, the editor is still there wit ha document... move on
        PyRefactorAction pyRefactorAction = getRefactorAction(pyEdit);
        String currToken = this.ps.getCurrToken().o1;
        if (LOCAL_TEXT_SEARCHES_ON.contains(currToken) && IDocument.DEFAULT_CONTENT_TYPE
                .equals(ParsingUtils.getContentType(this.ps.getDoc(), this.ps.getAbsoluteCursorOffset()))) {
            return new TextBasedLocalMarkOccurrencesRequest(currToken);
        }

        final RefactoringRequest req = getRefactoringRequest(pyEdit, pyRefactorAction,
                PySelection.fromTextSelection(this.ps));

        if (req == null || !req.nature.getRelatedInterpreterManager().isConfigured()) { //we check if it's configured because it may still be a stub...
            return new PyMarkOccurrencesRequest(false, null, null);
        }

        PyReferenceSearcher searcher = new PyReferenceSearcher(req);
        //to see if a new request was not created in the meantime (in which case this one will be cancelled)
        if (monitor.isCanceled()) {
            return new PyMarkOccurrencesRequest(false, null, null);
        }

        try {
            searcher.prepareSearch(req);
            if (monitor.isCanceled()) {
                return new PyMarkOccurrencesRequest(false, null, null);
            }
            searcher.search(req);
            if (monitor.isCanceled()) {
                return new PyMarkOccurrencesRequest(false, null, null);
            }
            // Ok, search succeeded.
            return new PyMarkOccurrencesRequest(true, req, searcher);
        } catch (PyReferenceSearcher.SearchException | BadLocationException e) {
            // Suppress search failures.
            return new PyMarkOccurrencesRequest(false, null, null);
        } catch (Throwable e) {
            throw new RuntimeException("Error in occurrences while analyzing modName:" + req.moduleName
                    + " initialName:" + req.initialName + " line (start at 0):" + req.ps.getCursorLine(), e);
        }
    }

    /**
     * @param markOccurrencesRequest
     * @return true if the annotations were removed and added without any problems and false otherwise
     */
    @Override
    protected synchronized Map<Annotation, Position> getAnnotationsToAddAsMap(final BaseEditor baseEditor,
            IAnnotationModel annotationModel, MarkOccurrencesRequest markOccurrencesRequest, IProgressMonitor monitor)
                    throws BadLocationException {
        PyEdit pyEdit = (PyEdit) baseEditor;
        PySourceViewer viewer = pyEdit.getPySourceViewer();
        if (viewer == null || monitor.isCanceled()) {
            return null;
        }
        if (viewer.getIsInToggleCompletionStyle() || monitor.isCanceled()) {
            return null;
        }

        if (markOccurrencesRequest instanceof TextBasedLocalMarkOccurrencesRequest) {
            TextBasedLocalMarkOccurrencesRequest textualMarkOccurrencesRequest = (TextBasedLocalMarkOccurrencesRequest) markOccurrencesRequest;
            PySelection pySelection = PySelection.fromTextSelection(ps);
            Tuple<Integer, Integer> startEndLines = pySelection.getCurrentMethodStartEndLines();

            int initialOffset = pySelection.getAbsoluteCursorOffset(startEndLines.o1, 0);
            int finalOffset = pySelection.getEndLineOffset(startEndLines.o2);

            List<IRegion> occurrences = ps.searchOccurrences(textualMarkOccurrencesRequest.currToken);
            if (occurrences.size() == 0) {
                return null;
            }
            Map<Annotation, Position> toAddAsMap = new HashMap<Annotation, Position>();
            for (Iterator<IRegion> it = occurrences.iterator(); it.hasNext();) {
                IRegion iRegion = it.next();
                if (iRegion.getOffset() < initialOffset || iRegion.getOffset() > finalOffset) {
                    continue;
                }

                try {
                    Annotation annotation = new Annotation(getOccurrenceAnnotationsType(), false, "occurrence");
                    Position position = new Position(iRegion.getOffset(), iRegion.getLength());
                    toAddAsMap.put(annotation, position);

                } catch (Exception e) {
                    Log.log(e);
                }
            }
            return toAddAsMap;
        }

        PyMarkOccurrencesRequest pyMarkOccurrencesRequest = (PyMarkOccurrencesRequest) markOccurrencesRequest;
        Set<ASTEntry> occurrences = pyMarkOccurrencesRequest.getOccurrences();
        if (occurrences == null) {
            if (DEBUG) {
                System.out.println("Occurrences == null");
            }
            return null;
        }

        IDocument doc = pyEdit.getDocument();
        Map<Annotation, Position> toAddAsMap = new HashMap<Annotation, Position>();
        boolean markOccurrencesInStrings = MarkOccurrencesPreferencesPage.useMarkOccurrencesInStrings();

        //get the annotations to add
        for (ASTEntry entry : occurrences) {
            if (!markOccurrencesInStrings) {
                if (entry.node instanceof Name) {
                    Name name = (Name) entry.node;
                    if (name.ctx == Name.Artificial) {
                        continue;
                    }
                }
            }

            SimpleNode node = entry.getNameNode();
            IRegion lineInformation = doc.getLineInformation(node.beginLine - 1);

            try {
                Annotation annotation = new Annotation(getOccurrenceAnnotationsType(), false, "occurrence");
                Position position = new Position(lineInformation.getOffset() + node.beginColumn - 1,
                        pyMarkOccurrencesRequest.getInitialName().length());
                toAddAsMap.put(annotation, position);

            } catch (Exception e) {
                Log.log(e);
            }
        }
        return toAddAsMap;
    }

    /**
     * @param pyEdit the editor where we should look for the occurrences
     * @param pyRefactorAction the action that will return the initial refactoring request
     * @param ps the pyselection used (if null it will be created in this method)
     * @return a refactoring request suitable for finding the locals in the file
     * @throws BadLocationException
     * @throws MisconfigurationException
     */
    public static RefactoringRequest getRefactoringRequest(final PyEdit pyEdit, PyRefactorAction pyRefactorAction,
            PySelection ps) throws BadLocationException, MisconfigurationException {
        final RefactoringRequest req = pyRefactorAction.getRefactoringRequest();
        req.ps = ps;
        req.fillInitialNameAndOffset();
        req.inputName = "foo";
        req.setAdditionalInfo(RefactoringRequest.FIND_DEFINITION_IN_ADDITIONAL_INFO, false);
        req.setAdditionalInfo(RefactoringRequest.FIND_REFERENCES_ONLY_IN_LOCAL_SCOPE, true);
        return req;
    }

    /**
     * @param pyEdit the editor that will have this action
     * @return the action (with the pyedit attached to it)
     */
    public static PyRefactorAction getRefactorAction(PyEdit pyEdit) {
        PyRefactorAction pyRefactorAction = new PyRefactorAction() {

            @Override
            protected String perform(IAction action, IProgressMonitor monitor) throws Exception {
                throw new RuntimeException("Perform should not be called in this case.");
            }
        };
        pyRefactorAction.setEditor(pyEdit);
        return pyRefactorAction;
    }

    private static final String ANNOTATIONS_CACHE_KEY = "MarkOccurrencesJob Annotations";
    private static final String OCCURRENCE_ANNOTATION_TYPE = "com.python.pydev.occurrences";

    @Override
    protected String getOccurrenceAnnotationsCacheKey() {
        return ANNOTATIONS_CACHE_KEY;
    }

    @Override
    protected String getOccurrenceAnnotationsType() {
        return OCCURRENCE_ANNOTATION_TYPE;
    }

    /**
     * This is the function that should be called when we want to schedule a request for
     * a mark occurrences job.
     */
    public static synchronized void scheduleRequest(WeakReference<BaseEditor> editor2, TextSelectionUtils ps) {
        BaseMarkOccurrencesJob.scheduleRequest(new MarkOccurrencesJob(editor2, ps));
    }

    public static synchronized void scheduleRequest(WeakReference<BaseEditor> editor2, TextSelectionUtils ps,
            int time) {
        BaseMarkOccurrencesJob.scheduleRequest(new MarkOccurrencesJob(editor2, ps), time);
    }

}
