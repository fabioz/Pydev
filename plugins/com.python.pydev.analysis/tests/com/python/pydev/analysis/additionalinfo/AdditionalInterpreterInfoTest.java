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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.shared_core.callbacks.ICallbackListener;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.ui.interpreters.PythonInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

import com.python.pydev.analysis.system_info_builder.InterpreterInfoBuilder;

public class AdditionalInterpreterInfoTest extends AdditionalInfoTestsBase {

    private AbstractAdditionalDependencyInfo info;
    private File baseDir;

    public static void main(String[] args) {
        try {
            AdditionalInterpreterInfoTest test = new AdditionalInterpreterInfoTest();
            test.setUp();
            test.testCompleteIndex();
            test.tearDown();

            junit.textui.TestRunner.run(AdditionalInterpreterInfoTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        info = new AbstractAdditionalDependencyInfo() {

            @Override
            protected File getPersistingLocation() {
                return null;
            }

            @Override
            protected File getPersistingFolder() {
                return null;
            }

            @Override
            protected Set<String> getPythonPathFolders() {
                return new HashSet<>(Arrays.asList(baseDir.getAbsolutePath()));
            }

            @Override
            protected String getUIRepresentation() {
                return "Stub for: " + baseDir;
            }

        };

        baseDir = FileUtils.getTempFileAt(new File("."), "data_temp_additional_info_test");
        if (baseDir.exists()) {
            FileUtils.deleteDirectoryTree(baseDir);
        }
        baseDir.mkdir();
    }

    @Override
    public void tearDown() throws Exception {
        if (baseDir.exists()) {
            FileUtils.deleteDirectoryTree(baseDir);
        }
        super.tearDown();
    }

    public void testMap() {
        FuncInfo info2 = new FuncInfo(((NameTok) createFuncDef("metz").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        info2 = new FuncInfo(((NameTok) createFuncDef("metZ").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        info2 = new FuncInfo(((NameTok) createFuncDef("met9").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        info2 = new FuncInfo(((NameTok) createFuncDef("met0").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        info2 = new FuncInfo(((NameTok) createFuncDef("meta").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        info2 = new FuncInfo(((NameTok) createFuncDef("metA").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        Collection<IInfo> tokensStartingWith = info
                .getTokensStartingWith("met", AbstractAdditionalTokensInfo.TOP_LEVEL);
        assertEquals(6, tokensStartingWith.size());

        Collection<IInfo> tokensEqualTo = info.getTokensEqualTo("metz", AbstractAdditionalTokensInfo.TOP_LEVEL);
        assertEquals(1, tokensEqualTo.size());
    }

    public void testMap2() {
        FuncInfo info2 = new FuncInfo(((NameTok) createFuncDef("m").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        info2 = new FuncInfo(((NameTok) createFuncDef("mm").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        info2 = new FuncInfo(((NameTok) createFuncDef("mmm").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        info2 = new FuncInfo(((NameTok) createFuncDef("mmmm").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        Collection<IInfo> tokensStartingWith = info.getTokensStartingWith("m", AbstractAdditionalTokensInfo.TOP_LEVEL);
        assertEquals(4, tokensStartingWith.size());

        tokensStartingWith = info.getTokensStartingWith("mm", AbstractAdditionalTokensInfo.TOP_LEVEL);
        assertEquals(3, tokensStartingWith.size());

        tokensStartingWith = info.getTokensStartingWith("mmm", AbstractAdditionalTokensInfo.TOP_LEVEL);
        assertEquals(2, tokensStartingWith.size());

        tokensStartingWith = info.getTokensStartingWith("mmmm", AbstractAdditionalTokensInfo.TOP_LEVEL);
        assertEquals(1, tokensStartingWith.size());
    }

    public void testAddFunc() {
        FuncInfo info2 = new FuncInfo(((NameTok) createFuncDef("met1").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        info2 = new FuncInfo(((NameTok) createFuncDef("met2").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        info2 = new FuncInfo(((NameTok) createFuncDef("func1").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        info2 = new FuncInfo(((NameTok) createFuncDef("func2").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        Collection<IInfo> tokensStartingWith = info.getTokensStartingWith("me", AbstractAdditionalTokensInfo.TOP_LEVEL);
        assertEquals(2, tokensStartingWith.size());
        assertIsIn("met1", tokensStartingWith);
        assertIsIn("met2", tokensStartingWith);

        tokensStartingWith = info.getTokensStartingWith("func", AbstractAdditionalTokensInfo.TOP_LEVEL);
        assertEquals(2, tokensStartingWith.size());
        assertIsIn("func1", tokensStartingWith);
        assertIsIn("func2", tokensStartingWith);
        for (IInfo info : tokensStartingWith) {
            assertEquals("mod1", info.getDeclaringModuleName());
        }
    }

    public void testAddClass() {
        ClassInfo info1 = new ClassInfo(((NameTok) createClassDef("cls1").name).id, "mod1", null);
        info.add(info1, AbstractAdditionalTokensInfo.TOP_LEVEL);
        ClassInfo info2 = new ClassInfo(((NameTok) createClassDef("cls2").name).id, "mod1", null);
        info.add(info2, AbstractAdditionalTokensInfo.TOP_LEVEL);
        ClassInfo info3 = new ClassInfo(((NameTok) createClassDef("class1").name).id, "mod2", null);
        info.add(info3, AbstractAdditionalTokensInfo.TOP_LEVEL);
        ClassInfo info4 = new ClassInfo(((NameTok) createClassDef("class2").name).id, "mod2", null);
        info.add(info4, AbstractAdditionalTokensInfo.TOP_LEVEL);
        Collection<IInfo> tokensStartingWith = info
                .getTokensStartingWith("cls", AbstractAdditionalTokensInfo.TOP_LEVEL);
        assertEquals(2, tokensStartingWith.size());
        assertIsIn("cls1", tokensStartingWith);
        assertIsIn("cls2", tokensStartingWith);

        info.removeInfoFromModule("mod2", false);
        tokensStartingWith = info.getTokensStartingWith("class", AbstractAdditionalTokensInfo.TOP_LEVEL);
        assertEquals(0, tokensStartingWith.size());
    }

    public void testAddInner() throws MisconfigurationException {
        String doc = "class Test:\n" +
                "    def m1(self):\n" +
                "        pass";
        SourceModule module = AbstractModule.createModuleFromDoc("test", null, new Document(doc),
                nature, true);
        info.addAstInfo(module.getAst(), module.getModulesKey(), false);

        Collection<IInfo> tokensStartingWith = info.getTokensStartingWith("Tes", AbstractAdditionalTokensInfo.TOP_LEVEL
                | AbstractAdditionalTokensInfo.INNER);
        assertEquals(1, tokensStartingWith.size());
        assertIsIn("Test", tokensStartingWith);

        tokensStartingWith = info.getTokensStartingWith("m1", AbstractAdditionalTokensInfo.TOP_LEVEL
                | AbstractAdditionalTokensInfo.INNER);
        assertEquals(1, tokensStartingWith.size());
        assertIsIn("m1", tokensStartingWith);
        IInfo i = tokensStartingWith.iterator().next();
        assertEquals("Test", i.getPath());

    }

    public void testAddAttrs() throws MisconfigurationException {
        String doc = "GLOBAL_ATTR = 1\n" +
                "GLOBAL2.IGNORE_THIS = 2\n" +
                "" +
                "class Test:\n" +
                "    test_attr = 1\n"
                +
                "    test_attr.ignore = 2\n" +
                "    test_attr2.ignore_this = 3\n" +
                "" +
                "    class Test2:\n"
                +
                "        def mmm(self):\n" +
                "            self.attr1 = 10";

        SourceModule module = AbstractModule.createModuleFromDoc("test", null, new Document(doc),
                nature, true);
        info.addAstInfo(module.getAst(), module.getModulesKey(), false);

        Collection<IInfo> tokensStartingWith = null;
        IInfo i = null;

        tokensStartingWith = info.getTokensStartingWith("global", AbstractAdditionalTokensInfo.TOP_LEVEL
                | AbstractAdditionalTokensInfo.INNER);
        //        assertEquals(2, tokensStartingWith.size());
        assertIsIn("GLOBAL_ATTR", tokensStartingWith);
        assertIsIn("GLOBAL2", tokensStartingWith);

        tokensStartingWith = info.getTokensStartingWith("", AbstractAdditionalTokensInfo.TOP_LEVEL
                | AbstractAdditionalTokensInfo.INNER);
        //        assertEquals(2, tokensStartingWith.size());
        i = assertIsIn("Test", tokensStartingWith);
        assertEquals(null, i.getPath());

        i = assertIsIn("Test2", tokensStartingWith);
        assertEquals("Test", i.getPath());

        i = assertIsIn("test_attr", tokensStartingWith);
        assertEquals("Test", i.getPath());

        i = assertIsIn("test_attr2", tokensStartingWith);
        assertEquals("Test", i.getPath());

        i = assertIsIn("attr1", tokensStartingWith);
        assertEquals("Test.Test2.mmm", i.getPath());

    }

    public void testAddInner2() throws MisconfigurationException {
        String doc = "class Test:\n" +
                "    class Test2:\n" +
                "        def mmm(self):\n" +
                "            pass";
        SourceModule module = AbstractModule.createModuleFromDoc("test", null, new Document(doc),
                nature, true);
        info.addAstInfo(module.getAst(), module.getModulesKey(), false);

        Collection<IInfo> tokensStartingWith = null;

        tokensStartingWith = info.getTokensStartingWith("m", AbstractAdditionalTokensInfo.TOP_LEVEL
                | AbstractAdditionalTokensInfo.INNER);
        assertEquals(1, tokensStartingWith.size());
        assertIsIn("mmm", tokensStartingWith);
        IInfo i = tokensStartingWith.iterator().next();
        assertEquals("Test.Test2", i.getPath());

        tokensStartingWith = info.getTokensStartingWith("Test", AbstractAdditionalTokensInfo.TOP_LEVEL
                | AbstractAdditionalTokensInfo.INNER);
        assertEquals(2, tokensStartingWith.size());
        i = assertIsIn("Test", tokensStartingWith);
        assertEquals(null, i.getPath());
        i = assertIsIn("Test2", tokensStartingWith);
        assertEquals("Test", i.getPath());

    }

    public void testCompleteIndex() throws Exception {
        String doc = "class Test:\n" +
                "    class Test2:\n" +
                "        def mmm(self):\n" +
                "            a = mmm1\n"
                +
                "            print mmm1";
        File tempFileAt = FileUtils.getTempFileAt(baseDir, "data_temporary_file_on_additional_interpreter_info_test",
                ".py");
        FileUtils.writeStrToFile(doc, tempFileAt);
        try {
            SourceModule module = AbstractModule.createModuleFromDoc("test", tempFileAt, new Document(
                    doc), nature, true);
            info.addAstInfo(module.getAst(), new ModulesKey("test", tempFileAt), false);

            List<ModulesKey> modulesWithTokensStartingWith = null;

            modulesWithTokensStartingWith = info.getModulesWithToken(null, "mmm", null);
            assertEquals(1, modulesWithTokensStartingWith.size());

            modulesWithTokensStartingWith = info.getModulesWithToken(null, "mmm1", null);
            assertEquals(1, modulesWithTokensStartingWith.size());

            modulesWithTokensStartingWith = info.getModulesWithToken(null, "mmm4", null);
            assertEquals(0, modulesWithTokensStartingWith.size());

            synchronized (this) {
                wait(1000);
            }

            doc = "new contents";
            FileUtils.writeStrToFile(doc, tempFileAt);

            info.removeInfoFromModule("test", true);
            info.addAstInfo(new ModulesKey("test", tempFileAt), true);
            modulesWithTokensStartingWith = info.getModulesWithToken(null, "mmm", null);
            assertEquals(0, modulesWithTokensStartingWith.size());

            modulesWithTokensStartingWith = info.getModulesWithToken(null, "contents", null);
            assertEquals(1, modulesWithTokensStartingWith.size());
        } finally {
            tempFileAt.delete();
        }
    }

    @SuppressWarnings("unchecked")
    public void testForcedBuiltinsInAdditionalInfo() throws Exception {
        IInterpreterManager interpreterManager = getInterpreterManager();
        String defaultInterpreter = interpreterManager.getDefaultInterpreterInfo(false).getExecutableOrJar();

        AbstractAdditionalDependencyInfo additionalSystemInfo = AdditionalSystemInterpreterInfo
                .getAdditionalSystemInfo(interpreterManager, defaultInterpreter);

        checkItertoolsToken(additionalSystemInfo, false);
        InterpreterInfo defaultInterpreterInfo = (InterpreterInfo) interpreterManager.getDefaultInterpreterInfo(false);
        HashSet<String> set = new HashSet<>(Arrays.asList(defaultInterpreterInfo.getBuiltins()));
        assertTrue(set.contains("itertools"));

        //Now, update the information to contain the builtin tokens!
        new InterpreterInfoBuilder().syncInfoToPythonPath(new NullProgressMonitor(), defaultInterpreterInfo);

        checkItertoolsToken(additionalSystemInfo, true);

        //Remove and re-update to check if it's fixed.
        additionalSystemInfo.removeInfoFromModule("itertools", false);
        checkItertoolsToken(additionalSystemInfo, false);

        new InterpreterInfoBuilder().syncInfoToPythonPath(new NullProgressMonitor(), defaultInterpreterInfo);
        checkItertoolsToken(additionalSystemInfo, true);

        int indexSize = additionalSystemInfo.completeIndex.keys().size();

        AdditionalSystemInterpreterInfo newAdditionalInfo = new AdditionalSystemInterpreterInfo(interpreterManager,
                defaultInterpreter);
        AdditionalSystemInterpreterInfo.setAdditionalSystemInfo((PythonInterpreterManager) interpreterManager,
                defaultInterpreter, newAdditionalInfo);

        newAdditionalInfo.load();
        assertEquals(indexSize, newAdditionalInfo.completeIndex.keys().size());

        final List<ModulesKey> added = new ArrayList<>();
        final List<ModulesKey> removed = new ArrayList<>();
        ICallbackListener listener = new ICallbackListener() {

            @Override
            public Object call(Object obj) {
                Tuple t = (Tuple) obj;
                added.addAll((List<ModulesKey>) t.o1);
                removed.addAll((List<ModulesKey>) t.o2);
                return null;
            }
        };
        AbstractAdditionalDependencyInfo.modulesAddedAndRemoved.registerListener(listener);
        try {
            new InterpreterInfoBuilder().syncInfoToPythonPath(new NullProgressMonitor(), defaultInterpreterInfo);
        } finally {
            AbstractAdditionalDependencyInfo.modulesAddedAndRemoved.unregisterListener(listener);
        }

        if (added.size() > 0) {
            throw new AssertionError(
                    "Expected no modules to be added as we just loaded from a clean save. Found: " + added);
        }
        if (removed.size() > 0) {
            throw new AssertionError(
                    "Expected no modules to be removed as we just loaded from a clean save. Found: " + removed);
        }

        checkItertoolsToken(newAdditionalInfo, true);

    }

    private void checkItertoolsToken(AbstractAdditionalDependencyInfo additionalSystemInfo, boolean expect) {
        Collection<IInfo> tokensStartingWith;
        tokensStartingWith = additionalSystemInfo.getTokensStartingWith("izip_longest",
                AbstractAdditionalTokensInfo.TOP_LEVEL);
        if (expect) {
            assertEquals(1, tokensStartingWith.size());

        } else {
            assertEquals(0, tokensStartingWith.size());

        }
    }

    private ClassDef createClassDef(String name) {
        return new ClassDef(new NameTok(name, NameTok.FunctionName), null, null, null, null, null, null);
    }

    private IInfo assertIsIn(String req, Collection<IInfo> tokensStartingWith) {
        for (IInfo info : tokensStartingWith) {
            if (info.getName().equals(req)) {
                return info;
            }
        }
        fail("The token requested (" + req + ") was not found.");
        return null;
    }

    private FunctionDef createFuncDef(String metName) {
        return new FunctionDef(new NameTok(metName, NameTok.FunctionName), null, null, null, null);
    }

}
