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

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.ObjectsPool;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.DefinitionsASTIteratorVisitor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;


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
     * Defines that some operation should be done on top level tokens
     */
    public final static int TOP_LEVEL = 1;
    
    /**
     * Defines that some operation should be done on inner level tokens
     */
    public final static int INNER = 2;
    
    

    /**
     * indexes used so that we can access the information faster - it is ordered through a tree map, and should be
     * very fast to access given its initials.
     * 
     * It contains only top/level information for a module
     * 
     * This map is persisted.
     */
    protected TreeMap<String, List<IInfo>> topLevelInitialsToInfo = new TreeMap<String, List<IInfo>>();
    
    /**
     * indexes so that we can get 'inner information' from classes, such as methods or inner classes from a class 
     */
    protected TreeMap<String, List<IInfo>> innerInitialsToInfo = new TreeMap<String, List<IInfo>>();
    

    /**
     * Should be used before re-creating the info, so that we have enough memory. 
     */
    public void clearAllInfo() {
        synchronized (lock) {
        	if(topLevelInitialsToInfo != null){
        		topLevelInitialsToInfo.clear();
        	}
        	if(innerInitialsToInfo != null){
        		innerInitialsToInfo.clear();
        	}
        }
    }
        
    
    protected Object lock = new Object();


    /**
     * The filter interface
     */
    public interface Filter{
        boolean doCompare(String lowerCaseQual, IInfo info);
        boolean doCompare(String lowerCaseQual, String infoName);
    }
    
    /**
     * A filter that checks if tokens are equal
     */
    private Filter equalsFilter  = new Filter(){
        public boolean doCompare(String qualifier, IInfo info) {
            return doCompare(qualifier, info.getName());
        }
        public boolean doCompare(String qualifier, String infoName) {
        	return infoName.equals(qualifier);
        }
    };

    /**
     * A filter that checks if the tokens starts with a qualifier
     */
    private Filter startingWithFilter = new Filter(){

        public boolean doCompare(String lowerCaseQual, IInfo info) {
            return doCompare(lowerCaseQual, info.getName());
        }
        public boolean doCompare(String qualifier, String infoName) {
        	return infoName.toLowerCase().startsWith(qualifier);
        }
        
    };

    public AbstractAdditionalInterpreterInfo(){
    }
    
    /**
     * That's the function actually used to add some info
     * 
     * @param info information to be added
     */
    protected void add(IInfo info, boolean generateDelta, int doOn) {
        String name = info.getName();
        String initials = getInitials(name);
        TreeMap<String, List<IInfo>> initialsToInfo;
        
        if(doOn == TOP_LEVEL){
            if(info.getPath() != null && info.getPath().length() > 0){
                throw new RuntimeException("Error: the info being added is added as an 'top level' info, but has path. Info:"+info);
            }
            initialsToInfo = topLevelInitialsToInfo;
            
        }else if (doOn == INNER){
            if(info.getPath() == null || info.getPath().length() == 0){
                throw new RuntimeException("Error: the info being added is added as an 'inner' info, but does not have a path. Info: "+info);
            }
            initialsToInfo = innerInitialsToInfo;
            
        }else{
            throw new RuntimeException("List to add is invalid: "+doOn);
        }
        List<IInfo> listForInitials = getAndCreateListForInitials(initials, initialsToInfo);
        listForInitials.add(info);

    }

    /**
     * @param name the name from where we want to get the initials
     * @return the initials for the name
     */
    protected String getInitials(String name) {
        if(name.length() < NUMBER_OF_INITIALS_TO_INDEX){
            return name;
        }
        return name.substring(0, NUMBER_OF_INITIALS_TO_INDEX).toLowerCase();
    }
    
    /**
     * @param initials the initials we are looking for
     * @param initialsToInfo this is the list we should use (top level or inner)
     * @return the list of tokens with the specified initials (must be exact match)
     */
    protected List<IInfo> getAndCreateListForInitials(String initials, TreeMap<String, List<IInfo>> initialsToInfo) {
        List<IInfo> lInfo = initialsToInfo.get(initials);
        if(lInfo == null){
            lInfo = new ArrayList<IInfo>();
            initialsToInfo.put(initials, lInfo);
        }
        return lInfo;
    }

    protected final static ObjectsPool<String> pool = new ObjectsPool<String>();
    
    /**
     * adds a method to the definition
     * @param doOn 
     */
    protected void addMethod(FunctionDef def, String moduleDeclared, boolean generateDelta, int doOn, String path) {
        synchronized (lock) {
	        FuncInfo info2 = FuncInfo.fromFunctionDef(def, moduleDeclared, path, pool);
	        add(info2, generateDelta, doOn);
        }
    }
    
    /**
     * Adds a class to the definition
     * @param doOn 
     */
    protected void addClass(ClassDef def, String moduleDeclared, boolean generateDelta, int doOn, String path) {
    	synchronized (lock) {
	        ClassInfo info = ClassInfo.fromClassDef(def, moduleDeclared, path, pool);
	        add(info, generateDelta, doOn);
    	}
    }
    
    
    /**
     * Adds an attribute to the definition (this is either a global, a class attribute or an instance (self) attribute
     */
    protected void addAttribute(String def, String moduleDeclared, boolean generateDelta, int doOn, String path) {
        synchronized (lock) {
            
            AttrInfo info = AttrInfo.fromAssign(def, moduleDeclared, path, pool);
            add(info, generateDelta, doOn);
        }
    }

    /**
     * Adds a class or a function to the definition
     * 
     * @param classOrFunc the class or function we want to add
     * @param moduleDeclared the module where it is declared
     * @param doOn 
     */
    protected void addClassOrFunc(SimpleNode classOrFunc, String moduleDeclared, boolean generateDelta, int doOn, String path) {
        if(classOrFunc instanceof ClassDef){
            addClass((ClassDef) classOrFunc, moduleDeclared, generateDelta, doOn, path);
        }else{
            addMethod((FunctionDef) classOrFunc, moduleDeclared, generateDelta, doOn, path);
        }
    }

    private void addAssignTargets(ASTEntry entry, String moduleName, boolean generateDelta, int doOn, String path, boolean lastIsMethod ) {
        String rep = NodeUtils.getFullRepresentationString(entry.node);
        if(lastIsMethod){
            String[] parts = StringUtils.dotSplit(rep);
            if(parts.length >= 2){
                //at least 2 parts are required
                if(parts[0].equals("self")){
                    rep = parts[1];
                    addAttribute(rep, moduleName, generateDelta, doOn, path);
                }
            }
        }else{
            addAttribute(FullRepIterable.getFirstPart(rep), moduleName, generateDelta, doOn, path);
        }
    }

    /**
     * Adds information for a source module
     * @param m the module we want to add to the info
     */
    public void addSourceModuleInfo(SourceModule m, PythonNature nature, boolean generateDelta) {
        addAstInfo(m.getAst(), m.getName(), nature, generateDelta);
    }

    
    /**
     * Add info from a generated ast
     * @param node the ast root
     */
    public void addAstInfo(SimpleNode node, String moduleName, PythonNature nature, boolean generateDelta) {
    	if(node == null || moduleName == null){
    		return;
    	}
    	
        try {
            DefinitionsASTIteratorVisitor visitor = new DefinitionsASTIteratorVisitor();
            node.accept(visitor);
            Iterator<ASTEntry> entries = visitor.getOutline();

            while (entries.hasNext()) {
                ASTEntry entry = entries.next();
                
                if(entry.parent == null){ //we only want those that are in the global scope
                    if(entry.node instanceof ClassDef || entry.node instanceof FunctionDef){
                        addClassOrFunc(entry.node, moduleName, generateDelta, TOP_LEVEL, null);
                    }else{
                        //it is an assign
                        addAssignTargets(entry, moduleName, generateDelta, TOP_LEVEL, null, false);
                    }
                }else{
                    if(entry.node instanceof ClassDef || entry.node instanceof FunctionDef){
                        //ok, it has a parent, so, let's check to see if the path we got only has class definitions
                        //as the parent (and get that path)
                        Tuple<String,Boolean> pathToRoot = getPathToRoot(entry, false, false);
                        if(pathToRoot != null && pathToRoot.o1 != null && pathToRoot.o1.length() > 0){
                            //if the root is not valid, it is not only classes in the path (could be a method inside
                            //a method, or something similar).
                            addClassOrFunc(entry.node, moduleName, generateDelta, INNER, pathToRoot.o1);
                        }
                    }else{
                        //it is an assign
                        Tuple<String,Boolean> pathToRoot = getPathToRoot(entry, true, false);
                        if(pathToRoot != null && pathToRoot.o1 != null && pathToRoot.o1.length() > 0){
                            addAssignTargets(entry, moduleName, generateDelta, INNER, pathToRoot.o1, pathToRoot.o2);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            PydevPlugin.log(e);
        }

    }
    


    /**
     * @param lastMayBeMethod if true, it gets the path and accepts a method (if it is the last in the stack)
     * if false, null is returned if a method is found. 
     * 
     * @return a tuple, where the first element is the path where the entry is located (may return null).
     * and the second element is a boolen that indicates if the last was actually a method or not.
     */
    private Tuple<String, Boolean> getPathToRoot(ASTEntry entry, boolean lastMayBeMethod, boolean acceptAny) {
        if(entry.parent == null){
            return null;
        }
        boolean lastIsMethod = false; 
        //if the last 'may be a method', in this case, we have to remember that it will actually be the first one 
        //to be analyzed.
        
        //let's get the stack
        FastStack<SimpleNode> stack = new FastStack<SimpleNode>();
        while(entry.parent != null){
            if(entry.parent.node instanceof ClassDef){
                stack.push(entry.parent.node);
                
            }else if(entry.parent.node instanceof FunctionDef){
                if(!acceptAny){
                    if(lastIsMethod){
                        //already found a method
                        return null;
                    }
                    
                    if(!lastMayBeMethod){
                        return null;
                    }
                    
                    //ok, the last one may be a method... (in this search, it MUST be the first one...)
                    if(stack.size() != 0){
                        return null; 
                    }
                }
                
                //ok, there was a class, so, let's go and set it
                stack.push(entry.parent.node);
                lastIsMethod = true;
                
            }else{
                return null;
                
            }
            entry = entry.parent;
        }

        //now that we have the stack, let's make it into a path...
        StringBuffer buf = new StringBuffer();
        while(stack.size() > 0){
            if(buf.length() > 0){
                buf.append(".");
            }
            buf.append(NodeUtils.getRepresentationString(stack.pop()));
        }
        return new Tuple<String, Boolean>(buf.toString(), lastIsMethod);
    }

    /**
     * Removes all the info associated with a given module
     * @param moduleName the name of the module we want to remove info from
     */
    public void removeInfoFromModule(String moduleName, boolean generateDelta) {
        synchronized (lock) {
            removeInfoFromMap(moduleName, topLevelInitialsToInfo);
            removeInfoFromMap(moduleName, innerInitialsToInfo);
        }
        
    }

    /**
     * @param moduleName
     * @param initialsToInfo
     */
    private void removeInfoFromMap(String moduleName, TreeMap<String, List<IInfo>> initialsToInfo) {
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
     * Checks if there is some available info on the given module
     */
	public boolean hasInfoOn(String moduleName) {
		synchronized (lock) {
			//we just check the top level (it is not possible to have info on an inner structure without
			//having it in the top level too).
	        Iterator<List<IInfo>> itListOfInfo = topLevelInitialsToInfo.values().iterator();
	        while (itListOfInfo.hasNext()) {
	
	            Iterator<IInfo> it = itListOfInfo.next().iterator();
	            while (it.hasNext()) {
	
	                IInfo info = it.next();
	                if(info != null && info.getDeclaringModuleName() != null){
	                    if(info.getDeclaringModuleName().equals(moduleName)){
	                        return true;
	                    }
	                }
	            }
	        }
			return false;
		}
	}
	
	/**
     * This is the function for which we are most optimized!
     * 
     * @param qualifier the tokens returned have to start with the given qualifier
     * @return a list of info, all starting with the given qualifier
     */
    public List<IInfo> getTokensStartingWith(String qualifier, int getWhat) {
        synchronized (lock) {
	        return getWithFilter(qualifier, getWhat, startingWithFilter, true);
        }
    }


    public List<IInfo> getTokensEqualTo(String qualifier, int getWhat) {
        synchronized (lock) {
            return getWithFilter(qualifier, getWhat, equalsFilter, false);
        }
    }
    
    protected List<IInfo> getWithFilter(String qualifier, int getWhat, Filter filter, boolean useLowerCaseQual) {
    	synchronized (lock) {
	        ArrayList<IInfo> toks = new ArrayList<IInfo>();
	        
	        if((getWhat & TOP_LEVEL) != 0){
	            getWithFilter(qualifier, topLevelInitialsToInfo, toks, filter, useLowerCaseQual);
	        }
	        if((getWhat & INNER) != 0){
	            getWithFilter(qualifier, innerInitialsToInfo, toks, filter, useLowerCaseQual);
	        }
	        return toks;
    	}
    }
    	
    

    /**
     * @param qualifier
     * @param initialsToInfo this is where we are going to get the info from (currently: inner or top level list)
     * @param toks (out) the tokens will be added to this list
     * @return
     */
    protected void getWithFilter(String qualifier, TreeMap<String, List<IInfo>> initialsToInfo, List<IInfo> toks, Filter filter, boolean useLowerCaseQual) {
        String initials = getInitials(qualifier);
        String qualToCompare = qualifier;
        if(useLowerCaseQual){
            qualToCompare = qualifier.toLowerCase();
        }
        
        //get until the end of the alphabet
        SortedMap<String, List<IInfo>> subMap = initialsToInfo.subMap(initials, initials+"z");
        
        for (List<IInfo> listForInitials : subMap.values()) {
            
            for (IInfo info : listForInitials) {
                if(filter.doCompare(qualToCompare, info)){
                    toks.add(info);
                }
            }
        }
    }
    

    /**
     * @return all the tokens that are in this info (top level or inner)
     */
    public Collection<IInfo> getAllTokens(){
        synchronized (lock) {
	        Collection<List<IInfo>> lInfo = this.topLevelInitialsToInfo.values();
	        
	        ArrayList<IInfo> toks = new ArrayList<IInfo>();
	        for (List<IInfo> list : lInfo) {
	            for (IInfo info : list) {
	                toks.add(info);
	            }
	        }

            lInfo = this.innerInitialsToInfo.values();
	        for (List<IInfo> list : lInfo) {
	            for (IInfo info : list) {
	                toks.add(info);
	            }
	        }
	        return toks;
        }
    }
    
    /**
     * this can be used to save the file
     */
    public void save() {
        File persistingLocation = getPersistingLocation();
        if(DEBUG_ADDITIONAL_INFO){
            System.out.println("Saving to "+persistingLocation);
        }
        saveTo(persistingLocation);
    }

    /**
     * @return the location where we can persist this info.
     */
    protected abstract File getPersistingLocation();


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
    protected abstract File getPersistingFolder();
    

    private void saveTo(File pathToSave) {
        synchronized (lock) {
	        if(DEBUG_ADDITIONAL_INFO){
	            System.out.println("Saving info "+this.getClass().getName()+" to file (size = "+getAllTokens().size()+") "+pathToSave);
	        }
	        REF.writeToFile(getInfoToSave(), pathToSave);
        }
    }

    /**
     * @return the information to be saved (if overriden, restoreSavedInfo should be overriden too)
     */
    @SuppressWarnings("unchecked")
    protected Object getInfoToSave(){
        return new Tuple(this.topLevelInitialsToInfo, this.innerInitialsToInfo);
    }
    
    /**
     * actually does the load
     * @return true if it was successfully loaded and false otherwise
     */
    protected boolean load() {
        synchronized (lock) {
	        File file = getPersistingLocation();
	        if(file.exists() && file.isFile()){
	            try {
	                restoreSavedInfo(IOUtils.readFromFile(file));
	                setAsDefaultInfo();
	                return true;
	            } catch (Throwable e) {
	                PydevPlugin.log("Unable to restore previous info... new info should be restored in a thread.",e);
	            }
	        }
        }
        return false;
    }

    /**
     * Restores the saved info in the object (if overriden, getInfoToSave should be overriden too)
     * @param o the read object from the file
     */
    @SuppressWarnings("unchecked")
    protected void restoreSavedInfo(Object o){
        synchronized (lock) {
	        Tuple readFromFile = (Tuple) o;
	        this.topLevelInitialsToInfo = (TreeMap<String, List<IInfo>>) readFromFile.o1;
	        this.innerInitialsToInfo = (TreeMap<String, List<IInfo>>) readFromFile.o2;
        }
    }

    /**
     * this method should be overriden so that the info sets itself as the default info given the info it holds
     * (e.g. default for a project, default for python interpreter, etc.)
     */
    protected abstract void setAsDefaultInfo();


    @Override
    public String toString() {
        synchronized (lock) {
	    	StringBuffer buffer = new StringBuffer();
	    	buffer.append("AdditionalInfo{");
	
	    	buffer.append("topLevel=[");
	    	entrySetToString(buffer, this.topLevelInitialsToInfo.entrySet());
	    	buffer.append("]\n");
	    	buffer.append("inner=[");
	    	entrySetToString(buffer, this.innerInitialsToInfo.entrySet());
	    	buffer.append("]");
	
	        buffer.append("}");
	    	return buffer.toString();
        }
    }

    /**
     * @param buffer
     * @param name
     */
    private void entrySetToString(StringBuffer buffer, Set<Entry<String, List<IInfo>>> name) {
        synchronized (lock) {
	        for (Entry<String, List<IInfo>> entry : name) {
				List<IInfo> value = entry.getValue();
				for (IInfo info : value) {
					buffer.append(info.toString());
					buffer.append("\n");
				}
			}
        }
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