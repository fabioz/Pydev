package com.python.pydev.browsing.view;

//package com.python.pydev.browsing.view;

import java.io.StringReader;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dnd.DelegatingDropAdapter;
import org.eclipse.jdt.internal.ui.dnd.JdtViewerDragAdapter;
import org.eclipse.jdt.internal.ui.dnd.ResourceTransferDragAdapter;
import org.eclipse.jdt.internal.ui.filters.OutputFolderFilter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.packageview.PackagesMessages;
import org.eclipse.jdt.internal.ui.packageview.SelectionTransferDragAdapter;
import org.eclipse.jdt.internal.ui.packageview.SelectionTransferDropAdapter;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jdt.internal.ui.workingsets.ConfigureWorkingSetAction;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetModel;
import org.eclipse.jdt.ui.IContextMenuConstants;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.util.TransferDropTargetListener;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.navigator.LocalSelectionTransfer;
import org.python.copiedfromeclipsesrc.PydevFileEditorInput;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.browsing.BrowsingPlugin;
import com.python.pydev.view.copiedfromeclipsesrc.Core;
import com.python.pydev.view.copiedfromeclipsesrc.FileTransferDragAdapter;
import com.python.pydev.view.copiedfromeclipsesrc.FileTransferDropAdapter;
import com.python.pydev.view.copiedfromeclipsesrc.JarEntryEditorInput;
import com.python.pydev.view.copiedfromeclipsesrc.WorkingSetDropAdapter;
import com.python.pydev.view.copiedfromeclipsesrc.actions.PackageExplorerActionGroup;

public class PydevPackageExplorer extends ViewPart implements ISetSelectionTarget {
	private TreeViewer treeViewer;
	private IFile fileOpened;	
	public static final String NAME = "Package Explorer View";
	private boolean linkingEnabled;
	private ISelection fLastOpenSelection;
	private int fRootMode = 1;
	private WorkingSetModel fWorkingSetModel;
	private PackageExplorerLabelProvider fLabelProvider;
	private String fWorkingSetName;
	private boolean fIsCurrentLayoutFlat; // true means flat, false means hierachical
	private PydevPackageExplorerContentProvider fContentProvider;
	
	// For memento purpose
	private IMemento fMemento;
	static final String TAG_SELECTION= "selection"; //$NON-NLS-1$
	static final String TAG_EXPANDED= "expanded"; //$NON-NLS-1$
	static final String TAG_ELEMENT= "element"; //$NON-NLS-1$
	static final String TAG_PATH= "path"; //$NON-NLS-1$
	static final String TAG_VERTICAL_POSITION= "verticalPosition"; //$NON-NLS-1$
	static final String TAG_HORIZONTAL_POSITION= "horizontalPosition"; //$NON-NLS-1$
	static final String TAG_FILTERS = "filters"; //$NON-NLS-1$
	static final String TAG_FILTER = "filter"; //$NON-NLS-1$
	static final String TAG_LAYOUT= "layout"; //$NON-NLS-1$
	static final String TAG_CURRENT_FRAME= "currentFramge"; //$NON-NLS-1$
	static final String TAG_ROOT_MODE= "rootMode"; //$NON-NLS-1$
	static final String SETTING_MEMENTO= "memento"; //$NON-NLS-1$
	
	public static final int SHOW_PROJECTS= 1;
	public static final int SHOW_WORKING_SETS= 2;
	
	private static final int HIERARCHICAL_LAYOUT= 0x1;
	private static final int FLAT_LAYOUT= 0x2;
	
	private PackageExplorerActionGroup fActionSet;
	
	private ISelectionChangedListener fPostSelectionListener;	
	public PydevPackageExplorer() {		
		fPostSelectionListener= new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				handlePostSelectionChanged(event);
			}
		};
	}	

	@Override
	public void createPartControl(Composite parent) {
		fLabelProvider = new PackageExplorerLabelProvider();		
		
		treeViewer = new TreeViewer(parent);
		
		//PydevPackageExplorerContentProvider.getInstance().setViewer( treeViewer );
		//PydevPackageExplorerContentProvider.getInstance().setWorkingSetModel( fWorkingSetModel );
		
		fContentProvider = PydevPackageExplorerContentProvider.getInstance();	
		treeViewer.setContentProvider( fContentProvider );
		treeViewer.setLabelProvider( fLabelProvider );	
		treeViewer.setComparer( createElementComparer() );
		getSite().setSelectionProvider(treeViewer);
		//treeViewer.setInput(PydevPlugin.getWorkspace().getRoot());      
		treeViewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
		
		
		if (fMemento != null) {
			restoreLinkingEnabled(fMemento);
		}
		
		makeActions();
		initFrameActions();
		initKeyListener();		
		
		initDragAndDrop();
		addDoubleClickListener();
		
		treeViewer.addOpenListener(new IOpenListener() {
			public void open(OpenEvent event) {
				//fActionSet.handleOpen(event);
				fLastOpenSelection = event.getSelection();
			}
		});
		
		if (fMemento != null)
			restoreUIState(fMemento);
		fMemento= null;
		
		createMenu();
		createContextMenu();		
		fillActionBars();
	}
	
// COPIED FROM ECLIPSE SOURCE
	private IPartListener fPartListener= new IPartListener() {
		public void partActivated(IWorkbenchPart part) {
			if (part instanceof IEditorPart)
				editorActivated((IEditorPart) part);
		}
		public void partBroughtToTop(IWorkbenchPart part) {
		}
		public void partClosed(IWorkbenchPart part) {
		}
		public void partDeactivated(IWorkbenchPart part) {
		}
		public void partOpened(IWorkbenchPart part) {
		}
	};
	
	private IElementComparer createElementComparer() {
		if (showProjects()) 
			return null;
		else
			return WorkingSetModel.COMPARER;
	}

	private void initKeyListener() {
		treeViewer.getControl().addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
				fActionSet.handleKeyEvent(event);
			}
		});
	}
	
	private void initFrameActions() {
		fActionSet.getUpAction().update();
		fActionSet.getBackAction().update();
		fActionSet.getForwardAction().update();
	}
	
	private void initDragAndDrop() {
		initDrag();
		initDrop();
	}
	
	private void initDrag() {
		int ops= DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;
		Transfer[] transfers= new Transfer[] {
			LocalSelectionTransfer.getInstance(), 
			ResourceTransfer.getInstance(),
			FileTransfer.getInstance()};
		TransferDragSourceListener[] dragListeners= new TransferDragSourceListener[] {
			new SelectionTransferDragAdapter(treeViewer),
			new ResourceTransferDragAdapter(treeViewer),
			new FileTransferDragAdapter(treeViewer)
		};
		treeViewer.addDragSupport(ops, transfers, new JdtViewerDragAdapter(treeViewer, dragListeners));
	}

	private void initDrop() {
		int ops= DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK | DND.DROP_DEFAULT;
		Transfer[] transfers= new Transfer[] {
			LocalSelectionTransfer.getInstance(), 
			FileTransfer.getInstance()};
		TransferDropTargetListener[] dropListeners= new TransferDropTargetListener[] {
			new SelectionTransferDropAdapter(treeViewer),
			new FileTransferDropAdapter(treeViewer),
			new WorkingSetDropAdapter(this)
		};
		treeViewer.addDropSupport(ops, transfers, new DelegatingDropAdapter(dropListeners));
	}

	private void createContextMenu() {		
		MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener(){
			public void menuAboutToShow(IMenuManager manager) {				
				fillContextMenu( manager );
			}        	
        });       
       
        
        Menu menu = menuMgr.createContextMenu(treeViewer.getTree());        
        treeViewer.getTree().setMenu(menu);        
        getSite().registerContextMenu( menuMgr, treeViewer );
        
        // Register viewer with site. This must be done before making the actions.
		IWorkbenchPartSite site= getSite();
		site.registerContextMenu(menuMgr, treeViewer);
		site.setSelectionProvider(treeViewer);
		site.getPage().addPartListener(fPartListener);
        
        menuMgr.add(new Separator("EndFilterGroup")); //$NON-NLS-1$)
	}

	private void fillActionBars() {
		IActionBars actionBars= getViewSite().getActionBars();
		fActionSet.fillActionBars(actionBars);
	}
	
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(new Separator(IContextMenuConstants.GROUP_NEW));
		manager.add(new GroupMarker(IContextMenuConstants.GROUP_GOTO));
		manager.add(new Separator(IContextMenuConstants.GROUP_OPEN));
		manager.add(new GroupMarker(IContextMenuConstants.GROUP_SHOW));
		manager.add(new Separator(IContextMenuConstants.GROUP_REORGANIZE));
		manager.add(new Separator(IContextMenuConstants.GROUP_GENERATE));
		manager.add(new Separator(IContextMenuConstants.GROUP_SEARCH));
		manager.add(new Separator(IContextMenuConstants.GROUP_BUILD));
		manager.add(new Separator(IContextMenuConstants.GROUP_ADDITIONS));
		manager.add(new Separator(IContextMenuConstants.GROUP_VIEWER_SETUP));
		manager.add(new Separator(IContextMenuConstants.GROUP_PROPERTIES));
		
		fActionSet.setContext(new ActionContext(treeViewer.getSelection()));
		fActionSet.fillContextMenu(manager);
		fActionSet.setContext(null);
	}

	private void createMenu() {    
	}	
	
	private void addDoubleClickListener() {
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {				
				Object element = ((IStructuredSelection)event.getSelection()).getFirstElement();
				System.out.println(element.getClass());
				PyOpenAction action = null;
				if( element instanceof IFile ) {
					fileOpened = (IFile)element;
					action = new PyOpenAction();
					action.run( new ItemPointer( element ) );				
				} else if( element instanceof CompositeASTEntry ) {
					ASTEntry entry = ((CompositeASTEntry)element ).getEntry();
					Location location = new Location( entry.node.beginLine-1, entry.node.beginColumn );					
					action = new PyOpenAction();
					action.run( new ItemPointer( ((CompositeASTEntry)element ).getFile(), location, location ) );					
				} else if(treeViewer.isExpandable(element)) {
					treeViewer.setExpandedState(element, !treeViewer.getExpandedState(element));
				}				
			}
		});
	}	
		
	@Override
	public void setFocus() {
		
	}
	
	void editorActivated(IEditorPart editor) {
		if (!isLinkingEnabled())  
			return;		
		Object input= getElementOfInput(editor.getEditorInput());
		
		if (!inputIsSelected(editor.getEditorInput()))
			showInput(input);
		else
			treeViewer.getTree().showSelection();
	}
	
	boolean showInput(Object input) {
		Object element= input;	
		element = input;
			
		if (element != null) {			
			ISelection newSelection= new StructuredSelection(element);
			ISelection oldSelection= treeViewer.getSelection();
			if (treeViewer.getSelection().equals(newSelection)) {
				treeViewer.reveal(element);
			} else {
				try {
					treeViewer.removePostSelectionChangedListener(fPostSelectionListener);						
					treeViewer.setSelection(newSelection, true);
	
					while (element != null && treeViewer.getSelection().isEmpty()) {
						// Try to select parent in case element is filtered
						element= getParent(element);
						if (element != null) {
							newSelection= new StructuredSelection(element);
							treeViewer.setSelection(newSelection, true);
						}
					}
				} finally {
					treeViewer.addPostSelectionChangedListener(fPostSelectionListener);
				}
			}
			return true;
		}
		return false;
	}
	
	Object getElementOfInput(IEditorInput input) {
		if (input instanceof IFileEditorInput)
			return ((IFileEditorInput)input).getFile();
		else if (input instanceof JarEntryEditorInput)
			return ((JarEntryEditorInput)input).getStorage();
		else if (input instanceof PydevFileEditorInput) {			
			IWorkspace w = ResourcesPlugin.getWorkspace();
			IPath path = ((PydevFileEditorInput)input).getPath();
	        IFile file = w.getRoot().getFile( path );
			return file;
		}
		return null;
	}
	
	private Object getParent(Object element) {
		if (element instanceof CompositeASTEntry) {
			ASTEntry entry = ((CompositeASTEntry)element).getEntry();
			return entry.parent;
		} else if (element instanceof IResource) {
			return ((IResource)element).getParent();
		}
		return null;
	}

	private boolean inputIsSelected(IEditorInput input) {
		IStructuredSelection selection= (IStructuredSelection)treeViewer.getSelection();
		if (selection.size() != 1) 
			return false;
		IEditorInput selectionAsInput= null;
		try {
			//selectionAsInput= PydevEditorUtility.getEditorInput(selection.getFirstElement());
			selectionAsInput= EditorUtility.getEditorInput(selection.getFirstElement());
		} catch (Exception e1) {
			return false;
		}
		return input.equals(selectionAsInput);		
	}

	public TreeViewer getViewer() {
		return treeViewer;
	}
	
	private void makeActions() {
		fActionSet= new PackageExplorerActionGroup(this);
	}
	
	public void selectReveal(ISelection selection) {
		selectReveal(selection, 0);
	}
	
	private void selectReveal(final ISelection selection, final int count) {
		Control ctrl= getViewer().getControl();
		if (ctrl == null || ctrl.isDisposed())
			return;
		ISelection javaSelection= convertSelection(selection);
		treeViewer.setSelection(javaSelection, true);
		PydevPackageExplorerContentProvider provider= (PydevPackageExplorerContentProvider)getViewer().getContentProvider();
		ISelection cs= treeViewer.getSelection();
		// If we have Pending changes and the element could not be selected then
		// we try it again on more time by posting the select and reveal asynchronuoulsy
		// to the event queue. See PR http://bugs.eclipse.org/bugs/show_bug.cgi?id=30700
		// for a discussion of the underlying problem.
		//if (count == 0 && provider.hasPendingChanges() && !javaSelection.equals(cs)) {
		if (count == 0  && !javaSelection.equals(cs)) {
			ctrl.getDisplay().asyncExec(new Runnable() {
				public void run() {
					selectReveal(selection, count + 1);
				}
			});
		}
	}
	
	private ISelection convertSelection(ISelection s) {
		if (!(s instanceof IStructuredSelection))
			return s;
			
		Object[] elements= ((StructuredSelection)s).toArray();
		if (!containsResources(elements))
			return s;
				
		for (int i= 0; i < elements.length; i++) {
			Object o= elements[i];
			if (!(o instanceof CompositeASTEntry)) {
				if (o instanceof IResource) {
					IResource jElement= Core.create((IResource)o);
					if (jElement != null && jElement.exists()) 
						elements[i]= jElement;
				}
				else if (o instanceof IAdaptable) {
					IResource r= (IResource)((IAdaptable)o).getAdapter(IResource.class);
					if (r != null) {
						IResource jElement= Core.create(r);
						if (jElement != null && jElement.exists()) 
							elements[i]= jElement;
						else
							elements[i]= r;
					}
				}
			}
		}
		
		return new StructuredSelection(elements);
	}
	
	private boolean containsResources(Object[] elements) {
		for (int i = 0; i < elements.length; i++) {
			Object o= elements[i];
			if (!(o instanceof CompositeASTEntry)) {
				if (o instanceof IResource)
					return true;
				if ((o instanceof IAdaptable) && ((IAdaptable)o).getAdapter(IResource.class) != null)
					return true;
				}
		}
		return false;
	}

	public boolean isLinkingEnabled() {
		return linkingEnabled;
	}

	public void setLinkingEnabled(boolean linkingEnabled) {
		this.linkingEnabled = linkingEnabled;
		
		if (linkingEnabled) {
			IEditorPart editor = getSite().getPage().getActiveEditor();
			if (editor != null) {
				editorActivated(editor);
			}
		}
	}
	
	/**
	 * Handles post selection changed in viewer.
	 * 
	 * Links to editor (if option enabled).
	 */
	private void handlePostSelectionChanged(SelectionChangedEvent event) {
		ISelection selection= event.getSelection();
		// If the selection is the same as the one that triggered the last
		// open event then do nothing. The editor already got revealed.
		if (isLinkingEnabled() && !selection.equals(fLastOpenSelection)) {
			linkToEditor((IStructuredSelection)selection);
		}
		fLastOpenSelection= null;
	}
	
	/**
	 * Links to editor (if option enabled)
	 */
	private void linkToEditor(IStructuredSelection selection) {
		// ignore selection changes if the package explorer is not the active part.
		// In this case the selection change isn't triggered by a user.
		if (!isActivePart())
			return;
		Object obj= selection.getFirstElement();

		if (selection.size() == 1) {
			//IEditorPart part= PydevEditorUtility.isOpenInEditor(obj);
			IEditorPart part= EditorUtility.isOpenInEditor(obj);
			if (part != null) {
				IWorkbenchPage page= getSite().getPage();
				page.bringToTop(part);
				if (obj instanceof CompositeASTEntry ) {
					ASTEntry entry = ((CompositeASTEntry)obj).getEntry();
					//PydevEditorUtility.revealInEditor(part, entry.node.beginLine, entry.node.beginColumn );
					EditorUtility.revealInEditor(part, entry.node.beginLine, entry.node.beginColumn );
				}
			}
		}
	}
	
	private boolean isActivePart() {
		return this == getSite().getPage().getActivePart();
	}

	public void collapseAll() {
		try {
			treeViewer.getControl().setRedraw(false);		
			treeViewer.collapseToLevel(treeViewer.getInput(), AbstractTreeViewer.ALL_LEVELS);
		} finally {
			treeViewer.getControl().setRedraw(true);
		}		
	}
	
	public void rootModeChanged(int newMode) {
		fRootMode= newMode;
		if (showWorkingSets() && fWorkingSetModel == null) {
			createWorkingSetModel();
			if (fActionSet != null) {
				fActionSet.getWorkingSetActionGroup().setWorkingSetModel(fWorkingSetModel);
			}
		}
		ISelection selection= treeViewer.getSelection();
		Object input= treeViewer.getInput();
		boolean isRootInputChange= ResourcesPlugin.getWorkspace().getRoot().equals(input) 
			|| (fWorkingSetModel != null && fWorkingSetModel.equals(input))
			|| input instanceof IWorkingSet;
		try {
			treeViewer.getControl().setRedraw(false);
			if (isRootInputChange) {
				treeViewer.setInput(null);
			}			
			//setSorter();
			fActionSet.getWorkingSetActionGroup().fillFilters(treeViewer);
			if (isRootInputChange) {
				treeViewer.setInput(findInputElement());
			}
			treeViewer.setSelection(selection, true);
		} finally {
			treeViewer.getControl().setRedraw(true);
		}
		if (isRootInputChange && fWorkingSetModel.needsConfiguration()) {
			System.out.println("passou por needsConfiguration()");
			ConfigureWorkingSetAction action= new ConfigureWorkingSetAction(getSite());
			action.setWorkingSetModel(fWorkingSetModel);
			action.run();
			fWorkingSetModel.configured();
		}
	}

	public boolean showWorkingSets() {
		int SHOW_WORKING_SETS = 2;
		return fRootMode == SHOW_WORKING_SETS;
	}
	
	private void createWorkingSetModel() {
		Platform.run(new ISafeRunnable() {
			public void run() throws Exception {
				fWorkingSetModel= fMemento != null 
					? new WorkingSetModel(fMemento) 
					: new WorkingSetModel();
				//PydevPackageExplorerContentProvider.getInstance().setWorkingSetModel(fWorkingSetModel);
			}
			public void handleException(Throwable exception) {
				fWorkingSetModel= new WorkingSetModel();
				//PydevPackageExplorerContentProvider.getInstance().setWorkingSetModel(fWorkingSetModel);
			}
		});
	}
	
	private Object findInputElement() {
		if (showWorkingSets()) {
			return fWorkingSetModel;
		} else {
			Object input= getSite().getPage().getInput();
			if (input instanceof IWorkspace) { 
				return JavaCore.create(((IWorkspace)input).getRoot());
			} else if (input instanceof IContainer) {
				IJavaElement element= JavaCore.create((IContainer)input);
				if (element != null && element.exists())
					return element;
				return input;
			}
			//1GERPRT: ITPJUI:ALL - Packages View is empty when shown in Type Hierarchy Perspective
			// we can't handle the input
			// fall back to show the workspace
			return JavaCore.create(JavaPlugin.getWorkspace().getRoot());
		}
	}
	
	/**
	 * Returns the name for the given element.
	 * Used as the name for the current frame. 
	 */
	public String getFrameName(Object element) {
		if (element instanceof IJavaElement) {
			return ((IJavaElement) element).getElementName();
		} else if (element instanceof WorkingSetModel) {
			return ""; //$NON-NLS-1$
		} else {
			return fLabelProvider.getText(element);
		}
	}
	
	/**
	 * Returns the tool tip text for the given element.
	 */
	public String getToolTipText(Object element) {
		String result;
		if (!(element instanceof IResource)) {
			if (element instanceof IJavaModel) {
				result= PackagesMessages.PackageExplorerPart_workspace; 
			} else if (element instanceof IJavaElement){
				result= JavaElementLabels.getTextLabel(element, AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS);
			} else if (element instanceof IWorkingSet) {
				result= ((IWorkingSet)element).getName();
			} else if (element instanceof WorkingSetModel) {
				result= PackagesMessages.PackageExplorerPart_workingSetModel; 
			} else {
				result= fLabelProvider.getText(element);
			}
		} else {
			IPath path= ((IResource) element).getFullPath();
			if (path.isRoot()) {
				result= PackagesMessages.PackageExplorer_title; 
			} else {
				result= path.makeRelative().toString();
			}
		}
		
		if (fWorkingSetName == null)
			return result;

		String wsstr= Messages.format(PackagesMessages.PackageExplorer_toolTip, new String[] { fWorkingSetName }); 
		if (result.length() == 0)
			return wsstr;
		return Messages.format(PackagesMessages.PackageExplorer_toolTip2, new String[] { result, fWorkingSetName }); 
	}
	
	/**
	 * Updates the title text and title tool tip.
	 * Called whenever the input of the viewer changes.
	 */ 
	public void updateTitle() {		
		Object input= treeViewer.getInput();
		
		setContentDescription(""); //$NON-NLS-1$
		setTitleToolTip(""); //$NON-NLS-1$
		
/*		if (input == null
			|| (input instanceof IJavaModel)) {
			setContentDescription(""); //$NON-NLS-1$
			setTitleToolTip(""); //$NON-NLS-1$
		} else {
			String inputText= JavaElementLabels.getTextLabel(input, AppearanceAwareLabelProvider.DEFAULT_TEXTFLAGS);
			setContentDescription(inputText);
			setTitleToolTip(getToolTipText(input));
		} */
	}
	
	public int getRootMode() {
		return fRootMode;
	}
	
	public boolean showProjects() {
		return fRootMode == SHOW_PROJECTS;
	}
	
	public WorkingSetModel getWorkingSetModel() {
		return fWorkingSetModel;
	}
	
	public void setWorkingSetName(String workingSetName) {
		fWorkingSetName= workingSetName;
	}

	public StructuredViewer getTreeViewer() {
		return treeViewer;
	}
	
	public boolean isFlatLayout() {
		return fIsCurrentLayoutFlat;
	}
	
	public void toggleLayout() {

		// Update current state and inform content and label providers
		fIsCurrentLayoutFlat= !fIsCurrentLayoutFlat;
		//saveLayoutState(null);
		
		fContentProvider.setFlatLayout(isFlatLayout());
		//fLabelProvider.setIsFlatLayout(isFlatLayout());
		
		treeViewer.getControl().setRedraw(false);
		treeViewer.refresh();
		treeViewer.getControl().setRedraw(true);
	}
	
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		fMemento= memento;
		if (fMemento == null) {
			IDialogSettings section= BrowsingPlugin.getDefault().getDialogSettings().getSection(getSectionName());
			if (section != null) {
				String settings= section.get(SETTING_MEMENTO);
				if (settings != null) {
					try {
						fMemento= XMLMemento.createReadRoot(new StringReader(settings));
					} catch (WorkbenchException e) {
						// don't restore the memento when the settings can't be read.
					}
				}
			}
		}
		restoreRootMode(fMemento);
		if (showWorkingSets()) {
			createWorkingSetModel();
		}
		//restoreLayoutState(memento);
	}
	
	private String getSectionName() {
    	return "com.python.pydev.browsing.view.packageExplorer"; //$NON-NLS-1$
    }
	
	private void restoreRootMode(IMemento memento) {
		if (memento != null) {
			Integer value= fMemento.getInteger(TAG_ROOT_MODE);
			fRootMode= value == null ? SHOW_PROJECTS : value.intValue();
			if (fRootMode != SHOW_PROJECTS && fRootMode != SHOW_WORKING_SETS)
				fRootMode= SHOW_PROJECTS;
		} else {
			fRootMode= SHOW_PROJECTS;
		}
	}
	
	public void saveState(IMemento memento) {
		if (treeViewer == null) {
			// part has not been created
			if (fMemento != null) //Keep the old state;
				memento.putMemento(fMemento);
			return;
		}
		
		memento.putInteger(TAG_ROOT_MODE, fRootMode);
		if (fWorkingSetModel != null)
			fWorkingSetModel.saveState(memento);
		
		// disable the persisting of state which can trigger expensive operations as
		// a side effect: see bug 52474 and 53958
		// saveCurrentFrame(memento);
		// saveExpansionState(memento);
		// saveSelectionState(memento);
		saveLayoutState(memento);
		saveLinkingEnabled(memento);
		// commented out because of http://bugs.eclipse.org/bugs/show_bug.cgi?id=4676
		// saveScrollState(memento, fViewer.getTree());
		fActionSet.saveFilterAndSorterState(memento);
	}
	
	/*
	private void saveCurrentFrame(IMemento memento) {
        FrameAction action = fActionSet.getUpAction();
        FrameList frameList= action.getFrameList();

		if (frameList.getCurrentIndex() > 0) {
			TreeFrame currentFrame = (TreeFrame) frameList.getCurrentFrame();
			// don't persist the working set model as the current frame
			if (currentFrame.getInput() instanceof WorkingSetModel)
				return;
			IMemento frameMemento = memento.createChild(TAG_CURRENT_FRAME);
			currentFrame.saveState(frameMemento);
		}
	}
	*/

	private void saveLinkingEnabled(IMemento memento) {
		memento.putInteger(PreferenceConstants.LINK_PACKAGES_TO_EDITOR, linkingEnabled ? 1 : 0);
	}

	private void saveLayoutState(IMemento memento) {
		if (memento != null) {	
			memento.putInteger(TAG_LAYOUT, getLayoutAsInt());
		} else {
		//if memento is null save in preference store
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			store.setValue(TAG_LAYOUT, getLayoutAsInt());
		}
	}

	private int getLayoutAsInt() {
		if (fIsCurrentLayoutFlat)
			return FLAT_LAYOUT;
		else
			return HIERARCHICAL_LAYOUT;
	}

	protected void saveScrollState(IMemento memento, Tree tree) {
		ScrollBar bar= tree.getVerticalBar();
		int position= bar != null ? bar.getSelection() : 0;
		memento.putString(TAG_VERTICAL_POSITION, String.valueOf(position));
		//save horizontal position
		bar= tree.getHorizontalBar();
		position= bar != null ? bar.getSelection() : 0;
		memento.putString(TAG_HORIZONTAL_POSITION, String.valueOf(position));
	}

	protected void saveSelectionState(IMemento memento) {
		Object elements[]= ((IStructuredSelection) treeViewer.getSelection()).toArray();
		if (elements.length > 0) {
			IMemento selectionMem= memento.createChild(TAG_SELECTION);
			for (int i= 0; i < elements.length; i++) {
				IMemento elementMem= selectionMem.createChild(TAG_ELEMENT);
				// we can only persist JavaElements for now
				Object o= elements[i];
				if (o instanceof IJavaElement)
					elementMem.putString(TAG_PATH, ((IJavaElement) elements[i]).getHandleIdentifier());
			}
		}
	}

	protected void saveExpansionState(IMemento memento) {
		Object expandedElements[]= treeViewer.getVisibleExpandedElements();
		if (expandedElements.length > 0) {
			IMemento expandedMem= memento.createChild(TAG_EXPANDED);
			for (int i= 0; i < expandedElements.length; i++) {
				IMemento elementMem= expandedMem.createChild(TAG_ELEMENT);
				// we can only persist JavaElements for now
				Object o= expandedElements[i];
				if (o instanceof IJavaElement)
					elementMem.putString(TAG_PATH, ((IJavaElement) expandedElements[i]).getHandleIdentifier());
			}
		}
	}

	private void restoreFilterAndSorter() {
		treeViewer.addFilter(new OutputFolderFilter());
		//setSorter();
		if (fMemento != null)	
			fActionSet.restoreFilterAndSorterState(fMemento);
	}

	private void restoreUIState(IMemento memento) {
		// see comment in save state
		// restoreCurrentFrame(memento);
		// restoreExpansionState(memento);
		// restoreSelectionState(memento);
		// commented out because of http://bugs.eclipse.org/bugs/show_bug.cgi?id=4676
		// restoreScrollState(memento, fViewer.getTree());
	}

	/*
	private void restoreCurrentFrame(IMemento memento) {
		IMemento frameMemento = memento.getChild(TAG_CURRENT_FRAME);
		
		if (frameMemento != null) {
	        FrameAction action = fActionSet.getUpAction();
	        FrameList frameList= action.getFrameList();
			TreeFrame frame = new TreeFrame(fViewer);
			frame.restoreState(frameMemento);
			frame.setName(getFrameName(frame.getInput()));
			frame.setToolTipText(getToolTipText(frame.getInput()));
			frameList.gotoFrame(frame);
		}
	}
	*/

	private void restoreLinkingEnabled(IMemento memento) {
		Integer val= memento.getInteger(PreferenceConstants.LINK_PACKAGES_TO_EDITOR);
		if (val != null) {
			linkingEnabled= val.intValue() != 0;
		}
	}

	protected void restoreScrollState(IMemento memento, Tree tree) {
		ScrollBar bar= tree.getVerticalBar();
		if (bar != null) {
			try {
				String posStr= memento.getString(TAG_VERTICAL_POSITION);
				int position;
				position= new Integer(posStr).intValue();
				bar.setSelection(position);
			} catch (NumberFormatException e) {
				// ignore, don't set scrollposition
			}
		}
		bar= tree.getHorizontalBar();
		if (bar != null) {
			try {
				String posStr= memento.getString(TAG_HORIZONTAL_POSITION);
				int position;
				position= new Integer(posStr).intValue();
				bar.setSelection(position);
			} catch (NumberFormatException e) {
				// ignore don't set scroll position
			}
		}
	}

	protected void restoreSelectionState(IMemento memento) {
		IMemento childMem;
		childMem= memento.getChild(TAG_SELECTION);
		if (childMem != null) {
			ArrayList list= new ArrayList();
			IMemento[] elementMem= childMem.getChildren(TAG_ELEMENT);
			for (int i= 0; i < elementMem.length; i++) {
				Object element= JavaCore.create(elementMem[i].getString(TAG_PATH));
				if (element != null)
					list.add(element);
			}
			treeViewer.setSelection(new StructuredSelection(list));
		}
	}

	protected void restoreExpansionState(IMemento memento) {
		IMemento childMem= memento.getChild(TAG_EXPANDED);
		if (childMem != null) {
			ArrayList elements= new ArrayList();
			IMemento[] elementMem= childMem.getChildren(TAG_ELEMENT);
			for (int i= 0; i < elementMem.length; i++) {
				Object element= JavaCore.create(elementMem[i].getString(TAG_PATH));
				if (element != null)
					elements.add(element);
			}
			treeViewer.setExpandedElements(elements.toArray());
		}
	}
}
