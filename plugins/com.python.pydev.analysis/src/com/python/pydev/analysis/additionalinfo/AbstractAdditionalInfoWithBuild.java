/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.DeltaSaver;
import org.python.pydev.core.IDeltaProcessor;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.model.ErrorDescription;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * @author fabioz
 *
 */
public abstract class AbstractAdditionalInfoWithBuild extends AbstractAdditionalDependencyInfo implements
        IDeltaProcessor<Object> {

    /**
     * @param callInit
     * @throws MisconfigurationException
     */
    public AbstractAdditionalInfoWithBuild(boolean callInit) throws MisconfigurationException {
        super(callInit);
    }

    @Override
    protected void init() throws MisconfigurationException {
        super.init();
        deltaSaver = createDeltaSaver();
    }

    /**
     * This is the maximum number of deltas that can be generated before saving everything in a big chunk and
     * clearing the deltas. 50 means that it's something as 25 modules (because usually a module change
     * is composed of a delete and an addition).
     */
    public static final int MAXIMUN_NUMBER_OF_DELTAS = 50;

    /**
     * If the delta size is big enough, save the current state and discard the deltas.
     */
    private void checkDeltaSize() {
        synchronized (lock) {
            if (deltaSaver.availableDeltas() > MAXIMUN_NUMBER_OF_DELTAS) {
                this.save();
            }
        }
    }

    /**
     * Used to save things in deltas
     */
    protected DeltaSaver<Object> deltaSaver;

    @Override
    public void removeInfoFromModule(String moduleName, boolean generateDelta) {
        synchronized (lock) {
            super.removeInfoFromModule(moduleName, generateDelta);
            if (generateDelta) {
                this.deltaSaver.addDeleteCommand(moduleName);
                checkDeltaSize();
            }
        }
    }

    @Override
    public List<IInfo> addAstInfo(SimpleNode node, ModulesKey key, boolean generateDelta) {
        List<IInfo> addAstInfo = super.addAstInfo(node, key, generateDelta);
        if (generateDelta && addAstInfo.size() > 0) {
            deltaSaver.addInsertCommand(new Tuple<ModulesKey, List<IInfo>>(key, addAstInfo));
            checkDeltaSize();
        }
        return addAstInfo;
    }

    @Override
    protected void restoreSavedInfo(Object o) throws MisconfigurationException {
        synchronized (lock) {
            super.restoreSavedInfo(o);
            //when we do a load, we have to process the deltas that may exist
            if (deltaSaver.availableDeltas() > 0) {
                deltaSaver.processDeltas(this);
            }
        }
    }

    protected DeltaSaver<Object> createDeltaSaver() {
        return new DeltaSaver<Object>(getPersistingFolder(), "v1_projectinfodelta", new ICallback<Object, String>() {

            public Object call(String arg) {
                if (arg.startsWith("STR")) {
                    return arg.substring(3);
                }
                if (arg.startsWith("TUP")) {
                    //Backward compatibility
                    String tup = arg.substring(3);
                    int i = tup.indexOf('\n');
                    int j = tup.indexOf('\n', i + 1);

                    String modName = new String(tup.substring(0, i));
                    File file = new File(tup.substring(i + 1, j));

                    return new Tuple<ModulesKey, List<IInfo>>(new ModulesKey(modName, file),
                            InfoStrFactory.strToInfo(tup
                                    .substring(j + 1)));
                }
                if (arg.startsWith("LST")) {
                    //Backward compatibility
                    return InfoStrFactory.strToInfo(arg.substring(3));
                }

                throw new AssertionError("Expecting string starting with STR or LST");
            }
        },

                new ICallback<String, Object>() {

                    /**
                     * Here we'll convert the object we added to a string.
                     *
                     * The objects we can add are:
                     * Tuple<String (module name), List<IInfo>) -- on addition
                     * String (module name) -- on deletion
                     */
                    public String call(Object arg) {
                        if (arg instanceof String) {
                            return "STR" + (String) arg;
                        }
                        if (arg instanceof Tuple) {
                            Tuple tuple = (Tuple) arg;
                            if (tuple.o1 instanceof ModulesKey && tuple.o2 instanceof List) {
                                ModulesKey modName = (ModulesKey) tuple.o1;
                                List<IInfo> l = (List<IInfo>) tuple.o2;
                                String infoToString = InfoStrFactory.infoToString(l);
                                String fileStr = modName.file != null ? modName.file.toString() : "no_source_available";

                                FastStringBuffer buf = new FastStringBuffer("TUP", modName.name.length()
                                        + fileStr.length()
                                        + infoToString.length() + 3);
                                buf.append(modName.name);
                                buf.append('\n');
                                buf.append(fileStr);
                                buf.append('\n');
                                buf.append(infoToString);
                                return buf.toString();
                            }
                        }
                        throw new AssertionError("Expecting Tuple<String, List<IInfo>> or String. Found: " + arg);
                    }
                });
    }

    public void processUpdate(Object data) {
        throw new RuntimeException("There is no update generation, only add.");
    }

    public void processDelete(Object data) {
        synchronized (lock) {
            //the moduleName is generated on delete
            this.removeInfoFromModule((String) data, false);
        }
    }

    public void processInsert(Object data) {
        synchronized (lock) {
            if (data instanceof Tuple) {
                this.addInfoToModuleOnRestoreInsertCommand((Tuple<ModulesKey, List<IInfo>>) data);
            }
        }
    }

    public void endProcessing() {
        //save it when the processing is finished
        synchronized (lock) {
            this.save();
        }
    }

    /**
     * Whenever it's properly saved, clear all the deltas.
     */
    @Override
    public void save() {
        synchronized (lock) {
            super.save();
            deltaSaver.clearAll();
        }
    }

    /**
     * Restores the info for a module manager
     *
     * @param monitor a monitor to keep track of the progress
     * @param m the module manager
     * @param nature the associated nature (may be null if there is no associated nature -- as is the case when
     * restoring system info).
     *
     * @return the info generated from the module manager
     */
    public static AbstractAdditionalTokensInfo restoreInfoForModuleManager(IProgressMonitor monitor, IModulesManager m,
            String additionalFeedback, AbstractAdditionalTokensInfo info, IPythonNature nature, int grammarVersion) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        //TODO: Check if keeping a zip file open makes things faster...
        //Timer timer = new Timer();
        ModulesKey[] allModules = m.getOnlyDirectModules();
        int i = 0;

        FastStringBuffer msgBuffer = new FastStringBuffer();

        for (ModulesKey key : allModules) {
            if (monitor.isCanceled()) {
                return null;
            }
            i++;

            if (PythonPathHelper.canAddAstInfoForSourceModule(key)) {
                //Note: at this point (on the interpreter configuration), we only add the tokens for source modules
                //but later on in InterpreterInfoBuilder, it'll actually go on and create the contents for compiled modules
                //(which is a slower process as it has to connect through a shell).

                if (i % 17 == 0) {
                    msgBuffer.clear();
                    msgBuffer.append("Creating ");
                    msgBuffer.append(additionalFeedback);
                    msgBuffer.append(" additional info (");
                    msgBuffer.append(i);
                    msgBuffer.append(" of ");
                    msgBuffer.append(allModules.length);
                    msgBuffer.append(") for ");
                    msgBuffer.append(key.file.getName());
                    monitor.setTaskName(msgBuffer.toString());
                    monitor.worked(1);
                }

                try {
                    if (info.addAstInfo(key, false) == null) {
                        String str = "Unable to generate ast -- using %s.\nError:%s";
                        ErrorDescription errorDesc = null;
                        throw new RuntimeException(StringUtils.format(str, PyParser
                                .getGrammarVersionStr(grammarVersion),
                                (errorDesc != null && errorDesc.message != null) ? errorDesc.message
                                        : "unable to determine"));
                    }

                } catch (Throwable e) {
                    Log.log(IStatus.ERROR, "Problem parsing the file :" + key.file + ".", e);
                }
            }
        }
        //timer.printDiff("Time to restore additional info");
        return info;
    }

}
