/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.TreeNode;
import org.python.pydev.shared_ui.SharedUiPlugin;

/**
 * @author fabioz
 *
 */
public class InterpreterInfoTreeNodeRoot<X> extends InterpreterInfoTreeNode<X> {

    public final IInterpreterInfo interpreterInfo;

    private final PythonpathTreeNode belowRootFiles; //Could be null!

    private final InterpreterInfoTreeNode<LabelAndImage> systemLibs;

    private final InterpreterInfoTreeNode<LabelAndImage> externalLibs;

    private final InterpreterInfoTreeNode<LabelAndImage> predefinedCompletions;

    private final InterpreterInfoTreeNode<LabelAndImage> forcedBuiltins;

    public InterpreterInfoTreeNodeRoot(IInterpreterInfo interpreterInfo, IPythonNature nature, Object parent, X data) {
        super(parent, data);
        this.interpreterInfo = interpreterInfo;
        Assert.isNotNull(interpreterInfo);
        InterpreterInfoTreeNodeRoot root = this;
        IImageCache imageCache = SharedUiPlugin.getImageCache();

        String executableOrJar = interpreterInfo.getExecutableOrJar();
        File file = new File(executableOrJar);
        if (FileUtils.enhancedIsFile(file)) {
            belowRootFiles = new PythonpathTreeNode(root, file.getParentFile(),
                    imageCache.get(UIConstants.PY_INTERPRETER_ICON), true);
        } else {
            belowRootFiles = null;
        }

        systemLibs = new InterpreterInfoTreeNode<LabelAndImage>(root, new LabelAndImage("System Libs",
                imageCache.get(UIConstants.LIB_SYSTEM_ROOT)));

        List<String> pythonPath = interpreterInfo.getPythonPath();
        Collections.sort(pythonPath);
        for (String string : pythonPath) {
            new PythonpathTreeNode(systemLibs, new File(string), imageCache.get(UIConstants.LIB_SYSTEM), true);
        }

        externalLibs = new InterpreterInfoTreeNode<LabelAndImage>(root, new LabelAndImage("External Libs",
                imageCache.get(UIConstants.LIB_SYSTEM_ROOT)));

        IPythonPathNature pythonPathNature = nature.getPythonPathNature();
        try {
            List<String> projectExternalSourcePath = pythonPathNature.getProjectExternalSourcePathAsList(true);
            Collections.sort(projectExternalSourcePath);
            for (String string : projectExternalSourcePath) {
                File f = new File(string);
                new PythonpathTreeNode(externalLibs, f, imageCache.get(UIConstants.LIB_SYSTEM), true);
            }
        } catch (CoreException e) {
            Log.log(e);
        }

        predefinedCompletions = new InterpreterInfoTreeNode<LabelAndImage>(root, new LabelAndImage(
                "Predefined Completions", imageCache.get(UIConstants.LIB_SYSTEM_ROOT)));

        for (String string : interpreterInfo.getPredefinedCompletionsPath()) {
            new PythonpathTreeNode(predefinedCompletions, new File(string), imageCache.get(UIConstants.LIB_SYSTEM),
                    true);
        }

        forcedBuiltins = new InterpreterInfoTreeNode<LabelAndImage>(root, new LabelAndImage("Forced builtins",
                imageCache.get(UIConstants.LIB_SYSTEM_ROOT)));

        for (Iterator<String> it = interpreterInfo.forcedLibsIterator(); it.hasNext();) {
            String string = it.next();
            new InterpreterInfoTreeNode<LabelAndImage>(forcedBuiltins, new LabelAndImage(string,
                    imageCache.get(UIConstants.LIB_FORCED_BUILTIN)));
        }
    }

    /**
     * @return the nodes for a file search. Note that some nodes may not even be added (as they don't contain files).
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<TreeNode> getNodesOrderedForFileSearch() {
        ArrayList<TreeNode> ret = new ArrayList();
        ret.add(externalLibs);
        ret.add(systemLibs);
        if (belowRootFiles != null) {
            ret.add(belowRootFiles);
        }
        ret.add(predefinedCompletions);

        return ret;
    }

}
