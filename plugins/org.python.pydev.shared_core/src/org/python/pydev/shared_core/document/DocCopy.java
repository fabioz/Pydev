/******************************************************************************
* Copyright (C) 2015 Brainwy Software Ltda
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.document;

import java.util.HashMap;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IDocumentPartitioningListener;
import org.eclipse.jface.text.IDocumentRewriteSessionListener;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.python.pydev.shared_core.log.Log;

/**
 * Partial implementation of a document to be used as a throw-away copy
 * (things which change the document should not be implemented).
 */
public class DocCopy implements IDocument, IDocumentExtension4 {

    private String contents;
    private IDocument document;
    private HashMap<String, Position[]> categoryToPos;
    private long modificationStamp;
    private ILineTracker fLineTracker;

    public DocCopy(IDocument document) {
        this.contents = document.get();
        this.document = document;
        categoryToPos = new HashMap<>();
        String[] positionCategories = document.getPositionCategories();
        for (String string : positionCategories) {
            try {
                categoryToPos.put(string, document.getPositions(string));
            } catch (BadPositionCategoryException e) {
                Log.log(e);
            }
        }

        IDocumentExtension4 doc4 = (IDocumentExtension4) document;
        modificationStamp = doc4.getModificationStamp();
    }

    private ILineTracker getLineTracker() {
        if (fLineTracker == null) {
            fLineTracker = new DefaultLineTracker();
            fLineTracker.set(this.contents);
        }
        return fLineTracker;
    }

    public void dispose() {
        contents = null;
        document = null;
        categoryToPos = null;
        fLineTracker = null;
    }

    @Override
    public char getChar(int offset) throws BadLocationException {
        return contents.charAt(offset);
    }

    @Override
    public int getLength() {
        return this.contents.length();
    }

    @Override
    public String get() {
        return this.contents;
    }

    @Override
    public String get(int offset, int length) throws BadLocationException {
        return this.contents.substring(offset, offset + length);
    }

    @Override
    public void set(String text) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void replace(int offset, int length, String text) throws BadLocationException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void addDocumentListener(IDocumentListener listener) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void removeDocumentListener(IDocumentListener listener) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void addPrenotifiedDocumentListener(IDocumentListener documentAdapter) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void removePrenotifiedDocumentListener(IDocumentListener documentAdapter) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void addPositionCategory(String category) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void removePositionCategory(String category) throws BadPositionCategoryException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public String[] getPositionCategories() {
        return this.categoryToPos.entrySet().toArray(new String[this.categoryToPos.size()]);
    }

    @Override
    public boolean containsPositionCategory(String category) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void addPosition(Position position) throws BadLocationException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void removePosition(Position position) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void addPosition(String category, Position position) throws BadLocationException,
            BadPositionCategoryException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void removePosition(String category, Position position) throws BadPositionCategoryException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public Position[] getPositions(String category) throws BadPositionCategoryException {
        return this.categoryToPos.get(category);
    }

    @Override
    public boolean containsPosition(String category, int offset, int length) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public int computeIndexInCategory(String category, int offset) throws BadLocationException,
            BadPositionCategoryException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void addPositionUpdater(IPositionUpdater updater) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void removePositionUpdater(IPositionUpdater updater) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void insertPositionUpdater(IPositionUpdater updater, int index) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public IPositionUpdater[] getPositionUpdaters() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public String[] getLegalContentTypes() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public String getContentType(int offset) throws BadLocationException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ITypedRegion getPartition(int offset) throws BadLocationException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ITypedRegion[] computePartitioning(int offset, int length) throws BadLocationException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void addDocumentPartitioningListener(IDocumentPartitioningListener listener) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void removeDocumentPartitioningListener(IDocumentPartitioningListener listener) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setDocumentPartitioner(IDocumentPartitioner partitioner) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public IDocumentPartitioner getDocumentPartitioner() {
        return document.getDocumentPartitioner();
    }

    public int getLineLength(int line) throws BadLocationException {
        return getLineTracker().getLineLength(line);
    }

    public int getLineOfOffset(int pos) throws BadLocationException {
        return getLineTracker().getLineNumberOfOffset(pos);
    }

    public int getLineOffset(int line) throws BadLocationException {
        return getLineTracker().getLineOffset(line);
    }

    public IRegion getLineInformation(int line) throws BadLocationException {
        return getLineTracker().getLineInformation(line);
    }

    public IRegion getLineInformationOfOffset(int offset) throws BadLocationException {
        return getLineTracker().getLineInformationOfOffset(offset);
    }

    public int getNumberOfLines() {
        return getLineTracker().getNumberOfLines();
    }

    public int getNumberOfLines(int offset, int length) throws BadLocationException {
        return getLineTracker().getNumberOfLines(offset, length);
    }

    public int computeNumberOfLines(String text) {
        return getLineTracker().computeNumberOfLines(text);
    }

    public String[] getLegalLineDelimiters() {
        return getLineTracker().getLegalLineDelimiters();
    }

    public String getLineDelimiter(int line) throws BadLocationException {
        return getLineTracker().getLineDelimiter(line);
    }

    @Override
    public int search(int startOffset, String findString, boolean forwardSearch, boolean caseSensitive,
            boolean wholeWord) throws BadLocationException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public DocumentRewriteSession startRewriteSession(DocumentRewriteSessionType sessionType)
            throws IllegalStateException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void stopRewriteSession(DocumentRewriteSession session) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public DocumentRewriteSession getActiveRewriteSession() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void addDocumentRewriteSessionListener(IDocumentRewriteSessionListener listener) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void removeDocumentRewriteSessionListener(IDocumentRewriteSessionListener listener) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void replace(int offset, int length, String text, long modificationStamp) throws BadLocationException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void set(String text, long modificationStamp) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public long getModificationStamp() {
        return modificationStamp;
    }

    @Override
    public String getDefaultLineDelimiter() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setInitialLineDelimiter(String lineDelimiter) {
        throw new RuntimeException("not implemented");
    }

}
