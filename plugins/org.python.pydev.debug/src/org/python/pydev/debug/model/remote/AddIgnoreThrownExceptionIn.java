/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model.remote;

import java.io.File;
import java.util.Collection;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.PyExceptionBreakPointManager;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;

public class AddIgnoreThrownExceptionIn extends AbstractDebuggerCommand {

    private File file;
    private int lineNumber;

    /**
     * Used to bulk-create all currently ignored.
     */
    public AddIgnoreThrownExceptionIn(AbstractDebugTarget debugger) {
        super(debugger);
    }

    public AddIgnoreThrownExceptionIn(AbstractDebugTarget debugger, File file, int lineNumber) {
        super(debugger);
        Assert.isNotNull(file);
        this.file = file;
        this.lineNumber = lineNumber;
    }

    @Override
    public String getOutgoing() {
        if (file != null) {
            return makeCommand(AbstractDebuggerCommand.CMD_IGNORE_THROWN_EXCEPTION_AT, sequence,
                    StringUtils.join("|", FileUtils.getFileAbsolutePath(file), this.lineNumber));
        } else {
            //Bulk-creation
            Collection<String> ignoreThrownExceptions = PyExceptionBreakPointManager.getInstance().
                    ignoreCaughtExceptionsWhenThrownFrom.getIgnoreThrownExceptions();
            return makeCommand(AbstractDebuggerCommand.CMD_IGNORE_THROWN_EXCEPTION_AT, sequence,
                    "REPLACE:" + StringUtils.join("||", ignoreThrownExceptions));
        }
    }
}
