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

public class ThreadPriorityHelper {

    private Thread thisThread;
    private int initialPriority;

    public ThreadPriorityHelper(Thread thread) {
        thisThread = thread;
        if (thread != null) {
            initialPriority = thread.getPriority();
        }
    }

    public void setMinPriority() {
        if (thisThread != null) {
            thisThread.setPriority(Thread.MIN_PRIORITY);
        }
    }

    public void restoreInitialPriority() {
        if (thisThread != null) {
            thisThread.setPriority(initialPriority);
        }
    }
}
