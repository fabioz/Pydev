/*
 * Created on Oct 25, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public abstract class PyDevBuilderVisitor implements IResourceDeltaVisitor {

    public static final int MAX_TO_VISIT_INFINITE = -1;

    /**
     * Method to return whether a resource is an __init__
     * 
     * this is needed because when we create an __init__, all sub-folders 
     * and files on the same folder become valid modules.
     * 
     * @return whether the resource is an init resource
     */
    protected boolean isInitFile(IResource resource){
        return resource.getName().startsWith("__init__.");
    }
    
    /**
     * @param initResource
     * @return all the IFiles that are below the folder where initResource is located.
     */
    protected IResource[] getInitDependents(IResource initResource){
        
        List<IResource> toRet = new ArrayList<IResource>();
        IContainer parent = initResource.getParent();
        
        try {
            fillWithMembers(toRet, parent);
            return toRet.toArray(new IResource[0]);
        } catch (CoreException e) {
            //that's ok, it might not exist anymore
            return new IResource[0];
        }
    }
    
	/**
     * @param toRet
     * @param parent
     * @throws CoreException
     */
    private void fillWithMembers(List<IResource> toRet, IContainer parent) throws CoreException {
        IResource[] resources = parent.members();
        
        for (int i = 0; i < resources.length; i++) {
            if(resources[i].getType() == IResource.FILE){
                toRet.add(resources[i]);
            }else if(resources[i].getType() == IResource.FOLDER){
                fillWithMembers(toRet, (IFolder)resources[i]);
            }
        }
    }


    /**
	 * Visits the resource delta tree determining which files to rebuild (*.py).
	 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
	 */
	public boolean visit(IResourceDelta delta) throws CoreException {
		if(delta == null){
		    return true;
		}

		IResource resource = delta.getResource();

		if(resource == null){
		    return true;
		}

        int type = resource.getType();
	

        //related bug https://sourceforge.net/tracker/index.php?func=detail&aid=1238850&group_id=85796&atid=577329
        
        //the team-support plugins of eclipse use the IResource
        //method setTeamPrivateMember to indicate resources
        //that are only in the project for the team-stuff (e.g. .svn or
        //.cvs or _darcs directories).
        if (resource.isTeamPrivateMember()){
            return true;
        }
        
		if (type == IResource.FOLDER) {
			switch (delta.getKind()) {
				case IResourceDelta.REMOVED:
				    visitRemovedResource(resource, null);
					break;
				//for folders, we don't have to do anything if added or changed (we just treat their children, that should
				//resolve for modules -- we do, however have to treat __init__.py differently).
			}
		}
		
		
		if (type == IResource.FILE) {
			String ext = resource.getFileExtension();
            if(ext == null){ //resource.getFileExtension() may return null if it has none.
			    return true;
			}
			
            IProject project = resource.getProject();
            PythonNature nature = PythonNature.getPythonNature(project);
            
            if(project != null && nature != null){
                boolean isValidSourceFile = PythonPathHelper.isValidSourceFile("."+ext);
                ProjectModulesManager modulesManager = nature.getAstManager().getProjectModulesManager();

                //we just want to make the visit if it is a valid python file and it is in the pythonpath
                if (isValidSourceFile && modulesManager.isInPythonPath(resource, project)) {
                    
    			    boolean isAddOrChange = false;
    				switch (delta.getKind()) {
    					case IResourceDelta.ADDED :
    					    visitAddedResource(resource, PyDevBuilder.getDocFromResource(resource));
    					    isAddOrChange = true;
    						break;
    					case IResourceDelta.CHANGED:
    					    visitChangedResource(resource, PyDevBuilder.getDocFromResource(resource));
    					    isAddOrChange = true;
    						break;
    					case IResourceDelta.REMOVED:
    					    visitRemovedResource(resource, null);
    						break;
    				}
    
    				//ok, standard visiting ended... now, we have to check if we should visit the other
                    //resources if it was an __init__.py file that changed
    			    if(isAddOrChange && shouldVisitInitDependency() && isInitFile(resource)){
    			        IResource[] initDependents = getInitDependents(resource);
                        
    			        for (int i = 0; i < initDependents.length; i++) {
    			            visitChangedResource(initDependents[i], PyDevBuilder.getDocFromResource(initDependents[i]));
                        }
    			    }
                }
			}
		}
		
		return true;
	}

	/**
	 * 
	 * @return the maximun number of resources that it is allowed to visit (if this
	 * number is higher than the number of resources changed, this visitor is not called).
     */
    public int maxResourcesToVisit() {
        return MAX_TO_VISIT_INFINITE;
    }
	
    /**
     * if all the files below a folder that has an __init__.py just added or removed should 
     * be visited, this method should return true, otherwise it should return false 
     * 
     * @return false by default, but may be reimplemented in subclasses. 
     */
    public boolean shouldVisitInitDependency(){
        return false;
    }

    /**
     * Called when a resource is changed
     * 
     * @param resource to be visited.
     */
    public abstract boolean visitChangedResource(IResource resource, IDocument document);

    
    /**
     * Called when a resource is added. Default implementation calls the same method
     * used for change.
     * 
     * @param resource to be visited.
     */
    public boolean visitAddedResource(IResource resource, IDocument document){
        return visitChangedResource(resource, document);
    }

    /**
     * Called when a resource is removed
     * 
     * @param resource to be visited.
     */
    public abstract boolean visitRemovedResource(IResource resource, IDocument document);

    /**
     * Checks pre-existance of marker.
     */
    public static IMarker markerExists(IResource resource, String message, int charStart, int charEnd, String type) {
        IMarker[] tasks;
        try {
            tasks = resource.findMarkers(type, true, IResource.DEPTH_ZERO);
            for (int i = 0; i < tasks.length; i++) {
                IMarker task = tasks[i];
                
                boolean eqMessage = task.getAttribute(IMarker.MESSAGE).equals(message);
                
                boolean eqCharStart = (Integer)task.getAttribute(IMarker.CHAR_START) == charStart;
                boolean eqCharEnd = (Integer)task.getAttribute(IMarker.CHAR_END) == charEnd;
                
                if (eqMessage && eqCharStart && eqCharEnd){
                    return task;
                }
            }
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
        return null;
    }


    /**
     * Checks pre-existance of marker.
     * 
     * @param resource resource in wich marker will searched
     * @param message message for marker
     * @param lineNumber line number where marker should exist
     * @return pre-existance of marker
     */
    public static IMarker markerExists(IResource resource, String message, int lineNumber, String type) {
        IMarker[] tasks;
        try {
            tasks = resource.findMarkers(type, true, IResource.DEPTH_ZERO);
            for (int i = 0; i < tasks.length; i++) {
                IMarker task = tasks[i];
                boolean eqLineNumber = (Integer)task.getAttribute(IMarker.LINE_NUMBER) == lineNumber;
                boolean eqMessage = task.getAttribute(IMarker.MESSAGE).equals(message);
                if (eqLineNumber && eqMessage){
                    return task;
                }
            }
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Creates the marker for the problem.
     * 
     * @param resource resource for wich marker will be created
     * @param message message for marker
     * @param lineNumber line number of where marker will be tagged on to resource
     * @return
     */
    public static void createProblemMarker(IResource resource, String message, int lineNumber) {
        String markerType = IMarker.PROBLEM;

        createWarningMarker(resource, message, lineNumber, markerType);
    }

    /**
     * @param resource
     * @param message
     * @param lineNumber
     * @param markerType
     * @return
     */
    public static void createWarningMarker(IResource resource, String message, int lineNumber, String markerType) {
    
        int severity = IMarker.SEVERITY_WARNING;
        createMarker(resource, message, lineNumber, markerType, severity);
    }

    public static void createMarker(IResource resource, IDocument doc, String message, 
            int lineStart, int colStart, int lineEnd, int colEnd, 
            String markerType, int severity) {

        if(lineStart < 0){
            lineStart = 0;
        }
        
        int startAbsolute;
        int endAbsolute;
        
        try {
            IRegion start = doc.getLineInformation(lineStart);
            startAbsolute = start.getOffset() + colStart;
            if (lineEnd >= 0 && colEnd >= 0) {
                IRegion end = doc.getLineInformation(lineEnd);
                endAbsolute = end.getOffset() + colEnd;
            } else {
                //ok, we have to calculate it based on the line contents...
                String line = doc.get(start.getOffset(), start.getLength());
                int i;
                StringBuffer buffer;
                if((i = line.indexOf('#')) != -1){
                    buffer = new StringBuffer(line.substring(0, i));
                }else{
                    buffer = new StringBuffer(line);
                }
                while(buffer.length() > 0 && Character.isWhitespace(buffer.charAt(buffer.length() - 1))){
                    buffer.deleteCharAt(buffer.length() -1);
                }
                endAbsolute = start.getOffset() + buffer.length();
            }
        } catch (BadLocationException e) {
            throw new RuntimeException("Unable to get the location requested for the resource "+resource.getLocation(), e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        IMarker marker = markerExists(resource, message, startAbsolute, endAbsolute, markerType);
        if (marker == null) {
            try {
                
                
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put(IMarker.MESSAGE, message);
                map.put(IMarker.LINE_NUMBER, new Integer(lineStart));
                map.put(IMarker.CHAR_START, new Integer(startAbsolute));
                map.put(IMarker.CHAR_END, new Integer(endAbsolute));
                map.put(IMarker.SEVERITY, new Integer(severity));
                
                MarkerUtilities.createMarker(resource, map, markerType);
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }
    }
    
    public static void createMarker(IResource resource, String message, int lineNumber, String markerType, int severity) {
        if(lineNumber <= 0)
            lineNumber = 0;
        IMarker marker = markerExists(resource, message, lineNumber, markerType);
        if (marker == null) {
            try {
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put(IMarker.MESSAGE, message);
                map.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
                map.put(IMarker.SEVERITY, new Integer(severity));

                MarkerUtilities.createMarker(resource, map, markerType);
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void createMarker(IResource resource, String message, int lineNumber, String markerType, int severity, boolean userEditable, boolean istransient) {
        if(lineNumber <= 0)
            lineNumber = 0;
        IMarker marker = markerExists(resource, message, lineNumber, markerType);
        if (marker == null) {
            try {
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put(IMarker.MESSAGE, message);
                map.put(IMarker.LINE_NUMBER, new Integer(lineNumber));
                map.put(IMarker.SEVERITY, new Integer(severity));
                map.put(IMarker.USER_EDITABLE, new Boolean(userEditable));
                map.put(IMarker.TRANSIENT, new Boolean(istransient));

                MarkerUtilities.createMarker(resource, map, markerType);
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
}
