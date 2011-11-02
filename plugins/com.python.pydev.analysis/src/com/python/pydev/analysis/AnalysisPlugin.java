/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.ui.UIConstants;

import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * The main plugin class to be used in the desktop.
 */
public class AnalysisPlugin extends AbstractUIPlugin {

    //The shared instance.
    private static AnalysisPlugin plugin;
//    private IWorkbenchWindow activeWorkbenchWindow;
    
    /**
     * The constructor.
     */
    public AnalysisPlugin() {
        plugin = this;
    }

    /**
     * This method is called upon plug-in activation
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);

// Leaving code around to know when we get to the PyDev perspective in the active window (may be
// useful in the future).
//        Display.getDefault().asyncExec(new Runnable() {
//            public void run() {
//                IWorkbench workbench = PlatformUI.getWorkbench();
//                
//                activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
//                handleActivePage();
//                
//                workbench.addWindowListener(new IWindowListener() {
//                    
//                    public void windowOpened(IWorkbenchWindow window) {
//                    }
//                    
//                    public void windowDeactivated(IWorkbenchWindow window) {
//                    }
//                    
//                    public void windowClosed(IWorkbenchWindow window) {
//                    }
//                    
//                    public void windowActivated(IWorkbenchWindow window) {
//                        //When a window is activated, remove from the previous and add to the new one.
//                        if(activeWorkbenchWindow != null){
//                            activeWorkbenchWindow.removePerspectiveListener(perspectiveObserver);
//                        }
//                        activeWorkbenchWindow = window;
//                        handleActivePage();
//                    }
//                });
//            }
//
//            protected void handleActivePage() {
//                if(activeWorkbenchWindow != null){
//                    activeWorkbenchWindow.addPerspectiveListener(perspectiveObserver);
//                    IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
//                    if(activePage != null){
//                        IPerspectiveDescriptor perspective = activePage.getPerspective();
//                        if(perspective != null){
// TO KNOW ABOUT PYDEV PERSPECTIVE: perspectiveId.indexOf(PythonPerspectiveFactory.PERSPECTIVE_ID) > -1
//                            perspectiveObserver.handleStateChange(perspective.getId());
//                        }
//                    }
//                }
//            }
//        });
    }

    /**
     * This method is called when the plug-in is stopped
     */
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
    }


    /**
     * @param pointers the list where the pointers will be added
     * @param manager the manager to be used to get the definition
     * @param nature the nature to be used
     * @param info the info that we are looking for
     */
    public static void getDefinitionFromIInfo(List<ItemPointer> pointers, ICodeCompletionASTManager manager, IPythonNature nature, 
            IInfo info, ICompletionCache completionCache) {
        IModule mod;
        String tok;
        mod = manager.getModule(info.getDeclaringModuleName(), nature, true);
        if(mod != null){
            if(info.getType() == IInfo.MOD_IMPORT_TYPE){
                Definition definition = new Definition(1, 1, "", null, null, mod);
                PyRefactoringFindDefinition.getAsPointers(pointers, new Definition[]{definition});
                return;
            }
            //ok, now that we found the module, we have to get the actual definition
            tok = "";
            String path = info.getPath();
            
            if(path != null && path.length() > 0){
                tok = path+".";
            }
            tok += info.getName();
            try {
                IDefinition[] definitions = mod.findDefinition(
                        CompletionStateFactory.getEmptyCompletionState(tok, nature, completionCache), -1, -1, nature);

                if((definitions == null || definitions.length == 0) && path != null && path.length() > 0){
                    //this can happen if we have something as an attribute in the path:
                    
                    //class Bar(object):
                    //    def __init__(self):
                    //        self.xxx = 10
                    //
                    //so, we'de get a find definition for Bar.__init__.xxx which is something we won't find
                    //for now, let's simply return a match in the correct context (although the correct way of doing
                    //it would be analyzing that context to find the match)
                    definitions = mod.findDefinition(
                            CompletionStateFactory.getEmptyCompletionState(path, nature, completionCache), -1, -1, nature);
                    
                }
                PyRefactoringFindDefinition.getAsPointers(pointers, (Definition[]) definitions);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Returns the shared instance.
     */
    public static AnalysisPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given
     * plug-in relative path.
     *
     * @param path the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin("com.python.pydev.analysis", path);
    }


    private static final Object lock = new Object();
    public static Image autoImportClassWithImportType;
    public static Image autoImportMethodWithImportType;
    public static Image autoImportAttributeWithImportType;
    public static Image autoImportModImportType;
    
    public static Image getImageForAutoImportTypeInfo(IInfo info){
        switch(info.getType()){
            case IInfo.CLASS_WITH_IMPORT_TYPE:
                if(autoImportClassWithImportType == null){
                    synchronized (lock) {
                        ImageCache imageCache = org.python.pydev.plugin.PydevPlugin.getImageCache();
                        autoImportClassWithImportType = imageCache.getImageDecorated(
                                UIConstants.CLASS_ICON, 
                                UIConstants.CTX_INSENSITIVE_DECORATION_ICON, 
                                ImageCache.DECORATION_LOCATION_BOTTOM_RIGHT); 
                    }
                }
                return autoImportClassWithImportType;
                
            case IInfo.METHOD_WITH_IMPORT_TYPE:
                if(autoImportMethodWithImportType == null){
                    synchronized (lock) {
                        ImageCache imageCache = org.python.pydev.plugin.PydevPlugin.getImageCache();
                        autoImportMethodWithImportType = imageCache.getImageDecorated(
                                UIConstants.METHOD_ICON, 
                                UIConstants.CTX_INSENSITIVE_DECORATION_ICON, 
                                ImageCache.DECORATION_LOCATION_BOTTOM_RIGHT);
                    }
                }
                return autoImportMethodWithImportType; 
                
            case IInfo.ATTRIBUTE_WITH_IMPORT_TYPE:
                if(autoImportAttributeWithImportType == null){
                    synchronized (lock) {
                        ImageCache imageCache = org.python.pydev.plugin.PydevPlugin.getImageCache();
                        autoImportAttributeWithImportType = imageCache.getImageDecorated(
                                UIConstants.PUBLIC_ATTR_ICON, 
                                UIConstants.CTX_INSENSITIVE_DECORATION_ICON, 
                                ImageCache.DECORATION_LOCATION_BOTTOM_RIGHT);
                    }
                }
                return autoImportAttributeWithImportType; 
                
            case IInfo.MOD_IMPORT_TYPE:
                if(autoImportModImportType == null){
                    synchronized (lock) {
                        ImageCache imageCache = org.python.pydev.plugin.PydevPlugin.getImageCache();
                        autoImportModImportType = imageCache.getImageDecorated(
                                UIConstants.FOLDER_PACKAGE_ICON, 
                                UIConstants.CTX_INSENSITIVE_DECORATION_ICON, 
                                ImageCache.DECORATION_LOCATION_BOTTOM_RIGHT);
                    }
                }
                return autoImportModImportType; 
                
            default:                  
                throw new RuntimeException("Undefined type.");

        }

    }
    
    
    public static Image classWithImportType;
    public static Image methodWithImportType;
    public static Image attributeWithImportType;
    public static Image modImportType;

    
    public static Image getImageForTypeInfo(IInfo info){
        switch(info.getType()){
            case IInfo.CLASS_WITH_IMPORT_TYPE:
                if(classWithImportType == null){
                    synchronized (lock) {
                        ImageCache imageCache = org.python.pydev.plugin.PydevPlugin.getImageCache();
                        classWithImportType = imageCache.get(UIConstants.CLASS_ICON);
                    }
                }
                return classWithImportType; 
                
            case IInfo.METHOD_WITH_IMPORT_TYPE:
                if(methodWithImportType == null){
                    synchronized (lock) {
                        ImageCache imageCache = org.python.pydev.plugin.PydevPlugin.getImageCache();
                        methodWithImportType = imageCache.get(UIConstants.METHOD_ICON);
                    }
                }
                return methodWithImportType;
                
            case IInfo.ATTRIBUTE_WITH_IMPORT_TYPE:
                if(attributeWithImportType == null){
                    synchronized (lock) {
                        ImageCache imageCache = org.python.pydev.plugin.PydevPlugin.getImageCache();
                        attributeWithImportType = imageCache.get(UIConstants.PUBLIC_ATTR_ICON);
                    }
                }
                return attributeWithImportType;
                
            case IInfo.MOD_IMPORT_TYPE:
                if(modImportType == null){
                    synchronized (lock) {
                        ImageCache imageCache = org.python.pydev.plugin.PydevPlugin.getImageCache();
                        modImportType = imageCache.get(UIConstants.FOLDER_PACKAGE_ICON);
                    }
                }
                return modImportType;
            default:                  
                throw new RuntimeException("Undefined type.");
        
        }
    }

    /**
     * Returns the directory that can be used to store things for some project
     */
    public static File getStorageDirForProject(IProject p) {
        IPath location = p.getWorkingLocation(plugin.getBundle().getSymbolicName());
        IPath path = location;
    
        File file = new File(path.toOSString());
        return file;
    }

    public static String getPluginID() {
        return getDefault().getBundle().getSymbolicName();
    }

}
