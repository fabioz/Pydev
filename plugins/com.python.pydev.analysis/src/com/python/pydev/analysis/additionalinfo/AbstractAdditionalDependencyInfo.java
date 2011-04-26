/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 28/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ObjectsPool;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.cache.DiskCache;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.structure.FastStringBuffer;
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
     * Maximum number of modules to have in memory (when reaching that limit, a module will have to be removed
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
    public DiskCache<Set<String>> completeIndex; 

    /**
     * default constructor
     * @throws MisconfigurationException 
     */
    public AbstractAdditionalDependencyInfo() throws MisconfigurationException {
        init();
    }

    public AbstractAdditionalDependencyInfo(boolean callInit) throws MisconfigurationException {
        if(callInit){
            init();
        }
    }
    
    private static ICallback<Set<String>, String> readFromFileMethod = new ICallback<Set<String>, String>() {

        public Set<String> call(String arg) {
            HashSet<String> hashSet = new HashSet<String>();
            StringUtils.splitWithIntern(arg, '\n', hashSet);
            return hashSet;
        }
    };
    
    private static ICallback<String, Set<String>> toFileMethod = new ICallback<String, Set<String>>() {

        public String call(Set<String> arg) {
            FastStringBuffer buf = new FastStringBuffer(arg.size()*30);
            for(String s:arg){
                buf.append(s);
                buf.append('\n');
            }
            return buf.toString();
        }
    };
    
    /**
     * Initializes the internal DiskCache with the indexes.
     * @throws MisconfigurationException 
     */
    protected void init() throws MisconfigurationException {
        File persistingFolder = getCompleteIndexPersistingFolder();

        completeIndex = new DiskCache<Set<String>>(
                DISK_CACHE_IN_MEMORY, 
                persistingFolder, 
                ".v1_indexcache",
                readFromFileMethod,
                toFileMethod
        );
    }

    /**
     * @return a folder where the index should be persisted
     * @throws MisconfigurationException 
     */
    protected File getCompleteIndexPersistingFolder() throws MisconfigurationException {
        File persistingFolder = getPersistingFolder();
        persistingFolder = new File(persistingFolder, "v1_indexcache");
        
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
                                //no intern construct (we'll keep the version we're passed on memory anyways).
                                toks.add(new NameInfo(infoName, modName, null, true));
                            }    
                        }
                    }
                }
            }
            return toks;
        }

    }
    
    @Override
    public List<IInfo> addAstInfo(SimpleNode node, String moduleName, IPythonNature nature, boolean generateDelta) {
    	List<IInfo> addAstInfo = new ArrayList<IInfo>();
        if(node == null || moduleName == null){
            return addAstInfo;
        }
        HashSet<String> nameIndexes = new HashSet<String>();
        SequencialASTIteratorVisitor visitor2 = new SequencialASTIteratorVisitor();
        try {
            node.accept(visitor2);
            Iterator<ASTEntry> iterator = visitor2.getNamesIterator();
            
            synchronized (lock) {
                addAstInfo = super.addAstInfo(node, moduleName, nature, generateDelta);
                
                synchronized(ObjectsPool.lock){
                    //ok, now, add 'all the names'
                    while (iterator.hasNext()) {
                        ASTEntry entry = iterator.next();
                        String id;
                        if (entry.node instanceof Name) {
                            id = ((Name) entry.node).id;
                        } else {
                            id = ((NameTok) entry.node).id;
                        }
                        id = ObjectsPool.internUnsynched(id);
                        nameIndexes.add(id);
                    }
                }
                
                completeIndex.add(moduleName, nameIndexes);
            }
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
        return addAstInfo;
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
        synchronized (lock) {
            return new Tuple<Object, Object>(super.getInfoToSave(), completeIndex);
        }
    }
    
    
    @Override
    protected void restoreSavedInfo(Object o) throws MisconfigurationException{
        synchronized (lock) {
            Tuple readFromFile = (Tuple) o;
            if(!(readFromFile.o1 instanceof Tuple3)){
                throw new RuntimeException("Type Error: the info must be regenerated (changed across versions).");
            }
            
            completeIndex = (DiskCache) readFromFile.o2;
            if(completeIndex == null){
                throw new RuntimeException("Type Error (index == null): the info must be regenerated (changed across versions).");
            }
            completeIndex.readFromFileMethod = readFromFileMethod;
            completeIndex.toFileMethod = toFileMethod;
            
            String shouldBeOn = REF.getFileAbsolutePath(getCompleteIndexPersistingFolder());
            if(!completeIndex.getFolderToPersist().equals(shouldBeOn)){
                //this can happen if the user moves its .metadata folder (so, we have to validate it).
                completeIndex.setFolderToPersist(shouldBeOn);
            }
            
            super.restoreSavedInfo(readFromFile.o1);
        }
    }




}
