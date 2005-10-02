/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.pydev.core.REF;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.interpreters.IInterpreterManager;

import com.python.pydev.analysis.AnalysisPlugin;


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
public abstract class AbstractAdditionalInterpreterInfo {

    /**
     * this is the number of initials that is used for indexing
     */
    public static final int NUMBER_OF_INITIALS_TO_INDEX = 3;

    /**
     * Do you want to debug this class?
     */
    private static final boolean DEBUG_ADDITIONAL_INFO = false;

    /**
     * indexes used so that we can access the information faster - it is ordered through a tree map, and should be
     * very fast to access given its initials.
     */
    protected TreeMap<String, List<IInfo>> initialsToInfo = new TreeMap<String, List<IInfo>>();

    
    public AbstractAdditionalInterpreterInfo(){
    }
    
    /**
     * That's the function actually used to add some info
     * 
     * @param info information to be added
     */
    private void add(IInfo info) {
        String name = info.getName();
        String initials = getInitials(name);
        List<IInfo> listForInitials = getAndCreateListForInitials(initials);
        listForInitials.add(info);
    }

    /**
     * @param name the name from where we want to get the initials
     * @return the initials for the name
     */
    private String getInitials(String name) {
        if(name.length() < NUMBER_OF_INITIALS_TO_INDEX){
            return name;
        }
        return name.substring(0, NUMBER_OF_INITIALS_TO_INDEX).toLowerCase();
    }
    
    /**
     * @param initials the initials we are looking for
     * @return the list of tokens with the specified initials (must be exact match)
     */
    private List<IInfo> getAndCreateListForInitials(String initials) {
        if(initialsToInfo == null){
            PydevPlugin.log("Additional info not correctly generated.");
            return new ArrayList<IInfo>();
        }
        List<IInfo> lInfo = initialsToInfo.get(initials);
        if(lInfo == null){
            lInfo = new ArrayList<IInfo>();
            initialsToInfo.put(initials, lInfo);
        }
        return lInfo;
    }

    /**
     * adds a method to the definition
     */
    public void addMethod(FunctionDef def, String moduleDeclared) {
        FuncInfo info2 = FuncInfo.fromFunctionDef(def, moduleDeclared);
        add(info2);
    }
    
    /**
     * Adds a class to the definition
     */
    public void addClass(ClassDef def, String moduleDeclared) {
        ClassInfo info = ClassInfo.fromClassDef(def, moduleDeclared);
        add(info);
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
     * Adds information for a source module
     * @param m the module we want to add to the info
     */
    public void addSourceModuleInfo(SourceModule m, PythonNature nature) {
        addAstInfo(m.getAst(), m.getName(), nature);
    }

    
    /**
     * Add info from a generated ast
     * @param node the ast root
     * @param m 
     */
    public void addAstInfo(SimpleNode node, String moduleName, PythonNature nature) {
        try {
            EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
            node.accept(visitor);
            Iterator<ASTEntry> classesAndMethods = visitor.getClassesAndMethodsIterator();

            while (classesAndMethods.hasNext()) {
                ASTEntry entry = classesAndMethods.next();
                
                if(entry.parent == null){ //we only want those that are in the global scope
					SimpleNode classOrFunc = entry.node;
	                addClassOrFunc(classOrFunc, moduleName);
                }
            }
            
        } catch (Exception e) {
            PydevPlugin.log(e);
        }

    }

    /**
     * Removes all the info associated with a given module
     * @param moduleName the name of the module we want to remove info from
     */
    public void removeInfoFromModule(String moduleName) {
        if(initialsToInfo == null){
            PydevPlugin.log("Additional info not correctly generated. ("+this.getClass().getName()+")");
            return;
        }

        Iterator<List<IInfo>> itListOfInfo = initialsToInfo.values().iterator();
        while (itListOfInfo.hasNext()) {

            Iterator<IInfo> it = itListOfInfo.next().iterator();
            while (it.hasNext()) {

                IInfo info = it.next();
                if(info != null && info.getDeclaringModuleName() != null){
                    if(info.getDeclaringModuleName().equals(moduleName)){
                        it.remove();
                    }
                }
            }
            
        }
        
    }

    /**
     * This is the function for which we are most optimized!
     * 
     * @param qualifier the tokens returned have to start with the given qualifier
     * @return a list of info, all starting with the given qualifier
     */
    public List<IInfo> getTokensStartingWith(String qualifier) {
        ArrayList<IInfo> toks = new ArrayList<IInfo>();
        if(this.initialsToInfo == null){
            PydevPlugin.log("No additional info generated for a new completion.");
            return toks;
        }
        String initials = getInitials(qualifier);
        String lowerCaseQual = qualifier.toLowerCase();
        
        //get until the end of the alphabet
        SortedMap<String, List<IInfo>> subMap = this.initialsToInfo.subMap(initials, initials+"z");
        
        for (List<IInfo> listForInitials : subMap.values()) {
            
            for (IInfo info : listForInitials) {
                if(info.getName().toLowerCase().startsWith(lowerCaseQual)){
                    toks.add(info);
                }
            }
        }
        return toks;
    }

    public List<IInfo> getTokensEqualTo(String qualifier) {
        String initials = getInitials(qualifier);
        ArrayList<IInfo> toks = new ArrayList<IInfo>();
        
        //get until the end of the alphabet
        SortedMap<String, List<IInfo>> subMap = this.initialsToInfo.subMap(initials, initials+"z");
        
        for (List<IInfo> listForInitials : subMap.values()) {
            
            for (IInfo info : listForInitials) {
                if(info.getName().equals(qualifier)){
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
        Collection<List<IInfo>> lInfo = this.initialsToInfo.values();
        
        ArrayList<IInfo> toks = new ArrayList<IInfo>();
        for (List<IInfo> list : lInfo) {
            for (IInfo info : list) {
                toks.add(info);
            }
        }
        return toks;
    }

    /**
     * this can be used to save the file
     */
    public void save() {
        String persistingLocation = getPersistingLocation();
        if(DEBUG_ADDITIONAL_INFO){
            System.out.println("Saving to "+persistingLocation);
        }
        saveTo(persistingLocation);
    }

    /**
     * @return the location where we can persist this info.
     */
    protected abstract String getPersistingLocation();


    /**
     * save the information contained for the given manager
     */
    public static void saveAdditionalSystemInfo(IInterpreterManager manager) {
        AbstractAdditionalInterpreterInfo info = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(manager);
        info.save();
    }

    /**
     * @return the path to the folder we want to keep things on
     */
    protected static String getPersistingFolder() {
        try {
            IPath stateLocation = AnalysisPlugin.getDefault().getStateLocation();
            String osString = stateLocation.toOSString();
            if (osString.length() > 0) {
                char c = osString.charAt(osString.length() - 1);
                if (c != '\\' && c != '/') {
                    osString += '/';
                }
            }
            return osString;
        } catch (NullPointerException e) {
            //it may fail in tests... (save it in default folder in this cases)
            PydevPlugin.log(IStatus.ERROR, "Error getting persisting folder", e, false);
            return "";
        }
    }
    

    private void saveTo(String pathToSave) {
        if(DEBUG_ADDITIONAL_INFO){
            System.out.println("Saving info "+this.getClass().getName()+" to file (size = "+getAllTokens().size()+") "+pathToSave);
        }
        REF.writeToFile(getInfoToSave(), new File(pathToSave));
    }

    /**
     * @return the information to be saved (if overriden, restoreSavedInfo should be overriden too)
     */
    protected Object getInfoToSave(){
        return this.initialsToInfo;
    }
    
    /**
     * actually does the load
     * @return true if it was successfully loaded and false otherwise
     */
    protected boolean load() {
        File file = new File(getPersistingLocation());
        if(file.exists() && file.isFile()){
            try {
                restoreSavedInfo(IOUtils.readFromFile(file));
                setAsDefaultInfo();
                return true;
            } catch (Throwable e) {
                PydevPlugin.log("Unable to restore previous info... new info should be restored in a thread.",e);
            }
        }
        return false;
    }

    /**
     * Restores the saved info in the object (if overriden, getInfoToSave should be overriden too)
     * @param o the read object from the file
     */
    protected void restoreSavedInfo(Object o){
        this.initialsToInfo = (TreeMap<String, List<IInfo>>) o;

    }

    /**
     * this method should be overriden so that the info sets itself as the default info given the info it holds
     * (e.g. default for a project, default for python interpreter, etc.)
     */
    protected abstract void setAsDefaultInfo();


    @Override
    public String toString() {
    	StringBuffer buffer = new StringBuffer();
    	buffer.append("AdditionalInfo[");

    	Set<Entry<String, List<IInfo>>> name = this.initialsToInfo.entrySet();
    	for (Entry<String, List<IInfo>> entry : name) {
			List<IInfo> value = entry.getValue();
			for (IInfo info : value) {
				buffer.append(info.toString());
				buffer.append("\n");
			}
		}
    	buffer.append("]");
    	return buffer.toString();
    }

}

class IOUtils {

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
            throw new RuntimeException(e);
        }
    }

}