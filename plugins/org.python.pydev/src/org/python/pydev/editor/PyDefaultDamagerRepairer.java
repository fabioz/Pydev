package org.python.pydev.editor;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.python.pydev.shared_core.partitioner.IToken;
import org.python.pydev.shared_core.partitioner.ITokenScanner;
import org.python.pydev.shared_core.partitioner.Token;

/**
 * A standard implementation of a syntax driven presentation damager
 * and presentation repairer. It uses a token scanner to scan
 * the document and to determine its damage and new text presentation.
 * The tokens returned by the scanner are supposed to return text attributes
 * as their data.
 *
 * @see ITokenScanner
 * @since 2.0
 */
public class PyDefaultDamagerRepairer implements IPresentationDamager, IPresentationRepairer {

    /** The document this object works on */
    protected IDocument fDocument;
    /** The scanner it uses */
    protected ITokenScanner fScanner;
    /** The default text attribute if non is returned as data by the current token */
    protected TextAttribute fDefaultTextAttribute;

    /**
     * Creates a damager/repairer that uses the given scanner. The scanner may not be <code>null</code>
     * and is assumed to return only token that carry text attributes.
     *
     * @param scanner the token scanner to be used, may not be <code>null</code>
     */
    public PyDefaultDamagerRepairer(ITokenScanner scanner) {

        Assert.isNotNull(scanner);

        fScanner = scanner;
        fDefaultTextAttribute = new TextAttribute(null);
    }

    /*
     * @see IPresentationDamager#setDocument(IDocument)
     * @see IPresentationRepairer#setDocument(IDocument)
     */
    @Override
    public void setDocument(IDocument document) {
        fDocument = document;
    }

    //---- IPresentationDamager

    /**
     * Returns the end offset of the line that contains the specified offset or
     * if the offset is inside a line delimiter, the end offset of the next line.
     *
     * @param offset the offset whose line end offset must be computed
     * @return the line end offset for the given offset
     * @exception BadLocationException if offset is invalid in the current document
     */
    protected int endOfLineOf(int offset) throws BadLocationException {

        IRegion info = fDocument.getLineInformationOfOffset(offset);
        if (offset <= info.getOffset() + info.getLength()) {
            return info.getOffset() + info.getLength();
        }

        int line = fDocument.getLineOfOffset(offset);
        try {
            info = fDocument.getLineInformation(line + 1);
            return info.getOffset() + info.getLength();
        } catch (BadLocationException x) {
            return fDocument.getLength();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation damages entire lines unless clipped by the given partition.
     * </p>
     *
     * @return the full lines containing the document changes described by the document event,
     *         clipped by the given partition. If there was a partitioning change then the whole
     *         partition is returned.
     */
    @Override
    public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent e, boolean documentPartitioningChanged) {

        if (!documentPartitioningChanged) {
            try {

                IRegion info = fDocument.getLineInformationOfOffset(e.getOffset());
                int start = Math.max(partition.getOffset(), info.getOffset());

                int end = e.getOffset() + (e.getText() == null ? e.getLength() : e.getText().length());

                if (info.getOffset() <= end && end <= info.getOffset() + info.getLength()) {
                    // optimize the case of the same line
                    end = info.getOffset() + info.getLength();
                } else {
                    end = endOfLineOf(end);
                }

                end = Math.min(partition.getOffset() + partition.getLength(), end);
                return new Region(start, end - start);

            } catch (BadLocationException x) {
            }
        }

        return partition;
    }

    //---- IPresentationRepairer

    @Override
    public void createPresentation(TextPresentation presentation, ITypedRegion region) {

        if (fScanner == null) {
            // will be removed if deprecated constructor will be removed
            addRange(presentation, region.getOffset(), region.getLength(), fDefaultTextAttribute);
            return;
        }

        int lastStart = region.getOffset();
        int length = 0;
        boolean firstToken = true;
        IToken lastToken = Token.UNDEFINED;
        TextAttribute lastAttribute = getTokenTextAttribute(lastToken);

        fScanner.setRange(fDocument, lastStart, region.getLength());

        while (true) {
            IToken token = fScanner.nextToken();
            if (token.isEOF()) {
                break;
            }

            TextAttribute attribute = getTokenTextAttribute(token);
            if (lastAttribute != null && lastAttribute.equals(attribute)) {
                length += fScanner.getTokenLength();
                firstToken = false;
            } else {
                if (!firstToken) {
                    addRange(presentation, lastStart, length, lastAttribute);
                }
                firstToken = false;
                lastToken = token;
                lastAttribute = attribute;
                lastStart = fScanner.getTokenOffset();
                length = fScanner.getTokenLength();
            }
        }

        addRange(presentation, lastStart, length, lastAttribute);
    }

    /**
     * Returns a text attribute encoded in the given token. If the token's
     * data is not <code>null</code> and a text attribute it is assumed that
     * it is the encoded text attribute. It returns the default text attribute
     * if there is no encoded text attribute found.
     *
     * @param token the token whose text attribute is to be determined
     * @return the token's text attribute
     */
    protected TextAttribute getTokenTextAttribute(IToken token) {
        Object data = token.getData();
        if (data instanceof TextAttribute) {
            return (TextAttribute) data;
        }
        return fDefaultTextAttribute;
    }

    /**
     * Adds style information to the given text presentation.
     *
     * @param presentation the text presentation to be extended
     * @param offset the offset of the range to be styled
     * @param length the length of the range to be styled
     * @param attr the attribute describing the style of the range to be styled
     */
    protected void addRange(TextPresentation presentation, int offset, int length, TextAttribute attr) {
        if (attr != null) {
            int style = attr.getStyle();
            int fontStyle = style & (SWT.ITALIC | SWT.BOLD | SWT.NORMAL);
            StyleRange styleRange = new StyleRange(offset, length, attr.getForeground(), attr.getBackground(),
                    fontStyle);
            styleRange.strikeout = (style & TextAttribute.STRIKETHROUGH) != 0;
            styleRange.underline = (style & TextAttribute.UNDERLINE) != 0;
            styleRange.font = attr.getFont();
            presentation.addStyleRange(styleRange);
        }
    }
}
