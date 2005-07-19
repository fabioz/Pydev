/*
 * License: Common Public License v1.0
 * Created on Jul 19, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;

public class PyDocumentSetupParticipant implements IDocumentSetupParticipant{

    public PyDocumentSetupParticipant() {
        super();
    }

    public void setup(IDocument document) {
        PyPartitionScanner.addPartitionScanner(document);
    }

}
