/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.pythonpathconf;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.UIConstants;

/**
 * @author fabioz
 *
 */
public final class PythonSelectionLibrariesDialog implements Runnable {

    /**
     * @author fabioz
     *
     */
    private static final class LabelProvider implements ILabelProvider {
        @Override
        public Image getImage(Object element) {
            return PydevPlugin.getImageCache().get(UIConstants.LIB_SYSTEM);
        }

        @Override
        public String getText(Object element) {
            return element.toString();
        }

        @Override
        public void addListener(ILabelProviderListener listener) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean isLabelProperty(Object element, String property) {
            return true;
        }

        @Override
        public void removeListener(ILabelProviderListener listener) {
        }
    }

    /**
     * @author fabioz
     *
     */
    private static final class ContentProvider implements IStructuredContentProvider {
        @Override
        @SuppressWarnings("unchecked")
        public Object[] getElements(Object inputElement) {
            List<String> elements = (List<String>) inputElement;
            return elements.toArray(new String[0]);
        }

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

    private final List<String> initialSelection;
    private final List<String> allItems;
    private final ArrayList<String> selection = new ArrayList<String>();
    private boolean result;
    private boolean addSelectAllNotInWorkspace;

    /**
     */
    public PythonSelectionLibrariesDialog(List<String> initialSelection, List<String> allItems,
            boolean addSelectAllNotInWorkspace) {
        this.initialSelection = initialSelection;
        this.allItems = allItems;
        this.addSelectAllNotInWorkspace = addSelectAllNotInWorkspace;
    }

    private String msg = "Select the folders to be added to the SYSTEM pythonpath!\n"
            + "\n"
            + "IMPORTANT: The folders for your PROJECTS should NOT be added here, but in your project configuration.\n\n"
            + "Check:http://pydev.org/manual_101_interpreter.html for more details.";

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public void run() {

        PyListSelectionDialog dialog = new PyListSelectionDialog(Display.getDefault().getActiveShell(), allItems,
                new ContentProvider(), new LabelProvider(), msg, addSelectAllNotInWorkspace);
        dialog.setInitialSelections(initialSelection.toArray(new String[0]));
        int i = dialog.open();
        if (i == Window.OK) {
            result = true;
            Object[] result = dialog.getResult();
            selection.clear();
            for (Object string : result) {
                selection.add((String) string);
            }
        } else {
            result = false;

        }

    }

    /**
     * @return
     */
    public boolean getOkResult() {
        return result;
    }

    /**
     * @return
     */
    public ArrayList<String> getSelection() {
        return selection;
    }
}