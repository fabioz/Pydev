/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.indexview;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.utils.PyFileListing.PyFileInfo;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;

public class NatureGroup extends ElementWithChildren {

    public static class IntegrityInfo {
        public IPythonNature nature;

        public List<ModulesKey> modulesNotInDisk = new ArrayList<ModulesKey>();
        public List<ModulesKey> modulesNotInMemory = new ArrayList<ModulesKey>();

        public List<SourceModule> moduleNotInAdditionalInfo = new ArrayList<SourceModule>();
        public List<String> additionalModulesNotInDisk = new ArrayList<String>();
    }

    private PythonNature nature;

    public NatureGroup(ITreeElement parent, PythonNature nature) {
        super(parent);
        this.nature = nature;
    }

    public boolean hasChildren() {
        return true;
    }

    @Override
    protected void calculateChildren() throws MisconfigurationException {
        ICodeCompletionASTManager astManager = nature.getAstManager();
        if (astManager == null) {
            addLeaf("AST manager == null (should happen only in the plugin initialization) -- skipping other checks.");
            return;
        }

        IModulesManager projectModulesManager = astManager.getModulesManager();
        if (projectModulesManager == null) {
            addLeaf("Modules manager == null (should happen only in the plugin initialization) -- skipping other checks.");
            return;
        }

        PythonPathHelper pythonPathHelper = (PythonPathHelper) projectModulesManager.getPythonPathHelper();
        if (pythonPathHelper == null) {
            addLeaf("PythonPathHelper == null (should happen only in the plugin initialization) -- skipping other checks.");
            return;
        }
        List<String> pythonpath = pythonPathHelper.getPythonpath();
        for (String s : pythonpath) {
            addLeaf("PYTHONPATH: " + s);
        }

        HashSet<ModulesKey> expectedModuleNames = new HashSet<ModulesKey>();
        for (String string : pythonpath) {
            File file = new File(string);
            if (file.isDirectory()) { //TODO: Handle zip file modules!
                Collection<PyFileInfo> modulesBelow = PythonPathHelper.getModulesBelow(file, new NullProgressMonitor())
                        .getFoundPyFileInfos();
                for (PyFileInfo fileInfo : modulesBelow) {
                    File moduleFile = fileInfo.getFile();
                    String modName = pythonPathHelper.resolveModule(FileUtils.getFileAbsolutePath(moduleFile), true,
                            nature.getProject());
                    if (modName != null) {
                        expectedModuleNames.add(new ModulesKey(modName, moduleFile));
                    } else {
                        if (PythonPathHelper.isValidModuleLastPart(StringUtils.stripExtension((moduleFile.getName())))) {
                            addLeaf(StringUtils.format(
                                    "Unable to resolve module: %s (gotten null module name)",
                                    moduleFile));
                        }
                    }
                }
            } else {
                if (!file.exists()) {
                    addLeaf(StringUtils.format(
                            "File %s is referenced in the pythonpath but does not exist.", file));
                } else {
                    addLeaf(org.python.pydev.shared_core.string.StringUtils
                            .format("File %s not handled (TODO: Fix zip files support in the viewer).", file));
                }
            }
        }

        IntegrityInfo info = new IntegrityInfo();
        info.nature = nature;

        ModulesKey[] onlyDirectModules = projectModulesManager.getOnlyDirectModules();
        TreeSet<ModulesKey> inModulesManager = new TreeSet<ModulesKey>(Arrays.asList(onlyDirectModules));

        Set<String> allAdditionalInfoModuleNames = new TreeSet<String>();
        List<Tuple<AbstractAdditionalTokensInfo, IPythonNature>> additionalInfoAndNature = AdditionalProjectInterpreterInfo
                .getAdditionalInfoAndNature(nature, false, false, false);
        AbstractAdditionalTokensInfo additionalProjectInfo;
        if (additionalInfoAndNature.size() == 0) {
            addChild(new LeafElement(this, "No additional infos found (1 expected) -- skipping other checks."));
            return;

        } else {
            if (additionalInfoAndNature.size() > 1) {
                addChild(new LeafElement(this, StringUtils.format(
                        "%s additional infos found (only 1 expected) -- continuing checks but analysis may be wrong.",
                        additionalInfoAndNature.size())));
            }
            additionalProjectInfo = additionalInfoAndNature.get(0).o1;
            allAdditionalInfoModuleNames.addAll(additionalProjectInfo.getAllModulesWithTokens());
        }

        for (ModulesKey key : inModulesManager) {
            if (!expectedModuleNames.contains(key)) {
                info.modulesNotInDisk.add(key);
                addChild(new LeafElement(this, StringUtils.format(
                        "%s exists in memory but not in the disk.", key)));
            }
        }

        ModulesKey tempKey = new ModulesKey(null, null);
        for (String s : allAdditionalInfoModuleNames) {
            tempKey.name = s;
            if (!expectedModuleNames.contains(tempKey)) {
                info.additionalModulesNotInDisk.add(s);
                addChild(new LeafElement(this, StringUtils.format(
                        "%s exists in the additional info but not in the disk.", s)));
            }
        }

        for (ModulesKey key : expectedModuleNames) {
            boolean isInModulesManager = inModulesManager.contains(key);
            if (!isInModulesManager) {
                info.modulesNotInMemory.add(key);
                addChild(new LeafElement(this, StringUtils.format(
                        "%s exists in the disk but not in memory.", key)));
            }
            if (!allAdditionalInfoModuleNames.contains(key.name)) {
                try {
                    AbstractModule mod = AbstractModule.createModule(key.name, key.file, info.nature, true);
                    if (!(mod instanceof SourceModule)) {
                        continue;
                    }
                    SourceModule module = (SourceModule) mod;
                    if (module == null || module.getAst() == null) {
                        addChild(new LeafElement(this, StringUtils.format(
                                "Warning: cannot parse: %s - %s (so, it's ok not having additional info on it)",
                                key.name, key.file)));
                    } else {
                        try {
                            Iterator<ASTEntry> innerEntriesForAST = AbstractAdditionalDependencyInfo
                                    .getInnerEntriesForAST(module.getAst()).o2;
                            if (innerEntriesForAST.hasNext()) {
                                info.moduleNotInAdditionalInfo.add(module);
                                addChild(new LeafElement(this, StringUtils.format(
                                        "The additional info index of the module: %s is not updated.", key.name)));
                            }
                        } catch (Exception e) {
                            addChild(new LeafElement(this, StringUtils.format(
                                    "Unexpected error happened on: %s - %s: %s", key.name, key.file, e.getMessage())));
                        }
                    }
                } catch (IOException e) {
                    //OK, it cannot be parsed, so, we cannot generate its info
                    addChild(new LeafElement(this, StringUtils.format(
                            "Warning: cannot parse: %s - %s (so, it's ok not having additional info on it)", key.name,
                            key.file)));
                }
            }
        }

        //modules manager
        if (info.modulesNotInDisk.size() > 0) {
            for (ModulesKey m : info.modulesNotInDisk) {
                addChild(new LeafElement(this, StringUtils.format(
                        "FIX: Removing from modules manager: %s", m)));
            }
            projectModulesManager.removeModules(info.modulesNotInDisk);
        }

        for (ModulesKey key : info.modulesNotInMemory) {
            addChild(new LeafElement(this, "FIX: Adding to modules manager: " + key));
            projectModulesManager.addModule(key);
        }

        //additional info
        for (String s : info.additionalModulesNotInDisk) {
            addChild(new LeafElement(this, StringUtils.format(
                    "FIX: Removing from additional info: %s", s)));
            additionalProjectInfo.removeInfoFromModule(s, true);
        }

        for (SourceModule mod : info.moduleNotInAdditionalInfo) {
            addChild(new LeafElement(this, StringUtils.format(
                    "FIX: Adding to additional info: %s", mod.getName())));
            additionalProjectInfo.addAstInfo(mod.getAst(), mod.getModulesKey(), true);
        }

    }

    private void addLeaf(String msg) {
        addChild(new LeafElement(this, msg));
    }

    @Override
    public String toString() {
        IProject project = nature.getProject();
        if (project != null) {
            return project.getName();
        }
        return "Project not set";
    }

}
