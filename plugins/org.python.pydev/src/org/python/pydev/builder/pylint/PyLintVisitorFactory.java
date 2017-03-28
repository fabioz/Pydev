/**
 * Copyright (c) 2017 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.builder.pylint;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IModule;
import org.python.pydev.shared_core.callbacks.ICallback;

public class PyLintVisitorFactory {

    public static IPyLintVisitor create(IResource resource, IDocument document, ICallback<IModule, Integer> module,
            IProgressMonitor internalCancelMonitor) {
        if (PyLintPrefPage.usePyLint() == false) {
            return new OnlyRemoveMarkersPyLintVisitor(resource);
        } else {
            return new PyLintVisitor(resource, document, module, internalCancelMonitor);
        }

    }
}
