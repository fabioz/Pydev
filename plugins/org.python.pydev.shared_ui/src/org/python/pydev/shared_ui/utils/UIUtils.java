/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.utils;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.python.pydev.shared_core.log.Log;

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
        if (!PlatformUI.isWorkbenchRunning()) {
            return null;
        }
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench == null) {
            return null;
        }
        return workbench.getActiveWorkbenchWindow();
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

    public static ViewPart getView(String viewId, boolean forceVisible) {
        IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        try {
            if (workbenchWindow == null) {
                return null;
            }
            IWorkbenchPage page = workbenchWindow.getActivePage();
            if (forceVisible) {
                return (ViewPart) page.showView(viewId, null, IWorkbenchPage.VIEW_VISIBLE);

            } else {
                IViewReference viewReference = page.findViewReference(viewId);
                if (viewReference != null) {
                    //if it's there, return it (but don't restore it if it's still not there).
                    //when made visible, it'll handle things properly later on.
                    return (ViewPart) viewReference.getView(false);
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return null;

    }

}
