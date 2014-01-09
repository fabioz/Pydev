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
package org.python.pydev.shared_ui.utils;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ProgressMonitorWrapper;

public class AsynchronousProgressMonitorWrapper extends ProgressMonitorWrapper {

    private long lastChange;

    public AsynchronousProgressMonitorWrapper(IProgressMonitor monitor) {
        super(monitor);
    }

    @Override
    public void setTaskName(String name) {
        long curr = System.currentTimeMillis();
        if (curr - lastChange > AsynchronousProgressMonitorDialog.UPDATE_INTERVAL_MS) {
            this.lastChange = curr;
            super.setTaskName(name);
        }
    }
}
