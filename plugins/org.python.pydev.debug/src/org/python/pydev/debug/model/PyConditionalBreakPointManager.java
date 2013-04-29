package org.python.pydev.debug.model;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Handles any exception raised while evaluating the conditional breakpoint
 *
 * @author hussain.bohra
 *
 */
public class PyConditionalBreakPointManager {

    private static final String DELIMETER = "\t";
    private static final int ERROR_CODE = 1;
    private static final String PYTHON_TRACEBACK = "Traceback (most recent call last):";
    private static final String SHELL_TEXT = "Conditional Breakpoint";
    private static final String PID = "Error";
    private static final String ERROR_MESSAGE = "An exception has occurred when evaluating a conditional breakpoint";
    private static final String TITLE = "Error in executing conditional breakpoint";

    private static PyConditionalBreakPointManager pyConditionalBreakPointManager;

    /**
     * Singleton: private constructor.
     */
    private PyConditionalBreakPointManager() {

    }

    public static synchronized PyConditionalBreakPointManager getInstance() {
        if (pyConditionalBreakPointManager == null) {
            pyConditionalBreakPointManager = new PyConditionalBreakPointManager();
        }
        return pyConditionalBreakPointManager;
    }

    /**
     * Represents Python stacktrace
     *
     * @author hussain.bohra
     *
     */
    @SuppressWarnings("unused")
    static class ExceptionStackTrace {
        private AbstractDebugTarget target;
        private PyThread in_thread;
        private String filename;
        private int lineNo;
        private String methodName;
        private String methodObj;

        public ExceptionStackTrace(AbstractDebugTarget target,
                PyThread in_thread, String filename, int lineNo,
                String methodName, String methodObj) {
            this.target = target;
            this.in_thread = in_thread;
            this.filename = filename;
            this.lineNo = lineNo;
            this.methodName = methodName;
            this.methodObj = methodObj;
        }

        @Override
        public String toString() {
            return "\tFile " + filename + "\n line " + lineNo + ", in"
                    + methodName + ", " + methodObj;
        }
    }

    /**
     * Display an error dialog and traceback raised while evaluating the
     * conditional breakpoint.
     *
     * @param payload
     *      would contain exception_type + "\t" + stacktrace_xml
     */
    public void handleBreakpointException(final AbstractDebugTarget target,
            final String payload) {
        if (payload.indexOf(DELIMETER) > 0) {
            // exceptionDetailList = ["exceptionType", "<frame>traceback</frame>"]
            final String[] exceptionDetailList = payload.split(DELIMETER);
            final Display display = Display.getDefault();
            display.asyncExec(new Runnable() {
                public void run() {
                    // adding exception detail with error message
                    String errorMessage = ERROR_MESSAGE + "\n"
                            + exceptionDetailList[0];
                    List<ExceptionStackTrace> exceptionStackTraceList =
                        new ArrayList<ExceptionStackTrace>();
                    Shell shell = new Shell(display);
                    shell.setText(SHELL_TEXT);
                    MultiStatus multiStatusInfo = new MultiStatus(PID,
                            ERROR_CODE, errorMessage, null);
                    multiStatusInfo.add(new Status(IStatus.ERROR, PID,
                            ERROR_CODE, PYTHON_TRACEBACK, null));
                    try {
                        // Parse traceback xml
                        exceptionStackTraceList = XMLUtils
                                .getExceptionStackTrace(target,
                                        exceptionDetailList[1]);
                    } catch (CoreException e) {
                        e.printStackTrace();
                    }
                    // Adding traceback details to multiStatusInfo
                    for (ExceptionStackTrace exceptionStackTrace : exceptionStackTraceList) {
                        multiStatusInfo.add(new Status(IStatus.ERROR, PID,
                                ERROR_CODE, exceptionStackTrace.toString(),
                                null));
                    }
                    ErrorDialog.openError(shell, TITLE, null, multiStatusInfo);
                }
            });
        }
    }

}
