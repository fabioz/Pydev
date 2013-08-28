/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Ecliplse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.utils;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ISynchronizable;
import org.python.pydev.shared_core.callbacks.ICallback;

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

}
