/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.DeltaSaver;
import org.python.pydev.core.IDeltaProcessor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.ModulesKeyForJava;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

public abstract class ModulesManagerWithBuild extends ModulesManager implements IDeltaProcessor<ModulesKey> {

    /**
     * Determines whether we are testing it.
     */
    public static boolean IN_TESTS = false;

    /**
     * Used to process deltas (in case we have the process killed for some reason)
     * 
     * Note that it may become null during normal processing when not generating deltas.
     */
    protected volatile DeltaSaver<ModulesKey> deltaSaver;

    protected static ICallback<ModulesKey, String> readFromFileMethod = new ICallback<ModulesKey, String>() {

        public ModulesKey call(String arg) {
            List<String> split = StringUtils.split(arg, '|');
            if (split.size() == 1) {
                return new ModulesKey(split.get(0), null);
            }
            if (split.size() == 2) {
                return new ModulesKey(split.get(0), new File(split.get(1)));
            }

            return null;
        }
    };

    protected static ICallback<String, ModulesKey> toFileMethod = new ICallback<String, ModulesKey>() {

        public String call(ModulesKey arg) {
            FastStringBuffer buf = new FastStringBuffer();
            buf.append(arg.name);
            if (arg.file != null) {
                buf.append("|");
                buf.append(arg.file.toString());
            }
            return buf.toString();
        }
    };

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#processUpdate(org.python.pydev.core.ModulesKey)
     */
    public void processUpdate(ModulesKey data) {
        //updates are ignored because we always start with 'empty modules' (so, we don't actually generate them -- updates are treated as inserts).
        throw new RuntimeException("Not impl");
    }

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#processDelete(org.python.pydev.core.ModulesKey)
     */
    public void processDelete(ModulesKey key) {
        doRemoveSingleModule(key);
    }

    /** 
     * @see org.python.pydev.core.IProjectModulesManager#processInsert(org.python.pydev.core.ModulesKey)
     */
    public void processInsert(ModulesKey key) {
        addModule(key);
    }

    private final Object lockNoDeltas = new Object();
    private int noDeltas = 0;

    /**
     * This method can be used to signal that some processing may be done under which no deltas should be generated.
     * 
     * The returned AutoCloseable must be closed afterwards (use in try block).
     */
    @Override
    public AutoCloseable withNoGenerateDeltas() {
        synchronized (lockNoDeltas) {
            noDeltas++;
            final DeltaSaver<ModulesKey> tempDeltaSaver;
            if (noDeltas == 1) {
                tempDeltaSaver = deltaSaver;
                if (tempDeltaSaver != null) {
                    deltaSaver = null;
                }
            } else {
                tempDeltaSaver = null;
            }
            return new AutoCloseable() {

                @Override
                public void close() throws Exception {
                    synchronized (lockNoDeltas) {
                        noDeltas--;
                        if (noDeltas == 0 && tempDeltaSaver != null && deltaSaver == null) {
                            DeltaSaver<ModulesKey> d = deltaSaver = tempDeltaSaver;
                            endProcessing();
                            d.clearAll();
                        }
                    }
                }
            };
        }
    }

    @Override
    public void doRemoveSingleModule(ModulesKey key) {
        super.doRemoveSingleModule(key);
        DeltaSaver<ModulesKey> d = deltaSaver;
        if (d != null && !IN_TESTS) { //we don't want deltas in tests
            //overridden to add delta
            d.addDeleteCommand(key);
            checkDeltaSize();
        }
    }

    @Override
    public void doAddSingleModule(ModulesKey key, AbstractModule n) {
        super.doAddSingleModule(key, n);
        DeltaSaver<ModulesKey> d = deltaSaver;
        if ((d != null && !IN_TESTS) && !(key instanceof ModulesKeyForZip)
                && !(key instanceof ModulesKeyForJava)) {
            //we don't want deltas in tests nor in zips/java modules
            //overridden to add delta
            d.addInsertCommand(key);
            checkDeltaSize();
        }
    }

    /**
     * If the delta size is big enough, save the current state and discard the deltas.
     */
    private void checkDeltaSize() {
        DeltaSaver<ModulesKey> d = deltaSaver;
        if (d != null && d.availableDeltas() > MAXIMUN_NUMBER_OF_DELTAS) {
            endProcessing();
            d.clearAll();
        }
    }

    //end delta processing

    /**
     * @see org.python.pydev.core.ICodeCompletionASTManager#removeModule(java.io.File, org.eclipse.core.resources.IProject,
     *      org.eclipse.core.runtime.IProgressMonitor)
     */
    public void removeModule(File file, IProject project, IProgressMonitor monitor) {
        if (file == null) {
            return;
        }

        if (file.isDirectory()) {
            removeModulesBelow(file, project, monitor);

        } else {
            if (file.getName().startsWith("__init__.")) {
                removeModulesBelow(file.getParentFile(), project, monitor);
            } else {
                removeModulesWithFile(file);
            }
        }
    }

    /**
     * @param file
     */
    private void removeModulesWithFile(File file) {
        if (file == null) {
            return;
        }

        List<ModulesKey> toRem = new ArrayList<ModulesKey>();
        synchronized (modulesKeysLock) {

            for (Iterator<ModulesKey> iter = modulesKeys.keySet().iterator(); iter.hasNext();) {
                ModulesKey key = iter.next();
                if (key.file != null && key.file.equals(file)) {
                    toRem.add(key);
                }
            }

            removeThem(toRem);
        }
    }

    /**
     * removes all the modules that have the module starting with the name of the module from
     * the specified file.
     */
    private void removeModulesBelow(File file, IProject project, IProgressMonitor monitor) {
        if (file == null) {
            return;
        }

        String absolutePath = FileUtils.getFileAbsolutePath(file);
        List<ModulesKey> toRem = new ArrayList<ModulesKey>();

        synchronized (modulesKeysLock) {

            for (ModulesKey key : modulesKeys.keySet()) {
                if (key.file != null && FileUtils.getFileAbsolutePath(key.file).startsWith(absolutePath)) {
                    toRem.add(key);
                }
            }

            removeThem(toRem);
        }
    }

    // ------------------------ building

    public void rebuildModule(File f, ICallback0<IDocument> doc, final IProject project, IProgressMonitor monitor,
            IPythonNature nature) {
        final String m = pythonPathHelper.resolveModule(FileUtils.getFileAbsolutePath(f), false, project);
        if (m != null) {
            addModule(new ModulesKey(m, f));

        } else if (f != null) { //ok, remove the module that has a key with this file, as it can no longer be resolved
            synchronized (modulesKeysLock) {
                Set<ModulesKey> toRemove = new HashSet<ModulesKey>();
                for (Iterator<ModulesKey> iter = modulesKeys.keySet().iterator(); iter.hasNext();) {
                    ModulesKey key = iter.next();
                    if (key.file != null && key.file.equals(f)) {
                        toRemove.add(key);
                    }
                }
                removeThem(toRemove);
            }
        }
    }

}
