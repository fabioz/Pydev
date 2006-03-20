package com.python.pydev.browsing.view;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.editor.model.AttributeNode;
import org.python.pydev.editor.model.ImportFromNode;
import org.python.pydev.editor.model.ImportNode;
import org.python.pydev.editor.model.NameEqualsMainNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.plugin.PydevPlugin;

import com.python.pydev.browsing.ui.UIConstants;

public class PackageExplorerLabelProvider extends LabelProvider{	
	private String PYDEV_PROJECT_NATURE = "org.python.pydev.pythonNature";
	private ImageCache imageCache;

	public PackageExplorerLabelProvider() {
		imageCache = new ImageCache(PydevPlugin.getDefault().getBundle().getEntry("/"));
	}
	
	@Override
	public String getText(Object element) {		
		if( element instanceof IProject ) {
			return ((IProject)element).getName();		
		} else if( element instanceof IFolder ) {
			return ((IFolder)element).getName();
		} else if( element instanceof IFile ) {
			return ((IFile)element).getName();
		} else if( element instanceof CompositeASTEntry ) {
			ASTEntry entry = ((CompositeASTEntry)element).getEntry();			
			return entry.getName();
		} else if( element instanceof IWorkingSet ) {
			return ((IWorkingSet)element).getName();
		} else if( element instanceof PackageFragment ) {
			return ((PackageFragment)element).getText();
		}
		return super.getText(element);
	}
	
	@Override
	public Image getImage(Object element) {		
		if( element instanceof IFolder ) {
			return imageCache.get(UIConstants.FOLDER_ICON);
		} else if( element instanceof CompositeASTEntry ) {
			ASTEntry entry = ((CompositeASTEntry)element).getEntry();			
			if( entry.node instanceof ClassDef ) {
				return imageCache.get(UIConstants.CLASS_ICON);
			} else if( entry.node instanceof FunctionDef ) {
				if (NodeUtils.getNameFromNameTok((NameTok) ((FunctionDef)entry.node).name).startsWith("_")) {
					return imageCache.get(UIConstants.PRIVATE_METHOD_ICON);
				}
				else
					return imageCache.get(UIConstants.PUBLIC_METHOD_ICON);
			} else if ( entry.node instanceof Import || entry.node instanceof ImportFrom ) {
				return imageCache.get(UIConstants.IMPORT_ICON);
			} else if ( entry.node instanceof Attribute ) {				
				return imageCache.get(UIConstants.PROTECTED_METHOD_ICON);
			} else if ( entry.node instanceof Name ) {
				return imageCache.get(UIConstants.PUBLIC_ATTR_ICON);
			}
		} else {
			return WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider().getImage(element);
		}
		return null;
	}
}
