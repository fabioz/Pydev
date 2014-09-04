/*******************************************************************************
 * Copyright (c) 2014 Brainwy Software Ltda.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabio Zadrozny
 *******************************************************************************/
package org.python.pydev.shared_core.utils.internal.win32;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.utils.IProcessInfo;
import org.python.pydev.shared_core.utils.IProcessList;
import org.python.pydev.shared_core.utils.internal.ProcessInfo;

/*
 * This implementation uses the tasklist.exe from windows (must be on the path).
 * 
 * Use through PlatformUtils.
 */
public class ProcessListWin32 implements IProcessList {

    public IProcessInfo[] getProcessList() {
        Process p = null;
        InputStream in = null;
        IProcessInfo[] procInfos = new IProcessInfo[0];

        try {

            try {
                try {
                    p = ProcessUtils.createProcess(new String[] { "tasklist.exe", "/fo", "csv", "/nh", "/v" }, null,
                            null);
                } catch (Exception e) {
                    //Use fallback
                    return new ProcessListWin32Internal().getProcessList();
                }
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
        } catch (IOException e) {
        }
        return procInfos;
    }

    public IProcessInfo[] parseListTasks(InputStreamReader reader) {
        BufferedReader br = new BufferedReader(reader);
        CSVReader csvReader = new CSVReader(br);
        List<ProcessInfo> processList = new ArrayList<>();
        String[] next;
        do {
            try {
                next = csvReader.readNext();
                if (next != null) {
                    int pid = Integer.parseInt(next[1]);
                    String name = StringUtils.join(" - ", next[0], next[next.length - 1]);
                    processList.add(new ProcessInfo(pid, name));

                }
            } catch (IOException e) {
                break;
            }
        } while (next != null);

        return processList.toArray(new IProcessInfo[processList.size()]);
    }

    public static void main(String[] args) {
        IProcessInfo[] processList = new ProcessListWin32().getProcessList();
        for (IProcessInfo iProcessInfo : processList) {
            System.out.println(iProcessInfo);
        }
    }
}