/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.python.pydev.shared_core.utils.internal.macos;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.utils.IProcessInfo;
import org.python.pydev.shared_core.utils.IProcessList;
import org.python.pydev.shared_core.utils.internal.ProcessInfo;

/**
 * Use through PlatformUtils.
 */
public class ProcessListMac implements IProcessList {

    ProcessInfo[] empty = new ProcessInfo[0];

    public ProcessListMac() {
    }

    /**
     * Insert the method's description here.
     * @see IProcessList#getProcessList
     */
    public IProcessInfo[] getProcessList() {
        Process ps;
        BufferedReader psOutput;
        String[] args = { "/bin/ps", "-a", "-x", "-o", "pid,command" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ 

        try {
            ps = ProcessUtils.createProcess(args, null, null);
            psOutput = new BufferedReader(new InputStreamReader(ps.getInputStream()));
        } catch (Exception e) {
            return new IProcessInfo[0];
        }

        //Read the output and parse it into an array list
        List<IProcessInfo> procInfo = new ArrayList<>();

        try {
            String lastline;
            while ((lastline = psOutput.readLine()) != null) {
                //The format of the output should be 
                //PID space name
                lastline = lastline.trim();
                int index = lastline.indexOf(' ');
                if (index != -1) {
                    String pidString = lastline.substring(0, index).trim();
                    try {
                        int pid = Integer.parseInt(pidString);
                        String arg = lastline.substring(index + 1);
                        procInfo.add(new ProcessInfo(pid, arg));
                    } catch (NumberFormatException e) {
                    }
                }
            }
            psOutput.close();
        } catch (Exception e) {
            /* Ignore */
        }

        ps.destroy();
        return procInfo.toArray(new IProcessInfo[procInfo.size()]);
    }

    public static void main(String[] args) {
        IProcessInfo[] processList = new ProcessListMac().getProcessList();
        for (IProcessInfo iProcessInfo : processList) {
            System.out.println(iProcessInfo);
        }
    }
}