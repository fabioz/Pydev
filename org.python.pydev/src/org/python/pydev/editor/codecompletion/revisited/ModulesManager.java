/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.REF;
import org.python.pydev.core.cache.Cache;
import org.python.pydev.core.cache.LRUCache;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 */
public abstract class ModulesManager implements IModulesManager, Serializable {

	private final class ModulesManagerCache extends LRUCache<ModulesKey, AbstractModule> {
		private ModulesManagerCache(int size) {
			super(size);
		}

		/**
		 * Overriden so that if we do not find the key, we have the chance to create it.
		 */
		public AbstractModule getObj(ModulesKey key) {
			AbstractModule obj = super.getObj(key);
			if(obj == null && modulesKeys.containsKey(key)){
				key = modulesKeys.get(key); //get the 'real' key
				obj = AbstractModule.createEmptyModule(key.name, key.file);
				this.add(key, obj);
			}
			return obj;
		}
	}

	public ModulesManager(){
	}
	
    /**
     * Modules that we have in memory. This is persisted when saved.
     * 
     * Keys are ModulesKey with the name of the module. Values are AbstractModule objects.
     * 
     * Implementation changed to contain a cache, so that it does not grow to much (some out of memo errors
     * were thrown because of the may size when having too many modules).
     * 
     * It is sorted so that we can get things in a 'subtree' faster
     */
//    protected transient Map<ModulesKey, AbstractModule> modules = new HashMap<ModulesKey, AbstractModule>();
	protected transient SortedMap<ModulesKey, ModulesKey> modulesKeys = new TreeMap<ModulesKey, ModulesKey>();
	protected transient Cache<ModulesKey, AbstractModule> cache = createCache();
    
	protected Cache<ModulesKey, AbstractModule> createCache(){
		return new ModulesManagerCache(300);
	}
	
    /**
     * This is the set of files that was found just right after unpickle (it should not be changed after that,
     * and serves only as a reference cache).
     */
    protected transient Set<File> files = new HashSet<File>();

    /**
     * Helper for using the pythonpath. Also persisted.
     */
    protected PythonPathHelper pythonPathHelper = new PythonPathHelper();

    private static final long serialVersionUID = 2L;

    /**
     * Custom deserialization is needed.
     */
    private void readObject(ObjectInputStream aStream) throws IOException, ClassNotFoundException {
    	cache = createCache();
    	modulesKeys = new TreeMap<ModulesKey, ModulesKey>();
    	
        files = new HashSet<File>();
        aStream.defaultReadObject();
        Set set = (Set) aStream.readObject();
        for (Iterator iter = set.iterator(); iter.hasNext();) {
            ModulesKey key = (ModulesKey) iter.next();
            //restore with empty modules.
            modulesKeys.put(key, key);
            if(key.file != null){
            	files.add(key.file);
            }
        }
    }

    /**
     * Custom serialization is needed.
     */
    private void writeObject(ObjectOutputStream aStream) throws IOException {
        aStream.defaultWriteObject();
        //write only the keys
        aStream.writeObject(new HashSet(modulesKeys.keySet()));
    }

    /**
     * @param modules The modules to set.
     */
    private void setModules(SortedMap<ModulesKey, ModulesKey> keys) {
        this.modulesKeys = keys;
    }
    

    /**
     * @return Returns the modules.
     */
    protected Map<ModulesKey, AbstractModule> getModules() {
        throw new RuntimeException("Deprecated");
    }

    /**
     * Must be overriden so that the available builtins (forced or not) are returned.
     * @param defaultSelectedInterpreter 
     */
    public abstract String[] getBuiltins(String defaultSelectedInterpreter);

	/**
	 * 
	 * @param monitor this is the monitor
	 * @param pythonpathList this is the pythonpath
	 * @param completions OUT - the files that were gotten as valid python modules 
	 * @param fromJar OUT - the names of the modules that were found inside a jar
	 * @return the total number of modules found (that's completions + fromJar)
	 */
	private int listFilesForCompletion(IProgressMonitor monitor, List<String> pythonpathList, List<File> completions, List<String> fromJar) {
		int total = 0;
		//first thing: get all files available from the python path and sum them up.
        for (Iterator iter = pythonpathList.iterator(); iter.hasNext() && monitor.isCanceled() == false;) {
            String element = (String) iter.next();

            //the slow part is getting the files... not much we can do (I think).
            File root = new File(element);
            List<File>[] below = pythonPathHelper.getModulesBelow(root, monitor);
            if(below != null){
                completions.addAll(below[0]);
                total += below[0].size();
                
            }else{ //ok, it was null, so, maybe this is not a folder, but  zip file with java classes...
                List<String> currFromJar = PythonPathHelper.getFromJar(root, monitor);
                if(currFromJar != null){
                    fromJar.addAll(currFromJar);
                    total += currFromJar.size();
                }
            }
        }
		return total;
	}

	public void changePythonPath(String pythonpath, final IProject project, IProgressMonitor monitor, String defaultSelectedInterpreter) {
		List<String> pythonpathList = pythonPathHelper.setPythonPath(pythonpath);
		List<File> completions = new ArrayList<File>();
		List<String> fromJar = new ArrayList<String>();
		int total = listFilesForCompletion(monitor, pythonpathList, completions, fromJar);
		changePythonPath(pythonpath, project, monitor, pythonpathList, completions, fromJar, total, defaultSelectedInterpreter);
	}
	
    /**
     * @param pythonpath string with the new pythonpath (separated by |)
     * @param project may be null if there is no associated project.
     */
    private void changePythonPath(String pythonpath, final IProject project, IProgressMonitor monitor, 
    		List<String> pythonpathList, List<File> completions, List<String> fromJar, int total, String defaultSelectedInterpreter) {

    	SortedMap<ModulesKey, ModulesKey> keys = new TreeMap<ModulesKey, ModulesKey>();
        int j = 0;

        //now, create in memory modules for all the loaded files (empty modules).
        for (Iterator iterator = completions.iterator(); iterator.hasNext() && monitor.isCanceled() == false; j++) {
            Object o = iterator.next();
            if (o instanceof File) {
                File f = (File) o;
                String fileAbsolutePath = REF.getFileAbsolutePath(f);
                String m = pythonPathHelper.resolveModule(fileAbsolutePath);

                monitor.setTaskName(new StringBuffer("Module resolved: ").append(j).append(" of ").append(total).append(" (").append(m)
                        .append(")").toString());
                monitor.worked(1);
                if (m != null) {
                    //we don't load them at this time.
                    ModulesKey modulesKey = new ModulesKey(m, f);
                    
                    //ok, now, let's resolve any conflicts that we might find...
                    boolean add = false;
                    
                    //no conflict (easy)
                    if(!keys.containsKey(modulesKey)){
                        add = true;
                    }else{
                        //we have a conflict, so, let's resolve which one to keep (the old one or this one)
                        if(PythonPathHelper.isValidSourceFile(fileAbsolutePath)){
                            //source files have priority over other modules (dlls) -- if both are source, there is no real way to resolve
                            //this priority, so, let's just add it over.
                            add = true;
                        }
                    }
                    
                    if(add){
                    	//the previous must be removed (if there was any)
                    	keys.remove(modulesKey);
                    	keys.put(modulesKey, modulesKey);
                    }
                }
            }
        }
        
        for (String modName : fromJar) {
        	final ModulesKey k = new ModulesKey(modName, null);
			keys.put(k, k);
        }

        //create the builtin modules
        String[] builtins = getBuiltins(defaultSelectedInterpreter);
        if(builtins != null){
	        for (int i = 0; i < builtins.length; i++) {
	            String name = builtins[i];
	            final ModulesKey k = new ModulesKey(name, null);
				keys.put(k, k);
	        }
        }

        //assign to instance variable
        this.setModules(keys);

    }


    /**
     * @see org.python.pydev.core.ICodeCompletionASTManager#rebuildModule(java.io.File, org.eclipse.jface.text.IDocument,
     *      org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void rebuildModule(File f, IDocument doc, final IProject project, IProgressMonitor monitor, IPythonNature nature) {
        final String m = pythonPathHelper.resolveModule(REF.getFileAbsolutePath(f));
        if (m != null) {
            //behaviour changed, now, only set it as an empty module (it will be parsed on demand)
            final ModulesKey key = new ModulesKey(m, f);
            doAddSingleModule(key, new EmptyModule(key.name, key.file));

            
        }else if (f != null){ //ok, remove the module that has a key with this file, as it can no longer be resolved
            Set<ModulesKey> toRemove = new HashSet<ModulesKey>();
            for (Iterator iter = modulesKeys.keySet().iterator(); iter.hasNext();) {
                ModulesKey key = (ModulesKey) iter.next();
                if(key.file != null && key.file.equals(f)){
                    toRemove.add(key);
                }
            }
            removeThem(toRemove);
        }
    }


    /**
     * @see org.python.pydev.core.ICodeCompletionASTManager#removeModule(java.io.File, org.eclipse.core.resources.IProject,
     *      org.eclipse.core.runtime.IProgressMonitor)
     */
    public void removeModule(File file, IProject project, IProgressMonitor monitor) {
        if(file == null){
            return;
        }
        
        if (file.isDirectory()) {
            removeModulesBelow(file, project, monitor);

        } else {
            if(file.getName().startsWith("__init__.")){
                removeModulesBelow(file.getParentFile(), project, monitor);
            }else{
                removeModulesWithFile(file);
            }
        }
    }

    /**
     * @param file
     */
    private void removeModulesWithFile(File file) {
        if(file == null){
            return;
        }
        
        List<ModulesKey> toRem = new ArrayList<ModulesKey>();
        for (Iterator iter = modulesKeys.keySet().iterator(); iter.hasNext();) {
            ModulesKey key = (ModulesKey) iter.next();
            if (key.file != null && key.file.equals(file)) {
                toRem.add(key);
            }
        }

        removeThem(toRem);
    }

    /**
     * removes all the modules that have the module starting with the name of the module from
     * the specified file.
     */
    private void removeModulesBelow(File file, IProject project, IProgressMonitor monitor) {
        if(file == null){
            return;
        }
        
        String absolutePath = REF.getFileAbsolutePath(file);
        List<ModulesKey> toRem = new ArrayList<ModulesKey>();
        
        for (Iterator iter = modulesKeys.keySet().iterator(); iter.hasNext();) {
            ModulesKey key = (ModulesKey) iter.next();
            if (key.file != null && REF.getFileAbsolutePath(key.file).startsWith(absolutePath)) {
                toRem.add(key);
            }
        }

        removeThem(toRem);
    }


    /**
     * This method that actually removes some keys from the modules. 
     * 
     * @param toRem the modules to be removed
     */
    protected void removeThem(Collection<ModulesKey> toRem) {
        //really remove them here.
        for (Iterator<ModulesKey> iter = toRem.iterator(); iter.hasNext();) {
            doRemoveSingleModule(iter.next());
        }
    }

    /**
     * This is the only method that should remove a module.
     * No other method should remove them directly.
     * 
     * @param key this is the key that should be removed
     */
    protected void doRemoveSingleModule(ModulesKey key) {
        this.modulesKeys.remove(key);
        this.cache.remove(key);
    }

    /**
     * This is the only method that should add / update a module.
     * No other method should add it directly (unless it is loading or rebuilding it).
     * 
     * @param key this is the key that should be added
     * @param n 
     */
    public void doAddSingleModule(final ModulesKey key, AbstractModule n) {
    	this.modulesKeys.put(key, key);
        this.cache.add(key, n);
    }

    /**
     * @return a set of all module keys
     */
    public Set<String> getAllModuleNames() {
        Set<String> s = new HashSet<String>();
        for (ModulesKey key : this.modulesKeys.keySet()) {
            s.add(key.name);
        }
        return s;
    }

    public SortedMap<ModulesKey,ModulesKey> getAllDirectModulesStartingWith(String strStartingWith) {
    	if(strStartingWith.length() == 0){
    		return modulesKeys;
    	}
    	ModulesKey startingWith = new ModulesKey(strStartingWith, null);
    	ModulesKey endingWith = new ModulesKey(startingWith+"z", null);
    	return modulesKeys.subMap(startingWith, endingWith);
    }
    
    public SortedMap<ModulesKey,ModulesKey> getAllModulesStartingWith(String strStartingWith) {
    	return getAllDirectModulesStartingWith(strStartingWith);
    }
    
    public ModulesKey[] getOnlyDirectModules() {
    	return (ModulesKey[]) this.modulesKeys.keySet().toArray(new ModulesKey[0]);
    }

    /**
     * @return
     */
    public int getSize() {
        return this.modulesKeys.size();
    }

    /**
     * This method returns the module that corresponds to the path passed as a parameter.
     * 
     * @param name
     * @param dontSearchInit is used in a negative form because initially it was isLookingForRelative, but
     * it actually defines if we should look in __init__ modules too, so, the name matches the old signature.
     * 
     * NOTE: isLookingForRelative description was: when looking for relative imports, we don't check for __init__
     * @return the module represented by this name
     */
    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit) {
        AbstractModule n = null;
        
        //check for supported builtins these don't have files associated.
        //they are the first to be passed because the user can force a module to be builtin, because there
        //is some information that is only useful when you have builtins, such as os and wxPython (those can
        //be source modules, but they are so hacked that it is almost impossible to get useful information
        //from them).
        String[] builtins = getBuiltins();
        
        boolean foundStartingWithBuiltin = false;
        for (int i = 0; i < builtins.length; i++) {
            String forcedBuiltin = builtins[i];
            if (name.startsWith(forcedBuiltin)) {
                if(name.length() > forcedBuiltin.length() && name.charAt(forcedBuiltin.length()) == '.'){
                	foundStartingWithBuiltin = true;
                	n = cache.getObj(new ModulesKey(name, null));
                	if(n == null && dontSearchInit == false){
                		n = cache.getObj(new ModulesKey(name+".__init__", null));
                	}
                	if(n instanceof EmptyModule || n instanceof SourceModule){ //it is actually found as a source module, so, we have to 'coerce' it to a compiled module
                		n = new CompiledModule(name, PyCodeCompletion.TYPE_BUILTIN, nature.getAstManager());
                		doAddSingleModule(new ModulesKey(n.getName(), null), n);
                		return n;
                	}
                }
                
                if(name.equals(forcedBuiltin)){
                    n = cache.getObj(new ModulesKey(name, null));
                    if(n == null || n instanceof EmptyModule || n instanceof SourceModule){ //still not created or not defined as compiled module (as it should be)
                        n = new CompiledModule(name, PyCodeCompletion.TYPE_BUILTIN, nature.getAstManager());
                        doAddSingleModule(new ModulesKey(n.getName(), null), n);
                        return n;
                    }
                }
                if(n instanceof CompiledModule){
                	return n;
                }
            }
        }
        if(foundStartingWithBuiltin){
        	return null;
        }


        if(n == null){
            if(!dontSearchInit){
                if(n == null){
                    n = cache.getObj(new ModulesKey(name + ".__init__", null));
                    if(n != null){
                        name += ".__init__";
                    }
                }
            }
            if (n == null) {
            	ModulesKey key = new ModulesKey(name, null);
            	n = cache.getObj(key);
            }
        }

        if (n instanceof SourceModule) {
            //ok, module exists, let's check if it is synched with the filesystem version...
            SourceModule s = (SourceModule) n;
            if (!s.isSynched()) {
                //change it for an empty and proceed as usual.
                n = new EmptyModule(s.getName(), s.getFile());
                doAddSingleModule(new ModulesKey(s.getName(), s.getFile()), n);
            }
        }

        if (n instanceof EmptyModule) {
            EmptyModule e = (EmptyModule) n;

            //let's treat os as a special extension, since many things it has are too much
            //system dependent, and being so, many of its useful completions are not goten
            //e.g. os.path is defined correctly only on runtime.

            boolean found = false;

            if (!found && e.f != null) {
                try {
                    n = AbstractModule.createModule(name, e.f, nature, -1);
                } catch (FileNotFoundException exc) {
                    doRemoveSingleModule(new ModulesKey(name, e.f));
                    n = null;
                }

            }else{ //ok, it does not have a file associated, so, we treat it as a builtin (this can happen in java jars)
                n = new CompiledModule(name, PyCodeCompletion.TYPE_BUILTIN, nature.getAstManager());
            }
            
            if (n != null) {
                doAddSingleModule(new ModulesKey(name, e.f), n);
            } else {
                System.err.println("The module " + name + " could not be found nor created!");
            }
        }

        if (n instanceof EmptyModule) {
            throw new RuntimeException("Should not be an empty module anymore!");
        }
        return n;

    }


    /** 
     * @see org.python.pydev.core.IProjectModulesManager#isInPythonPath(org.eclipse.core.resources.IResource, org.eclipse.core.resources.IProject)
     */
    public boolean isInPythonPath(IResource member, IProject container) {
        return resolveModule(member, container) != null;
    }

    
    /** 
     * @see org.python.pydev.core.IProjectModulesManager#resolveModule(org.eclipse.core.resources.IResource, org.eclipse.core.resources.IProject)
     */
    public String resolveModule(IResource member, IProject container) {
        IPath location = PydevPlugin.getLocation(member.getFullPath(), container);
        if(location == null){
            //not in workspace?... maybe it was removed, so, do nothing, but let the user know about it
            PydevPlugin.log(getResolveModuleErr(member));
            return null;
        }else{
            File inOs = new File(location.toOSString());
            return resolveModule(REF.getFileAbsolutePath(inOs));
        }
    }

    protected String getResolveModuleErr(IResource member) {
		return "Unable to find the path "+member+" in the project were it\n" +
        "is added as a source folder for pydev."+this.getClass();
	}

	/**
     * @param full
     * @return
     */
    public String resolveModule(String full) {
        return pythonPathHelper.resolveModule(full, false);
    }

    public List<String> getPythonPath(){
        return new ArrayList<String>(pythonPathHelper.pythonpath);
    }

}
