/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole.env;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.NotConfiguredInterpreterException;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.model.PyStackFrameConsole;
import org.python.pydev.debug.newconsole.prefs.InteractiveConsolePrefs;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * Helper to choose which kind of jython run will it be.
 */
final class ChooseProcessTypeDialog extends Dialog {

    private Button checkboxForCurrentEditor;

    private Button checkboxPython;

    private Button checkboxPythonDebug;

    private Button checkboxJython;

    private Button checkboxIronpython;

    private Button checkboxJythonEclipse;

    private PyEdit activeEditor;

    private IInterpreterManager interpreterManager;

    private List<IPythonNature> natures = new ArrayList<IPythonNature>();

    private PyStackFrame selectedFrame;

    private Link link;

    ChooseProcessTypeDialog(Shell shell, PyEdit activeEditor) {
        super(shell);
        this.activeEditor = activeEditor;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);

        boolean debugButtonCreated = false;
        if (getSuspendedFrame() != null) {
            // when debugger is running and valid frame is selected then
            // displaying debug console as first option
            createDebugButton(area);
            debugButtonCreated = true;
        }

        checkboxForCurrentEditor = new Button(area, SWT.RADIO);
        checkboxForCurrentEditor
                .setToolTipText("Creates a console with the PYTHONPATH used by the current editor (and Jython/Python/IronPython depending on the project type).");
        configureEditorButton();

        checkboxPython = new Button(area, SWT.RADIO);
        checkboxPython
                .setToolTipText("Creates a Python console with the PYTHONPATH containing all the python projects in the workspace.");
        configureButton(checkboxPython, "Python", PydevPlugin.getPythonInterpreterManager());

        checkboxJython = new Button(area, SWT.RADIO);
        checkboxJython
                .setToolTipText("Creates a Jython console with the PYTHONPATH containing all the python projects in the workspace.");
        configureButton(checkboxJython, "Jython", PydevPlugin.getJythonInterpreterManager());

        checkboxIronpython = new Button(area, SWT.RADIO);
        checkboxIronpython
                .setToolTipText("Creates an IronPython console with the PYTHONPATH containing all the python projects in the workspace.");
        configureButton(checkboxIronpython, "IronPython", PydevPlugin.getIronpythonInterpreterManager());

        checkboxJythonEclipse = new Button(area, SWT.RADIO);
        checkboxJythonEclipse
                .setToolTipText("Creates a Jython console using the running Eclipse environment (can potentially halt Eclipse depending on what's done).");
        configureButton(checkboxJythonEclipse, "Jython using VM running Eclipse", new JythonEclipseInterpreterManager());

        if (!debugButtonCreated) {
            createDebugButton(area);
        }

        link = new Link(area, SWT.LEFT | SWT.WRAP);
        link.setText("<a>Configure interactive console preferences.</a>\n"
                + "I.e.: send contents to console on creation,\n" + "connect to variables view, initial commands, etc.");

        link.addSelectionListener(new SelectionListener() {
            public void widgetSelected(SelectionEvent e) {
                PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null,
                        InteractiveConsolePrefs.PREFERENCES_ID, null, null);
                dialog.open();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
        });

        return area;
    }

    private void createDebugButton(Composite area) {
        checkboxPythonDebug = new Button(area, SWT.RADIO);
        checkboxPythonDebug
                .setToolTipText("Creates a Python debug console associated with the frame selected in the debug view");
        configureDebugButton();
    }

    /**
     * Configures a button related to a given interpreter manager.
     */
    private void configureButton(Button checkBox, String python, IInterpreterManager interpreterManager) {
        boolean enabled = false;
        String text;
        try {
            if (interpreterManager.getDefaultInterpreterInfo(false) != null) {
                text = python + " console";
                enabled = true;
            } else {
                throw new NotConfiguredInterpreterException();
            }
        } catch (MisconfigurationException e) {
            text = "Unable to create console for " + python + " (interpreter not configured)";
        }
        checkBox.setText(text);
        checkBox.setEnabled(enabled);
    }

    /**
     * Configures a button related to an editor.
     * @throws MisconfigurationException 
     */
    private void configureEditorButton() {
        boolean enabled = false;
        String text;
        try {
            if (this.activeEditor != null) {
                IPythonNature nature = this.activeEditor.getPythonNature();
                if (nature != null) {

                    if (nature.getRelatedInterpreterManager().getDefaultInterpreterInfo(false) != null) {
                        text = "Console for currently active editor";
                        enabled = true;
                    } else {
                        throw new NotConfiguredInterpreterException();
                    }
                } else {
                    text = "No python nature configured for the current editor";
                }
            } else {
                text = "Unable to create console for current editor (no active editor)";
            }
        } catch (MisconfigurationException e) {
            //expected
            text = "Unable to create console for current editor (interpreter not configured for the editor)";
        }
        checkboxForCurrentEditor.setText(text);
        checkboxForCurrentEditor.setEnabled(enabled);
    }

    /**
     * Enable/Disable Pydev debug console radio button
     * 
     * @param checkBox
     * @param python
     * @param interpreterManager
     */
    private void configureDebugButton() {
        boolean enabled = false;
        String text = "PyDev Debug Console (Start the debugger and select the valid frame)";
        if (getSuspendedFrame() != null) {
            enabled = true;
            text = "PyDev Debug Console";
        }
        checkboxPythonDebug.setText(text);
        checkboxPythonDebug.setEnabled(enabled);
    }

    /**
     * Determine if any frame is selected in the Launch view
     * 
     * @return
     */
    private PyStackFrame getSuspendedFrame() {
        IAdaptable context = DebugUITools.getDebugContext();
        if (context instanceof PyStackFrame) {
            if (context instanceof PyStackFrameConsole) {
                // We already have a real console opened on the Interactive Console, we don't support
                // opening a special debug console on it
                return null;
            }
            return (PyStackFrame) context;
        }
        return null;
    }

    /**
     * Sets the internal pythonpath chosen.
     */
    @Override
    protected void okPressed() {
        setSelectedFrame(null);
        if (checkboxForCurrentEditor.isEnabled() && checkboxForCurrentEditor.getSelection()) {
            IProject project = this.activeEditor.getProject();
            PythonNature nature = PythonNature.getPythonNature(project);
            natures.add(nature);
            IInterpreterManager relatedInterpreterManager = nature.getRelatedInterpreterManager();
            this.interpreterManager = relatedInterpreterManager;

        } else if (checkboxPython.isEnabled() && checkboxPython.getSelection()) {
            this.interpreterManager = PydevPlugin.getPythonInterpreterManager();

        } else if (checkboxPythonDebug.isEnabled() && checkboxPythonDebug.getSelection()) {
            setSelectedFrame(getSuspendedFrame());
            this.interpreterManager = PydevPlugin.getPythonInterpreterManager();

        } else if (checkboxJython.isEnabled() && checkboxJython.getSelection()) {
            this.interpreterManager = PydevPlugin.getJythonInterpreterManager();

        } else if (checkboxJythonEclipse.isEnabled() && checkboxJythonEclipse.getSelection()) {
            this.interpreterManager = new JythonEclipseInterpreterManager();

        } else if (checkboxIronpython.isEnabled() && checkboxIronpython.getSelection()) {
            this.interpreterManager = PydevPlugin.getIronpythonInterpreterManager();

        }

        super.okPressed();
    }

    /**
     * @return the pythonpath/nature to be used or null if not configured (note that the nature can be null)
     */
    public Tuple<Collection<String>, IPythonNature> getPythonpathAndNature(IInterpreterInfo interpreter) {

        if (this.interpreterManager != null) {
            if (this.natures.size() == 1) {
                //chosen for the editor
                IPythonNature nature = this.natures.get(0);
                return new Tuple<Collection<String>, IPythonNature>(new ArrayList<String>(nature.getPythonPathNature()
                        .getCompleteProjectPythonPath(interpreter, this.interpreterManager)), nature);

            }

            // collect all the python path (no duplicates, hence a set)
            HashSet<String> pythonpath = new LinkedHashSet<String>();

            // Add all the paths in the interpreter (note: it's important that this goes before the 
            // path for the other natures so that if we have something as IPython, the one used will
            // be the one from the interpreter).
            pythonpath.addAll(interpreter.getPythonPath());

            //we need to get the natures matching the one selected in all the projects.
            IWorkspace w = ResourcesPlugin.getWorkspace();
            for (IProject p : w.getRoot().getProjects()) {
                PythonNature nature = PythonNature.getPythonNature(p);
                try {
                    if (nature != null) {
                        if (nature.getRelatedInterpreterManager() == this.interpreterManager) {
                            natures.add(nature);
                            List<String> completeProjectPythonPath = nature.getPythonPathNature()
                                    .getCompleteProjectPythonPath(interpreter, this.interpreterManager);
                            if (completeProjectPythonPath != null) {
                                pythonpath.addAll(completeProjectPythonPath);
                            } else {
                                Log.logInfo("Unable to get pythonpath for project: " + nature.getProject()
                                        + " (initialization not finished).");
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.log(e);
                }
            }
            return new Tuple<Collection<String>, IPythonNature>(pythonpath, null);
        }

        return null;
    }

    public IInterpreterManager getInterpreterManager() {
        return this.interpreterManager;
    }

    public List<IPythonNature> getNatures() {
        return natures;
    }

    /**
     * Return the selected frame
     * 
     * @return
     */
    public PyStackFrame getSelectedFrame() {
        return selectedFrame;
    }

    /**
     * Set the selectedFrame
     * 
     * @param selectedFrame
     */
    public void setSelectedFrame(PyStackFrame selectedFrame) {
        this.selectedFrame = selectedFrame;
    }
}