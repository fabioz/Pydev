/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.utils;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Fabio Zadrozny
 */
public class PrintProgressMonitor implements IProgressMonitor {

    /**
     * @see org.eclipse.core.runtime.IProgressMonitor#beginTask(java.lang.String, int)
     */
    public void beginTask(String name, int totalWork) {
        System.out.println(name + " total = " + totalWork);
    }

    /**
     * @see org.eclipse.core.runtime.IProgressMonitor#done()
     */
    public void done() {
        System.out.println("done");
    }

    /**
     * @see org.eclipse.core.runtime.IProgressMonitor#internalWorked(double)
     */
    public void internalWorked(double work) {
    }

    /**
     * @see org.eclipse.core.runtime.IProgressMonitor#isCanceled()
     */
    public boolean isCanceled() {
        return false;
    }

    /**
     * @see org.eclipse.core.runtime.IProgressMonitor#setCanceled(boolean)
     */
    public void setCanceled(boolean value) {
    }

    /**
     * @see org.eclipse.core.runtime.IProgressMonitor#setTaskName(java.lang.String)
     */
    public void setTaskName(String name) {
        System.out.println(name);
    }

    /**
     * @see org.eclipse.core.runtime.IProgressMonitor#subTask(java.lang.String)
     */
    public void subTask(String name) {
        System.out.println(name);
    }

    /**
     * @see org.eclipse.core.runtime.IProgressMonitor#worked(int)
     */
    public void worked(int work) {
    }

}
