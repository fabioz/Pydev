package com.python.pydev.refactoring.refactorer.search;


import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Adapting a document to a CharSequence
 */
public class DocumentCharSequence implements CharSequence {

    private final IDocument fDocument;

    /**
     * @param document The document to wrap
     */
    public DocumentCharSequence(IDocument document) {
        fDocument= document;
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#length()
     */
    public int length() {
        return fDocument.getLength();
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#charAt(int)
     */
    public char charAt(int index) {
        try {
            return fDocument.getChar(index);
        } catch (BadLocationException e) {
            throw new IndexOutOfBoundsException();
        }
    }

    /* (non-Javadoc)
     * @see java.lang.CharSequence#subSequence(int, int)
     */
    public CharSequence subSequence(int start, int end) {
        try {
            return fDocument.get(start, end - start);
        } catch (BadLocationException e) {
            throw new IndexOutOfBoundsException();
        }
    }

}
