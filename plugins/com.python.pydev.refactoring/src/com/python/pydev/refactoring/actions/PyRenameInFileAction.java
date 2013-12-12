/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 21, 2006
 */
package com.python.pydev.refactoring.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.parsing.IParserObserver;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.refactoring.markoccurrences.MarkOccurrencesJob;
import com.python.pydev.refactoring.wizards.rename.PyRenameEntryPoint;

/**
 * This action should mark to rename all the occurrences found for some name in the file
 */
public class PyRenameInFileAction extends Action {

    /**
     * This class makes the rename when the reparse we asked for is triggered.
     */
    private class RenameInFileParserObserver implements IParserObserver {

        /**
         * As soon as the reparse is done, this method is called to actually make the rename.
         */
        public void parserChanged(ISimpleNode root, IAdaptable file, IDocument doc, long docModificationStamp) {
            pyEdit.getParser().removeParseListener(this); //we'll only listen for this single parse

            /**
             * Create an ui job to actually make the rename (otherwise we can't make ui.enter() nor create a PySelection.)
             */
            UIJob job = new UIJob("Rename") {

                @Override
                public IStatus runInUIThread(IProgressMonitor monitor) {
                    try {
                        ISourceViewer viewer = pyEdit.getPySourceViewer();
                        IDocument document = viewer.getDocument();
                        PySelection ps = new PySelection(pyEdit);
                        LinkedPositionGroup group = new LinkedPositionGroup();

                        if (!fillWithOccurrences(document, group, new NullProgressMonitor(), ps)) {
                            return Status.OK_STATUS;
                        }

                        if (group.isEmpty()) {
                            return Status.OK_STATUS;
                        }

                        LinkedModeModel model = new LinkedModeModel();
                        model.addGroup(group);
                        model.forceInstall();
                        if (model.getTabStopSequence().size() > 0) {
                            final LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
                            Tuple<String, Integer> currToken = ps.getCurrToken();
                            ui.setCyclingMode(LinkedModeUI.CYCLE_ALWAYS);
                            ui.setExitPosition(viewer, currToken.o2 + currToken.o1.length(), 0, 0 /*ordered so that 0 is current pos*/);
                            ui.enter();
                        }
                    } catch (BadLocationException e) {
                        Log.log(e);
                    } catch (Throwable e) {
                        Log.log(e);
                    }
                    return Status.OK_STATUS;
                }
            };
            job.setPriority(Job.INTERACTIVE);
            job.schedule();
        }

        public void parserError(Throwable error, IAdaptable file, IDocument doc) {
            pyEdit.getParser().removeParseListener(this); //we'll only listen for this single parse
        }
    }

    /**
     * This class adds an observer and triggers a reparse that this listener should listen to. 
     */
    private class RenameInFileJob extends Job {

        private RenameInFileJob(String name) {
            super(name);
        }

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            IParserObserver observer = new RenameInFileParserObserver();
            PyParser parser = pyEdit.getParser();
            parser.addParseListener(observer); //it will analyze when the next parse is finished
            parser.forceReparse();
            return Status.OK_STATUS;
        }
    }

    private PyEdit pyEdit;

    public PyRenameInFileAction(PyEdit edit) {
        this.pyEdit = edit;
    }

    @Override
    public void run() {
        Job j = new RenameInFileJob("Rename In File");
        j.setPriority(Job.INTERACTIVE);
        j.schedule();
    }

    /**
     * Puts the found positions referente to the occurrences in the group
     * 
     * @param document the document that will contain this positions 
     * @param group the group that will contain this positions
     * @param ps the selection used
     * @return 
     * 
     * @throws BadLocationException
     * @throws OperationCanceledException
     * @throws CoreException
     * @throws MisconfigurationException 
     */
    private boolean fillWithOccurrences(IDocument document, LinkedPositionGroup group, IProgressMonitor monitor,
            PySelection ps) throws BadLocationException, OperationCanceledException, CoreException,
            MisconfigurationException {

        RefactoringRequest req = MarkOccurrencesJob.getRefactoringRequest(pyEdit,
                MarkOccurrencesJob.getRefactorAction(pyEdit), ps);
        if (monitor.isCanceled()) {
            return false;
        }

        PyRenameEntryPoint processor = new PyRenameEntryPoint(req);

        //process it to get what we need
        processor.checkInitialConditions(monitor);
        processor.checkFinalConditions(monitor, null);
        HashSet<ASTEntry> occurrences = processor.getOccurrences();

        if (monitor.isCanceled()) {
            return false;
        }

        //used so that we don't add duplicates
        Set<Tuple<Integer, Integer>> found = new HashSet<Tuple<Integer, Integer>>();
        List<ProposalPosition> groupPositions = new ArrayList<ProposalPosition>();

        if (occurrences != null) {

            //first, just sort by position (line, col)
            ArrayList<ASTEntry> sortedOccurrences = new ArrayList<ASTEntry>(occurrences);
            Collections.sort(sortedOccurrences, new Comparator<ASTEntry>() {

                public int compare(ASTEntry o1, ASTEntry o2) {
                    int thisVal = o1.node.beginLine;
                    int anotherVal = o2.node.beginLine;
                    int ret;
                    if (thisVal == anotherVal) { //if it's in the same line, let's sort by column
                        thisVal = o1.node.beginColumn;
                        anotherVal = o2.node.beginColumn;
                        ret = (thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1));
                    } else {
                        ret = (thisVal < anotherVal ? -1 : 1);
                    }
                    return ret;

                }
            });

            //now, gather positions to add to the group

            int i = 0;
            int firstPosition = -1;
            int absoluteCursorOffset = ps.getAbsoluteCursorOffset();

            for (ASTEntry entry : sortedOccurrences) {
                try {
                    IRegion lineInformation = document.getLineInformation(entry.node.beginLine - 1);
                    int colDef = NodeUtils.getClassOrFuncColDefinition(entry.node) - 1;

                    int offset = lineInformation.getOffset() + colDef;
                    int len = req.initialName.length();
                    Tuple<Integer, Integer> foundAt = new Tuple<Integer, Integer>(offset, len);

                    if (!found.contains(foundAt)) {
                        i++;
                        ProposalPosition proposalPosition = new ProposalPosition(document, offset, len, i,
                                new ICompletionProposal[0]);
                        found.add(foundAt);
                        groupPositions.add(proposalPosition);
                        if (offset <= absoluteCursorOffset && absoluteCursorOffset < offset + len) {
                            firstPosition = i;
                        }
                    }
                } catch (Exception e) {
                    Log.log(e);
                    return false;
                }
            }

            if (firstPosition != -1) {
                ArrayList<ProposalPosition> newGroupPositions = new ArrayList<ProposalPosition>();

                //add from current to end
                for (i = firstPosition - 1; i < groupPositions.size(); i++) {
                    newGroupPositions.add(groupPositions.get(i));
                }
                //and now from the start up to the current
                for (i = 0; i < firstPosition - 1; i++) {
                    newGroupPositions.add(groupPositions.get(i));
                }

                groupPositions = newGroupPositions;
            }

            for (ProposalPosition proposalPosition : groupPositions) {
                group.addPosition(proposalPosition);
            }
        }
        return groupPositions.size() > 0;
    }

}
