/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.pythontests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IStatus;
import org.python.pydev.core.log.Log;
import org.python.pydev.jython.JythonPlugin;

public abstract class AbstractBasicRunTestCase extends TestCase {

    public void execAllAndCheckErrors(final String startingWith, File[] beneathFolders) throws Exception {
        List<Throwable> errors = execAll(startingWith, beneathFolders);
        if (errors.size() > 0) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("There have been errors while executing the test scripts.\n\n".getBytes());
            for (Throwable throwable : errors) {
                throwable.printStackTrace(new PrintStream(out));
            }
            fail(new String(out.toByteArray()));
        }

    }

    public List<Throwable> execAll(final String startingWith, File[] beneathFolders) {
        List<Throwable> errors = new ArrayList<Throwable>();
        for (File file : beneathFolders) {
            if (file != null) {
                if (!file.exists()) {
                    String msg = "The folder:" + file + " does not exist and therefore cannot be used to "
                            + "find scripts to run starting with:" + startingWith;
                    Log.log(IStatus.ERROR, msg, null);
                    errors.add(new RuntimeException(msg));
                }
                File[] files = JythonPlugin.getFilesBeneathFolder(startingWith, file);
                if (files != null) {
                    for (File f : files) {
                        Throwable throwable = exec(f);
                        if (throwable != null) {
                            errors.add(throwable);
                        }
                    }
                }
            }
        }
        return errors;
    }

    protected abstract Throwable exec(File f);

}
