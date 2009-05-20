package com.python.pydev.analysis.additionalinfo;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IMemento;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.actions.AdditionalInfoAndIInfo;

/**
 * The InfoFactory is used to save and recreate an AdditionalInfoAndIInfo object.
 *
 * @see org.eclipse.ui.internal.ide.model.ResourceFactory
 */
@SuppressWarnings("restriction")
public class InfoFactory {

    //These constants are stored in the XML. Do not change them.
    
    private static final String TAG_MODULE_NAME = "module_name";

    private static final String TAG_PATH = "path";
    
    private static final String TAG_NAME = "name";
    
    private static final String TAG_TYPE = "type";
    
    private static final String TAG_PROJECT_NAME = "project";
    
    private static final String TAG_MANAGER_IS_PYTHON = "is_python";
    
    private static final String TAG_MANAGER_INTERPRETER = "interpreter";

    /**
     * Data to persist
     */
    private AdditionalInfoAndIInfo info;

    
    public InfoFactory() {
    }

    
    public InfoFactory(AdditionalInfoAndIInfo input) {
        info = input;
    }

    public AdditionalInfoAndIInfo createElement(IMemento memento) {
        String[] attributeKeys = null;
        try{
            attributeKeys = memento.getAttributeKeys();
        }catch(Exception e){
            Log.log(e);
            return null;
        }
        HashSet<String> keys = new HashSet<String>(Arrays.asList(attributeKeys));
        if(!keys.contains(TAG_NAME) || !keys.contains(TAG_MODULE_NAME) || !keys.contains(TAG_PATH) || !keys.contains(TAG_TYPE)){
            return null;
        }
        
        String name = memento.getString(TAG_NAME);
        String moduleName = memento.getString(TAG_MODULE_NAME);
        String path = memento.getString(TAG_PATH);
        final int type = memento.getInteger(TAG_TYPE);
        
        AbstractInfo info;
        if(type == IInfo.ATTRIBUTE_WITH_IMPORT_TYPE){
            info = new AttrInfo();
        }else if(type == IInfo.CLASS_WITH_IMPORT_TYPE){
            info = new ClassInfo();
        }else if(type == IInfo.METHOD_WITH_IMPORT_TYPE){
            info = new FuncInfo();
        }else if(type == IInfo.NAME_WITH_IMPORT_TYPE){
            info = new NameInfo();
        }else{
            throw new AssertionError("Cannot restore type: "+type);
        }
        
        if(name != null && name.length() > 0){
            info.name = name;
        }
        
        if(moduleName != null && moduleName.length() > 0){
            info.moduleDeclared = moduleName;
        }
        
        if(path != null && path.length() > 0){
            info.path = path;
        }
        
        String projectName = null;
        if(keys.contains(TAG_PROJECT_NAME)){
            projectName = memento.getString(TAG_PROJECT_NAME);
        }
        if(projectName != null){
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
            if(project != null){
                IPythonNature nature = PythonNature.getPythonNature(project);
                if(nature != null){
                    AbstractAdditionalDependencyInfo additionalInfo = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature);
                    return new AdditionalInfoAndIInfo(additionalInfo, info);
                }
            }
        }else if(keys.contains(TAG_MANAGER_IS_PYTHON) && keys.contains(TAG_MANAGER_INTERPRETER)){
            Boolean isPython = memento.getBoolean(TAG_MANAGER_IS_PYTHON);
            if(isPython != null){
                IInterpreterManager manager;
                if(isPython){
                    manager = PydevPlugin.getPythonInterpreterManager();
                }else{
                    manager = PydevPlugin.getJythonInterpreterManager();
                    
                }
                String interpreter = memento.getString(TAG_MANAGER_INTERPRETER);
                
                AbstractAdditionalInterpreterInfo additionalInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(manager, interpreter);
                if(additionalInfo != null){
                    return new AdditionalInfoAndIInfo(additionalInfo, info);
                }
            }
        }
        return null;
    }

    
    public void saveState(IMemento memento) {
        if(info.info == null){
            return;
        }
        String declaringModuleName = info.info.getDeclaringModuleName();
        if(declaringModuleName == null){
            declaringModuleName = "";
        }
        memento.putString(TAG_MODULE_NAME, declaringModuleName);
        
        
        String path = info.info.getPath();
        if(path == null){
            path = "";
        }
        memento.putString(TAG_PATH, path);
        
        
        String name = info.info.getName();
        if(name == null){
            name = "";
        }
        memento.putString(TAG_NAME, name);
        
        
        memento.putString(TAG_TYPE, info.info.getType()+"");
        if(info.additionalInfo instanceof AdditionalProjectInterpreterInfo){
            AdditionalProjectInterpreterInfo projectInterpreterInfo = (AdditionalProjectInterpreterInfo) info.additionalInfo;
            memento.putString(TAG_PROJECT_NAME, projectInterpreterInfo.getProject().getName());
            
        }else if (info.additionalInfo instanceof AdditionalSystemInterpreterInfo){
            AdditionalSystemInterpreterInfo systemInterpreterInfo = (AdditionalSystemInterpreterInfo) info.additionalInfo;
            IInterpreterManager manager = systemInterpreterInfo.getManager();
            memento.putBoolean(TAG_MANAGER_IS_PYTHON, manager.isPython());
            memento.putString(TAG_MANAGER_INTERPRETER, systemInterpreterInfo.getAdditionalInfoInterpreter());
            
        }
        
    }
}
