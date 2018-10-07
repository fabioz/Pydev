/**
 * Copyright (c) 2018 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.mypy;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IModule;
import org.python.pydev.shared_core.callbacks.ICallback;

import com.python.pydev.analysis.external.IExternalCodeAnalysisVisitor;

public class MypyVisitorFactory {

    public static IExternalCodeAnalysisVisitor create(IResource resource, IDocument document,
            ICallback<IModule, Integer> module,
            IProgressMonitor internalCancelMonitor) {
        if (MypyPreferences.useMypy(resource) == false) {
            return new OnlyRemoveMarkersMypyVisitor(resource);
        } else {
            return new MypyVisitor(resource, document, module, internalCancelMonitor);
        }
    }
}
