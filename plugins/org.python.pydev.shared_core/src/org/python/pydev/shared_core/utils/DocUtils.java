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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.ISynchronizable;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.utils.diff_match_patch.Patch;

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

        updateDocRangeWithContents(new IDocumentUpdateAPI() {

            @Override
            public void set(String string) {
                doc.set(string);
            }

            @Override
            public void replace(int offset, int length, String text) throws BadLocationException {
                doc.replace(offset, length, text);

            }
        }, doc, docContents, newDocContents, endLineDelimiter);
    }

    public static interface IDocumentUpdateAPI {

        void replace(int offset, int length, String text) throws BadLocationException;

        void set(String text) throws BadLocationException;
    }

    /**
     * @param docUpdateAPI any document mutation is done through this parameter (so, it's possible to record any changes done).
     * @throws BadLocationException
     */
    public static void updateDocRangeWithContents(final IDocumentUpdateAPI docUpdateAPI, final IDocument docToUpdate,
            final String docContents, final String newDocContents, final String endLineDelimiter) {
        diff_match_patch diff_match_patch = new diff_match_patch();
        diff_match_patch.Diff_Timeout = 0.5f;
        diff_match_patch.Match_Distance = 200;
        diff_match_patch.Patch_Margin = 10;
        diff_match_patch.Diff_EditCost = 8;
        LinkedList<Patch> patches = diff_match_patch.patch_make(docContents, newDocContents);
        try {
            diff_match_patch.patch_apply(patches, docContents, docUpdateAPI);
        } catch (BadLocationException e) {
            Log.log(e);
        }
    }

    public static class EmptyLinesComputer {

        private final IDocument doc;
        private Map<Integer, Boolean> lineToIsEmpty = new HashMap<>();
        private final int numberOfLines;

        public EmptyLinesComputer(IDocument doc) {
            this.doc = doc;
            numberOfLines = this.doc.getNumberOfLines();
        }

        public boolean isLineEmpty(int line) {
            Boolean b = this.lineToIsEmpty.get(line);
            if (b == null) {
                String lineContents = TextSelectionUtils.getLine(doc, line);
                if (lineContents.trim().isEmpty()) {
                    this.lineToIsEmpty.put(line, true);
                    b = true;
                } else {
                    this.lineToIsEmpty.put(line, false);
                    b = false;
                }
            }
            return b;
        }

        /**
         * Note: will add the current line if it's not empty and will add
         * surrounding empty lines (even if the passed line is not empty).
         */
        public void addToSetEmptyLinesCloseToLine(Set<Integer> hashSet, int line) {
            if (line < 0) {
                return;
            }
            if (line >= numberOfLines) {
                return;
            }
            if (isLineEmpty(line)) {
                hashSet.add(line);
            }
            for (int i = line + 1; i < numberOfLines; i++) {
                if (isLineEmpty(i)) {
                    hashSet.add(i);
                } else {
                    break;
                }
            }
            for (int i = line - 1; i >= 0 && i < numberOfLines; i--) {
                if (isLineEmpty(i)) {
                    hashSet.add(i);
                } else {
                    break;
                }
            }
        }

    }
}
