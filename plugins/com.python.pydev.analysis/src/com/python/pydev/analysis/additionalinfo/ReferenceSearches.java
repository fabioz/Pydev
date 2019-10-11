/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.ast.codecompletion.revisited.ModulesFoundStructure;
import org.python.pydev.ast.codecompletion.revisited.ModulesFoundStructure.ZipContents;
import org.python.pydev.ast.codecompletion.revisited.ModulesManager;
import org.python.pydev.ast.codecompletion.revisited.PyPublicTreeMap;
import org.python.pydev.ast.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.ModulesKeyForZip;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.out_of_memory.OnExpectedOutOfMemory;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.OrderedMap;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo.IBufferFiller;

/**
 * @deprecated
 */
@Deprecated
public class ReferenceSearches implements IReferenceSearches {

    private class Command {

        public final boolean finish;
        public final IBufferFiller bufferFiller;
        public final ModulesKey modulesKey;

        public Command(ModulesKey modulesKey, IBufferFiller bufferFiller) {
            this.modulesKey = modulesKey;
            this.bufferFiller = bufferFiller;
            this.finish = false;
        }

        public Command() {
            this.modulesKey = null;
            this.bufferFiller = null;
            this.finish = true;
        }

    }

    private static class Searcher implements Runnable {

        private final BlockingQueue<Command> queue;
        private final Collection<String> searchTokens;
        private final List<ModulesKey> ret;
        private final FastStringBuffer temp = new FastStringBuffer();
        private final Object retLock;

        public Searcher(BlockingQueue<Command> linkedBlockingQueue, Collection<String> token,
                List<ModulesKey> ret, Object retLock) {
            this.queue = linkedBlockingQueue;
            if (token.size() == 1) {
                final String searchfor = token.iterator().next();
                this.searchTokens = new AbstractCollection<String>() {
                    @Override
                    public boolean contains(Object o) {
                        return searchfor.equals(o); // implementation should be a bit faster than using a set (only for when we know there's a single entry)
                    }

                    @Override
                    public Iterator<String> iterator() {
                        throw new RuntimeException("not implemented");
                    }

                    @Override
                    public int size() {
                        throw new RuntimeException("not implemented");
                    }
                };
            } else {
                this.searchTokens = new HashSet<String>(token);
            }
            this.retLock = retLock;
            this.ret = ret;
        }

        @Override
        public void run() {
            FastStringBuffer buf = new FastStringBuffer();
            while (true) {
                Command cmd;
                try {
                    cmd = queue.take();
                    if (cmd.finish) {
                        break;
                    }
                    cmd.bufferFiller.fillBuffer(buf.clear());
                    this.search(cmd.modulesKey, buf);
                } catch (InterruptedException e) {
                    Log.log("Not expecting to be interrupted in searcher. Results may be wrong.", e);
                    break;
                }
            }
        }

        private void search(ModulesKey modulesKey, FastStringBuffer bufFileContents) {
            temp.clear();
            int length = bufFileContents.length();
            char[] internalCharsArray = bufFileContents.getInternalCharsArray();
            for (int i = 0; i < length; i++) {
                char c = internalCharsArray[i];
                if (Character.isJavaIdentifierStart(c)) {
                    temp.clear();
                    temp.append(c);
                    i++;
                    for (; i < length; i++) {
                        c = internalCharsArray[i];
                        if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                            break; //Fast forward through the most common case...
                        }
                        if (Character.isJavaIdentifierPart(c)) {
                            temp.append(c);
                        } else {
                            break;
                        }
                    }
                    String str = temp.toString();
                    if (PySelection.ALL_KEYWORD_TOKENS.contains(str)) {
                        continue;
                    }
                    if (searchTokens.contains(str)) {
                        if (AbstractAdditionalDependencyInfo.DEBUG) {
                            System.out.println("Found in: " + modulesKey);
                        }
                        synchronized (retLock) {
                            ret.add(modulesKey);
                        }
                        break;
                    }
                }
            }
        }
    }

    private WeakReference<AbstractAdditionalDependencyInfo> abstractAdditionalDependencyInfo;

    public ReferenceSearches(AbstractAdditionalDependencyInfo abstractAdditionalDependencyInfo) {
        this.abstractAdditionalDependencyInfo = new WeakReference<>(abstractAdditionalDependencyInfo);
    }

    private void fill(FastStringBuffer bufFileContents, InputStream stream) throws IOException {
        for (int i = 0; i < 5; i++) {
            try {
                bufFileContents.clear();
                FileUtils.fillBufferWithStream(stream, null, new NullProgressMonitor(), bufFileContents);
                return; //if it worked, return, otherwise go to the next iteration
            } catch (OutOfMemoryError e) {
                //We went too fast and have no more memory... (consumers are slow) retry again in a few moments...
                bufFileContents.clearMemory();
                Object o = new Object();
                synchronized (o) {
                    try {
                        o.wait(50);
                    } catch (InterruptedException e1) {

                    }
                }
                if (i == 3) { //Maybe we can't really load it because too much is cached?
                    OnExpectedOutOfMemory.clearCacheOnOutOfMemory.call(null);
                }
            }
        }

        //If we haven't returned, try a last iteration which will make any error public.
        bufFileContents.clear();
        FileUtils.fillBufferWithStream(stream, null, new NullProgressMonitor(), bufFileContents);
    }

    @Override
    public List<ModulesKey> search(IProject project, OrderedMap<String, Set<String>> fieldNameToValues,
            IProgressMonitor monitor) {
        final List<ModulesKey> ret = new ArrayList<ModulesKey>();
        AbstractAdditionalDependencyInfo abstractAdditionalDependencyInfo = this.abstractAdditionalDependencyInfo.get();
        if (abstractAdditionalDependencyInfo == null) {
            Log.log("AbstractAdditionalDependencyInfo already collected!");
            return ret;
        }
        final NullProgressMonitor nullMonitor = new NullProgressMonitor();

        Set<String> pythonPathFolders = abstractAdditionalDependencyInfo.getPythonPathFolders();
        LinkedBlockingQueue<Command> queue = new LinkedBlockingQueue<>();

        int searchers = Runtime.getRuntime().availableProcessors();
        //The 'ret' should be filled with the module keys where the tokens are found.
        final Object retLock = new Object();

        // Create 2 consumers
        Thread[] threads = new Thread[searchers];
        for (int i = 0; i < searchers; i++) {
            Searcher searcher = new Searcher(queue, fieldNameToValues.get(IReferenceSearches.FIELD_CONTENTS), ret,
                    retLock);
            //Spawn a thread to do the search while we load the contents.
            Thread t = new Thread(searcher);
            threads[i] = t;
            t.start();
        }

        try {
            PythonPathHelper pythonPathHelper = new PythonPathHelper();
            pythonPathHelper.setPythonPath(new ArrayList<String>(pythonPathFolders));
            ModulesFoundStructure modulesFound = pythonPathHelper.getModulesFoundStructure(project, nullMonitor);
            int totalSteps = modulesFound.regularModules.size() + modulesFound.zipContents.size();
            monitor.beginTask("Get modules with token in: " + abstractAdditionalDependencyInfo.getUIRepresentation(),
                    totalSteps);

            PyPublicTreeMap<ModulesKey, ModulesKey> keys = new PyPublicTreeMap<>();
            boolean includeOnlySourceModules = true; //no point in searching dlls.
            ModulesManager.buildKeysForRegularEntries(nullMonitor, modulesFound, keys, includeOnlySourceModules);

            //Get from regular files found
            for (ModulesKey entry : keys.values()) {
                if (monitor.isCanceled()) {
                    break;
                }
                if (AbstractAdditionalDependencyInfo.DEBUG) {
                    System.out.println("Loading: " + entry);
                }

                final File file = entry.file;
                try {
                    queue.put(new Command(entry, new IBufferFiller() {

                        @Override
                        public void fillBuffer(FastStringBuffer bufFileContents) {
                            try (FileInputStream stream = new FileInputStream(file)) {
                                fill(bufFileContents, stream);
                            } catch (Exception e) {
                                Log.log(e);
                            }
                        }
                    }));
                } catch (InterruptedException e) {
                    Log.log(e);
                }
            }

            //Get from zip files found
            List<ZipContents> allZipsZipContents = modulesFound.zipContents;
            for (ZipContents zipContents : allZipsZipContents) {
                keys.clear();
                if (monitor.isCanceled()) {
                    break;
                }

                ModulesManager.buildKeysForZipContents(keys, zipContents);
                try (ZipFile zipFile = new ZipFile(zipContents.zipFile)) {
                    for (ModulesKey entry : keys.values()) {
                        if (AbstractAdditionalDependencyInfo.DEBUG) {
                            System.out.println("Loading: " + entry);
                        }
                        if (monitor.isCanceled()) {
                            break;
                        }
                        final ModulesKeyForZip z = (ModulesKeyForZip) entry;
                        if (!z.isFile) {
                            continue;
                        }

                        queue.put(new Command(entry, new IBufferFiller() {

                            @Override
                            public void fillBuffer(FastStringBuffer bufFileContents) {
                                try (InputStream stream = zipFile.getInputStream(zipFile.getEntry(z.zipModulePath))) {
                                    fill(bufFileContents, stream);
                                } catch (Exception e) {
                                    Log.log(e);
                                }
                            }
                        }));
                    }

                } catch (Exception e) {
                    Log.log(e);
                }
            }

        } finally {
            for (int i = 0; i < searchers; i++) {
                queue.add(new Command()); // add it to wait for the thread to finish.
            }
        }
        int j = 0;
        while (true) {
            j++;
            boolean liveFound = false;
            for (Thread t : threads) {
                if (t.isAlive()) {
                    liveFound = true;
                    break;
                }
            }
            if (liveFound) {

                if (j % 50 == 0) {
                    monitor.setTaskName("Searching references...");
                    monitor.worked(1);
                }
                Thread.yield();
            } else {
                break;
            }
        }
        return ret;

    }

}
