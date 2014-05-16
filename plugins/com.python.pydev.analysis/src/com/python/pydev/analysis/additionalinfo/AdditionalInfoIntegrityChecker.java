/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;
import org.python.pydev.utils.PyFileListing.PyFileInfo;

import com.python.pydev.util.UIUtils;

/**
 * Checks the integrity of the internal pydev caches.
 * 
 * @author Fabio
 */
public class AdditionalInfoIntegrityChecker implements IPyEditListener {

    public static class IntegrityInfo {
        public boolean allOk = true;
        public StringBuffer desc = new StringBuffer();

        public IPythonNature nature;

        public IModulesManager modulesManager;
        public List<ModulesKey> modulesNotInDisk = new ArrayList<ModulesKey>();
        public List<ModulesKey> modulesNotInMemory = new ArrayList<ModulesKey>();

        public AdditionalProjectInterpreterInfo additionalProjectInfo;
        public List<SourceModule> moduleNotInAdditionalInfo = new ArrayList<SourceModule>();
        public List<String> additionalModulesNotInDisk = new ArrayList<String>();

        @Override
        public String toString() {
            return desc.toString();
        }
    }

    public static IntegrityInfo checkIntegrity(IPythonNature nature, IProgressMonitor monitor, boolean fix)
            throws MisconfigurationException {
        IntegrityInfo info = new IntegrityInfo();
        StringBuffer buffer = info.desc;

        info.nature = nature;
        info.modulesManager = nature.getAstManager().getModulesManager();
        info.additionalProjectInfo = (AdditionalProjectInterpreterInfo) AdditionalProjectInterpreterInfo
                .getAdditionalInfoForProject(nature);
        if (info.additionalProjectInfo == null) {
            buffer.append(StringUtils.format(
                    "Unable to get additional project info for: %s (gotten null)",
                    nature.getProject()));
            info.allOk = false;
        }

        PythonPathHelper pythonPathHelper = (PythonPathHelper) info.modulesManager.getPythonPathHelper();
        List<String> pythonpath = pythonPathHelper.getPythonpath();
        buffer.append(org.python.pydev.shared_core.string.StringUtils
                .format("Checking the integrity of the project: %s\n\n", nature.getProject().getName()));
        buffer.append("Pythonpath:\n");
        for (String string : pythonpath) {
            buffer.append(string);
            buffer.append("\n");
        }
        buffer.append("\n");

        HashSet<ModulesKey> expectedModuleNames = new HashSet<ModulesKey>();
        for (String string : pythonpath) {
            File file = new File(string);
            if (file.exists() && file.isDirectory()) { //TODO: Handle zip file modules!
                Collection<PyFileInfo> modulesBelow = PythonPathHelper.getModulesBelow(file, monitor)
                        .getFoundPyFileInfos();
                for (PyFileInfo fileInfo : modulesBelow) {
                    File moduleFile = fileInfo.getFile();
                    String modName = pythonPathHelper.resolveModule(FileUtils.getFileAbsolutePath(moduleFile), true,
                            nature.getProject());
                    if (modName != null) {
                        expectedModuleNames.add(new ModulesKey(modName, moduleFile));
                        buffer.append(StringUtils.format("Found module: %s - %s\n",
                                modName, moduleFile));
                    } else {
                        if (PythonPathHelper.isValidModuleLastPart(StringUtils.stripExtension((moduleFile.getName())))) {
                            info.allOk = false;
                            buffer.append(StringUtils.format(
                                    "Unable to resolve module: %s (gotten null module name)\n", moduleFile));
                        }
                    }
                }
            } else {
                info.allOk = false;
                buffer.append(StringUtils.format(
                        "File %s is referenced in the pythonpath but does not exist.", file));
            }
        }

        check(expectedModuleNames, info, fix);
        return info;
    }

    /**
     * @param expectedModuleNames the modules that exist in the disk (an actual file is found and checked for the module it resolves to)
     * @throws MisconfigurationException 
     */
    private static void check(HashSet<ModulesKey> expectedModuleNames, IntegrityInfo info, boolean fix)
            throws MisconfigurationException {
        StringBuffer buffer = info.desc;
        ModulesKey[] onlyDirectModules = info.modulesManager.getOnlyDirectModules();
        TreeSet<ModulesKey> inModulesManager = new TreeSet<ModulesKey>(Arrays.asList(onlyDirectModules));
        Set<String> allAdditionalInfoTrackedModules = info.additionalProjectInfo.getAllModulesWithTokens();

        for (ModulesKey key : inModulesManager) {
            if (!expectedModuleNames.contains(key)) {
                info.allOk = false;
                info.modulesNotInDisk.add(key);
                buffer.append(StringUtils.format(
                        "ModulesKey %s exists in memory but not in the disk.\n", key));
            }
        }

        for (String s : allAdditionalInfoTrackedModules) {
            if (!expectedModuleNames.contains(new ModulesKey(s, null))) {
                info.allOk = false;
                info.additionalModulesNotInDisk.add(s);
                buffer.append(StringUtils.format(
                        "The module %s exists in the additional info memory but not in the disk.\n", s));
            }
        }

        for (ModulesKey key : expectedModuleNames) {
            if (!inModulesManager.contains(key)) {
                info.allOk = false;
                info.modulesNotInMemory.add(key);
                buffer.append(StringUtils.format(
                        "ModulesKey %s exists in the disk but not in memory.\n", key));
            }
            if (!allAdditionalInfoTrackedModules.contains(key.name)) {
                try {
                    AbstractModule mod = AbstractModule.createModule(key.name, key.file, info.nature, true);
                    if (!(mod instanceof SourceModule)) {
                        continue;
                    }
                    SourceModule module = (SourceModule) mod;
                    if (module == null || module.getAst() == null) {
                        buffer.append(StringUtils.format(
                                "Warning: cannot parse: %s - %s (so, it's ok not having additional info on it)\n",
                                key.name, key.file));
                    } else {
                        try {
                            Iterator<ASTEntry> innerEntriesForAST = AbstractAdditionalDependencyInfo
                                    .getInnerEntriesForAST(module.getAst()).o2;
                            if (innerEntriesForAST.hasNext()) {
                                info.allOk = false;
                                info.moduleNotInAdditionalInfo.add(module);
                                buffer.append(StringUtils.format(
                                        "The additional info index of the module: %s is not updated.\n", key.name));
                            }
                        } catch (Exception e) {
                            buffer.append(StringUtils.format(
                                    "Unexpected error happened on: %s - %s: %s\n", key.name,
                                    key.file, e.getMessage()));
                        }
                    }
                } catch (IOException e) {
                    //OK, it cannot be parsed, so, we cannot generate its info
                    buffer.append(StringUtils.format(
                            "Warning: cannot parse: %s - %s (so, it's ok not having additional info on it)\n",
                            key.name, key.file));
                }
            }
        }

        if (info.allOk) {
            buffer.append("All checks OK!\n");
        } else {
            if (fix) {
                buffer.append("Fixing:\n");
                //modules manager
                buffer.append(StringUtils.format(
                        "Removing modules from memory: %s\n", info.modulesNotInDisk));
                info.modulesManager.removeModules(info.modulesNotInDisk);

                buffer.append(StringUtils.format("Adding to memory modules: %s\n",
                        info.modulesNotInMemory));
                for (ModulesKey key : info.modulesNotInMemory) {
                    buffer.append("Adding modules ...\n");
                    info.modulesManager.addModule(key);
                }

                //additional info
                buffer.append(org.python.pydev.shared_core.string.StringUtils
                        .format("Removing from additional info: %s\n", info.additionalModulesNotInDisk));
                for (String s : info.additionalModulesNotInDisk) {
                    info.additionalProjectInfo.removeInfoFromModule(s, true);
                }

                buffer.append(StringUtils.format(
                        "Adding to additional info modules found in disk: %s\n",
                        info.moduleNotInAdditionalInfo));
                for (SourceModule mod : info.moduleNotInAdditionalInfo) {
                    info.additionalProjectInfo.addAstInfo(mod.getAst(), mod.getModulesKey(), true);
                }
            }
        }
    }

    public void onCreateActions(ListResourceBundle resources, final BaseEditor baseEditor, IProgressMonitor monitor) {
        PyEdit edit = (PyEdit) baseEditor;
        edit.addOfflineActionListener("--internal-test-modules", new Action() {
            @Override
            public void run() {
                List<IPythonNature> allPythonNatures = PythonNature.getAllPythonNatures();
                StringBuffer buf = new StringBuffer();
                try {
                    for (IPythonNature nature : allPythonNatures) {
                        buf.append(checkIntegrity(nature, new NullProgressMonitor(), true));
                    }
                } catch (MisconfigurationException e) {
                    buf.append(e.getMessage());
                }
                UIUtils.showString(buf.toString());
            }
        }, "Used just for testing (do not use).", true);
    }

    public void onDispose(BaseEditor edit, IProgressMonitor monitor) {

    }

    public void onSave(BaseEditor edit, IProgressMonitor monitor) {

    }

    public void onSetDocument(IDocument document, BaseEditor edit, IProgressMonitor monitor) {

    }

}
