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
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

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
        public Image getImage(Object element) {
            return PydevPlugin.getImageCache().get(UIConstants.LIB_SYSTEM);
        }

        public String getText(Object element) {
            return element.toString();
        }

        public void addListener(ILabelProviderListener listener) {
        }

        public void dispose() {
        }

        public boolean isLabelProperty(Object element, String property) {
            return true;
        }

        public void removeListener(ILabelProviderListener listener) {
        }
    }

    /**
     * @author fabioz
     *
     */
    private static final class ContentProvider implements IStructuredContentProvider {
        @SuppressWarnings("unchecked")
        public Object[] getElements(Object inputElement) {
            List<String> elements = (List<String>) inputElement;
            return elements.toArray(new String[0]);
        }

        public void dispose() {
        }

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        }
    }

    private final List<String> initialSelection;
    private final List<String> allItems;
    private final ArrayList<String> selection = new ArrayList<String>();
    private boolean result;

    /**
     */
    public PythonSelectionLibrariesDialog(List<String> initialSelection, List<String> allItems) {
        this.initialSelection = initialSelection;
        this.allItems = allItems;
    }

    private String msg = "Select the folders to be added to the SYSTEM pythonpath!\n" + "\n"
            + "IMPORTANT: The folders for your PROJECTS should NOT be added here, but in your project configuration.\n\n"
            + "Check:http://pydev.org/manual_101_interpreter.html for more details.";

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public void run() {

        ListSelectionDialog dialog = new ListSelectionDialog(Display.getDefault().getActiveShell(), allItems, new ContentProvider(),
                new LabelProvider(), msg);
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