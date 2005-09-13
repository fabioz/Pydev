/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.pydev.core.REF;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.interpreters.IInterpreterManager;

import sun.misc.BASE64Decoder;

import com.python.pydev.codecompletion.CodecompletionPlugin;


/**
 * This class contains additional information on an interpreter, so that we are able to make code-completion in
 * a context-insensitive way (and make additionally auto-import).
 * 
 * The information that is needed for that is the following:
 * 
 * - Classes that are available in the global context
 * - Methods that are available in the global context
 * 
 * We must access this information very fast, so the underlying structure has to take that into consideration.
 * 
 * It should not 'eat' too much memory because it should be all in memory at all times
 * 
 * It should also be easy to query it. 
 *      Some query situations include: 
 *          - which classes have the method xxx and yyy?
 *          - which methods and classes start with xxx?
 *          - is there any class or method with the name xxx?
 *      
 * The information must be persisted for reuse (and persisting and restoring it should be fast).
 * 
 * We need to store information for any interpreter, be it python, jython...
 * 
 * For creating and keeping this information up-to-date, we have to know when:
 * - the interpreter used changes (the InterpreterInfo should be passed after the change)
 * - some file changes (pydev_builder)
 * 
 * @author Fabio
 */
public class AdditionalInterpreterInfo {

    private static final boolean DEBUG_ADDITIONAL_INFO = false;

    /**
     * This is the place where the information is actually stored
     */
    private List<IInfo> additionalInfo;
    
    

    public AdditionalInterpreterInfo(){
        additionalInfo = new ArrayList<IInfo>();
    }
    
    /**
     * adds a method to the definition
     */
    public void addMethod(FunctionDef def, String moduleDeclared) {
        FuncInfo info2 = FuncInfo.fromFunctionDef(def, moduleDeclared);
        additionalInfo.add(info2);
    }
    
    /**
     * Adds a class to the definition
     */
    public void addClass(ClassDef def, String moduleDeclared) {
        ClassInfo info = ClassInfo.fromClassDef(def, moduleDeclared);
        additionalInfo.add(info);
    }

    /**
     * Adds a class or a function to the definition
     * 
     * @param classOrFunc the class or function we want to add
     * @param moduleDeclared the module where it is declared
     */
    public void addClassOrFunc(SimpleNode classOrFunc, String moduleDeclared) {
        if(classOrFunc instanceof ClassDef){
            addClass((ClassDef) classOrFunc, moduleDeclared);
        }else{
            addMethod((FunctionDef) classOrFunc, moduleDeclared);
        }
    }

    /**
     * Removes all the info associated with a given module
     * @param moduleName the name of the module we want to remove info from
     */
    public void removeInfoFromModule(String moduleName) {
        for (Iterator<IInfo> it = additionalInfo.iterator(); it.hasNext(); ) {
            IInfo info = it.next();
            if(info.getDeclaringModuleName().equals(moduleName)){
                it.remove();
            }
        }
    }

    /**
     * @param qualifier the tokens returned have to start with the given qualifier
     * @return a list of info, all starting with the given qualifier
     */
    public List<IInfo> getTokensStartingWith(String qualifier) {
        ArrayList<IInfo> toks = new ArrayList<IInfo>();
        String lowerCaseQual = qualifier.toLowerCase();
        if(additionalInfo != null){
            for (IInfo info : additionalInfo) {
                if(info.getName().toLowerCase().startsWith(lowerCaseQual)){
                    toks.add(info);
                }
            }
        }
        return toks;
    }
    
    /**
     * @return all the tokens that are in this info
     */
    public Collection<IInfo> getAllTokens(){
        return additionalInfo;
    }

    /**
     * holds nature info (project name points to info)
     */
    private static Map<String, AdditionalInterpreterInfo> additionalNatureInfo = new HashMap<String, AdditionalInterpreterInfo>();

    /**
     * holds system info (interpreter name points to system info)
     */
    private static Map<String, AdditionalInterpreterInfo> additionalSystemInfo = new HashMap<String, AdditionalInterpreterInfo>();
    
    /**
     * @param m the module manager that we want to get info on (python, jython...)
     * @return the additional info for the system
     */
    public static AdditionalInterpreterInfo getAdditionalSystemInfo(IInterpreterManager manager) {
        String key = manager.getManagerRelatedName();
        AdditionalInterpreterInfo info = additionalSystemInfo.get(key);
        if(info == null){
            info = new AdditionalInterpreterInfo();
            additionalSystemInfo.put(key, info);
        }
        return info;
    }

    /**
     * sets the additional info (overrides if already set)
     * @param manager the manager we want to set info on
     * @param additionalSystemInfoToSet the info to set
     */
    public static void setAdditionalSystemInfo(IInterpreterManager manager, AdditionalInterpreterInfo additionalSystemInfoToSet) {
        additionalSystemInfo.put(manager.getManagerRelatedName(), additionalSystemInfoToSet);
    }

    /**
     * sets the additional info (overrides if already set)
     * @param project the project we want to set info on
     * @param info the info to set
     */
    public static void setAdditionalInfoForProject(IProject project, AdditionalInterpreterInfo info) {
        additionalNatureInfo.put(project.getName(), info);
    }
    
    /**
     * @param project the project we want to get info on
     * @return the additional info for a given project (gotten from the cache with its name)
     */
    public static AdditionalInterpreterInfo getAdditionalInfoForProject(IProject project) {
        String name = project.getName();
        AdditionalInterpreterInfo info = additionalNatureInfo.get(name);
        if(info == null){
            info = new AdditionalInterpreterInfo();
            additionalNatureInfo.put(name, info);
        }
        return info;
    }
    
    
    
    
    
    /**
     * @param nature the nature we want to get info on
     * @return all the additional info that is bounded with some nature (including related projects)
     */
    public static List<AdditionalInterpreterInfo> getAdditionalInfo(PythonNature nature) {
        List<AdditionalInterpreterInfo> ret = new ArrayList<AdditionalInterpreterInfo>();
        IProject project = nature.getProject();
        
        //get for the system info
        AdditionalInterpreterInfo systemInfo = getAdditionalSystemInfo(PydevPlugin.getInterpreterManager(nature));
        ret.add(systemInfo);

        //get for the current project
        AdditionalInterpreterInfo additionalInfoForProject = getAdditionalInfoForProject(project);
        if(additionalInfoForProject != null){
            ret.add(additionalInfoForProject);
        }
        
        try {
            //get for the referenced projects
            IProject[] referencedProjects = project.getReferencedProjects();
            for (IProject refProject : referencedProjects) {
                
                additionalInfoForProject = getAdditionalInfoForProject(refProject);
                if(additionalInfoForProject != null){
                    ret.add(additionalInfoForProject);
                }
            }
            
            
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    public static void saveAdditionalInfoForProject(IProject project) {
        AdditionalInterpreterInfo info = getAdditionalInfoForProject(project);
        info.saveTo(getPersistingLocationForProject(project));
        if(DEBUG_ADDITIONAL_INFO){
            System.out.println("Saving project info..."+project.getName());
        }
    }

    /**
     * @param project
     * @return
     */
    private static String getPersistingLocationForProject(IProject project) {
        return getPersistingFolder()+project.getName()+".pydevinfo";
    }

    /**
     * save the information contained for the given manager
     */
    public static void saveAdditionalSystemInfo(IInterpreterManager manager) {
        AdditionalInterpreterInfo info = getAdditionalSystemInfo(manager);
        if(DEBUG_ADDITIONAL_INFO){
            System.out.println("Saving system info...");
        }
        info.saveTo(getPersistingLocationForManager(manager));
    }

    /**
     * @return the path to the folder we want to keep things on
     */
    private static String getPersistingFolder() {
        IPath stateLocation = CodecompletionPlugin.getDefault().getStateLocation();
        return stateLocation.toOSString();
    }
    
    /**
     * @return
     */
    private static String getPersistingLocationForManager(IInterpreterManager manager) {
        return getPersistingFolder()+manager.getManagerRelatedName()+".pydevsysteminfo";
    }

    private void saveTo(String pathToSave) {
        if(DEBUG_ADDITIONAL_INFO){
            System.out.println("Saving info to file (size = "+additionalInfo.size()+") "+pathToSave);
        }
        REF.writeToFile(additionalInfo, new File(pathToSave));
    }

    /**
     * @return whether the info was succesfully loaded or not
     */
    public static boolean loadAdditionalSystemInfo(IInterpreterManager manager) {
        File file = new File(getPersistingLocationForManager(manager));
        if(file.exists() && file.isFile()){
            try {
                List<IInfo> additionalInfo = (List<IInfo>) IOUtils.readFromFile(file);
                AdditionalInterpreterInfo info = new AdditionalInterpreterInfo();
                info.additionalInfo = additionalInfo;
                setAdditionalSystemInfo(manager, info);
                return true;
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }
        return false;
    }

    public static boolean loadAdditionalInfoForProject(IProject project) {
        File file = new File(getPersistingLocationForProject(project));
        if(file.exists() && file.isFile()){
            try {
                List<IInfo> additionalInfo = (List<IInfo>) IOUtils.readFromFile(file);
                AdditionalInterpreterInfo info = new AdditionalInterpreterInfo();
                info.additionalInfo = additionalInfo;
                setAdditionalInfoForProject(project, info);
                return true;
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }
        return false;
    }

    
}

class IOUtils {
    /**
     * @param persisted
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object getStrAsObj(String persisted) throws IOException, ClassNotFoundException {
        BASE64Decoder decoder = new BASE64Decoder();
        InputStream input = new ByteArrayInputStream(decoder.decodeBuffer(persisted));
        ObjectInputStream in = new ObjectInputStream(input);
        Object list = in.readObject();
        in.close();
        input.close();
        return list;
    }

    /**
     * @param astOutputFile
     * @return
     */
    public static Object readFromFile(File astOutputFile) {
        try {
            InputStream input = new FileInputStream(astOutputFile);
            ObjectInputStream in = new ObjectInputStream(input);
            Object o = in.readObject();
            in.close();
            input.close();
            return o;
        } catch (Exception e) {
            Log.log(e);
            return null;
        }
    }

}