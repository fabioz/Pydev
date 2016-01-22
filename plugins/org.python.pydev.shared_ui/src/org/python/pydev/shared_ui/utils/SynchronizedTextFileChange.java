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
package org.python.pydev.shared_ui.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.python.pydev.shared_core.log.Log;

public class SynchronizedTextFileChange extends TextFileChange {

    public SynchronizedTextFileChange(String name, IFile file) {
        super(name, file);
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
                    superPerform[0] = new RuntimeException(e);
                    Log.log(e);
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

}