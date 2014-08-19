/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.propertypages;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommand;
import org.eclipse.ui.commands.ICommandManager;
import org.eclipse.ui.commands.IKeySequenceBinding;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.PyBreakpoint;
import org.python.pydev.debug.model.PyDebugModelPresentation;

public class PythonBreakpointPage extends PropertyPage {

    private Button fEnableConditionButton;
    private BreakpointConditionEditor fConditionEditor;
    //private Label fConditionIsTrue;
    //private Button fConditionHasChanged;
    //private Label fSuspendWhenLabel;

    protected Button fEnabledButton;
    protected Button fHitCountButton;
    protected Button fSuspendThreadButton;
    protected Button fSuspendVMButton;
    protected Text fHitCountText;

    protected List<String> fErrorMessages = new ArrayList<String>();

    /**
     * Attribute used to indicate that a breakpoint should be deleted
     * when cancel is pressed.
     */
    public static final String ATTR_DELETE_ON_CANCEL = PyDebugModelPresentation.PY_DEBUG_MODEL_ID
            + ".ATTR_DELETE_ON_CANCEL"; //$NON-NLS-1$

    @Override
    public String getTitle() {
        return "Line Breakpoint";
    }

    @Override
    protected Control createContents(Composite parent) {
        noDefaultAndApplyButton();
        Composite mainComposite = createComposite(parent, 1);
        createLabels(mainComposite);
        try {
            createEnabledButton(mainComposite);
            //createHitCountEditor(mainComposite);
            createTypeSpecificEditors(mainComposite);
            //createSuspendPolicyEditor(mainComposite); // Suspend policy is considered uncommon. Add it last.
        } catch (CoreException e) {
            PydevDebugPlugin.log(IStatus.ERROR, e.getLocalizedMessage(), e);
        }
        setValid(true);
        // if this breakpoint is being created, change the shell title to indicate 'creation'
        try {
            if (getBreakpoint().getMarker().getAttribute(ATTR_DELETE_ON_CANCEL) != null) {
                getShell().addShellListener(new ShellListener() {
                    public void shellActivated(ShellEvent e) {
                        Shell shell = (Shell) e.getSource();
                        shell.setText(MessageFormat.format(
                                "Create Breakpoint for {0}", new String[] { getName(getBreakpoint()) })); //$NON-NLS-1$
                        shell.removeShellListener(this);
                    }

                    public void shellClosed(ShellEvent e) {
                    }

                    public void shellDeactivated(ShellEvent e) {
                    }

                    public void shellDeiconified(ShellEvent e) {
                    }

                    public void shellIconified(ShellEvent e) {
                    }
                });
            }
        } catch (CoreException e) {
        }
        return mainComposite;
    }

    /**
     * Creates a fully configured composite with the given number of columns
     * @param parent
     * @param numColumns
     * @return the configured composite
     */
    protected Composite createComposite(Composite parent, int numColumns) {
        Composite composit = new Composite(parent, SWT.NONE);
        composit.setFont(parent.getFont());
        GridLayout layout = new GridLayout();
        layout.numColumns = numColumns;
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composit.setLayout(layout);
        composit.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return composit;
    }

    /**
     * Creates a fully configured label with the given text.
     * @param parent the parent composite
     * @param text the test of the returned label
     * @return a fully configured label
     */
    protected Label createLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setFont(parent.getFont());
        label.setLayoutData(new GridData());
        return label;
    }

    /**
     * Creates the labels displayed for the breakpoint.
     * @param parent
     */
    protected void createLabels(Composite parent) {
        PyBreakpoint breakpoint = (PyBreakpoint) getElement();
        Composite labelComposite = createComposite(parent, 2);
        String typeName = breakpoint.getFile();
        if (typeName != null) {
            createLabel(labelComposite, "File"); //$NON-NLS-1$
            createLabel(labelComposite, typeName);
        }
        createTypeSpecificLabels(labelComposite);
    }

    /**
     * Creates the button to toggle enablement of the breakpoint
     * @param parent
     * @throws CoreException
     */
    protected void createEnabledButton(Composite parent) throws CoreException {
        fEnabledButton = createCheckButton(parent, "&Enabled"); //$NON-NLS-1$
        fEnabledButton.setSelection(getBreakpoint().isEnabled());
    }

    /**
     * Allows subclasses to add type specific labels to the common Java
     * breakpoint page.
     * @param parent
     */
    protected void createTypeSpecificLabels(Composite parent) {
        //         Line number
        PyBreakpoint breakpoint = getBreakpoint();
        StringBuffer lineNumber = new StringBuffer(4);
        try {
            int lNumber = breakpoint.getLineNumber();
            if (lNumber > 0) {
                lineNumber.append(lNumber);
            }
        } catch (CoreException ce) {
            PydevDebugPlugin.log(IStatus.ERROR, ce.getLocalizedMessage(), ce);
        }
        if (lineNumber.length() > 0) {
            createLabel(parent, "&Line Number:");
            createLabel(parent, lineNumber.toString());
        }
        // Member
        /*
        try {
            IMember member = BreakpointUtils.getMember(breakpoint);
            if (member == null) {
                return;
            }
            String label = PropertyPageMessages.JavaLineBreakpointPage_3; //$NON-NLS-1$
            String memberName = fJavaLabelProvider.getText(member);
            if (breakpoint instanceof IJavaMethodBreakpoint) {
                label = PropertyPageMessages.JavaLineBreakpointPage_4; //$NON-NLS-1$
            } else if (breakpoint instanceof IJavaWatchpoint) {
                label = PropertyPageMessages.JavaLineBreakpointPage_5; //$NON-NLS-1$
            }
            createLabel(parent, label);
            createLabel(parent, memberName);
        } catch (CoreException exception) {
            PydevDebugPlugin.log(IStatus.ERROR,e.getLocalizedMessage(),exception);
        }*/
    }

    /**
    * Allows subclasses to add type specific editors to the common Java
    * breakpoint page.
    * @param parent
    */
    protected void createTypeSpecificEditors(Composite parent) throws CoreException {
        PyBreakpoint breakpoint = getBreakpoint();
        if (breakpoint.supportsCondition()) {
            createConditionEditor(parent);
        }
    }

    /**
     * Creates a fully configured check button with the given text.
     * @param parent the parent composite
     * @param text the label of the returned check button
     * @return a fully configured check button
     */
    protected Button createCheckButton(Composite parent, String text) {
        Button button = new Button(parent, SWT.CHECK | SWT.LEFT);
        button.setText(text);
        button.setFont(parent.getFont());
        button.setLayoutData(new GridData());
        return button;
    }

    /**
     * Returns the breakpoint that this preference page configures
     * @return the breakpoint this page configures
     */
    protected PyBreakpoint getBreakpoint() {
        return (PyBreakpoint) getElement();
    }

    /**
     * Returns the name of the given element.
     * 
     * @param element
     *            the element
     * @return the name of the element
     */
    private String getName(IAdaptable element) {
        IWorkbenchAdapter adapter = (IWorkbenchAdapter) element.getAdapter(IWorkbenchAdapter.class);
        if (adapter != null) {
            return adapter.getLabel(element);
        }
        return "";//$NON-NLS-1$
    }

    /**
     * Store the breakpoint properties.
     * @see org.eclipse.jface.preference.IPreferencePage#performOk()
     */
    @Override
    public boolean performOk() {
        IWorkspaceRunnable wr = new IWorkspaceRunnable() {
            public void run(IProgressMonitor monitor) throws CoreException {
                PyBreakpoint breakpoint = getBreakpoint();
                boolean delOnCancel = breakpoint.getMarker().getAttribute(ATTR_DELETE_ON_CANCEL) != null;
                if (delOnCancel) {
                    // if this breakpoint is being created, remove the "delete on cancel" attribute
                    // and register with the breakpoint manager
                    breakpoint.getMarker().setAttribute(ATTR_DELETE_ON_CANCEL, (String) null);
                    breakpoint.setRegistered(true);
                }
                doStore();
            }
        };
        try {
            ResourcesPlugin.getWorkspace().run(wr, null, 0, null);
        } catch (CoreException e) {
            PydevDebugPlugin.errorDialog("An exception occurred while saving breakpoint properties.", e); //$NON-NLS-1$
            PydevDebugPlugin.log(IStatus.ERROR, e.getLocalizedMessage(), e);
        }
        return super.performOk();
    }

    /**
     * Check to see if the breakpoint should be deleted.
     */
    @Override
    public boolean performCancel() {
        try {
            if (getBreakpoint().getMarker().getAttribute(ATTR_DELETE_ON_CANCEL) != null) {
                // if this breakpoint is being created, delete on cancel
                getBreakpoint().delete();
            }
        } catch (CoreException e) {
            PydevDebugPlugin.errorDialog("Unable to cancel breakpoint creation", e); //$NON-NLS-1$
        }
        return super.performCancel();
    }

    /**
     * Stores the values configured in this page. This method
     * should be called from within a workspace runnable to
     * reduce the number of resource deltas.
     */
    protected void doStore() throws CoreException {
        PyBreakpoint breakpoint = getBreakpoint();
        storeEnabled(breakpoint);

        if (fConditionEditor != null) {
            boolean enableCondition = fEnableConditionButton.getSelection();
            String condition = fConditionEditor.getCondition();
            if (breakpoint.isConditionEnabled() != enableCondition) {
                breakpoint.setConditionEnabled(enableCondition);
            }
            if (!condition.equals(breakpoint.getCondition())) {
                breakpoint.setCondition(condition);
            }
        }
    }

    /**
     * Stores the value of the enabled state in the breakpoint.
     * @param breakpoint the breakpoint to update
     * @throws CoreException if an exception occurs while setting
     *  the enabled state
     */
    private void storeEnabled(PyBreakpoint breakpoint) throws CoreException {
        boolean enabled = fEnabledButton.getSelection();
        breakpoint.setEnabled(enabled);
    }

    /**
     * Creates the controls that allow the user to specify the breakpoint's
     * condition
     * @param parent the composite in which the condition editor should be created
     * @throws CoreException if an exception occurs accessing the breakpoint
     */
    private void createConditionEditor(Composite parent) throws CoreException {
        PyBreakpoint breakpoint = getBreakpoint();

        String label = null;
        ICommandManager commandManager = PlatformUI.getWorkbench().getCommandSupport().getCommandManager();
        ICommand command = commandManager.getCommand("org.eclipse.ui.edit.text.contentAssist.proposals"); //$NON-NLS-1$
        if (command != null) {
            List keyBindings = command.getKeySequenceBindings();
            if (keyBindings != null && keyBindings.size() > 0) {
                IKeySequenceBinding binding = (IKeySequenceBinding) keyBindings.get(0);
                label = MessageFormat.format("E&nable Condition", new String[] { binding.getKeySequence().format() }); //$NON-NLS-1$
            }
        }

        if (label == null) {
            label = "E&nable Condition (code assist not available)"; //$NON-NLS-1$
        }
        Composite conditionComposite = new Group(parent, SWT.NONE);
        conditionComposite.setFont(parent.getFont());
        conditionComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        conditionComposite.setLayout(new GridLayout());
        fEnableConditionButton = createCheckButton(conditionComposite, label);
        fEnableConditionButton.setSelection(breakpoint.isConditionEnabled());
        fEnableConditionButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setConditionEnabled(fEnableConditionButton.getSelection());
            }
        });

        fConditionEditor = new BreakpointConditionEditor(conditionComposite, this);

        //fSuspendWhenLabel= createLabel(conditionComposite, "Suspend when:");
        //fConditionIsTrue= createRadioButton(conditionComposite, "condition is \'tr&ue\'"); 
        //fConditionIsTrue= createLabel(conditionComposite, "condition is \'tr&ue\'");
        //fConditionHasChanged= createRadioButton(conditionComposite, "value of condition ch&anges");
        //        if (breakpoint.isConditionSuspendOnTrue()) {
        //            fConditionIsTrue.setSelection(true);
        //        } else {
        //            fConditionHasChanged.setSelection(true);
        //        }
        setConditionEnabled(fEnableConditionButton.getSelection());
    }

    @Override
    public void dispose() {
        super.dispose();
        fConditionEditor.dispose();
    }

    /**
     * Sets the enabled state of the condition editing controls.
     * @param enabled
     */
    private void setConditionEnabled(boolean enabled) {
        fConditionEditor.setEnabled(enabled);
        //        fSuspendWhenLabel.setEnabled(enabled);
        //        fConditionIsTrue.setEnabled(enabled);
        //fConditionHasChanged.setEnabled(enabled);
    }

    /**
     * Overridden here to increase visibility
     * @see org.eclipse.jface.dialogs.DialogPage#convertHeightInCharsToPixels(int)
     */
    @Override
    public int convertHeightInCharsToPixels(int chars) {
        return super.convertHeightInCharsToPixels(chars);
    }

    /**
     * Overridden here to increase visibility
     * @see org.eclipse.jface.dialogs.DialogPage#convertWidthInCharsToPixels(int)
     */
    @Override
    public int convertWidthInCharsToPixels(int chars) {
        return super.convertWidthInCharsToPixels(chars);
    }

    /**
     * Adds the given error message to the errors currently displayed on this page.
     * The page displays the most recently added error message.
     * Clients should retain messages that are passed into this method as the
     * message should later be passed into removeErrorMessage(String) to clear the error.
     * This method should be used instead of setErrorMessage(String).
     * @param message the error message to display on this page.
     */
    public void addErrorMessage(String message) {
        if (message == null) {
            return;
        }
        fErrorMessages.remove(message);
        fErrorMessages.add(message);
        setErrorMessage(message);
        setValid(false);
    }

    /**
     * Removes the given error message from the errors currently displayed on this page.
     * When an error message is removed, the page displays the error that was added
     * before the given message. This is akin to popping the message from a stack.
     * Clients should call this method instead of setErrorMessage(null).
     * @param message the error message to clear
     */
    public void removeErrorMessage(String message) {
        fErrorMessages.remove(message);
        if (fErrorMessages.isEmpty()) {
            setErrorMessage(null);
            setValid(true);
        } else {
            setErrorMessage(fErrorMessages.get(fErrorMessages.size() - 1));
        }
    }

    /**
     * Creates a fully configured radio button with the given text.
     * @param parent the parent composite
     * @param text the label of the returned radio button
     * @return a fully configured radio button
     */
    protected Button createRadioButton(Composite parent, String text) {
        Button button = new Button(parent, SWT.RADIO | SWT.LEFT);
        button.setText(text);
        button.setFont(parent.getFont());
        button.setLayoutData(new GridData());
        return button;
    }
}
