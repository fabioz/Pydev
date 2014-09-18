/*******************************************************************************
 * Copyright (c) 2000, 2010 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package org.python.pydev.shared_core.utils.internal.linux;

import java.io.File;
import java.io.FilenameFilter;

import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.utils.IProcessInfo;
import org.python.pydev.shared_core.utils.IProcessList;
import org.python.pydev.shared_core.utils.internal.ProcessInfo;

/**
 * Use through PlatformUtils.
 */
public class ProcessListLinux implements IProcessList {

    ProcessInfo[] empty = new ProcessInfo[0];

    public ProcessListLinux() {
    }

    /**
     * Insert the method's description here.
     * @see IProcessList#getProcessList
     */
    public IProcessInfo[] getProcessList() {
        File proc = new File("/proc"); //$NON-NLS-1$
        File[] pidFiles = null;

        // We are only interested in the pid so filter the rest out.
        try {
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    boolean isPID = false;
                    try {
                        Integer.parseInt(name);
                        isPID = true;
                    } catch (NumberFormatException e) {
                    }
                    return isPID;
                }
            };
            pidFiles = proc.listFiles(filter);
        } catch (SecurityException e) {
        }

        ProcessInfo[] processInfo = empty;
        if (pidFiles != null) {
            processInfo = new ProcessInfo[pidFiles.length];
            for (int i = 0; i < pidFiles.length; i++) {
                File cmdLine = new File(pidFiles[i], "cmdline"); //$NON-NLS-1$
                String name = FileUtils.getFileContents(cmdLine).replace('\0', ' ');
                if (name.length() == 0) {
                    name = "Unknown"; //$NON-NLS-1$
                }
                processInfo[i] = new ProcessInfo(pidFiles[i].getName(), name);
            }
        } else {
            pidFiles = new File[0];
        }
        return processInfo;
    }

    public static void main(String[] args) {
        IProcessInfo[] processList = new ProcessListLinux().getProcessList();
        for (IProcessInfo iProcessInfo : processList) {
            System.out.println(iProcessInfo);
        }
    }

}