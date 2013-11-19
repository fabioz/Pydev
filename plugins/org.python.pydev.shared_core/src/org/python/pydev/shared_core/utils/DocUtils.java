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
package org.python.pydev.shared_core.utils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static String[] getAllDocumentContentTypes(IDocument document) throws BadPartitioningException {
        if (document instanceof IDocumentExtension3) {
            IDocumentExtension3 ext = (IDocumentExtension3) document;
            String[] partitionings = ext.getPartitionings();

            Set contentTypes = new HashSet();
            contentTypes.add(IDocument.DEFAULT_CONTENT_TYPE);

            int len = partitionings.length;
            for (int i = 0; i < len; i++) {
                String[] legalContentTypes = ext.getLegalContentTypes(partitionings[i]);
                int len2 = legalContentTypes.length;
                for (int j = 0; j < len2; j++) {
                    contentTypes.add(legalContentTypes[j]);
                }
                contentTypes.addAll(Arrays.asList(legalContentTypes));
            }
            return (String[]) contentTypes.toArray(new String[contentTypes.size()]);
        }
        return document.getLegalContentTypes();
    }
}
