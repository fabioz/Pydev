package com.python.pydev.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.python.pydev.core.FindInfo;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.model.Location;
import org.python.pydev.plugin.nature.SystemPythonNature;
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

	static String getModNameFromFile(File file) {
    	if(file == null){
    		return null;
    	}
    	String name = file.getName();
    	int i = name.indexOf('.');
    	if (i != -1){
    		return name.substring(0, i);
    	}
    	return name;
    }

	/**
	 * @param file the file we want to get info on.
	 * @return a tuple with the pythonnature to be used and the name of the module represented by the file in that scenario.
	 */
    public static Tuple<SystemPythonNature, String> getInfoForFile(File file){
        String modName = null;
        IInterpreterManager pythonInterpreterManager = org.python.pydev.plugin.PydevPlugin.getPythonInterpreterManager();
        IInterpreterManager jythonInterpreterManager = org.python.pydev.plugin.PydevPlugin.getJythonInterpreterManager();
    
        SystemPythonNature systemPythonNature = new SystemPythonNature(pythonInterpreterManager);
        SystemPythonNature pySystemPythonNature = systemPythonNature;
        SystemPythonNature jySystemPythonNature = null;
        try {
            modName = systemPythonNature.resolveModule(file);
        } catch (Exception e) {
            // that's ok
        }
        if(modName == null){
            systemPythonNature = new SystemPythonNature(jythonInterpreterManager);
            jySystemPythonNature = systemPythonNature;
            try {
                modName = systemPythonNature.resolveModule(file);
            } catch (Exception e) {
                // that's ok
            }
        }
        if(modName != null){
            return new Tuple<SystemPythonNature, String>(systemPythonNature, modName);
        }else{
            //unable to discover it
            try {
                // the default one is python
                pythonInterpreterManager.getDefaultInterpreter();
                modName = getModNameFromFile(file);
                return new Tuple<SystemPythonNature, String>(pySystemPythonNature, modName);
            } catch (Exception e) {
                //the python interpreter manager is not valid or not configured
                try {
                    // the default one is jython
                    jythonInterpreterManager.getDefaultInterpreter();
                    modName = getModNameFromFile(file);
                    return new Tuple<SystemPythonNature, String>(jySystemPythonNature, modName);
                } catch (Exception e1) {
                    // ok, nothing to do about it, no interpreter is configured
                    return null;
                }
            }
        }
    }

    /**
     * @param pointers the list where the pointers will be added
     * @param manager the manager to be used to get the definition
     * @param nature the nature to be used
     * @param info the info that we are looking for
     */
    public static void getDefinitionFromIInfo(List<ItemPointer> pointers, ICodeCompletionASTManager manager, IPythonNature nature, IInfo info) {
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
                IDefinition[] definitions = mod.findDefinition(tok, 0, 0, nature, new ArrayList<FindInfo>());
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
            pointers.add(new ItemPointer(definition.module.getFile(),
                    new Location(definition.line-1, definition.col-1),
                    new Location(definition.line-1, definition.col-1)));
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
    
}
