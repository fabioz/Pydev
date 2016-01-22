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
package org.python.pydev.shared_core.partitioner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitionerExtension2;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.rules.ICharacterScanner;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.utils.ArrayUtils;

/**
 * A reader that'll only read based on a given partition type.
 *
 * @author Fabio Zadrozny
 */
public class PartitionCodeReader implements ICharacterScanner, IMarkScanner {

    /**
     * Note: not suitable for sub-partitions.
     */
    public static final String ALL_CONTENT_TYPES_AVAILABLE = "ALL_CONTENT_TYPES_AVAILABLE";

    /** The EOF character */
    public static final int EOF = -1;

    private boolean fForward = false;

    private IDocument fDocument;

    private int fOffset;

    private int fEnd = -1;

    private final String contentType;

    private int fcurrentPositionI = 0;

    private Position fCurrentPosition = null;

    private Position[] fPositions;

    private char[][] fDelimiters;

    private int fStartForwardOffset;

    private boolean fSupportKeepPositions;

    public PartitionCodeReader(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Returns the offset of the last read character. Should only be called after read has been called.
     */
    public int getOffset() {
        return fForward ? fOffset - 1 : fOffset + 1;
    }

    public void configureForwardReaderKeepingPositions(int offset, int end) throws IOException,
            BadPositionCategoryException {
        if (!fSupportKeepPositions) {
            throw new AssertionError("configureForwardReader must be called with supportKeepPositions=true.");
        }
        fOffset = offset;
        fStartForwardOffset = offset;
        fForward = true;
        fEnd = Math.min(fDocument.getLength(), end);
        fcurrentPositionI = 0;
        if (fPositions.length > 0) {
            fCurrentPosition = fPositions[0];
        } else {
            fCurrentPosition = null;
        }
    }

    public void configureForwardReader(IDocument document, int offset, int end) throws IOException,
            BadPositionCategoryException {
        configureForwardReader(document, offset, end, false);
    }

    public void configureForwardReader(IDocument document, int offset, int end, boolean supportKeepPositions)
            throws IOException,
            BadPositionCategoryException {
        fSupportKeepPositions = supportKeepPositions;
        fDocument = document;
        fOffset = offset;
        fStartForwardOffset = offset;
        fForward = true;
        fEnd = Math.min(fDocument.getLength(), end);
        fcurrentPositionI = 0;
        fPositions = createPositions(document);
        if (fPositions.length > 0) {
            fCurrentPosition = fPositions[0];
        } else {
            fCurrentPosition = null;
        }
    }

    public void configureBackwardReader(IDocument document, int offset) throws IOException,
            BadPositionCategoryException {
        fStartForwardOffset = 0;
        fDocument = document;
        fOffset = offset;
        fForward = false;
        fcurrentPositionI = 0;
        fPositions = createPositions(document);
        if (fPositions.length > 0) {
            fCurrentPosition = fPositions[0];
        } else {
            fCurrentPosition = null;
        }
    }

    private Position[] createPositions(IDocument document) throws BadPositionCategoryException {
        Position[] positions = getDocumentTypedPositions(document, contentType);
        List<TypedPosition> typedPositions = PartitionMerger.sortAndMergePositions(positions, document.getLength());
        int size = typedPositions.size();
        List<Position> list = new ArrayList<Position>(size);
        for (int i = 0; i < size; i++) {
            Position position = typedPositions.get(i);
            if (isPositionValid(position, contentType)) {
                list.add(position);
            }
        }

        Position[] ret = list.toArray(new Position[list.size()]);
        if (!fForward) {
            ArrayUtils.reverse(ret);
        }
        return ret;
    }

    private boolean isPositionValid(Position position, String contentType) {
        if (fSupportKeepPositions || (fForward && position.getOffset() + position.getLength() >= fOffset || !fForward
                && position.getOffset() <= fOffset)) {
            if (position instanceof TypedPosition) {
                TypedPosition typedPosition = (TypedPosition) position;
                if (contentType != null && !contentType.equals(ALL_CONTENT_TYPES_AVAILABLE)) {
                    if (!contentType.equals(typedPosition.getType())) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Note: this just gets the positions in the document. To cover for holes, use
     * StringUtils.sortAndMergePositions with the result of this call.
     */
    public static Position[] getDocumentTypedPositions(IDocument document, String defaultContentType) {
        if (ALL_CONTENT_TYPES_AVAILABLE.equals(defaultContentType)) {
            //Consider the whole document
            return new Position[] { new TypedPosition(0, document.getLength(), defaultContentType) };
        }
        Position[] positions;
        try {
            IDocumentPartitionerExtension2 partitioner = (IDocumentPartitionerExtension2) document
                    .getDocumentPartitioner();
            String[] managingPositionCategories = partitioner.getManagingPositionCategories();
            Assert.isTrue(managingPositionCategories.length == 1);
            positions = document.getPositions(managingPositionCategories[0]);
            if (positions == null) {
                positions = new Position[] { new TypedPosition(0, document.getLength(), defaultContentType) };
            }
        } catch (Exception e) {
            Log.log("Unable to get positions for: " + defaultContentType, e); //Shouldn't happen, but if it does, consider the whole doc.
            positions = new Position[] { new TypedPosition(0, document.getLength(), defaultContentType) };
        }
        return positions;
    }

    /*
     * @see Reader#close()
     */
    public void close() throws IOException {
        fDocument = null;
    }

    /*
     * @see SingleCharReader#read()
     */
    public int read() {
        try {
            return fForward ? readForwards() : readBackwards();
        } catch (BadLocationException x) {
            return EOF; //Document may have changed...
        }
    }

    public char[][] getLegalLineDelimiters() {
        if (fDelimiters == null) {
            String[] delimiters = fDocument.getLegalLineDelimiters();
            fDelimiters = new char[delimiters.length][];
            for (int i = 0; i < delimiters.length; i++) {
                fDelimiters[i] = delimiters[i].toCharArray();
            }
        }
        return fDelimiters;
    }

    public int getColumn() {
        try {
            final int offset = getOffset();
            final int line = fDocument.getLineOfOffset(offset);
            final int start = fDocument.getLineOffset(line);
            return offset - start;
        } catch (BadLocationException e) {
        }

        return -1;
    }

    public void unread() {
        if (fForward) {
            if (fCurrentPosition == null) { //unread EOF
                if (fPositions.length > 0) {
                    fcurrentPositionI = fPositions.length - 1;
                    fCurrentPosition = fPositions[fcurrentPositionI];
                    fOffset = fCurrentPosition.offset + fCurrentPosition.length;
                    if (fOffset < fStartForwardOffset) {
                        fOffset = fStartForwardOffset;
                    }
                }
                return;
            }

            fOffset--;
            if (fOffset < fStartForwardOffset) {
                fOffset = fStartForwardOffset;
                return;
            }

            while (true) {
                if (fOffset < fCurrentPosition.offset) {
                    if (fcurrentPositionI > 0) {
                        fcurrentPositionI--;
                        fCurrentPosition = fPositions[fcurrentPositionI];

                        if (fCurrentPosition.offset + fCurrentPosition.length <= fOffset) {
                            fOffset = fCurrentPosition.offset + fCurrentPosition.length - 1;
                            if (fOffset < fStartForwardOffset) {
                                fOffset = fStartForwardOffset;
                            }
                            return;
                        }
                    } else {
                        return;
                    }
                } else {
                    break;
                }
            }

        } else {
            throw new AssertionError("Unread when going backward not supported yet");
        }
    }

    private int readForwards() throws BadLocationException {
        while (true) {
            if (fCurrentPosition == null) {
                return EOF;
            }
            if (fCurrentPosition.offset + fCurrentPosition.length <= fOffset) {
                fcurrentPositionI++;
                if (fcurrentPositionI >= fPositions.length) {
                    fCurrentPosition = null;
                    return EOF;
                }
                fCurrentPosition = fPositions[fcurrentPositionI];
            } else {
                break;
            }
        }

        if (fCurrentPosition.offset > fOffset) {
            fOffset = fCurrentPosition.offset;
        }

        if (fOffset < fEnd) {
            char current = fDocument.getChar(fOffset);
            fOffset++;
            return current;
        } else {
            fCurrentPosition = null;
        }

        return EOF;
    }

    private int readBackwards() throws BadLocationException {
        while (true) {
            if (fCurrentPosition == null) {
                return EOF;
            }
            if (fCurrentPosition.offset > fOffset) {
                fcurrentPositionI++;
                if (fcurrentPositionI >= fPositions.length) {
                    return EOF;
                }
                fCurrentPosition = fPositions[fcurrentPositionI];
            } else {
                break;
            }
        }
        if (fCurrentPosition.offset + fCurrentPosition.length <= fOffset) {
            fOffset = fCurrentPosition.offset + fCurrentPosition.length - 1;
        }

        if (fOffset >= 0) {
            char current = fDocument.getChar(fOffset);
            fOffset--;
            return current;
        }

        return EOF;
    }

    public int getMark() {
        return fOffset;
    }

    public void setMark(int offset) {
        if (fForward) {
            fCurrentPosition = null;
            for (int i = 0; i < fPositions.length; i++) {
                Position p = fPositions[i];
                if (p.offset + p.length > offset) {
                    fcurrentPositionI = i;
                    fCurrentPosition = p;
                    fOffset = offset;
                    break;
                }
            }
        } else {
            fCurrentPosition = null;
            for (int i = 0; i < fPositions.length; i++) { //note: it's already backwards
                Position p = fPositions[i];
                if (p.offset < offset) {
                    fcurrentPositionI = i;
                    fCurrentPosition = p;
                    fOffset = offset;
                    break;
                }
            }
        }
    }

}
