/*
 * Created on 28/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.Tuple4;
import org.python.pydev.core.cache.DiskCache;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.SequencialASTIteratorVisitor;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Adds dependency information to the interpreter information. This should be used only for
 * classes that are part of a project (this info will not be gotten for the system interpreter) 
 * 
 * (Basically, it will index all the names that are found in a module so that we can easily know all the
 * places where some name exists)
 * 
 * 
 * @author Fabio
 */
public abstract class AbstractAdditionalDependencyInfo extends AbstractAdditionalInterpreterInfo{
    
    /**
     * Maximun number of modules to have in memory (when reaching that limit, a module will have to be removed
     * before another module is loaded).
     */
    public static final int DISK_CACHE_IN_MEMORY = 300;

    public static boolean TESTING = false;

    /**
     * Defines that some operation should be done on the complete name indexing.
     */
    public final static int COMPLETE_INDEX = 4;

    /**
     * indexes all the names that are available
     * 
     * It is actually a Cache<String<Set<String>>,
     * 
     * So the key is the module name and the value is a Set of the strings it contains.
     */
    public DiskCache completeIndex; 

    /**
     * default constructor
     */
    public AbstractAdditionalDependencyInfo() {
        init();
    }

    public AbstractAdditionalDependencyInfo(boolean callInit) {
        if(callInit){
            init();
        }
    }
    
    /**
     * Initializes the internal DiskCache with the indexes.
     */
    protected void init() {
        File persistingFolder = getCompleteIndexPersistingFolder();
        completeIndex = new DiskCache(DISK_CACHE_IN_MEMORY, persistingFolder, ".indexcache");
    }

    /**
     * @return a folder where the index should be persisted
     */
    protected File getCompleteIndexPersistingFolder() {
        File persistingFolder = getPersistingFolder();
        persistingFolder = new File(persistingFolder, "indexcache");
        
        if(persistingFolder.exists()){
            if(persistingFolder.isDirectory()){
                persistingFolder.delete();
            }
        }
        if(!persistingFolder.exists()){
            persistingFolder.mkdirs();
        }
        return persistingFolder;
    }
    
    @Override
    public void clearAllInfo() {
        synchronized(lock){
            super.clearAllInfo();
            try {
                completeIndex.clear();
            } catch (NullPointerException e) {
                //that's ok... because it might be called before actually having any values
            }
        }
    }
    
    
    @SuppressWarnings("unchecked")
    @Override
    protected List<IInfo> getWithFilter(String qualifier, int getWhat, Filter filter, boolean useLowerCaseQual) {
        synchronized(lock){
            List<IInfo> toks = super.getWithFilter(qualifier, getWhat, filter, useLowerCaseQual);
            
            if((getWhat & COMPLETE_INDEX) != 0){
                //note that this operation is not as fast as the others, as it relies on a cache that is optimized
                //for space and not for speed (but still, faster than having to do a text-search to know the tokens).
                String qualToCompare = qualifier;
                if(useLowerCaseQual){
                    qualToCompare = qualifier.toLowerCase();
                }
                
                for(String modName : completeIndex.keys()){
                    HashSet<String> obj = (HashSet<String>) completeIndex.getObj(modName);
                    if(obj == null){
                        //throw new RuntimeException("Null was returned when we were looking for the module:"+modName);
                    }else{
                        for(String infoName: obj){
                            if(filter.doCompare(qualToCompare, infoName)){
                                toks.add(NameInfo.fromName(infoName, modName, null, pool));
                            }    
                        }
                    }
                }
            }
            return toks;
        }

    }
    
    @Override
    public void addAstInfo(SimpleNode node, String moduleName, IPythonNature nature, boolean generateDelta) {
        if(node == null || moduleName == null){
            return;
        }
        super.addAstInfo(node, moduleName, nature, generateDelta);
        try {
            HashSet<String> nameIndexes = new HashSet<String>();
            
            //ok, now, add 'all the names'
            SequencialASTIteratorVisitor visitor2 = new SequencialASTIteratorVisitor();
            node.accept(visitor2);
            Iterator<ASTEntry> iterator = visitor2.getNamesIterator();
            while (iterator.hasNext()) {
                ASTEntry entry = iterator.next();
                String id;
                //I was having out of memory errors without using this pool (running with a 64mb vm)
                if (entry.node instanceof Name) {
                    id = ((Name) entry.node).id;
                } else {
                    id = ((NameTok) entry.node).id;
                }
                nameIndexes.add(id);
            }
            completeIndex.add(moduleName, nameIndexes);
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
    }
    
    @Override
    public void removeInfoFromModule(String moduleName, boolean generateDelta) {
        synchronized (lock) {
            if(moduleName == null){
                throw new AssertionError("The module name may not be null.");
            }
            completeIndex.remove(moduleName);
            super.removeInfoFromModule(moduleName, generateDelta);
        }
    }
    

    @Override
    protected Object getInfoToSave() {
        return new Tuple<Object, Object>(super.getInfoToSave(), completeIndex);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void restoreSavedInfo(Object o){
        Tuple readFromFile = (Tuple) o;
        if(!(readFromFile.o1 instanceof Tuple3)){
            throw new RuntimeException("Type Error: the info must be regenerated (changed across versions).");
        }
        
        completeIndex = (DiskCache) readFromFile.o2;
        if(completeIndex == null){
            throw new RuntimeException("Type Error (index == null): the info must be regenerated (changed across versions).");
        }
        
        String shouldBeOn = REF.getFileAbsolutePath(getCompleteIndexPersistingFolder());
        if(!completeIndex.getFolderToPersist().equals(shouldBeOn)){
            //this can happen if the user moves its .metadata folder (so, we have to validate it).
            completeIndex.setFolderToPersist(shouldBeOn);
        }
        
        super.restoreSavedInfo(readFromFile.o1);
    }




}
