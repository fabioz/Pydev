package org.python.pydev.editor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.concurrency.SingleJobRunningPool;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.uiutils.RunInUiThread;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PyTitlePreferencesPage;

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
 * the title so that all names are always unique (note that the same name may still be used if
 * the icon is different).
 */
/*default*/ final class PyEditTitle implements IPropertyChangeListener {
    
	/**
	 * Singleton access for the title management.
	 */
    private static PyEditTitle singleton;
    
    /**
     * Lock for accessing the singleton.
     */
    private static Object lock = new Object();
    
    /**
     * Helper to ensure that only a given job is running at some time.
     */
    private SingleJobRunningPool jobPool = new SingleJobRunningPool();
    
    private PyEditTitle(){
    	IPreferenceStore preferenceStore = PydevPlugin.getDefault().getPreferenceStore();
    	preferenceStore.addPropertyChangeListener(this);
    }
    
	public void propertyChange(PropertyChangeEvent event) {
		//When the 
		String property = event.getProperty();
		if(PyTitlePreferencesPage.isTitlePreferencesProperty(property)){
			
	    	Job job = new Job("Invalidate title") {
				
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						List<IEditorReference> currentEditorReferences;
						do{
							currentEditorReferences = getCurrentEditorReferences();
							synchronized (this) {
								try {
									Thread.sleep(200);
								} catch (InterruptedException e) {
									//ignore.
								}
							}
						}while(PydevPlugin.isAlive() && currentEditorReferences == null); //stop trying if the plugin is stopped;
						
						if(currentEditorReferences != null){
							final List<IEditorReference> refs = currentEditorReferences;
							RunInUiThread.sync(new Runnable() {
								
								public void run() {
									for (final IEditorReference iEditorReference : refs) {
										final IEditorPart editor = iEditorReference.getEditor(true);
										if(editor instanceof PyEdit){
											try {
												invalidateTitle((PyEdit) editor, iEditorReference.getEditorInput());
											} catch (PartInitException e) {
												//ignore
											}
										}
									}
								}
							});
						}

					} finally {
						jobPool.removeJob(this);
					}
					return Status.OK_STATUS;
				}
			};
			job.setPriority(Job.SHORT); 
			jobPool.addJob(job);	
		}
	}

    
    /**
     * This method will update the title of all the editors that have a title that would match
     * the passed input.
     * 
     * Note that on the first try it will update the images of all editors.
     * @param pyEdit 
     */
    public static void invalidateTitle(PyEdit pyEdit, IEditorInput input){
    	synchronized (lock) {
    		boolean createdSingleton = false;
    		if(singleton == null){
    			singleton = new PyEditTitle();
    			createdSingleton = true;
    			
    		}
			//updates the title and image for the passed input.
			singleton.invalidateTitleInput(pyEdit, input);
			
			if(createdSingleton){
				//In the first time, we need to invalidate all icons (because eclipse doesn't restore them the way we left them).
				//Note that we don't need to do that for titles because those are properly saved on close/restore.
				singleton.restoreAllPydevEditorsWithDifferentIcon();
			}
		}
    }
    
    
    /**
     * Sadly, we have to restore all pydev editors that have a different icon to make it correct.
     * 
     * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=308740
     */
    private void restoreAllPydevEditorsWithDifferentIcon() {
    	if(!PyTitlePreferencesPage.useCustomInitIcon()){
    		return;
    	}
    	Job job = new Job("Invalidate images") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					while(PydevPlugin.isAlive() && !doRestoreAllPydevEditorsWithDifferentIcons()){ //stop trying if the plugin is stopped
						synchronized (this) {
							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
								//ignore.
							}
						}
					};
				} finally {
					jobPool.removeJob(this);
				}
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.SHORT); 
		jobPool.addJob(job);	
    }
    
    /**
     * Sadly, we have to restore all pydev editors to make the icons correct.
     * 
     * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=308740
     */
	private boolean doRestoreAllPydevEditorsWithDifferentIcons() {
		if(!PyTitlePreferencesPage.useCustomInitIcon()){
			return true; //Ok, nothing custom!
		}
		
		final List<IEditorReference> editorReferences = getCurrentEditorReferences();
		if(editorReferences == null){
			//couldn't be gotten.
			return false;
		}
		//Update images
		
		RunInUiThread.async(new Runnable() {
			
			public void run() {
				for (IEditorReference iEditorReference : editorReferences) {
					try {
						IPath pathFromInput = getPathFromInput(iEditorReference.getEditorInput());
						String lastSegment = pathFromInput.lastSegment();
						if(lastSegment != null && lastSegment.startsWith("__init__.")){
							iEditorReference.getEditor(true); //restore it.
						}
					} catch (PartInitException e) {
						//ignore
					}
					
					//Note, removed the code below -- just restoring the editor is enough and the
					//only way to make it work for now. See: https://bugs.eclipse.org/bugs/show_bug.cgi?id=308740
					
	//			try {
	//				IEditorInput input = iEditorReference.getEditorInput();
	//				IPath path = getPathFromInput(input);
	//				updateImage(null, iEditorReference, path);
	//			} catch (PartInitException e) {
	//				//ignore
	//			}
				}
			}
		});
		
		return true;
	}
    

    
	/**
	 * Updates the title text and image of the given pyEdit (based on the passed input).
	 * 
	 * That will be done depending on the other open editors (if the user has chosen
	 * unique names).
	 */
    private void invalidateTitleInput(final PyEdit pyEdit, final IEditorInput input) {
    	if(input == null){
    		return;
    	}
    	
		final IPath pathFromInput = getPathFromInput(input);
		if(pathFromInput == null || pathFromInput.segmentCount() == 0){
			return; //not much we can do!
		}
		

		final String lastSegment = pathFromInput.lastSegment();
		if(lastSegment == null){
			return;
		}
		
		final String initHandling = PyTitlePreferencesPage.getInitHandling();
		//initially set this as the title (and change it later to a computed name).
		String computedEditorTitle = getPartNameInLevel(1, pathFromInput, initHandling).o1;

		pyEdit.setEditorTitle(computedEditorTitle);
		updateImage(pyEdit, null, pathFromInput);
		
		if(!PyTitlePreferencesPage.getEditorNamesUnique()){
			return; //the user accepts having the same name for 2 files, no more work to do.
		}
		
    	Job job = new Job("Invalidate title") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					while(PydevPlugin.isAlive() && !initializeTitle(
							pyEdit, input, pathFromInput, lastSegment, initHandling)){ //stop trying if the plugin is stopped
						synchronized (this) {
							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
								//ignore.
							}
						}
					};
				} finally {
					jobPool.removeJob(this);
				}
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.SHORT); 
		jobPool.addJob(job);
    	
	}



    /**
     * Updates the image of the passed editor.
     */
	private void updateImage(PyEdit pyEdit, IEditorReference iEditorReference, IPath path) {
		String lastSegment = path.lastSegment();
		if(lastSegment != null && lastSegment.startsWith("__init__.")){
			Image initIcon = PyTitlePreferencesPage.getInitIcon();
			if(initIcon != null){
				if(pyEdit != null){
					pyEdit.setEditorImage(initIcon);
				}else{
					setEditorReferenceImage(iEditorReference, initIcon);
				}
			}
		}
	}


	/**
     * 2 pydev editors should never have the same title, so, this method will make sure that
     * this won't happen.
     * 
     * @return true if it was able to complete and false if some requisite is not available.
     */
	private boolean initializeTitle(
			final PyEdit pyEdit, 
			IEditorInput input, 
			final IPath pathFromInput, 
			String lastSegment, 
			String initHandling) {
		
		List<IEditorReference> editorReferences = getCurrentEditorReferences();
		if(editorReferences == null){
			//couldn't be gotten.
			return false;
		}
		
		List<Tuple<IEditorReference, IPath>> partsAndPaths = removeEditorsNotMatchingCurrentName(lastSegment,
				editorReferences);
		
		if(partsAndPaths.size() > 1){
			//There are other editors with the same name... (1 is the editor that requested the change)
			//let's make them unique!
			int level = 0;
			List<String> names = new ArrayList<String>();
			Map<String, Integer> allNames = new HashMap<String, Integer>();
			
			do{
				names.clear();
				allNames.clear();
				level ++;
				for (int i=0; i<partsAndPaths.size();i++) {
					Tuple<IEditorReference, IPath> tuple = partsAndPaths.get(i);
					
					Tuple<String, Boolean> nameAndReachedMax = getPartNameInLevel(level, tuple.o2, initHandling);
					if(nameAndReachedMax.o2){ //maximum level reached for path
						setEditorReferenceTitle(tuple.o1, nameAndReachedMax.o1);
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
					String finalName = names.get(i);
					Integer count = allNames.get(finalName);
					if(count == 1){ //no duplicate found
						Tuple<IEditorReference, IPath> tuple = partsAndPaths.get(i);
						setEditorReferenceTitle(tuple.o1, finalName);
						
						partsAndPaths.remove(i);
						names.remove(i);
						allNames.remove(finalName);
						i--; //make up for the removed editor.
					}
				}
			}while(allNames.size() > 0);
		}
		return true;
	}

	/**
	 * @return a list of all the editors that have the last segment as 'currentName'
	 */
	private List<Tuple<IEditorReference, IPath>> removeEditorsNotMatchingCurrentName(String currentName,
			List<IEditorReference> editorReferences) {
		ArrayList<Tuple<IEditorReference, IPath>> ret = new ArrayList<Tuple<IEditorReference, IPath>>();
		for (Iterator<IEditorReference> it= editorReferences.iterator(); it.hasNext();) {
			IEditorReference iEditorReference = it.next();
			try {
				IEditorInput otherInput = iEditorReference.getEditorInput();
				
				//Always get the 'original' name and not the currently set name, because
				//if we previously had an __init__.py editor which we renamed to package/__init__.py
				//and we open a new __init__.py, we want it renamed to new_package/__init__.py
				IPath pathFromOtherInput = getPathFromInput(otherInput);
				if(pathFromOtherInput == null){
					continue;
				}
				
				String lastSegment = pathFromOtherInput.lastSegment();
				if(lastSegment == null){
					continue;
				}

				if(!currentName.equals(lastSegment)){
					continue;
				}
				ret.add(new Tuple<IEditorReference, IPath>(iEditorReference, pathFromOtherInput));
			} catch (Throwable e) {
				PydevPlugin.log(e);
			}
		}
		return ret;
	}

	/**
	 * @return the current editor references or null if no editor references are available.
	 * 
	 * Note that this method may be slow as it will need UI access (which is asynchronously
	 * gotten)
	 */
	private List<IEditorReference> getCurrentEditorReferences() {
		final List<IEditorReference[]> editorReferencesFound = new ArrayList<IEditorReference[]>(); 
		
		RunInUiThread.async(new Runnable() {
			
			public void run() {
				IEditorReference[] found = null;
				try{
					IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					if(workbenchWindow == null){
						return;
					}
					IWorkbenchPage activePage = workbenchWindow.getActivePage();
					if(activePage == null){
						return;
					}
					found = activePage.getEditorReferences();
				}finally{
					editorReferencesFound.add(found);
				}
			}
		});
		while(editorReferencesFound.size() == 0){
			synchronized (this) {
				try {
					wait(10);
				} catch (InterruptedException e) {
					//ignore
				}
			}
		}
		IEditorReference[] editorReferences = editorReferencesFound.get(0);
		if(editorReferences == null){
			return null;
		}
		ArrayList<IEditorReference> ret = new ArrayList<IEditorReference>();
		for(IEditorReference iEditorReference:editorReferences){
			if(!PyEdit.EDITOR_ID.equals(iEditorReference.getId())){
				continue; //only analyze Pydev editors
			}
			ret.add(iEditorReference);
		}
		return ret;
	}

	/**
	 * Sets the image of the passed editor reference. Will try to restore the editor for
	 * doing that. 
	 * 
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=308740
	 */
	private void setEditorReferenceImage(final IEditorReference iEditorReference, final Image image) {
		RunInUiThread.async(new Runnable() {
			
			public void run() {
				try {
					IEditorPart editor = iEditorReference.getEditor(true);
					if(editor instanceof PyEdit){
						((PyEdit) editor).setEditorImage(image);
					}
						
				} catch (Throwable e) {
					PydevPlugin.log(e);
				}
			}
		});
	}
	
	
	/**
	 * Sets the title of the passed editor reference. Will try to restore the editor for
	 * doing that. 
	 * 
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=308740
	 */
	private void setEditorReferenceTitle(final IEditorReference iEditorReference, final String title) {
		if(title.equals(iEditorReference.getTitle())){
			//Nothing to do if it's already the same.
			return;
		}
		
		RunInUiThread.async(new Runnable() {
			
			public void run() {
				try {
					IEditorPart editor = iEditorReference.getEditor(true);
					if(editor instanceof PyEdit){
						((PyEdit) editor).setEditorTitle(title);
					}
				} catch (Throwable e) {
					PydevPlugin.log(e);
				}
			}
		});
	}
	
	
	/**
	 * @return a tuple with the part name to be used and a boolean indicating if the maximum level 
	 * has been reached for this path.
	 */
	private Tuple<String, Boolean> getPartNameInLevel(int level, IPath path, String initHandling) {
		String[] segments = path.segments();
		if(segments.length == 0){
			return new Tuple<String, Boolean>("", true);
		}
		if(segments.length == 1){
			return new Tuple<String, Boolean>(segments[1], true);
		}
		String lastSegment = segments[segments.length-1];
		
		//Yes, in this case we can compare a string with == and !=
		if(initHandling != PyTitlePreferencesPage.TITLE_EDITOR_INIT_HANDLING_IN_TITLE){
			if(lastSegment.startsWith("__init__.")){
				//remove the __init__.
				String[] dest = new String[segments.length-1];
				System.arraycopy(segments, 0, dest, 0, dest.length);
				segments = dest;
			}
		}
		
		int startAt = segments.length - level;
		if(startAt < 0){
			startAt = 0;
		}
		
		int endAt = segments.length-1;
		
		String modulePart = StringUtils.join(".", segments, startAt, endAt);
		
		String name = segments[segments.length-1];
		if(!PyTitlePreferencesPage.getTitleShowExtension()){
			name = FullRepIterable.getFirstPart(name);
		}
		if(modulePart.length() > 0){
			return new Tuple<String, Boolean>(name+" ("+modulePart+")", startAt == 0);
		}else{
			return new Tuple<String, Boolean>(name, startAt == 0);
		}
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
				//Ignore (IURIEditorInput not available on 3.2)
			}
		}
		return path;
	}

    
}
