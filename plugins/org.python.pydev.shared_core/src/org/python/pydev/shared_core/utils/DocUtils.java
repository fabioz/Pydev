/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ISynchronizable;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.string.StringUtils;

public class DocUtils {

    public static Object runWithDocumentSynched(IDocument document, ICallback<Object, IDocument> iCallback) {
        Object lockObject = null;
        if (document instanceof ISynchronizable) {
            ISynchronizable sync = (ISynchronizable) document;
            lockObject = sync.getLockObject();
        }
        if (lockObject != null) {
            synchronized (lockObject) {
                return iCallback.call(document);
            }
        } else { //unsynched
            return iCallback.call(document);
        }

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static String[] getAllDocumentContentTypes(IDocument document) throws BadPartitioningException {
        if (document instanceof IDocumentExtension3) {
            IDocumentExtension3 ext = (IDocumentExtension3) document;
            String[] partitionings = ext.getPartitionings();

            Set contentTypes = new HashSet();
            contentTypes.add(IDocument.DEFAULT_CONTENT_TYPE);

            int len = partitionings.length;
            for (int i = 0; i < len; i++) {
                String[] legalContentTypes = ext.getLegalContentTypes(partitionings[i]);
                int len2 = legalContentTypes.length;
                for (int j = 0; j < len2; j++) {
                    contentTypes.add(legalContentTypes[j]);
                }
                contentTypes.addAll(Arrays.asList(legalContentTypes));
            }
            return (String[]) contentTypes.toArray(new String[contentTypes.size()]);
        }
        return document.getLegalContentTypes();
    }

    public static void updateDocRangeWithContents(final IDocument doc, final String docContents,
            final String newDocContents,
            final String endLineDelimiter) {
        try {
            Assert.isTrue(doc.getLength() == docContents.length());
            List<String> prevSplitInLines = StringUtils.splitInLines(docContents, false);
            List<String> iSortSplitInLines = StringUtils.splitInLines(newDocContents, false);

            int prevFirstIndex = 0;
            int iSortFirstIndex = 0;
            while (true) {
                if (prevFirstIndex >= prevSplitInLines.size()) {
                    // All lines matched... we may still have added lines if iSortSplitInLines.size() > prevSplitInLines.size().
                    if (iSortSplitInLines.size() > prevSplitInLines.size()) {
                        // The new lines
                        String addedContents = StringUtils.join(endLineDelimiter,
                                iSortSplitInLines.subList(prevSplitInLines.size(), iSortSplitInLines.size()));
                        // If it did have a new line delimiters in the end, add it too.
                        if (newDocContents.endsWith("\r") || newDocContents.endsWith("\n")) {
                            addedContents += endLineDelimiter;
                        }

                        doc.replace(doc.getLength(), 0, addedContents);
                    }
                    return;
                }
                if (iSortFirstIndex >= iSortSplitInLines.size()) {
                    // All lines matched... we may still have removed lines if iSortSplitInLines.size() < prevSplitInLines.size().
                    if (iSortSplitInLines.size() < prevSplitInLines.size()) {
                        if (prevFirstIndex > 0) {
                            int lineOffset = doc.getLineOffset(prevFirstIndex - 1);
                            int lineLength = doc.getLineLength(prevFirstIndex - 1);

                            doc.replace(lineOffset + lineLength, doc.getLength() - (lineOffset + lineLength), "");
                        } else {
                            doc.set(""); // Clear doc
                        }
                    }
                    return;
                }
                if (prevSplitInLines.get(prevFirstIndex)
                        .equals(iSortSplitInLines.get(iSortFirstIndex))) {
                    prevFirstIndex++;
                    iSortFirstIndex++;
                } else {
                    // Ok, reached first line they differ going forwards... go backwards to determine the range.

                    // Backwards: find first non-empty lines to start compare
                    int iSortLastIndex = iSortSplitInLines.size() - 1;
                    {
                        for (; iSortLastIndex > 0; iSortLastIndex--) {
                            if (iSortSplitInLines.get(iSortLastIndex).trim().equals("")) {
                                continue;
                            }
                            break;
                        }
                    }
                    int prevLastIndex = prevSplitInLines.size() - 1;
                    {
                        for (; prevLastIndex > 0; prevLastIndex--) {
                            if (prevSplitInLines.get(prevLastIndex).trim().equals("")) {
                                continue;
                            }
                            break;
                        }
                    }
                    while (true) {
                        if (prevLastIndex < 0) {
                            if (iSortLastIndex >= 0) {
                                doc.replace(0, 0, StringUtils.join(endLineDelimiter,
                                        iSortSplitInLines.subList(iSortFirstIndex, iSortLastIndex + 1))
                                        + endLineDelimiter);
                            }
                            return;
                        }
                        if (iSortLastIndex < 0) {
                            // This means that the first document has lines which aren't in the final document, so,
                            // go on and remove the lines we still didn't reach.
                            if (prevLastIndex >= 0) {
                                doc.replace(0, doc.getLineOffset(prevLastIndex + 1), "");
                            }
                            return;
                        }

                        if (prevSplitInLines.get(prevLastIndex).equals(iSortSplitInLines.get(iSortLastIndex))) {
                            prevLastIndex--;
                            iSortLastIndex--;
                        } else {
                            if (prevFirstIndex == 0 && prevLastIndex == prevSplitInLines.size() - 1) {
                                // Everything changed
                                doc.set(newDocContents);
                                return;
                            }
                            if (prevLastIndex < prevFirstIndex) {
                                // Reached the start, so, just insert the needed gap
                                IRegion lineInformation = doc.getLineInformation(prevFirstIndex);

                                int offset = lineInformation.getOffset();
                                try {
                                    doc.replace(offset, 0, StringUtils.join(endLineDelimiter,
                                            iSortSplitInLines.subList(iSortFirstIndex,
                                                    iSortLastIndex + (prevFirstIndex - prevLastIndex)))
                                            + endLineDelimiter);
                                } catch (BadLocationException e) {
                                    throw new BadLocationException(
                                            StringUtils.format(
                                                    "Error trying to access offset: %s, len: %s max doc len: %s",
                                                    offset, 0, doc.getLength()));
                                }
                                return;
                            }
                            // First line they differ going backwards
                            int offset = -10000;
                            int len = -10000;
                            try {
                                String replaceText;
                                if (iSortLastIndex < iSortFirstIndex) {
                                    IRegion lineInformationStart = doc.getLineInformation(prevFirstIndex);
                                    IRegion lineInformationEnd = doc.getLineInformation(prevLastIndex);

                                    offset = lineInformationStart.getOffset();
                                    len = (lineInformationEnd.getOffset() + doc.getLineLength(prevLastIndex)) - offset; // Get with end line delimiter
                                    replaceText = "";
                                } else {
                                    IRegion lineInformationStart = doc.getLineInformation(prevFirstIndex);
                                    IRegion lineInformationEnd = doc.getLineInformation(prevLastIndex);

                                    offset = lineInformationStart.getOffset();
                                    len = (lineInformationEnd.getOffset() + lineInformationEnd.getLength()) - offset;
                                    replaceText = StringUtils.join(endLineDelimiter,
                                            iSortSplitInLines.subList(iSortFirstIndex, iSortLastIndex + 1));
                                }
                                doc.replace(offset, len, replaceText);
                            } catch (BadLocationException e) {
                                throw new BadLocationException(
                                        StringUtils.format("Error trying to access offset: %s, len: %s max doc len: %s",
                                                offset, len, doc.getLength()));
                            }
                            return;
                        }
                    }
                }
            }

        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }
}
