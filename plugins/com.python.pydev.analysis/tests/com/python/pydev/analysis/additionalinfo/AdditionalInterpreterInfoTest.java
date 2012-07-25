/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;

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

        };

        baseDir = REF.getTempFileAt(new File("."), "data_temp_additional_info_test");
        if (baseDir.exists()) {
            REF.deleteDirectoryTree(baseDir);
        }
        baseDir.mkdir();
    }

    @Override
    public void tearDown() throws Exception {
        if (baseDir.exists()) {
            REF.deleteDirectoryTree(baseDir);
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
        SourceModule module = (SourceModule) AbstractModule.createModuleFromDoc("test", null, new Document(doc),
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

        SourceModule module = (SourceModule) AbstractModule.createModuleFromDoc("test", null, new Document(doc),
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
        SourceModule module = (SourceModule) AbstractModule.createModuleFromDoc("test", null, new Document(doc),
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

    public void testCompleteIndex() throws MisconfigurationException, InterruptedException {
        String doc = "class Test:\n" +
                "    class Test2:\n" +
                "        def mmm(self):\n" +
                "            a = mmm1\n"
                +
                "            print mmm1";
        File tempFileAt = REF.getTempFileAt(baseDir, "data_temporary_file_on_additional_interpreter_info_test", ".py");
        REF.writeStrToFile(doc, tempFileAt);
        try {
            SourceModule module = (SourceModule) AbstractModule.createModuleFromDoc("test", tempFileAt, new Document(
                    doc), nature, true);
            info.addAstInfo(module.getAst(), new ModulesKey("test", tempFileAt), false);

            List<ModulesKey> modulesWithTokensStartingWith = null;

            modulesWithTokensStartingWith = info.getModulesWithToken("mmm", null);
            assertEquals(1, modulesWithTokensStartingWith.size());

            modulesWithTokensStartingWith = info.getModulesWithToken("mmm1", null);
            assertEquals(1, modulesWithTokensStartingWith.size());

            modulesWithTokensStartingWith = info.getModulesWithToken("mmm4", null);
            assertEquals(0, modulesWithTokensStartingWith.size());

            synchronized (this) {
                wait(1000);
            }

            doc = "new contents";
            REF.writeStrToFile(doc, tempFileAt);
            modulesWithTokensStartingWith = info.getModulesWithToken("mmm", null);
            assertEquals(0, modulesWithTokensStartingWith.size());

            modulesWithTokensStartingWith = info.getModulesWithToken("contents", null);
            assertEquals(1, modulesWithTokensStartingWith.size());
        } finally {
            tempFileAt.delete();
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
