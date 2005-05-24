/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModule;
import org.python.pydev.editor.codecompletion.revisited.modules.ModulesKey;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.plugin.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public abstract class ModulesManager implements Serializable{

    /**
     * Modules that we have in memory. This is persisted when saved.
     * 
     * Keys are strings with the name of the module. Values are AbstractModule objects.
     */
    private Map modules = new HashMap();


    /**
     * @param modules The modules to set.
     */
    private void setModules(Map modules) {
        this.modules = modules;
    }

    /**
     * @return Returns the modules.
     */
    protected Map getModules() {
        return modules;
    }

    
    /**
     * Helper for using the pythonpath. Also persisted.
     */
    private PythonPathHelper pythonPathHelper = new PythonPathHelper();

    
    public transient static String [] BUILTINS = new String []{"sys", "__builtin__","math", "datetime"};
    
    /**
     * these exist in the filesystem, but still, are treated as compiled modules
     */
    public transient static String [] REPLACED_BUILTINS = new String []{"os"}; 


    /**
     * 
     * @param pythonpath
     * @param project may be null if there is no associated project.
     * @param monitor
     */
    public void changePythonPath(String pythonpath, final IProject project, IProgressMonitor monitor) {

        List pythonpathList = pythonPathHelper.setPythonPath(pythonpath);

        Map mods = new HashMap();

        List completions = new ArrayList();

        int total = 0;

        //first thing: get all files available from the python path and sum them up.
        for (Iterator iter = pythonpathList.iterator(); iter.hasNext() && monitor.isCanceled() == false;) {
            String element = (String) iter.next();

            //the slow part is getting the files... not much we can do (I think).
            List[] below = pythonPathHelper.getModulesBelow(new File(element), monitor);
            completions.addAll(below[0]);
            total += below[0].size();
        }

        int j = 0;
        
        //now, create in memory modules for all the loaded files (empty modules).
        for (Iterator iterator = completions.iterator(); iterator.hasNext() && monitor.isCanceled() == false; j++) {
            Object o = iterator.next();
            if (o instanceof File) {
                File f = (File) o;
                String m = pythonPathHelper.resolveModule(f.getAbsolutePath());

                monitor.setTaskName(new StringBuffer("Module resolved: ").append(j).append(" of ").append(total).append(" (").append(m)
                        .append(")").toString());
                monitor.worked(1);
                if (m != null) {
					//we don't load them at this time.
                    mods.put(new ModulesKey(m, f), AbstractModule.createEmptyModule(m, f));
                }
            }
        }
        
        //create the builtin modules
        
        for (int i = 0; i < BUILTINS.length; i++) {
            String name = BUILTINS[i];
            mods.put(new ModulesKey(name, null), AbstractModule.createEmptyModule(name, null));
        }

        //assign to instance variable 
        this.setModules(mods);
        
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.IASTManager#rebuildModule(java.io.File, org.eclipse.jface.text.IDocument, org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void rebuildModule(File f, IDocument doc, final IProject project, IProgressMonitor monitor, PythonNature nature) {
        final String m = pythonPathHelper.resolveModule(f.getAbsolutePath());
        if (m != null) {
            final AbstractModule value = AbstractModule.createModuleFromDoc(m, f, doc, nature, -1);
            final ModulesKey key = new ModulesKey(m, f);
            getModules().put(key, value);

        }
    }



    /**
     * @see org.python.pydev.editor.codecompletion.revisited.IASTManager#removeModule(java.io.File, org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void removeModule(File file, IProject project, IProgressMonitor monitor) {
        if (file.isDirectory()){
            removeModulesBelow(file, project, monitor);

        }else{
	        String m = pythonPathHelper.resolveModule(file.getAbsolutePath(), false);
	        m = PythonPathHelper.stripExtension(m);
	        if (m != null) {
	            getModules().remove(new ModulesKey(m, file));
	        }
        }
    }

    private void removeModulesBelow(File file, IProject project, IProgressMonitor monitor) {
        String m = pythonPathHelper.resolveModule(file.getAbsolutePath(), false);
        List toRem = new ArrayList();
        if (m != null) {
            for (Iterator iter = getModules().keySet().iterator(); iter.hasNext();) {
                ModulesKey key = (ModulesKey) iter.next();
                if(key.name.startsWith(m)){
                    toRem.add(key);
                }
            }
        }
        
        //really remove them here.
        for (Iterator iter = toRem.iterator(); iter.hasNext();) {
            getModules().remove(iter.next());
            
        }
    }

    /**
     * @return
     */
    public Set keySet() {
        Set s = new HashSet();
        s.addAll(getModules().keySet());
        return s;
    }
    
//    /**
//     * @return a Set of strings with all the modules.
//     */
//    public ModulesKey[] getAllModules() {
//        return (ModulesKey[]) getModules().keySet().toArray(new ModulesKey[0]);
//    }
//
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
     * @return the module represented by this name
     */
    public AbstractModule getModule(String name, PythonNature nature) {
        
        AbstractModule n = (AbstractModule) getModules().get(new ModulesKey(name, null));
        if (n == null){
            n = (AbstractModule) getModules().get(new ModulesKey(name+".__init__", null));
        }
        
        
        
        if(n instanceof SourceModule){
            //ok, module exists, let's check if it is synched with the filesystem version...
            SourceModule s = (SourceModule) n;
            if(! s.isSynched() ){
                //change it for an empty and proceed as usual.
                n = new EmptyModule(s.getName(), s.getFile());
                this.getModules().put(new ModulesKey(s.getName(), s.getFile()), n);
            }
        }
        
        if (n instanceof EmptyModule){
            EmptyModule e = (EmptyModule)n;

            //let's treat os as a special extension, since many things it has are too much
            //system dependent, and being so, many of its useful completions are not goten
            //e.g. os.path is defined correctly only on runtime.
            
            boolean found = false;
            if(e.f != null){
	            for (int i = 0; i < REPLACED_BUILTINS.length; i++) {
	                if(name.equals(REPLACED_BUILTINS[i])){
	                    n = new CompiledModule(name, PyCodeCompletion.TYPE_BUILTIN);
	                    found = true;
	                }   
	            }
            }
                
            if(!found && e.f != null){
                try{
                    n = AbstractModule.createModule(name, e.f, nature, -1);
                }catch(FileNotFoundException exc){
                    this.getModules().remove(new ModulesKey(name, e.f));
                    n = null;
                }
                
                
            }else{
                //check for supported builtins
                //these don't have files associated.
                for (int i = 0; i < BUILTINS.length; i++) {
                    if(name.equals(BUILTINS[i])){
                        n = new CompiledModule(name, PyCodeCompletion.TYPE_BUILTIN);
                    }   
                }
            }
            
            if(n != null){
	            this.getModules().put(new ModulesKey(name, e.f), n);
            }else{
                System.err.println("The module "+name+" could not be found nor created!");
            }
        }
        
        if( n instanceof EmptyModule){
            throw new RuntimeException("Should not be an empty module anymore!");
        }
        return n;

    }

    /**
     * @param full
     * @return
     */
    public String resolveModule(String full) {
        return pythonPathHelper.resolveModule(full);
    }

}
