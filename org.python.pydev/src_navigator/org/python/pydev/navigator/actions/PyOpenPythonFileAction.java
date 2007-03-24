/*
 * Created on Oct 17, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.actions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IWorkbenchPage;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.navigator.PythonNode;
import org.python.pydev.outline.ParsedItem;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.PydevPlugin;

public class PyOpenPythonFileAction extends Action {

    private List<IFile> filesSelected;

    private List<PythonNode> nodesSelected;

    private List<Object> containersSelected; // IContainer or IWrappedResource

    private ISelectionProvider provider;

    public PyOpenPythonFileAction(IWorkbenchPage page, ISelectionProvider selectionProvider) {
        this.setText("Open python");
        this.provider = selectionProvider;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#isEnabled()
     */
    public boolean isEnabled() {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    public void run() {
        // clear them
        filesSelected = new ArrayList<IFile>();
        nodesSelected = new ArrayList<PythonNode>();
        containersSelected = new ArrayList<Object>();

        fillSelections();

        if (filesSelected.size() > 0) {
            for (IFile f : filesSelected) {
                new PyOpenAction().run(new ItemPointer(f));
            }

        } else if (nodesSelected.size() > 0) {
            PythonNode node = nodesSelected.iterator().next();
            ParsedItem actualObject = node.getActualObject();
            new PyOpenAction().run(new ItemPointer(node.getPythonFile().getActualObject(), NodeUtils
                    .getNameTokFromNode(actualObject.astThis.node)));

        } else if (containersSelected.size() > 0) {
            if (this.provider instanceof TreeViewer) {
                TreeViewer viewer = (TreeViewer) this.provider;
                for (Object container : containersSelected) {
                    if (viewer.isExpandable(container)) {
                        viewer.setExpandedState(container, !viewer.getExpandedState(container));
                    }
                }
            } else {
                PydevPlugin.log("Expecting the provider to be a TreeViewer -- it is:" + this.provider.getClass());
            }
        }
    }

    private void fillSelections() {
        ISelection selection = provider.getSelection();
        if (!selection.isEmpty()) {
            IStructuredSelection sSelection = (IStructuredSelection) selection;
            Iterator iterator = sSelection.iterator();
            while (iterator.hasNext()) {
                Object element = iterator.next();

                if (element instanceof PythonNode) {
                    nodesSelected.add((PythonNode) element);

                } else if (element instanceof IAdaptable) {
                    IAdaptable adaptable = (IAdaptable) element;
                    IFile file = (IFile) adaptable.getAdapter(IFile.class);
                    if (file != null) {
                        filesSelected.add(file);

                    } else {
                        IContainer container = (IContainer) adaptable.getAdapter(IContainer.class);
                        if (container != null) {
                            containersSelected.add(element);
                        }
                    }
                }
            }
        }
    }

}
