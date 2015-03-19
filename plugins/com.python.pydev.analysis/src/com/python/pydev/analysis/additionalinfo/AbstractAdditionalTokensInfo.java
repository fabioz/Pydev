/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FileUtilsFileBuffer;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.core.ObjectsInternPool;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.PyPublicTreeMap;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.parser.fastparser.FastDefinitionsParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.DefinitionsASTIteratorVisitor;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;

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
public abstract class AbstractAdditionalTokensInfo {

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
    protected SortedMap<String, Set<IInfo>> topLevelInitialsToInfo = new PyPublicTreeMap<String, Set<IInfo>>();

    /**
     * indexes so that we can get 'inner information' from classes, such as methods or inner classes from a class
     */
    protected SortedMap<String, Set<IInfo>> innerInitialsToInfo = new PyPublicTreeMap<String, Set<IInfo>>();

    /**
     * Should be used before re-creating the info, so that we have enough memory.
     */
    public void clearAllInfo() {
        synchronized (lock) {
            if (topLevelInitialsToInfo != null) {
                topLevelInitialsToInfo.clear();
            }
            if (innerInitialsToInfo != null) {
                innerInitialsToInfo.clear();
            }
        }
    }

    protected Object lock = new Object();

    /**
     * The filter interface
     */
    public interface Filter {
        boolean doCompare(String lowerCaseQual, IInfo info);

        boolean doCompare(String lowerCaseQual, String infoName);
    }

    /**
     * A filter that checks if tokens are equal
     */
    private final Filter equalsFilter = new Filter() {
        public boolean doCompare(String qualifier, IInfo info) {
            return info.getName().equals(qualifier);
        }

        public boolean doCompare(String qualifier, String infoName) {
            return infoName.equals(qualifier);
        }
    };

    /**
     * A filter that checks if the tokens starts with a qualifier
     */
    private final Filter startingWithFilter = new Filter() {

        public boolean doCompare(String lowerCaseQual, IInfo info) {
            return doCompare(lowerCaseQual, info.getName());
        }

        public boolean doCompare(String qualifier, String infoName) {
            return infoName.toLowerCase().startsWith(qualifier);
        }

    };

    /**
     * 2: because we've removed some info (the hash is no longer saved)
     * 3: Changed from string-> list to string->set
     */
    protected static final int version = 4;

    public AbstractAdditionalTokensInfo() {
    }

    /**
     * That's the function actually used to add some info
     *
     * @param info information to be added
     */
    protected void add(IInfo info, int doOn) {
        synchronized (lock) {
            String name = info.getName();
            String initials = getInitials(name);
            SortedMap<String, Set<IInfo>> initialsToInfo;

            if (doOn == TOP_LEVEL) {
                if (info.getPath() != null && info.getPath().length() > 0) {
                    throw new RuntimeException(
                            "Error: the info being added is added as an 'top level' info, but has path. Info:" + info);
                }
                initialsToInfo = topLevelInitialsToInfo;

            } else if (doOn == INNER) {
                if (info.getPath() == null || info.getPath().length() == 0) {
                    throw new RuntimeException(
                            "Error: the info being added is added as an 'inner' info, but does not have a path. Info: "
                                    + info);
                }
                initialsToInfo = innerInitialsToInfo;

            } else {
                throw new RuntimeException("List to add is invalid: " + doOn);
            }
            Set<IInfo> listForInitials = getAndCreateListForInitials(initials, initialsToInfo);
            listForInitials.add(info);
        }
    }

    /**
     * @param name the name from where we want to get the initials
     * @return the initials for the name
     */
    protected String getInitials(String name) {
        if (name.length() < NUMBER_OF_INITIALS_TO_INDEX) {
            return name;
        }
        return name.substring(0, NUMBER_OF_INITIALS_TO_INDEX).toLowerCase();
    }

    /**
     * @param initials the initials we are looking for
     * @param initialsToInfo this is the list we should use (top level or inner)
     * @return the list of tokens with the specified initials (must be exact match)
     */
    protected Set<IInfo> getAndCreateListForInitials(String initials, SortedMap<String, Set<IInfo>> initialsToInfo) {
        Set<IInfo> lInfo = initialsToInfo.get(initials);
        if (lInfo == null) {
            lInfo = new HashSet<IInfo>();
            initialsToInfo.put(initials, lInfo);
        }
        return lInfo;
    }

    private IInfo addAssignTargets(ASTEntry entry, String moduleName, int doOn, String path, boolean lastIsMethod) {
        String rep = NodeUtils.getFullRepresentationString(entry.node);
        if (lastIsMethod) {
            List<String> parts = StringUtils.dotSplit(rep);
            if (parts.size() >= 2) {
                //at least 2 parts are required
                if (parts.get(0).equals("self")) {
                    rep = parts.get(1);
                    //no intern construct (locked in the loop that calls this method)
                    AttrInfo info = new AttrInfo(ObjectsInternPool.internUnsynched(rep), moduleName,
                            ObjectsInternPool.internUnsynched(path), false);
                    add(info, doOn);
                    return info;
                }
            }
        } else {
            //no intern construct (locked in the loop that calls this method)
            AttrInfo info = new AttrInfo(ObjectsInternPool.internUnsynched(FullRepIterable.getFirstPart(rep)), moduleName,
                    ObjectsInternPool.internUnsynched(path), false);
            add(info, doOn);
            return info;
        }
        return null;
    }

    public List<IInfo> addAstInfo(ModulesKey key, boolean generateDelta) throws Exception {
        boolean isZipModule = key instanceof ModulesKeyForZip;
        ModulesKeyForZip modulesKeyForZip = null;
        if (isZipModule) {
            modulesKeyForZip = (ModulesKeyForZip) key;
        }

        Object doc;
        if (isZipModule) {
            doc = FileUtilsFileBuffer.getCustomReturnFromZip(modulesKeyForZip.file, modulesKeyForZip.zipModulePath,
                    null);

        } else {
            doc = FileUtilsFileBuffer.getCustomReturnFromFile(key.file, true, null);
        }

        char[] charArray;
        int len;
        if (doc instanceof IDocument) {
            IDocument document = (IDocument) doc;
            charArray = document.get().toCharArray();
            len = charArray.length;

        } else if (doc instanceof FastStringBuffer) {
            FastStringBuffer fastStringBuffer = (FastStringBuffer) doc;
            //In this case, we can actually get the internal array without doing any copies (and just specifying the len).
            charArray = fastStringBuffer.getInternalCharsArray();
            len = fastStringBuffer.length();

        } else if (doc instanceof String) {
            String str = (String) doc;
            charArray = str.toCharArray();
            len = charArray.length;

        } else if (doc instanceof char[]) {
            charArray = (char[]) doc;
            len = charArray.length;

        } else {
            throw new RuntimeException("Don't know how to handle: " + doc + " -- " + doc.getClass());
        }

        SimpleNode node = FastDefinitionsParser.parse(charArray, key.file.getName(), len);
        if (node == null) {
            return null;
        }

        return addAstInfo(node, key, generateDelta);
    }

    /**
     * Adds ast info information for a module.
     *
     * @param m the module we want to add to the info
     */
    public List<IInfo> addAstInfo(SimpleNode node, ModulesKey key, boolean generateDelta) {
        List<IInfo> createdInfos = new ArrayList<IInfo>();
        if (node == null || key.name == null) {
            return createdInfos;
        }
        try {
            Tuple<DefinitionsASTIteratorVisitor, Iterator<ASTEntry>> tup = getInnerEntriesForAST(node);
            if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
                Log.toLogFile(this, "Adding ast info to: " + key.name);
            }

            try {
                Iterator<ASTEntry> entries = tup.o2;

                FastStack<SimpleNode> tempStack = new FastStack<SimpleNode>(10);

                synchronized (this.lock) {
                    synchronized (ObjectsInternPool.lock) {
                        key.name = ObjectsInternPool.internUnsynched(key.name);

                        while (entries.hasNext()) {
                            ASTEntry entry = entries.next();
                            IInfo infoCreated = null;

                            if (entry.parent == null) { //we only want those that are in the global scope
                                if (entry.node instanceof ClassDef) {
                                    //no intern construct (locked in this loop)
                                    ClassInfo info = new ClassInfo(
                                            ObjectsInternPool.internUnsynched(((NameTok) ((ClassDef) entry.node).name).id),
                                            key.name, null, false);
                                    add(info, TOP_LEVEL);
                                    infoCreated = info;

                                } else if (entry.node instanceof FunctionDef) {
                                    //no intern construct (locked in this loop)
                                    FuncInfo info2 = new FuncInfo(
                                            ObjectsInternPool.internUnsynched(((NameTok) ((FunctionDef) entry.node).name).id),
                                            key.name, null, false);
                                    add(info2, TOP_LEVEL);
                                    infoCreated = info2;

                                } else {
                                    //it is an assign
                                    infoCreated = this.addAssignTargets(entry, key.name, TOP_LEVEL, null, false);

                                }
                            } else {
                                if (entry.node instanceof ClassDef || entry.node instanceof FunctionDef) {
                                    //ok, it has a parent, so, let's check to see if the path we got only has class definitions
                                    //as the parent (and get that path)
                                    Tuple<String, Boolean> pathToRoot = this.getPathToRoot(entry, false, false,
                                            tempStack);
                                    if (pathToRoot != null && pathToRoot.o1 != null && pathToRoot.o1.length() > 0) {
                                        //if the root is not valid, it is not only classes in the path (could be a method inside
                                        //a method, or something similar).

                                        if (entry.node instanceof ClassDef) {
                                            ClassInfo info = new ClassInfo(
                                                    ObjectsInternPool
                                                            .internUnsynched(((NameTok) ((ClassDef) entry.node).name).id),
                                                    key.name, ObjectsInternPool.internUnsynched(pathToRoot.o1), false);
                                            add(info, INNER);
                                            infoCreated = info;

                                        } else {
                                            //FunctionDef
                                            FuncInfo info2 = new FuncInfo(
                                                    ObjectsInternPool
                                                            .internUnsynched(((NameTok) ((FunctionDef) entry.node).name).id),
                                                    key.name, ObjectsInternPool.internUnsynched(pathToRoot.o1), false);
                                            add(info2, INNER);
                                            infoCreated = info2;

                                        }
                                    }
                                } else {
                                    //it is an assign
                                    Tuple<String, Boolean> pathToRoot = this.getPathToRoot(entry, true, false,
                                            tempStack);
                                    if (pathToRoot != null && pathToRoot.o1 != null && pathToRoot.o1.length() > 0) {
                                        infoCreated = this.addAssignTargets(entry, key.name, INNER, pathToRoot.o1,
                                                pathToRoot.o2);
                                    }
                                }
                            }

                            if (infoCreated != null) {
                                createdInfos.add(infoCreated);
                            }

                        } //end while

                    }//end lock ObjectsPool.lock

                }//end this.lock

            } catch (Exception e) {
                Log.log(e);
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return createdInfos;
    }

    /**
     * @return an iterator that'll get the outline entries for the given ast.
     */
    public static Tuple<DefinitionsASTIteratorVisitor, Iterator<ASTEntry>> getInnerEntriesForAST(SimpleNode node)
            throws Exception {
        DefinitionsASTIteratorVisitor visitor = new DefinitionsASTIteratorVisitor();
        node.accept(visitor);
        Iterator<ASTEntry> entries = visitor.getOutline();
        return new Tuple<DefinitionsASTIteratorVisitor, Iterator<ASTEntry>>(visitor, entries);
    }

    /**
     * @return a set with the module names that have tokens.
     */
    public Set<String> getAllModulesWithTokens() {
        HashSet<String> ret = new HashSet<String>();
        synchronized (lock) {
            Set<Entry<String, Set<IInfo>>> entrySet = this.topLevelInitialsToInfo.entrySet();
            for (Entry<String, Set<IInfo>> entry : entrySet) {
                Set<IInfo> value = entry.getValue();
                for (IInfo info : value) {
                    ret.add(info.getDeclaringModuleName());
                }
            }

            entrySet = this.innerInitialsToInfo.entrySet();
            for (Entry<String, Set<IInfo>> entry : entrySet) {
                Set<IInfo> value = entry.getValue();
                for (IInfo info : value) {
                    ret.add(info.getDeclaringModuleName());
                }
            }
        }
        return ret;

    }

    /**
     * @param lastMayBeMethod if true, it gets the path and accepts a method (if it is the last in the stack)
     * if false, null is returned if a method is found.
     *
     * @param tempStack is a temporary stack object (which may be cleared)
     *
     * @return a tuple, where the first element is the path where the entry is located (may return null).
     * and the second element is a boolean that indicates if the last was actually a method or not.
     */
    private Tuple<String, Boolean> getPathToRoot(ASTEntry entry, boolean lastMayBeMethod, boolean acceptAny,
            FastStack<SimpleNode> tempStack) {
        if (entry.parent == null) {
            return null;
        }
        //just to be sure that it's empty
        tempStack.clear();

        boolean lastIsMethod = false;
        //if the last 'may be a method', in this case, we have to remember that it will actually be the first one
        //to be analyzed.

        //let's get the stack
        while (entry.parent != null) {
            if (entry.parent.node instanceof ClassDef) {
                tempStack.push(entry.parent.node);

            } else if (entry.parent.node instanceof FunctionDef) {
                if (!acceptAny) {
                    if (lastIsMethod) {
                        //already found a method
                        return null;
                    }

                    if (!lastMayBeMethod) {
                        return null;
                    }

                    //ok, the last one may be a method... (in this search, it MUST be the first one...)
                    if (tempStack.size() != 0) {
                        return null;
                    }
                }

                //ok, there was a class, so, let's go and set it
                tempStack.push(entry.parent.node);
                lastIsMethod = true;

            } else {
                return null;

            }
            entry = entry.parent;
        }

        //now that we have the stack, let's make it into a path...
        FastStringBuffer buf = new FastStringBuffer();
        while (tempStack.size() > 0) {
            String rep = NodeUtils.getRepresentationString(tempStack.pop());
            if (rep != null) {
                if (buf.length() > 0) {
                    buf.append(".");
                }
                buf.append(rep);
            }
        }
        return new Tuple<String, Boolean>(buf.toString(), lastIsMethod);
    }

    /**
     * Removes all the info associated with a given module
     * @param moduleName the name of the module we want to remove info from
     */
    public void removeInfoFromModule(String moduleName, boolean generateDelta) {
        if (DebugSettings.DEBUG_ANALYSIS_REQUESTS) {
            Log.toLogFile(this, "Removing ast info from: " + moduleName);
        }
        synchronized (lock) {
            removeInfoFromMap(moduleName, topLevelInitialsToInfo);
            removeInfoFromMap(moduleName, innerInitialsToInfo);
        }

    }

    /**
     * @param moduleName
     * @param initialsToInfo
     */
    private void removeInfoFromMap(String moduleName, SortedMap<String, Set<IInfo>> initialsToInfo) {
        Iterator<Set<IInfo>> itListOfInfo = initialsToInfo.values().iterator();
        while (itListOfInfo.hasNext()) {

            Iterator<IInfo> it = itListOfInfo.next().iterator();
            while (it.hasNext()) {

                IInfo info = it.next();
                if (info != null && info.getDeclaringModuleName() != null) {
                    if (info.getDeclaringModuleName().equals(moduleName)) {
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
    public Collection<IInfo> getTokensStartingWith(String qualifier, int getWhat) {
        synchronized (lock) {
            return getWithFilter(qualifier, getWhat, startingWithFilter, true, null);
        }
    }

    public Collection<IInfo> getTokensStartingWith(String qualifier, int getWhat, Collection<IInfo> result) {
        synchronized (lock) {
            return getWithFilter(qualifier, getWhat, startingWithFilter, true, result);
        }
    }

    public Collection<IInfo> getTokensEqualTo(String qualifier, int getWhat) {
        synchronized (lock) {
            return getWithFilter(qualifier, getWhat, equalsFilter, false, null);
        }
    }

    public Collection<IInfo> getTokensEqualTo(String qualifier, int getWhat, Collection<IInfo> result) {
        synchronized (lock) {
            return getWithFilter(qualifier, getWhat, equalsFilter, false, result);
        }
    }

    protected Collection<IInfo> getWithFilter(String qualifier, int getWhat, Filter filter, boolean useLowerCaseQual,
            Collection<IInfo> result) {
        synchronized (lock) {
            if (result == null) {
                result = new ArrayList<IInfo>();
            }

            if ((getWhat & TOP_LEVEL) != 0) {
                getWithFilter(qualifier, topLevelInitialsToInfo, result, filter, useLowerCaseQual);
            }
            if ((getWhat & INNER) != 0) {
                getWithFilter(qualifier, innerInitialsToInfo, result, filter, useLowerCaseQual);
            }
            return result;
        }
    }

    /**
     * @param qualifier
     * @param initialsToInfo this is where we are going to get the info from (currently: inner or top level list)
     * @param toks (out) the tokens will be added to this list
     * @return
     */
    protected void getWithFilter(String qualifier, SortedMap<String, Set<IInfo>> initialsToInfo,
            Collection<IInfo> toks, Filter filter, boolean useLowerCaseQual) {
        String initials = getInitials(qualifier);
        String qualToCompare = qualifier;
        if (useLowerCaseQual) {
            qualToCompare = qualifier.toLowerCase();
        }

        //get until the end of the alphabet
        SortedMap<String, Set<IInfo>> subMap = initialsToInfo.subMap(initials, initials + "\uffff\uffff\uffff\uffff");

        for (Set<IInfo> listForInitials : subMap.values()) {

            for (IInfo info : listForInitials) {
                if (filter.doCompare(qualToCompare, info)) {
                    toks.add(info);
                }
            }
        }
    }

    /**
     * @return all the tokens that are in this info (top level or inner)
     */
    public Collection<IInfo> getAllTokens() {
        synchronized (lock) {
            Collection<Set<IInfo>> lInfo = this.topLevelInitialsToInfo.values();

            ArrayList<IInfo> toks = new ArrayList<IInfo>();
            for (Set<IInfo> list : lInfo) {
                for (IInfo info : list) {
                    toks.add(info);
                }
            }

            lInfo = this.innerInitialsToInfo.values();
            for (Set<IInfo> list : lInfo) {
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
        File persistingLocation;
        try {
            persistingLocation = getPersistingLocation();
        } catch (MisconfigurationException e) {
            Log.log("Error. Unable to get persisting location for additional interprer info. Configuration may be corrupted.",
                    e);
            return;
        }
        if (DEBUG_ADDITIONAL_INFO) {
            System.out.println("Saving to " + persistingLocation);
        }

        save(persistingLocation);

    }

    protected void save(File persistingLocation) {
        try {
            FileOutputStream stream = new FileOutputStream(persistingLocation);
            OutputStreamWriter writer = new OutputStreamWriter(stream);
            try {
                FastStringBuffer tempBuf = new FastStringBuffer();
                tempBuf.append("-- VERSION_");
                tempBuf.append(AbstractAdditionalTokensInfo.version);
                tempBuf.append('\n');
                writer.write(tempBuf.getInternalCharsArray(), 0, tempBuf.length());
                tempBuf.clear();

                saveTo(writer, tempBuf, persistingLocation);
            } finally {
                try {
                    writer.close();
                } finally {
                    stream.close();
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

    /**
     * @return the location where we can persist this info.
     * @throws MisconfigurationException
     */
    protected abstract File getPersistingLocation() throws MisconfigurationException;

    /**
     * @return the path to the folder we want to keep things on
     * @throws MisconfigurationException
     */
    protected abstract File getPersistingFolder();

    protected void saveTo(OutputStreamWriter writer, FastStringBuffer tempBuf, File pathToSave) throws IOException {
        synchronized (lock) {
            if (DEBUG_ADDITIONAL_INFO) {
                System.out.println("Saving info " + this.getClass().getName() + " to file (size = "
                        + getAllTokens().size() + ") " + pathToSave);
            }

            Map<String, Integer> dictionary = new HashMap<String, Integer>();
            tempBuf.append("-- START TREE 1\n");
            TreeIO.dumpTreeToBuffer(this.topLevelInitialsToInfo, tempBuf, dictionary);

            tempBuf.append("-- START TREE 2\n");
            TreeIO.dumpTreeToBuffer(this.innerInitialsToInfo, tempBuf, dictionary);

            FastStringBuffer buf2 = new FastStringBuffer(50 * (dictionary.size() + 4));
            TreeIO.dumpDictToBuffer(dictionary, buf2);

            //Write the dictionary before the actual trees.
            writer.write(buf2.getInternalCharsArray(), 0, buf2.length());
            buf2 = null;

            //Note: tried LZFFileInputStream from https://github.com/ning/compress
            //and Snappy from https://github.com/dain/snappy checking to see if by writing less we'd
            //get a better time but it got a bit slower (gzip was slowest, then snappy and the faster was LZFFileInputStream)
            writer.write(tempBuf.getInternalCharsArray(), 0, tempBuf.length());
        }
    }

    /**
     * Restores the saved info in the object (if overridden, getInfoToSave should be overridden too)
     * @param o the read object from the file
     * @throws MisconfigurationException
     */
    @SuppressWarnings("unchecked")
    protected void restoreSavedInfo(Object o) throws MisconfigurationException {
        synchronized (lock) {
            Tuple3<Object, Object, Object> readFromFile = (Tuple3<Object, Object, Object>) o;
            SortedMap<String, Set<IInfo>> o1 = (SortedMap<String, Set<IInfo>>) readFromFile.o1;
            SortedMap<String, Set<IInfo>> o2 = (SortedMap<String, Set<IInfo>>) readFromFile.o2;

            if (o1 == null) {
                throw new RuntimeException("Error in I/O (topLevelInitialsToInfo is null). Rebuilding internal info.");
            }
            if (o2 == null) {
                throw new RuntimeException("Error in I/O (innerInitialsToInfo is null). Rebuilding internal info.");
            }
            this.topLevelInitialsToInfo = o1;
            this.innerInitialsToInfo = o2;
            if (readFromFile.o3 != null) {
                //may be null in new format (where that's checked during load time).
                if (AbstractAdditionalTokensInfo.version != (Integer) readFromFile.o3) {
                    throw new RuntimeException("I/O version doesn't match. Rebuilding internal info.");
                }
            }
        }
    }

    @Override
    public String toString() {
        synchronized (lock) {
            FastStringBuffer buffer = new FastStringBuffer();
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
    private void entrySetToString(FastStringBuffer buffer, Set<Entry<String, Set<IInfo>>> name) {
        synchronized (lock) {
            for (Entry<String, Set<IInfo>> entry : name) {
                Set<IInfo> value = entry.getValue();
                for (IInfo info : value) {
                    buffer.append(info.toString());
                    buffer.append("\n");
                }
            }
        }
    }

    /**
     * @param token the token we want to search for (must be an exact match). Only tokens which are valid identifiers
     * may be searched (i.e.: no dots in it or anything alike).
     *
     * @return List<ModulesKey> a list with all the modules that contains the passed token.
     */
    public abstract List<ModulesKey> getModulesWithToken(IProject project, String token, IProgressMonitor monitor);

}

class IOUtils {

    public static Object readFromFile(File astOutputFile) {
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(astOutputFile));
            try {
                ObjectInputStream stream = new ObjectInputStream(in);
                try {
                    Object o = stream.readObject();
                    return o;
                } finally {
                    stream.close();
                }
            } finally {
                in.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}