package org.python.pydev.core.docutils;

import org.eclipse.jface.text.IDocument;

/**
 * Class that represents an import found in a document.
 *
 * @author Fabio
 */
public class ImportHandle {

    /**
     * Document where the import was found
     */
    public IDocument doc;
    
    /**
     * The import string found. Note: it may contain comments and multi-lines.
     */
    public String importFound;
    
    /**
     * The initial line where the import was found
     */
    public int startFoundLine;
    
    /**
     * The final line where the import was found
     */
    public int endFoundLine;

    /**
     * Constructor.
     * 
     * Assigns parameters to fields.
     */
    public ImportHandle(IDocument doc, String importFound, int startFoundLine, int endFoundLine) {
        this.doc = doc;
        this.importFound = importFound;
        this.startFoundLine = startFoundLine;
        this.endFoundLine = endFoundLine;
    }

}
