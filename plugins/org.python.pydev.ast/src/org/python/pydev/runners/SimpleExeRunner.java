/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jun 25, 2006
 * @author Fabio
 */
package org.python.pydev.runners;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

public class SimpleExeRunner extends SimpleRunner {

    /**
     * Some notes on what has to be converted for cygwin:
     * - cygwin will accept both formats for the script
     * - PYTHONPATH, the paths MUST be in cygwin format
     * - In python, internally, all needs to be in cygwin paths.
     * 
     * below are the 'interactive' execution of python to validate what's written above.
     * 
     * 1. cygwin will accept both formats for the script:
     * 
     * >>c:\bin\cygwin\bin\python2.4.exe c:\temp\test.py
     * worked
     * >>c:\bin\cygwin\bin\python2.4.exe /cygdrive/c/temp/test.py
     * worked
     * 
     * 
     * 2. Now, for the pythonpath, the paths MUST be in cygwin format:
     * 
     * >>set pythonpath="c:/temp"
     * 
     * >>c:\bin\cygwin\bin\python2.4.exe /cygdrive/c/temp/test.py
     * /cygdrive/c/temp
     * /cygdrive/c/bin/4nt600/"c << yeap, here is the error. I have no idea why it happens.
     * /temp"
     * /usr/lib/python24.zip
     * ...
     * 
     * >>set pythonpath="/cygdrive/c/temp"
     * 
     * >>c:\bin\cygwin\bin\python2.4.exe /cygdrive/c/temp/test.py
     * /cygdrive/c/temp
     * /cygdrive/c/bin/"/cygdrive/c/temp"
     * /usr/lib/python24.zip
     * ...
     * 
     * 
     * In python, internally, all needs to be in cygwin paths:
     * print os.path.abspath(os.path.curdir)
     * will return /cygwin/c/bin
     * 
     * @param cygpathLoc the cygpath.exe location
     * @param paths the windows paths to be converted to cygwin.
     * @return a list of changed paths converted to cygwin.
     */
    public List<String> convertToCygwinPath(String cygpathLoc, String... paths) {
        for (int i = 0; i < paths.length; i++) {
            paths[i] = StringUtils.replaceAllSlashes(paths[i]);
        }
        ArrayList<String> ret = new ArrayList<String>();

        List<String> asList = new ArrayList<String>(Arrays.asList(paths));
        asList.add(0, cygpathLoc);

        Tuple<String, String> output = runAndGetOutput(asList.toArray(new String[0]), (File) null,
                (IPythonNature) null, new NullProgressMonitor(), "utf-8");
        if (output.o2 != null && output.o2.length() > 0) {
            throw new RuntimeException("Error converting windows paths to cygwin paths: " + output.o2
                    + ".\nCygpath location:" + cygpathLoc);
        }
        if (output.o1 == null || output.o1.length() == 0) {
            throw new RuntimeException("Unable to get the output.\nCygpath location:" + cygpathLoc);
        }
        StringTokenizer tokenizer = new StringTokenizer(output.o1, "\r\n");
        while (tokenizer.hasMoreTokens()) {
            String tok = tokenizer.nextToken();
            ret.add(tok.trim());
        }

        return ret;
    }

}
