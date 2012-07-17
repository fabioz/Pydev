/**
 * Aptana Studio
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the GNU Public License (GPL) v3 (with exceptions).
 * Please see the license.html included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.uiutils;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class UIUtils {

    public static Display getDisplay() {
        return PlatformUI.getWorkbench().getDisplay();
    }

    public static Shell getActiveShell() {
        Shell shell = getDisplay().getActiveShell();
        if (shell == null) {
            IWorkbenchWindow window = getActiveWorkbenchWindow();
            if (window != null) {
                shell = window.getShell();
            }
        }
        return shell;
    }

    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    }

    public static IEditorPart getActiveEditor() {
        IWorkbenchPage workbenchPage = getActivePage();
        if (workbenchPage == null) {
            return null;
        }
        return workbenchPage.getActiveEditor();
    }

    public static IWorkbenchPart getActivePart() {
        IWorkbenchPage workbenchPage = getActivePage();
        if (workbenchPage == null) {
            return null;
        }
        return workbenchPage.getActivePart();
    }

    public static IWorkbenchPage getActivePage() {
        IWorkbenchWindow workbench = getActiveWorkbenchWindow();
        if (workbench == null) {
            return null;
        }
        return workbench.getActivePage();
    }

    public static Display getStandardDisplay() {
        Display display;
        display = Display.getCurrent();
        if (display == null) {
            display = Display.getDefault();
        }
        return display;
    }

}
