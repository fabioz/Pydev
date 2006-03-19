package com.python.pydev.browsing.view;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.workingsets.HistoryWorkingSetUpdater;
import org.eclipse.jdt.internal.ui.workingsets.WorkingSetModel;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.DefinitionsASTIteratorVisitor;

import com.python.pydev.browsing.BrowsingPlugin;

public class PydevPackageExplorerContentProvider extends WorkbenchContentProvider 
							implements IPyEditListener {
	private Map<Object, DefinitionsASTIteratorVisitor> cache = new HashMap<Object, DefinitionsASTIteratorVisitor>();	
	
	private Object EMPTY_ELEMENT[] = new Object[0];
	private boolean flatLayout;
	
	protected TreeViewer fViewer;
	private WorkingSetModel fWorkingSetModel;
	private IPropertyChangeListener fListener;
	private static PydevPackageExplorerContentProvider contentProvider;
	
	public static PydevPackageExplorerContentProvider getInstance() {
		if( contentProvider==null ) {
			contentProvider = new PydevPackageExplorerContentProvider();			
		}
		return contentProvider;			
	}

	/*public void setViewer( TreeViewer viewer ) {
		fViewer = viewer;		
	}
	
	public void setWorkingSetModel( WorkingSetModel model ) {
		this.fWorkingSetModel = model;		
		
		if( fWorkingSetModel!=null ) {
			fWorkingSetModel.addPropertyChangeListener(fListener);
		}
	}*/
	
	public PydevPackageExplorerContentProvider() {
		IWorkbenchPage page = BrowsingPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
//		IWorkbenchPage pages[] = BrowsingPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getPages();
		//IWorkbenchPart part = page.getActivePart();		
//		PydevPackageExplorer pe = (PydevPackageExplorer)part;
		if( contentProvider==null ) {
			contentProvider = this;		
			
			fListener= new IPropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					workingSetModelChanged(event);
				}
			};		
		}
	}
	
	@Override
	public Object[] getElements(Object element) {
		System.out.println("getElements: " + element.getClass());
		return super.getElements(element);
	}

	@Override
	public Object[] getChildren(Object element) {		
		if( element instanceof IFile && ((IFile)element).getName().endsWith(".py") ) {
			IFile file = (IFile)element;
			InputStream is;
			try {										
				DefinitionsASTIteratorVisitor visitor = cache.get(element);
				if( visitor==null ) {
					is = file.getContents();
					byte temp[] = new byte[is.available()];
					is.read(temp);
					Document doc = new Document( new String(temp) );
					SourceModule module = (SourceModule)AbstractModule.createModuleFromDoc(file.getName(), null, doc, null, 0);
					visitor = DefinitionsASTIteratorVisitor.create(module.getAst());
				}
		        if(visitor == null){
		            return EMPTY_ELEMENT;
		        }
		        Iterator<ASTEntry> it = visitor.getOutline();
		        ArrayList<CompositeASTEntry> list = new ArrayList<CompositeASTEntry>();
		        while(it.hasNext()){
		            CompositeASTEntry composite = new CompositeASTEntry( it.next(), file );
		            cache.put( composite, visitor);
		            if(composite.getEntry().parent == null){			            	
		                list.add( composite );
		            }
		        }			        
		        cache.put( element, visitor );
		        return list.toArray(new CompositeASTEntry[0]);
			} catch (CoreException e) {				
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}	
		} else if( element instanceof CompositeASTEntry ) {		
			ASTEntry entry = ( (CompositeASTEntry) element ).getEntry();
		
	        if (entry.node instanceof ClassDef || entry.node instanceof FunctionDef) {
	        	DefinitionsASTIteratorVisitor visitor = cache.get(element);
	        	if( visitor==null ) {
	        		return EMPTY_ELEMENT;
	        	}
	            Iterator<ASTEntry> it = visitor.getOutline();
	            ArrayList<CompositeASTEntry> list = new ArrayList<CompositeASTEntry>();
	            while (it.hasNext()) {
	                CompositeASTEntry composite = new CompositeASTEntry( it.next(), ( (CompositeASTEntry) element ).getFile() );
	                if(composite.getEntry().parent != null && composite.getEntry().parent.node == entry.node){             
	                	list.add(composite);
	                }
	            }	            
	            CompositeASTEntry[] array = list.toArray(new CompositeASTEntry[0]);
	            return array;
	        }
		} /*else if( element instanceof IFolder ) {
			IFolder folder = (IFolder)element;			
			IPythonPathNature pathNature = PythonNature.getPythonPathNature(folder.getProject());
			String srcPath = null;
			try {
				srcPath = pathNature.getProjectSourcePath();
				System.out.println("srcPath " + srcPath);
				if( !isFlatLayout() ) {
					return super.getChildren(element);
				} else {
					return getChildrenInHierarchicalMode(folder);
				}				
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}*/ if (element instanceof WorkingSetModel) {
			PydevPackageExplorer part = null;
	        IWorkbenchPage temp[] = BrowsingPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getPages();
	        for( IWorkbenchPage p : temp ) {
	        	for(IViewReference r : p.getViewReferences()){
	        		if( r.getView(false) instanceof PydevPackageExplorer )
	        			part = (PydevPackageExplorer) r.getView(false);
	        	}
	        }
	        if( part!=null ) {
	        	fWorkingSetModel = part.getWorkingSetModel();
	        	Assert.isTrue(fWorkingSetModel == element);
				
				return fWorkingSetModel.getActiveWorkingSets();
	        }		
		} else if (element instanceof IWorkingSet) {
			return filterClosedElements(fWorkingSetModel.getChildren((IWorkingSet)element));
		} else if( element instanceof PackageFragment ) {
			return ((PackageFragment)element).getChildren();
		}
		return super.getChildren(element);		
	}
	
	private Object[] getChildrenInHierarchicalMode(IFolder currentFolder) {
		ArrayList<PackageFragment> fragments = new ArrayList<PackageFragment>();		
		
		PackageFragment fragment = new PackageFragment();
		fragment.insertFragment(currentFolder);
		fragments.add(fragment);
		
		Object[] elements = super.getChildren(currentFolder);
		ArrayList<IFolder> folders = new ArrayList<IFolder>();
		for( int i=0; i<elements.length; i++ ) {
			if( !( elements[i] instanceof IFolder)  ) {
				fragment.insertChildren(elements[i]);
			} else {
				folders.add( (IFolder)elements[i] );								
			}
		}
		
		return fragments.toArray();
	}
	
	private void extractChildrenOfPackageFragment( ArrayList<PackageFragment> fragments ) {
				
	}
	
	public boolean isFlatLayout() {
		return flatLayout;
	}

	public void setFlatLayout(boolean flatLayout) {
		this.flatLayout = flatLayout;
	}
	
	private void workingSetModelChanged(PropertyChangeEvent event) {	
		String property= event.getProperty();
		Object newValue= event.getNewValue();
		List toRefresh= new ArrayList(1);		
		if (WorkingSetModel.CHANGE_WORKING_SET_MODEL_CONTENT.equals(property)) {			
			toRefresh.add(fWorkingSetModel);
			fViewer.setInput( fWorkingSetModel );
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(property)) {			
			toRefresh.add(newValue);
			fViewer.setInput( newValue );
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE.equals(property)) {
			toRefresh.add(newValue);
			fViewer.setInput( newValue );
		}
		fViewer.refresh();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof IWorkingSet)
			return true;
		return super.hasChildren(element);
	}
	
	private Object[] filterClosedElements(Object[] children) {
		List result= new ArrayList(children.length);
		for (int i= 0; i < children.length; i++) {
			Object element= children[i];
			boolean add= false;
			if (element instanceof IProject) {
				add= true;
			} else if (element instanceof IResource) {
				IProject project= ((IResource)element).getProject();
				add= project == null || project.isOpen();
			}
			if (add) {
				result.add(element);
			}
		}
		return result.toArray();
	}
	
	public Object getParent(Object child) {
		if( child instanceof CompositeASTEntry ) {
			ASTEntry entry = ( (CompositeASTEntry) child ).getEntry();
			return entry.parent;
		} else if( fWorkingSetModel!=null ){
			Object[] parents= fWorkingSetModel.getAllParents(child);
			if(parents.length == 0)
				return super.getParent(child);
			Object first= parents[0];
			if (first instanceof IWorkingSet && HistoryWorkingSetUpdater.ID.equals(((IWorkingSet)first).getId())) {
				if (parents.length > 1) {
					return parents[1];
				} else {
					return super.getParent(child);
				}
			}
			return first;
		} else {
			return super.getParent(child);
		}
	}
	
	public void onSave(PyEdit edit) {
		IDocument document = edit.getDocument();
		
		IWorkbenchPage page = BrowsingPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IFile file = (IFile)page.getActiveEditor().getEditorInput().getAdapter(IFile.class);		
		
		SourceModule module = (SourceModule)AbstractModule.createModuleFromDoc(file.getName(), null, document, null, 0);
        if(module == null || module.getAst() == null){
            return;
        }
		DefinitionsASTIteratorVisitor visitor = DefinitionsASTIteratorVisitor.create(module.getAst());
		
		Iterator<ASTEntry> it = visitor.getOutline();
        ArrayList<CompositeASTEntry> list = new ArrayList<CompositeASTEntry>();        
        while(it.hasNext()){
            CompositeASTEntry composite = new CompositeASTEntry( it.next(), file );
            cache.put( composite, visitor);
            if(composite.getEntry().parent == null){			            	
                list.add( composite );
            }
        }			        
        cache.put( file, visitor );        
        
        PydevPackageExplorer part = null;
        IWorkbenchPage temp[] = BrowsingPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getPages();
        for( IWorkbenchPage p : temp ) {
        	for(IViewReference r : p.getViewReferences()){
        		if( r.getView(false) instanceof PydevPackageExplorer )
        			part = (PydevPackageExplorer) r.getView(false);
        	}
        }
//		IWorkbenchPage pages[] = BrowsingPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getPages();
		//IWorkbenchPart part = page.getActivePart();		
//		PydevPackageExplorer pe = (PydevPackageExplorer)part;
        if( part!=null ) {
        	part.getViewer().refresh();
        }                
	}

    public void onCreateActions(ListResourceBundle resources, PyEdit edit) {
        //do nothing
    }

    public void onDispose(PyEdit edit) {
    }

    public void onSetDocument(IDocument document, PyEdit edit) {
    }
}
