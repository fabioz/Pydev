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

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ISynchronizable;
import org.python.pydev.shared_core.callbacks.ICallback;

public class DocumentSync {

    public static Object runWithDocumentSynched(IDocument document, ICallback<Object, IDocument> iCallback,
            boolean createCopy) {
        Object lockObject = null;
        if (document instanceof ISynchronizable) {
            ISynchronizable sync = (ISynchronizable) document;
            lockObject = sync.getLockObject();
        }
        DocCopy docCopy = null;
        try {
            if (lockObject != null) {
                if (createCopy) {
                    synchronized (lockObject) {
                        docCopy = new DocCopy(document);
                    }
                    return iCallback.call(docCopy);
                } else {
                    synchronized (lockObject) {
                        return iCallback.call(document);
                    }
                }
            } else { //unsynched
                if (createCopy && !(document instanceof DocCopy)) {
                    docCopy = new DocCopy(document);
                    return iCallback.call(docCopy);
                }
                return iCallback.call(document);
            }
        } finally {
            if (docCopy != null) {
                docCopy.dispose();
            }
        }
    }

    public static IDocument createUnsynchedDocIfNeeded(IDocument doc) {
        if (doc instanceof ISynchronizable) {
            return new DocCopy(doc);
        }
        return doc;
    }

}
