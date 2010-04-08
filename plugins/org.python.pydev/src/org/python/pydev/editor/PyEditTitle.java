package org.python.pydev.editor;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPartReference;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.plugin.PydevPlugin;

/**
 * The whole picture:
 * 
 * 1. In django it's common to have multiple files with the same name
 * 
 * 2. __init__ files are everywhere
 * 
 * We need a way to uniquely identify those.
 * 
 * Options:
 * 
 * - For __init__ files, an option would be having a different icon and adding the package
 * name instead of the __init__ (so if an __init__ is under my_package, we would show
 * only 'my_package' and would change the icon for the opened editor).
 * 
 * - For the default django files (models.py, settings.py, tests.py, views.py), we could use
 * the same approach -- in fact, make that configurable!
 * 
 * - For any file (including the cases above), if the name would end up being duplicated, change
 * the title so that all names are always unique.
 * 
 */
/*default*/ class PyEditTitle {

    /**
     * A lock helper to use when initializing the title (as we'll access others, we cannot
     * have 2 initializing the title at the same time).
     */
    private static final Object titleLock = new Object();
    
    /**
     * 2 pydev editors should never have the same title, so, this method will make sure that
     * this won't happen.
     */
	/*default*/ void initializeTitle(PyEdit pyEdit, IEditorInput input) {
		synchronized(titleLock){
			if(input == null){
				return;
			}
			String currentName = pyEdit.getPartName();
			
			IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			IEditorReference[] editorReferences = workbenchWindow.getActivePage().getEditorReferences();
			List<IEditorReference> editorsWithSameName = new ArrayList<IEditorReference>();
			
			for (IEditorReference iEditorReference : editorReferences) {
				if(!PyEdit.EDITOR_ID.equals(iEditorReference.getId())){
					continue; //only analyze Pydev editors
				}
				try {
					IEditorInput otherInput = iEditorReference.getEditorInput();
					
					//Always get the 'original' name and not the currently set name, because
					//if we previously had an __init__.py editor which we renamed to package/__init__.py
					//and we open a new __init__.py, we want it renamed to new_package/__init__.py
					String editorPartName = otherInput.getName();
					if(currentName.equals(editorPartName)){
						if(otherInput == input){
							//Try to restore it at this point!
							try {
								Field attr = REF.getAttrFromClass(WorkbenchPartReference.class, "state");
								attr.setAccessible(true);
								int state = attr.getInt(iEditorReference);
								if(state == WorkbenchPartReference.STATE_CREATION_IN_PROGRESS){
									continue; //no need to keep going on (same input and creationg in progress means it's this editor).
								}
							} catch (Throwable e) {
								//Ignore... just trying to get around the ugly logged error: 
								//"Warning: Detected recursive attempt by part {0} to create itself (this is probably, but not necessarily, a bug)"
								//when getEditor(true) is used in the editor initialization
							}
							IEditorPart part = iEditorReference.getEditor(true);
							if(part == null || part == this){
								continue; // this is the current editor (we'll be unable to restore it if it still wasn't created and the input isn't just changing.)
							}
						}

						editorsWithSameName.add(iEditorReference);
					}
				} catch (PartInitException e) {
					PydevPlugin.log(e);
				}
			}
			
			if(editorsWithSameName.size() > 0){
				//There are other editors with the same name... let's make them unique!
				ArrayList<Tuple<PyEdit, IPath>> partsAndPaths = getPartsAndPaths(
						pyEdit, editorsWithSameName, input);
				int level = 0;
				List<String> names = new ArrayList<String>();
				Map<String, Integer> allNames = new HashMap<String, Integer>();
				
				do{
					names.clear();
					allNames.clear();
					level ++;
					for (int i=0; i<partsAndPaths.size();i++) {
						Tuple<PyEdit, IPath> tuple = partsAndPaths.get(i);
						
						Tuple<String, Boolean> nameAndReachedMax = getPartNameInLevel(level, tuple.o2);
						if(nameAndReachedMax.o2){
							tuple.o1.setEditorTitle(nameAndReachedMax.o1);
							partsAndPaths.remove(i);
							i--; //make up for the removed editor.
							continue;
						}
						names.add(nameAndReachedMax.o1);
						Integer count = allNames.get(nameAndReachedMax.o1);
						if(count == null){
							allNames.put(nameAndReachedMax.o1, 1);
						}else{
							allNames.put(nameAndReachedMax.o1, count+1);
						}
					}
					for (int i=0; i<partsAndPaths.size();i++) {
						String n = names.get(i);
						Integer count = allNames.get(n);
						if(count == 1){
							Tuple<PyEdit, IPath> tuple = partsAndPaths.get(i);
							tuple.o1.setEditorTitle(n);
							
							partsAndPaths.remove(i);
							names.remove(i);
							allNames.remove(n);
							i--; //make up for the removed editor.
						}
					}
				}while(allNames.size() > 0);
			}
		}
		
	}
	
	
	/**
	 * @return a tuple with the part name and a boolean indicating if the maximum level has been
	 * reached for this path.
	 */
	private Tuple<String, Boolean> getPartNameInLevel(int level, IPath path) {
		String[] segments = path.segments();
		if(segments.length == 0){
			return new Tuple<String, Boolean>("", true);
		}
		if(segments.length == 1){
			return new Tuple<String, Boolean>(segments[1], true);
		}
		
		int startAt = segments.length - level;
		if(startAt < 0){
			startAt = 0;
		}
		
		int endAt = segments.length-1;
		
		String modulePart = StringUtils.join(".", segments, startAt, endAt);
		
		if(modulePart.length() > 0){
			return new Tuple<String, Boolean>(segments[segments.length-1]+" ("+modulePart+")", startAt == 0);
		}else{
			return new Tuple<String, Boolean>(segments[segments.length-1], startAt == 0);
		}
		
	}

	
	/**
	 * Will get the path for each editor and the corresponding part (so that we can change its name).
	 * @param input 
	 * @return 
	 */
	private ArrayList<Tuple<PyEdit, IPath>> getPartsAndPaths(PyEdit pyEdit, List<IEditorReference> editorsWithSameName, IEditorInput input) {
		ArrayList<Tuple<PyEdit, IPath>> list = new ArrayList<Tuple<PyEdit, IPath>>();
		//Start adding the current editor to the list.
		IPath pathFromInput = getPathFromInput(input);
		if(pathFromInput == null){
			return list; //if we're unable to get the path for this editor, there's no point in going on.
		}
		list.add(new Tuple<PyEdit, IPath>(pyEdit, pathFromInput));

		
		for (IEditorReference other : editorsWithSameName) {
			IWorkbenchPart p = other.getPart(true); //Yes, we need it restored to change its name
			if(!(p instanceof PyEdit)){
				continue;
			}
			PyEdit part = (PyEdit) p; 
			IEditorInput otherInput;
			try {
				otherInput = other.getEditorInput();
				IPath path = getPathFromInput(otherInput);
				if(path != null){
					//if we weren't able to get the path, we won't change this editor!
					list.add(new Tuple<PyEdit, IPath>(part, path));
				}
			} catch (PartInitException e) {
				PydevPlugin.log(e);
				continue;
			}
		}
		return list;
	}

	/**
	 * @return This is the Path that the editor is editing.
	 */
	private IPath getPathFromInput(IEditorInput otherInput) {
		IPath path = null;
		if(otherInput instanceof IPathEditorInput){
			IPathEditorInput iPathEditorInput = (IPathEditorInput) otherInput;
			path = iPathEditorInput.getPath();
		}
		if(path == null){
			if(otherInput instanceof IFileEditorInput){
				IFileEditorInput iFileEditorInput = (IFileEditorInput) otherInput;
				path = iFileEditorInput.getFile().getFullPath();
			}
		}
		if(path == null){
			try {
				if(otherInput instanceof IURIEditorInput){
					IURIEditorInput iuriEditorInput = (IURIEditorInput) otherInput;
					path = Path.fromOSString(new File(iuriEditorInput.getURI()).toString());
				}
			} catch (Throwable e) {
				//Ignore (IFileEditorInput not available on 3.2)
			}
		}
		return path;
	}
    
}
