package org.python.pydev.debug.codecoverage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.part.ViewPart;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;
import org.python.pydev.editor.refactoring.PyRefactoring;

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

public class PyCodeCoverageView extends ViewPart implements IPropertyListener,
        IStructuredContentProvider{

    private TreeViewer viewer;
    

    private Action doubleClickAction;


    private Action chooseAction;


    protected Action clearAction;

    
    private Button clearButton;


    private List elements = new ArrayList();


    private Button chooseButton;


    private Composite rComposite;


    private Text text;


    protected String currentDir;


    /*
     * The content provider class is responsible for providing objects to the
     * view. It can wrap existing objects in adapters or simply return objects
     * as-is. These objects may be sensitive to the current input of the view,
     * or ignore it and always show the same content (like Task List, for
     * example).
     */

    class ViewLabelProvider extends LabelProvider implements
            ITreeContentProvider{

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
         */
        public Object[] getChildren(Object parentElement) {
            // TODO Auto-generated method stub
            return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
         */
        public Object getParent(Object element) {
            // TODO Auto-generated method stub
            return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
         */
        public boolean hasChildren(Object element) {
            // TODO Auto-generated method stub
            return false;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
         */
        public Object[] getElements(Object inputElement) {
            // TODO Auto-generated method stub
            return null;
        }

        /* (non-Javadoc)
         * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            // TODO Auto-generated method stub
            
        }
    }

    class NameSorter extends ViewerSorter {
    }

    /**
     * The constructor.
     */
    public PyCodeCoverageView() {
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
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 2;
		parent.setLayout(layout);

		rComposite = new Composite(parent,SWT.MULTI);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 2;
		layout.marginWidth = 0;
		layout.marginHeight = 2;
		GridData layoutData = new GridData();
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.grabExcessVerticalSpace = true;
		layoutData.horizontalAlignment = GridData.FILL;
		layoutData.verticalAlignment = GridData.FILL;
		rComposite.setLayoutData(layoutData);
		rComposite.setLayout(layout);
		

		text = new Text(parent,SWT.MULTI);
		layoutData = new GridData();
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.grabExcessVerticalSpace = true;
		layoutData.horizontalAlignment = GridData.FILL;
		layoutData.verticalAlignment = GridData.FILL;
		text.setLayoutData(layoutData);
		text.setEditable(false);
		
		parent = rComposite;
		//choose button
        chooseButton = new Button(parent, SWT.PUSH);
		chooseAction = new Action() {
            public void run() {
                DirectoryDialog dialog = new DirectoryDialog(getSite().getShell());
                String string = dialog.open();
                if (string!=null){
                    text.setText("Chosen dir:"+string);
                    notifyDirChanged(string);
                }
            }
        };
        createButton(parent, chooseButton, "Choose dir!", chooseAction);
        //end choose button
        
        
        
        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL
                | SWT.V_SCROLL);
        viewer.setContentProvider(this);
//        viewer.setLabelProvider(new ViewLabelProvider());
//        viewer.setSorter(new NameSorter());
//        viewer.setInput(getViewSite());

//        hookDoubleClickAction();

		layoutData = new GridData();
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.grabExcessVerticalSpace = true;
		layoutData.horizontalAlignment = GridData.FILL;
		layoutData.verticalAlignment = GridData.FILL;
		viewer.getControl().setLayoutData(layoutData);

		
		
		
		//clear results button
        clearButton = new Button(parent, SWT.PUSH);
		clearAction = new Action() {
            public void run() {
                text.setText("Clear action");
            }
        };
        createButton(parent, clearButton, "Clear coverage information!", clearAction);
        //end choose button

        
        this.refresh();
        

    }

    
    
    
    
    
    
    
    
    
    /**
     * @param string
     */
    protected void notifyDirChanged(String string) {
        // TODO Auto-generated method stub
        
    }

    /**
     * @param parent
     * @param button
     * @param string
     */
    private void createButton(Composite parent, Button button, String txt, final Action action) {
        GridData layoutData;
        button.setText(txt);
        button.addSelectionListener(new SelectionListener(){

            public void widgetSelected(SelectionEvent e) {
                action.run();
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
            
        });
        
        layoutData = new GridData();
		layoutData.grabExcessHorizontalSpace = true;
		layoutData.horizontalAlignment = GridData.FILL;
		button.setLayoutData(layoutData);
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

    public void propertyChanged(Object source, int propId) {
        if (source == null){
            return;
        }
        
        Object[] sources = (Object[]) source;
        
        if(sources[0]== null || sources[1]== null){
            return;
        }
        
        if (sources[0] == PyRefactoring.getPyRefactoring()
                && propId == PyRefactoring.REFACTOR_RESULT) {
            
            elements.clear();
            elements.addAll((Collection) sources[1]);
        }
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public Object[] getElements(Object parent) {
        return elements.toArray();
    }


}