/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.changed_lines;

import java.util.ArrayList;

import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.log.Log;

/**
 * Based on org.eclipse.jdt.internal.ui.javaeditor.EditorUtility.calculateChangedLineRegions
 */
public class ChangedLinesComputer {

    /**
     * Return the lines which have changed in the given buffer since the last save occurred. 
     * 
     * @param buffer the buffer to compare contents from
     * @param monitor to report progress to
     * @return the regions of the changed lines or null if something went wrong.
     */
    public static int[] calculateChangedLines(final ITextFileBuffer buffer, final IProgressMonitor monitor)
            throws CoreException {
        int[] result = null;

        try {
            monitor.beginTask("Calculating changed lines", 20);
            IFileStore fileStore = buffer.getFileStore();

            ITextFileBufferManager fileBufferManager = FileBuffers.createTextFileBufferManager();
            fileBufferManager.connectFileStore(fileStore, getSubProgressMonitor(monitor, 15));
            try {
                IDocument currentDocument = buffer.getDocument();
                IDocument oldDocument = ((ITextFileBuffer) fileBufferManager.getFileStoreFileBuffer(fileStore))
                        .getDocument();

                result = getChangedLines(oldDocument, currentDocument);
            } finally {
                fileBufferManager.disconnectFileStore(fileStore, getSubProgressMonitor(monitor, 5));
                monitor.done();
            }
        } catch (Exception e) {
            Log.log(e);
            return null;
        }

        return result;
    }

    /**
     * Return all the changed lines.
     * 
     * @param oldDocument a document containing the old content
     * @param currentDocument a document containing the current content
     * @return the changed regions
     * @throws BadLocationException if fetching the line information fails
     */
    public static int[] getChangedLines(IDocument oldDocument, IDocument currentDocument) throws BadLocationException {
        /*
         * Do not change the type of those local variables. We use Object
         * here in order to prevent loading of the Compare plug-in at load
         * time of this class.
         */
        Object leftSide = new LineComparator(oldDocument);
        Object rightSide = new LineComparator(currentDocument);

        RangeDifference[] differences = RangeDifferencer.findDifferences((IRangeComparator) leftSide,
                (IRangeComparator) rightSide);

        //It holds that:
        //1. Ranges are sorted:
        //     forAll r1,r2 element differences: indexOf(r1)<indexOf(r2) -> r1.rightStart()<r2.rightStart();
        //2. Successive changed lines are merged into on RangeDifference
        //     forAll r1,r2 element differences: r1.rightStart()<r2.rightStart() -> r1.rightEnd()<r2.rightStart

        ArrayList<Integer> regions = new ArrayList<Integer>();
        for (int i = 0; i < differences.length; i++) {
            RangeDifference curr = differences[i];
            if (curr.kind() == RangeDifference.CHANGE && curr.rightLength() > 0) {
                int startLine = curr.rightStart();
                int endLine = curr.rightEnd() - 1;

                if (startLine == endLine) {
                    regions.add(startLine);
                } else {
                    for (int iLine = startLine; iLine <= endLine; iLine++) {
                        regions.add(iLine);
                    }
                }
            }
        }

        int size = regions.size();
        int[] ret = new int[size];
        for (int i = 0; i < size; i++) {
            ret[i] = regions.get(i);
        }
        return ret;
    }

    /**
     * Creates and returns a new sub-progress monitor for the
     * given parent monitor.
     *
     * @param monitor the parent progress monitor
     * @param ticks the number of work ticks allocated from the parent monitor
     * @return the new sub-progress monitor
     * @since 3.4
     */
    private static IProgressMonitor getSubProgressMonitor(IProgressMonitor monitor, int ticks) {
        if (monitor != null) {
            return new SubProgressMonitor(monitor, ticks, SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
        }

        return new NullProgressMonitor();
    }

}
