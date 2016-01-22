/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jul 19, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.partition.PyPartitionScanner;

public class PyDocumentSetupParticipant implements IDocumentSetupParticipant {

    public PyDocumentSetupParticipant() {
        super();
    }

    public void setup(IDocument document) {
        PyPartitionScanner.addPartitionScanner(document, null);
    }

}
