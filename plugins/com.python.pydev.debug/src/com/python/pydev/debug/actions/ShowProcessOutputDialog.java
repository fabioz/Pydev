package com.python.pydev.debug.actions;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.shared_core.io.ThreadStreamReader;

/**
 * This is the window used to handle a process. Currently specific to google app engine (could be more customizable
 * if needed).
 */
public class ShowProcessOutputDialog extends Dialog {

    private static final int NUMBER_OF_COLUMNS = 2;

    protected Text output;

    //only while running
    private ThreadStreamReader err;
    private ThreadStreamReader std;
    private volatile boolean disposed = false;

    /**
     * We need to set the shell style to be resizable.
     * @param o1 
     */
    public ShowProcessOutputDialog(Shell parentShell, final Process process) {
        super(parentShell);
        setShellStyle(getShellStyle() | SWT.RESIZE);

        std = new ThreadStreamReader(process.getInputStream());
        err = new ThreadStreamReader(process.getErrorStream());

        std.start();
        err.start();

        parentShell.getDisplay().timerExec(250, new Runnable() {

            public void run() {
                // Will run in UI thread, so, no need for locking.
                if (disposed) {
                    std.stopGettingOutput();
                    err.stopGettingOutput();
                    return;
                }
                try {
                    int exitValue = process.exitValue();
                    String stdout = std.getAndClearContents();
                    String stderr = err.getAndClearContents();
                    output.append(stdout);
                    output.append(stderr);
                    output.append("Process finished with exitValue: " + exitValue);
                } catch (Exception e) {
                    //still hasn't exited: get outputs
                    String stdout = std.getAndClearContents();
                    String stderr = err.getAndClearContents();
                    output.append(stdout);
                    output.append(stderr);
                    Display.getCurrent().timerExec(250, this);
                }
            }
        });

    }

    @Override
    protected void configureShell(final Shell shell) {
        super.configureShell(shell);
        shell.setText("Process output");
    }

    /**
     * Create the dialog contents
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite top = (Composite) super.createDialogArea(parent);

        Composite composite = new Composite(top, SWT.None);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        composite.setLayout(new GridLayout(NUMBER_OF_COLUMNS, false));

        //--- main output
        output = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        gridData.horizontalSpan = NUMBER_OF_COLUMNS;
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        output.setLayoutData(gridData);

        return top;
    }

    @Override
    protected void constrainShellSize() {
        Shell shell = getShell();
        shell.setSize(640, 480);

        // Move the dialog to the center of the top level shell.
        Rectangle shellBounds = getParentShell().getBounds();
        shell.setLocation(shellBounds.x + (shellBounds.width - 640) / 2,
                shellBounds.y + (shellBounds.height - 480) / 2);
        super.constrainShellSize();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Close", true);
    }

    @Override
    public int open() {
        try {
            return super.open();
        } finally {
            disposed = true;
            std.stopGettingOutput();
            err.stopGettingOutput();
        }
    }
}
