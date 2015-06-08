package com.python.pydev.analysis.system_info_builder;

/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PydevTestUtils;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.ui.interpreters.PythonInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * @author fabioz
 *
 */
public class InterpreterInfoBuilderTest extends TestCase {

    private File baseDir;
    private File libDir;

    @Override
    protected void setUp() throws Exception {
        baseDir = new File(FileUtils.getFileAbsolutePath(new File("InterpreterInfoBuilderTest.temporary_dir")));
        try {
            FileUtils.deleteDirectoryTree(baseDir);
        } catch (Exception e) {
            //ignore
        }

        libDir = new File(baseDir, "Lib");
        libDir.mkdirs();

        FileUtils.writeStrToFile("class Module1:pass", new File(libDir, "module1.py"));
        FileUtils.writeStrToFile("class Module2:pass", new File(libDir, "module2.py"));
        FileUtils.writeStrToFile("class Module3:pass", new File(libDir, "module3.py"));

        PydevTestUtils.setTestPlatformStateLocation();
        ExtensionHelper.testingParticipants = new HashMap<String, List<Object>>();
        FileUtils.IN_TESTS = true;
        ProjectModulesManager.IN_TESTS = true;
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtils.deleteDirectoryTree(baseDir);
        ProjectModulesManager.IN_TESTS = false;
        FileUtils.IN_TESTS = false;
        ExtensionHelper.testingParticipants = null;
    }

    public void testInterpreterInfoBuilder() throws Exception {
        Collection<String> pythonpath = new ArrayList<String>();
        pythonpath.add(libDir.toString());

        final InterpreterInfo info = new InterpreterInfo("2.6", TestDependent.PYTHON_EXE, pythonpath);

        IPreferenceStore preferences = new PreferenceStore();
        final PythonInterpreterManager manager = new PythonInterpreterManager(preferences);
        PydevPlugin.setPythonInterpreterManager(manager);
        manager.setInfos(new IInterpreterInfo[] { info }, null, null);

        final AdditionalSystemInterpreterInfo additionalInfo = new AdditionalSystemInterpreterInfo(manager,
                info.getExecutableOrJar());
        AdditionalSystemInterpreterInfo.setAdditionalSystemInfo(manager, info.getExecutableOrJar(), additionalInfo);

        //Don't load it (otherwise it'll get the 'proper' info).
        //AdditionalSystemInterpreterInfo.loadAdditionalSystemInfo(manager, info.getExecutableOrJar());

        final ISystemModulesManager modulesManager = info.getModulesManager();
        assertEquals(0, modulesManager.getSize(false));
        assertEquals(0, additionalInfo.getAllTokens().size());

        InterpreterInfoBuilder builder = new InterpreterInfoBuilder();
        builder.syncInfoToPythonPath(null, info);

        int size = modulesManager.getSize(false);
        if (size != 3) {
            fail("Expected size = 3, found: " + size);
        }

        try {
            AbstractAdditionalDependencyInfo additionalSystemInfo = AdditionalSystemInterpreterInfo
                    .getAdditionalSystemInfo(manager, manager.getInterpreterInfos()[0].getExecutableOrJar(),
                            true);
            if (additionalInfo != additionalSystemInfo) {
                throw new RuntimeException("Expecting it to be the same instance.");
            }
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }

        Collection<IInfo> allTokens = additionalInfo.getAllTokens();
        size = allTokens.size();
        if (size != 3) {
            FastStringBuffer buf = new FastStringBuffer();
            for (IInfo i : allTokens) {
                buf.append(i.toString());
            }
            fail("Expected size = 3, found: " + size + "\nTokens: " + buf);
        }
    }

}
