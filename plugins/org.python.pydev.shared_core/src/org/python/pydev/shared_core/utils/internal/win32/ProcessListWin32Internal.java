/*******************************************************************************
 * Copyright (c) 2000, 2014 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Martin Oberhuber (Wind River) - [303083] Split out the Spawner
 *******************************************************************************/
package org.python.pydev.shared_core.utils.internal.win32;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.bundle.BundleUtils;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.utils.IProcessInfo;
import org.python.pydev.shared_core.utils.IProcessList;
import org.python.pydev.shared_core.utils.internal.ProcessInfo;

/*
 * This implementation uses a listtasks which is shipped together (so, it should always work on windows).
 * 
 * Use through PlatformUtils.
 */
public class ProcessListWin32Internal implements IProcessList {

    private IProcessInfo[] NOPROCESS = new IProcessInfo[0];

    public IProcessInfo[] getProcessList() {
        Process p = null;
        String command = null;
        InputStream in = null;
        Bundle bundle = Platform.getBundle(SharedCorePlugin.PLUGIN_ID);
        IProcessInfo[] procInfos = NOPROCESS;

        try {
            File file;
            IPath relative = new Path("win32").addTrailingSeparator().append("listtasks.exe");
            file = BundleUtils.getRelative(relative, bundle);

            if (file != null && file.exists()) {
                command = file.getCanonicalPath();
                if (command != null) {
                    try {
                        p = ProcessUtils.createProcess(new String[] { command }, null, null);
                        in = p.getInputStream();
                        InputStreamReader reader = new InputStreamReader(in);
                        procInfos = parseListTasks(reader);
                    } finally {
                        if (in != null) {
                            in.close();
                        }
                        if (p != null) {
                            p.destroy();
                        }
                    }
                }
            }
        } catch (IOException e) {
        }
        return procInfos;
    }

    public IProcessInfo[] parseListTasks(InputStreamReader reader) {
        BufferedReader br = new BufferedReader(reader);
        List<IProcessInfo> processList = new ArrayList<>();
        try {
            String line;
            while ((line = br.readLine()) != null) {
                int tab = line.indexOf('\t');
                if (tab != -1) {
                    String proc = line.substring(0, tab).trim();
                    String name = line.substring(tab).trim();
                    if (proc.length() > 0 && name.length() > 0) {
                        try {
                            int pid = Integer.parseInt(proc);
                            processList.add(new ProcessInfo(pid, name));
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }
        } catch (IOException e) {
        }
        return processList.toArray(new IProcessInfo[processList.size()]);
    }

    public static void main(String[] args) {
        IProcessInfo[] processList = new ProcessListWin32Internal().getProcessList();
        for (IProcessInfo iProcessInfo : processList) {
            System.out.println(iProcessInfo);
        }
    }
}