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
import org.python.pydev.editor.model.Location;
import org.python.pydev.ui.UIConstants;

import com.python.pydev.PydevPlugin;
import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * The main plugin class to be used in the desktop.
 */
public class AnalysisPlugin extends AbstractUIPlugin {

	//The shared instance.
	private static AnalysisPlugin plugin;
    private static ImageCache imageCache;
	
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
		PydevPlugin.getDefault().checkValid();
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
                
                getAsPointers(pointers, (Definition[]) definitions);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @param pointers: OUT: list where the pointers will be added
     * @param definitions the definitions that will be gotten as pointers
     */
    public static void getAsPointers(List<ItemPointer> pointers, Definition[] definitions) {
        for (Definition definition : definitions) {
            File file = definition.module.getFile();
            int line = definition.line;
            int col = definition.col;
            
            pointers.add(new ItemPointer(file,
                    new Location(line-1, col-1),
                    new Location(line-1, col-1), 
                    definition,
                    definition.module.getZipFilePath())
                    );
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


    public static ImageCache getImageCache() {
        try {
            if (imageCache == null) {
                imageCache = new ImageCache(AnalysisPlugin.getDefault().getBundle().getEntry("/"));
            }
        } catch (NullPointerException e) {
            // we don't have it on tests
            org.python.pydev.plugin.PydevPlugin.log("unable to get image cache", e, false);
            
            //return one that always return null
            return new ImageCache(){
                @Override
                public Image get(String key) {
                    return null;
                }
            };
        }
        return imageCache;
    }
    
    public static final String CLASS_WITH_IMPORT_ICON = "icons/class_obj_imp.gif";
    public static final String METHOD_WITH_IMPORT_ICON = "icons/method_obj_imp.gif";
    public static final String ATTR_WITH_IMPORT_ICON = "icons/attr_obj_imp.gif";
    
    public static Image getImageForAutoImportTypeInfo(IInfo info){
        ImageCache imageCache = getImageCache();
        switch(info.getType()){
            case IInfo.CLASS_WITH_IMPORT_TYPE:
                return imageCache.get(AnalysisPlugin.CLASS_WITH_IMPORT_ICON); 
            case IInfo.METHOD_WITH_IMPORT_TYPE:
                return imageCache.get(AnalysisPlugin.METHOD_WITH_IMPORT_ICON);
            case IInfo.ATTRIBUTE_WITH_IMPORT_TYPE:
                return imageCache.get(AnalysisPlugin.ATTR_WITH_IMPORT_ICON);
            default:                  
                throw new RuntimeException("Undefined type.");

        }

    }
    
    
    public static Image getImageForTypeInfo(IInfo info){
        ImageCache imageCache = org.python.pydev.plugin.PydevPlugin.getImageCache();
        switch(info.getType()){
            case IInfo.CLASS_WITH_IMPORT_TYPE:
                return imageCache.get(UIConstants.CLASS_ICON); 
            case IInfo.METHOD_WITH_IMPORT_TYPE:
                return imageCache.get(UIConstants.PUBLIC_METHOD_ICON);
            case IInfo.ATTRIBUTE_WITH_IMPORT_TYPE:
                return imageCache.get(UIConstants.PUBLIC_ATTR_ICON);
            default:                  
                throw new RuntimeException("Undefined type.");
        
        }
    }

    /**
     * Returns the directory that can be used to store things for some project
     */
    public static File getStorageDirForProject(IProject p) {
        IPath location = p.getWorkingLocation(getDefault().getBundle().getSymbolicName());
        IPath path = location;
    
        File file = new File(path.toOSString());
        return file;
    }

}
