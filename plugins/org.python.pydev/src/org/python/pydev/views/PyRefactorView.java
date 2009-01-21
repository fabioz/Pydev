package org.python.pydev.views;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.editor.refactoring.IPyRefactoring;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class PyRefactorView extends ViewPart implements IPropertyListener,
        IStructuredContentProvider{
    private TableViewer viewer;

    private Action doubleClickAction;

    /*
     * The content provider class is responsible for providing objects to the
     * view. It can wrap existing objects in adapters or simply return objects
     * as-is. These objects may be sensitive to the current input of the view,
     * or ignore it and always show the same content (like Task List, for
     * example).
     */

    class ViewLabelProvider extends LabelProvider implements
            ITableLabelProvider {
        public String getColumnText(Object obj, int index) {
            return getText(obj);
        }

        public Image getColumnImage(Object obj, int index) {
            return getImage(obj);
        }

        public Image getImage(Object obj) {
            return PlatformUI.getWorkbench().getSharedImages().getImage(
                    ISharedImages.IMG_OBJ_ELEMENT);
        }
    }

    class NameSorter extends ViewerSorter {
    }

    /**
     * The constructor.
     */
    public PyRefactorView() {
    }

    public void refresh() {
        viewer.refresh();
        getSite().getPage().bringToTop(this);
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    public void createPartControl(Composite parent) {
        viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
                | SWT.V_SCROLL);
        viewer.setContentProvider(this);
        viewer.setLabelProvider(new ViewLabelProvider());
        viewer.setSorter(new NameSorter());
        viewer.setInput(getViewSite());

        hookDoubleClickAction();
        
        AbstractPyRefactoring.addPropertiesListener(this);
        
        //now try to load the last results.
        AbstractPyRefactoring.firePropertiesChanged();
        this.refresh();
    }

    private void hookDoubleClickAction() {
        doubleClickAction = new Action() {
            public void run() {
                ISelection selection = viewer.getSelection();
                Object obj = ((IStructuredSelection) selection)
                        .getFirstElement();

                File realFile = new File(obj.toString());
                if (realFile.exists()) {
                    ItemPointer p = new ItemPointer(realFile, new Location(-1, -1), null);
                    new PyOpenAction().run(p);
                }
            }
        };
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                doubleClickAction.run();
            }
        });
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    @SuppressWarnings("unchecked")
    public void propertyChanged(Object source, int propId) {
        if (source == null || propId != IPyRefactoring.REFACTOR_RESULT_PROP){
            return;
        }
        
        if(source instanceof Tuple){
            Tuple<IPyRefactoring, List<String>> sources = (Tuple<IPyRefactoring, List<String>>) source;
            
            if(sources.o1== null || sources.o2== null){
                return;
            }
            
            elements.clear();
            elements.addAll(sources.o2);
        }
         
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public Object[] getElements(Object parent) {
        return elements.toArray();
    }

    private List<String> elements = new ArrayList<String>();

}