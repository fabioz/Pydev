package org.python.pydev.debug.model.remote;

import java.io.File;

import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.shared_core.io.FileUtils;

public class AddIgnoreThrownExceptionIn extends AbstractDebuggerCommand {

    private File file;
    private int lineNumber;

    public AddIgnoreThrownExceptionIn(AbstractDebugTarget debugger, File file, int lineNumber) {
        super(debugger);
        this.file = file;
        this.lineNumber = lineNumber;
    }

    @Override
    public String getOutgoing() {
        return makeCommand(AbstractDebuggerCommand.CMD_IGNORE_THROWN_EXCEPTION_AT, sequence,
                org.python.pydev.shared_core.string.StringUtils.join("|",
                        FileUtils.getFileAbsolutePath(file), this.lineNumber));
    }
}
