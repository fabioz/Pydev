/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.debug.model.PyBreakpoint;

/**
 * Action for editing the breakpoint properties.
 */
public class PythonBreakpointPropertiesRulerAction extends AbstractBreakpointRulerAction implements IAction {

    public static class PythonBreakpointDialogAction extends PropertyDialogAction {

        public PythonBreakpointDialogAction(IShellProvider shell, ISelectionProvider provider) {
            super(shell, provider);
        }

        @Override
        public void run() {
            PreferenceDialog dialog = createDialog();
            dialog.getShell().setText("Breakpoint Properties");
            if (dialog != null) {
                dialog.open();
            }
        }
    }

    public PythonBreakpointPropertiesRulerAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
        setInfo(rulerInfo);
        setTextEditor(editor);
        setText("Breakpoint &Properties...");
    }

    /**
     * @throws CoreException
     * @see Action#run()
     */
    @Override
    public void run() {
        IBreakpoint breakPoint = getBreakpoint();
        if (breakPoint != null) {
            PythonBreakpointDialogAction action = new PythonBreakpointDialogAction(getTextEditor().getEditorSite(),
                    new ISelectionProvider() {
                        public void addSelectionChangedListener(ISelectionChangedListener listener) {
                        }

                        public ISelection getSelection() {
                            return new StructuredSelection(getBreakpoint());
                        }

                        public void removeSelectionChangedListener(ISelectionChangedListener listener) {
                        }

                        public void setSelection(ISelection selection) {
                        }
                    });
            action.run();
        }
    }

    public void update() {
        IBreakpoint breakpoint = getBreakpointFromLastLineOfActivityInCurrentEditor();
        if (breakpoint == null || !(breakpoint instanceof PyBreakpoint)) {
            setBreakpoint(null);
            setEnabled(false);
        } else {
            setBreakpoint(breakpoint);
            setEnabled(true);
        }
        setText("Breakpoint &Properties...");
    }

}
