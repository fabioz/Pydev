/******************************************************************************
* Copyright (C) 2010-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.refactoring.core.base;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_ui.utils.RunInUiThread;

public class PyDocumentChange extends DocumentChange {

    /** Construct with factory method **/
    private PyDocumentChange(String name, IDocument document) {
        super(name, document);
    }

    @Override
    public Change perform(final org.eclipse.core.runtime.IProgressMonitor pm) throws CoreException {

        final Object[] superPerform = new Object[1];
        //We need to sync it to have UI access because otherwise we're unable to start a document rewrite session.
        RunInUiThread.sync(new Runnable() {

            public void run() {
                try {
                    superPerform[0] = superPerform(pm);
                } catch (CoreException e) {
                    superPerform[0] = e;
                    Log.log(e);
                } catch (Throwable e) {
                    superPerform[0] = e;
                    throw new RuntimeException(e);
                }
            }
        });
        Object object = superPerform[0];

        if (object == null) {
            return null;
        }

        if (object instanceof Change) {
            return (Change) object;
        }

        if (object instanceof CoreException) {
            throw (CoreException) object;
        } else {
            throw (RuntimeException) object;

        }
    }

    public Change superPerform(org.eclipse.core.runtime.IProgressMonitor pm) throws CoreException {
        return super.perform(pm);
    }

    public static TextChange create(String name, IDocument document) {
        if (ResourcesPlugin.getPlugin() != null) {
            return new PyDocumentChange(name, document);
        } else {
            return new PyDocumentChangeForTests(name, document);
        }
    }

    @Override
    public String getTextType() {
        return "py";
    }
}
