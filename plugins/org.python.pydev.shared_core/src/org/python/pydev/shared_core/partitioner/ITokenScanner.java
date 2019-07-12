package org.python.pydev.shared_core.partitioner;

import org.eclipse.jface.text.IDocument;

/**
 * A token scanner scans a range of a document and reports about the token it finds.
 * A scanner has state. When asked, the scanner returns the offset and the length of the
 * last found token.
 *
 * @see org.eclipse.jface.text.rules.IToken
 * @since 2.0
 */
public interface ITokenScanner {

    /**
     * Configures the scanner by providing access to the document range that should
     * be scanned.
     *
     * @param document the document to scan
     * @param offset the offset of the document range to scan
     * @param length the length of the document range to scan
     */
    void setRange(IDocument document, int offset, int length);

    /**
     * Returns the next token in the document.
     *
     * @return the next token in the document
     */
    IToken nextToken();

    /**
     * Returns the offset of the last token read by this scanner.
     *
     * @return the offset of the last token read by this scanner
     */
    int getTokenOffset();

    /**
     * Returns the length of the last token read by this scanner.
     *
     * @return the length of the last token read by this scanner
     */
    int getTokenLength();
}
