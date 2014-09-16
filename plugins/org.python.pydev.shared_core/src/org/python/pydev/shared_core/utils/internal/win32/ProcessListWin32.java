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

import org.python.pydev.shared_core.log.Log;
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

        try {
            return createFromWMIC();
        } catch (Exception e) {
            //Keep on going for other alternatives
        }

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

    private IProcessInfo[] createFromWMIC() throws Exception {
        Process p = ProcessUtils.createProcess(new String[] { "wmic.exe", "path", "win32_process", "get",
                "Caption,Processid,Commandline" }, null,
                null);
        List<IProcessInfo> lst = new ArrayList<IProcessInfo>();
        InputStream in = p.getInputStream();
        InputStreamReader reader = new InputStreamReader(in);
        try {
            BufferedReader br = new BufferedReader(reader);
            String line = br.readLine();
            //We should have something as: Caption      CommandLine      ProcessId
            //From this we get the number of characters for each column
            int commandLineI = line.indexOf("CommandLine");
            int processIdI = line.indexOf("ProcessId");
            if (commandLineI == -1) {
                throw new AssertionError("Could not find CommandLine in: " + line);
            }
            if (processIdI == -1) {
                throw new AssertionError("Could not find ProcessId in: " + line);
            }

            while (true) {
                line = br.readLine();
                if (line == null) {
                    break;
                }
                if (line.trim().length() == 0) {
                    continue;
                }
                String name = line.substring(0, commandLineI).trim();
                String commandLine = line.substring(commandLineI, processIdI).trim();
                String processId = line.substring(processIdI, line.length()).trim();
                lst.add(new ProcessInfo(Integer.parseInt(processId), name + "   " + commandLine));
            }
            if (lst.size() == 0) {
                throw new AssertionError("Error: no processes found");
            }
            return lst.toArray(new IProcessInfo[0]);

        } catch (Exception e) {
            Log.log(e);
            throw e;
        } finally {
            in.close();
        }

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