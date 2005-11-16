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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModule;
import org.python.pydev.editor.codecompletion.revisited.modules.ModulesKey;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public abstract class ModulesManager implements Serializable {

    /**
     * Modules that we have in memory. This is persisted when saved.
     * 
     * Keys are ModulesKey with the name of the module. Values are AbstractModule objects.
     */
    protected transient Map<ModulesKey, AbstractModule> modules = new HashMap<ModulesKey, AbstractModule>();

    /**
     * Helper for using the pythonpath. Also persisted.
     */
    protected PythonPathHelper pythonPathHelper = new PythonPathHelper();

    private static final long serialVersionUID = 1L;

    /**
     * Custom deserialization is needed.
     */
    private void readObject(ObjectInputStream aStream) throws IOException, ClassNotFoundException {
        modules = new HashMap<ModulesKey, AbstractModule>();
        aStream.defaultReadObject();
        Set set = (Set) aStream.readObject();
        for (Iterator iter = set.iterator(); iter.hasNext();) {
            ModulesKey key = (ModulesKey) iter.next();
            //restore with empty modules.
            modules.put(key, AbstractModule.createEmptyModule(key.name, key.file));
        }
    }

    /**
     * Custom serialization is needed.
     */
    private void writeObject(ObjectOutputStream aStream) throws IOException {
        aStream.defaultWriteObject();
        //write only the keys
        aStream.writeObject(new HashSet(this.modules.keySet()));
    }

    /**
     * @param modules The modules to set.
     */
    private void setModules(Map<ModulesKey, AbstractModule> modules) {
        this.modules = modules;
    }

    /**
     * @return Returns the modules.
     */
    protected Map<ModulesKey, AbstractModule> getModules() {
        return modules;
    }

    /**
     * Must be overriden so that the available builtins (forced or not) are returned.
     */
    public abstract String[] getBuiltins();

    /**
     * 
     * @param pythonpath
     * @param project may be null if there is no associated project.
     * @param monitor
     */
    public void changePythonPath(String pythonpath, final IProject project, IProgressMonitor monitor) {
        List pythonpathList = pythonPathHelper.setPythonPath(pythonpath);

        Map<ModulesKey, AbstractModule> mods = new HashMap<ModulesKey, AbstractModule>();

        List completions = new ArrayList();

        List<String> fromJar = new ArrayList<String>();
        int total = 0;

        //first thing: get all files available from the python path and sum them up.
        for (Iterator iter = pythonpathList.iterator(); iter.hasNext() && monitor.isCanceled() == false;) {
            String element = (String) iter.next();

            //the slow part is getting the files... not much we can do (I think).
            File root = new File(element);
            List[] below = pythonPathHelper.getModulesBelow(root, monitor);
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
                    AbstractModule module = mods.get(modulesKey);
                    
                    //ok, now, let's resolve any conflicts that we might find...
                    boolean add = false;
                    
                    //no conflict (easy)
                    if(module == null){
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
                        mods.put(modulesKey, AbstractModule.createEmptyModule(m, f));
                    }
                }
            }
        }
        
        for (String modName : fromJar) {
            mods.put(new ModulesKey(modName, null), AbstractModule.createEmptyModule(modName, null));
        }

        //create the builtin modules
        String[] builtins = getBuiltins();
        if(builtins != null){
	        for (int i = 0; i < builtins.length; i++) {
	            String name = builtins[i];
	            mods.put(new ModulesKey(name, null), AbstractModule.createEmptyModule(name, null));
	        }
        }

        //assign to instance variable
        this.setModules(mods);

    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager#rebuildModule(java.io.File, org.eclipse.jface.text.IDocument,
     *      org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void rebuildModule(File f, IDocument doc, final IProject project, IProgressMonitor monitor, PythonNature nature) {
        final String m = pythonPathHelper.resolveModule(REF.getFileAbsolutePath(f));
        if (m != null) {
            //behaviour changed, now, only set it as an empty module (it will be parsed on demand)
            final ModulesKey key = new ModulesKey(m, f);
            doAddSingleModule(key, new EmptyModule(key.name, key.file));

            
        }else if (f != null){ //ok, remove the module that has a key with this file, as it can no longer be resolved
            Set<ModulesKey> toRemove = new HashSet<ModulesKey>();
            for (Iterator iter = getModules().keySet().iterator(); iter.hasNext();) {
                ModulesKey key = (ModulesKey) iter.next();
                if(key.file != null && key.file.equals(f)){
                    toRemove.add(key);
                }
            }
            removeThem(toRemove);
        }
    }


    /**
     * @see org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager#removeModule(java.io.File, org.eclipse.core.resources.IProject,
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
        for (Iterator iter = getModules().keySet().iterator(); iter.hasNext();) {
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
        
        for (Iterator iter = getModules().keySet().iterator(); iter.hasNext();) {
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
        this.modules.remove(key);
    }

    /**
     * This is the only method that should add / update a module.
     * No other method should add it directly (unless it is loading or rebuilding it).
     * 
     * @param key this is the key that should be added
     * @param n 
     */
    protected void doAddSingleModule(final ModulesKey key, AbstractModule n) {
        this.modules.put(key, n);
    }

    /**
     * @return a set of all module keys
     */
    public Set<String> getAllModuleNames() {
        Set<String> s = new HashSet<String>();
        Set<ModulesKey> moduleKeys = getModules().keySet();
        for (ModulesKey key : moduleKeys) {
            s.add(key.name);
        }
        return s;
    }

    /**
     * @return a Set of strings with all the modules.
     */
    public ModulesKey[] getAllModules() {
        return (ModulesKey[]) getModules().keySet().toArray(new ModulesKey[0]);
    }
    
    public ModulesKey[] getOnlyDirectModules() {
        return getAllModules();
    }

    /**
     * @return
     */
    public int getSize() {
        return getModules().size();
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
    public AbstractModule getModule(String name, PythonNature nature, boolean dontSearchInit) {
        AbstractModule n = null;
        
        //check for supported builtins these don't have files associated.
        //they are the first to be passed because the user can force a module to be builtin, because there
        //is some information that is only useful when you have builtins, such as os and wxPython (those can
        //be source modules, but they are so hacked that it is almost impossible to get useful information
        //from them).
        String[] builtins = getBuiltins();
        
        for (int i = 0; i < builtins.length; i++) {
            if (name.equals(builtins[i])) {
                n = (AbstractModule) getModules().get(new ModulesKey(name, null));
                if(n == null || n instanceof EmptyModule || n instanceof SourceModule){ //still not created or not defined as compiled module (as it should be)
                    n = new CompiledModule(name, PyCodeCompletion.TYPE_BUILTIN, nature.getAstManager());
                    doAddSingleModule(new ModulesKey(n.getName(), null), n);
                }
            }
        }


        if(n == null){
            if(!dontSearchInit){
                if(n == null){
                    n = (AbstractModule) getModules().get(new ModulesKey(name + ".__init__", null));
                    if(n != null){
                        name += ".__init__";
                    }
                }
            }
            if (n == null) {
            	n = (AbstractModule) getModules().get(new ModulesKey(name, null));
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
     * @param full
     * @return
     */
    public String resolveModule(String full) {
        return pythonPathHelper.resolveModule(full, false);
    }

    public List getPythonPath(){
        return new ArrayList(pythonPathHelper.pythonpath);
    }
}
