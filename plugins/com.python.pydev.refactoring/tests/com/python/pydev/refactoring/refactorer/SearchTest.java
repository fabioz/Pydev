/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer;

import java.io.File;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.utils.PlatformUtils;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

public class SearchTest extends AdditionalInfoTestsBase {

    public static void main(String[] args) {
        try {
            SearchTest test = new SearchTest();
            test.setUp();
            test.testSearchParameter();
            test.tearDown();

            junit.textui.TestRunner.run(SearchTest.class);

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private Refactorer refactorer;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(false);
        refactorer = new Refactorer();
    }

    public void testSearch1() throws Exception {
        //searching for import.
        //Line contents (1):
        //from toimport import Test1
        String line = "from toimport import Test1";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/relative/testrelative.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/relative/toimport.py"), pointers[0].file);
        assertEquals(0, pointers[0].start.line);
        assertEquals(6, pointers[0].start.column);
    }

    public void testSearch2() throws Exception {
        String line = "from testlib.unittest import testcase as t";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/anothertest.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/testcase.py"), pointers[0].file);
        //found the module
        assertEquals(0, pointers[0].start.line);
        assertEquals(0, pointers[0].start.column);

    }

    private RefactoringRequest createRefactoringRequest(String line, final File file) {
        return new RefactoringRequest(file, new PySelection(new Document(FileUtils.getFileContents(file)),
                line.length()),
                nature);
    }

    private RefactoringRequest createRefactoringRequest(final IDocument doc, String modName, int line, int col) {
        PySelection sel = new PySelection(doc, line, col);
        RefactoringRequest req = new RefactoringRequest(null, sel, nature);
        req.moduleName = modName;
        return req;
    }

    public void testSearch3() throws Exception {
        String line = "from testlib.unittest import testcase as t";
        //            "from testlib.unittest import test" < -- that's the cursor pos
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/anothertest.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), line.length() - 9);

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/testcase.py"), pointers[0].file);
        //found the module
        assertEquals(0, pointers[0].start.line);
        assertEquals(0, pointers[0].start.column);

    }

    public void testSearch4() throws Exception {
        String line = "from testlib.unittest import testcase as t";
        //            "from testlib.unitt" < -- that's the cursor pos
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/anothertest.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), line.length() - 24);

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/__init__.py"), pointers[0].file);
        //found the module
        assertEquals(0, pointers[0].start.line);
        assertEquals(0, pointers[0].start.column);

    }

    public void testSearch5() throws Exception {
        //ring line = "from testlib.unittest import testcase as t";
        //            "from " < -- that's the cursor pos
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/anothertest.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest("", file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 6);

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "testlib/__init__.py"), pointers[0].file);
        //found the module
        assertEquals(0, pointers[0].start.line);
        assertEquals(0, pointers[0].start.column);
    }

    public void testSearch6() throws Exception {
        //from testlib.unittest import testcase as t
        String line = "class AnotherTest(t.TestCase):";
        //            "from " < -- that's the cursor pos
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/anothertest.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 2, line.length() - 5);

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/testcase.py"), pointers[0].file);
        //found the module
        assertEquals(8, pointers[0].start.line);
        assertEquals(6, pointers[0].start.column);
    }

    public void testSearch7() throws Exception {
        //        from static import TestStatic
        //        print TestStatic.static1
        //        class TestStaticExt(TestStatic):
        //            def __init__(self):
        //                print self.static1
        String line = "print TestStatic.static1";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "extendable/static2.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 1, line.length());

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "extendable/static.py"), pointers[0].file);
        //found the module
        assertEquals(3, pointers[0].start.line);
        assertEquals(8, pointers[0].start.column);
    }

    public void testSearch8() throws Exception {
        //        from static import TestStatic
        //        print TestStatic.static1
        //        class TestStaticExt(TestStatic):
        //            def __init__(self):
        //                print self.static1
        String line = "        print self.static1";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "extendable/static2.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 4, line.length());

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "extendable/static.py"), pointers[0].file);
        //found the module
        assertEquals(3, pointers[0].start.line);
        assertEquals(8, pointers[0].start.column);
    }

    public void testSearch9() throws Exception {
        //        from static import TestStatic
        //        print TestStatic.static1
        //        class TestStaticExt(TestStatic):
        //            def __init__(self):
        //                print self.static1
        //              from extendable.dependencies.file2 import Test
        String line = "        from extendable.dependencies.file2 import Test";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "extendable/static2.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 5, line.length());

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "extendable/dependencies/file2.py"), pointers[0].file);
        //found the module
        assertEquals(0, pointers[0].start.line);
        assertEquals(6, pointers[0].start.column);
    }

    public void testSearch10() throws Exception {
        //        from static import TestStatic
        //        print TestStatic.static1
        //        class TestStaticExt(TestStatic):
        //            def __init__(self):
        //                print self.static1
        //                from extendable.dependencies.file2 import Test
        //                import extendable.dependencies.file2.Test
        String line = "        import extendable.dependencies.file2.Test";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "extendable/static2.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 6, line.length());

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "extendable/dependencies/file2.py"), pointers[0].file);
        //found the module
        assertEquals(0, pointers[0].start.line);
        assertEquals(6, pointers[0].start.column);
    }

    public void testSearch11() throws Exception {
        //        from static import TestStatic
        //        print TestStatic.static1
        //        class TestStaticExt(TestStatic):
        //            def __init__(self):
        //                print self.static1
        //                from extendable.dependencies.file2 import Test
        //                import extendable.dependencies.file2.Test
        String line = "        import extendable.dependencies.file2.Test";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "extendable/static2.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 6, line.length() - 7); //find the file2 module itself

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "extendable/dependencies/file2.py"), pointers[0].file);
        //found the module
        assertEquals(0, pointers[0].start.line);
        assertEquals(0, pointers[0].start.column);
    }

    public void testSearch12() throws Exception {
        //        from static import TestStatic
        //        print TestStatic.static1
        //        class TestStaticExt(TestStatic):
        //            def __init__(self):
        //                print self.static1
        //                from extendable.dependencies.file2 import Test
        //                import extendable.dependencies.file2.Test
        String line = "        import extendable.dependencies.file2.Test";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "extendable/static2.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 6, line.length() - 16); //find the dependencies module itself

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "extendable/dependencies/__init__.py"), pointers[0].file);
        //found the module
        assertEquals(0, pointers[0].start.line);
        assertEquals(0, pointers[0].start.column);
    }

    public void testSearch13() throws Exception {
        //        from f1 import *
        //        print Class1

        String line = "print Class1";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "testrecwild/__init__.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 1, line.length());

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "testrecwild/f2.py"), pointers[0].file);
        //found the module
        assertEquals(0, pointers[0].start.line);
        assertEquals(6, pointers[0].start.column);
    }

    public void testSearch14() throws Exception {
        //        from someparent.somechild import configport config
        //        config.whateveryoulike()

        String line = "config.whateveryoulike()";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "otherparent/navigationtest.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 1, 0);

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
        if (pointers.length != 1) {
            for (ItemPointer pointer : pointers) {
                System.out.println(pointer);
            }
        }
        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "someparent/somechild/config.py"), pointers[0].file);
        //found the module
        assertEquals(0, pointers[0].start.line);
        assertEquals(0, pointers[0].start.column);
    }

    public void testSearchImport() throws Exception {
        //      from testlib.unittest import TestCase
        //      print TestCase

        String line = "print TestCase";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "extendable/deepimport.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 1, line.length());

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "testlib/unittest/testcase.py"), pointers[0].file);
        //found the module
        assertEquals(8, pointers[0].start.line);
        assertEquals(6, pointers[0].start.column);
    }

    public void testSearchImport2() throws Exception {
        //import mod2
        //mod2.Foo
        String line = "mod2.Foo";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "extendable/searching/mod3.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 1, line.length());

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "extendable/searching/mod1/foo.py"), pointers[0].file);
        //found the module
        assertEquals(0, pointers[0].start.line);
        assertEquals(6, pointers[0].start.column);
    }

    public void testSearchParameter() throws Exception {
        //        class Param(object): - this is line 0
        //            
        //            def hasParams(self, aa, bb):
        //                #TestStatic has static1 and static2
        //                print aa.static1() - line 4
        //                print aa.static2()        

        List<IInfo> tokens = AdditionalProjectInterpreterInfo.getTokensEqualTo("static1", nature,
                AbstractAdditionalTokensInfo.TOP_LEVEL | AbstractAdditionalTokensInfo.INNER);
        assertEquals(1, tokens.size()); //if this fails, the cache is outdated (i.e.: delete AdditionalProjectInterpreterInfo.pydevinfo)

        String line = "print aa.static1()";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "extendable/parameters.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 4, line.length());

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC + "extendable/static.py"), pointers[0].file);
        //found the module
        assertEquals(3, pointers[0].start.line);
        assertEquals(8, pointers[0].start.column);
    }

    public void testBuiltinSearch() throws Exception {
        //      import os
        String line = "import os";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "simpleosimport.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 0, line.length()); //find the os module

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.PYTHON_LIB + "os.py"), pointers[0].file);
        //found the module
        assertEquals(0, pointers[0].start.line);
        assertEquals(0, pointers[0].start.column);
    }

    public void testBuiltinSearch2() throws Exception {
        //      import os.path.normpath
        String line = "import os.path.normpath";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "definitions/__init__.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 0, line.length()); //find the os.path.normpath func pos

        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        File linuxFile = new File(TestDependent.PYTHON_LIB + "posixpath.py");
        File windowsFile = new File(TestDependent.PYTHON_LIB + "ntpath.py");

        File expectedFile;
        if (PlatformUtils.isWindowsPlatform()) {
            expectedFile = windowsFile;
            assertTrue("Expecting to find it at line > 300, found it at:" + pointers[0].start.line,
                    pointers[0].start.line > 300); //depends on python version
        } else {
            expectedFile = linuxFile;
            assertTrue("Expecting to find it at line > 300, found it at:" + pointers[0].start.line,
                    pointers[0].start.line > 300); //depends on python version (linux)
        }
        assertEquals(expectedFile, pointers[0].file);
        //found the module
        assertEquals(0, pointers[0].start.column);
    }

    public void testOnMethodFind() throws Exception {
        //class TestStatic(object):    --line 0  
        //          
        //    @staticmethod      
        //    def static1(self):      --line 3
        //        pass      
        //          
        //    @staticmethod      
        //    def static2(self):
        //        pass
        String line = "    def static1(self):";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "extendable/static.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 3, line.length() - "1(self):".length()); //find the 'static1' method itself

        refactoringRequest.setAdditionalInfo(RefactoringRequest.FIND_DEFINITION_IN_ADDITIONAL_INFO,
                false);
        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(file, pointers[0].file);
        //found the module
        assertEquals(8, pointers[0].start.column);
        assertEquals(3, pointers[0].start.line);
    }

    public void testOnClassFind() throws Exception {
        //class TestStatic(object):    --line 0  
        //          
        //    @staticmethod      
        //    def static1(self):      --line 3
        //        pass      
        //          
        //    @staticmethod      
        //    def static2(self):
        //        pass
        String line = "class TestStatic(object):";
        final File file = new File(TestDependent.TEST_PYSRC_LOC + "extendable/static.py");
        RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.getDoc(), 0, line.length()
                - "Static(object):".length()); //find the 'TestStatic' class itself

        refactoringRequest.setAdditionalInfo(RefactoringRequest.FIND_DEFINITION_IN_ADDITIONAL_INFO,
                false);
        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(file, pointers[0].file);
        //found the module
        assertEquals(6, pointers[0].start.column);
        assertEquals(0, pointers[0].start.line);
    }

    public void testOnSameName() throws Exception {
        String str = "" + "class Foo:\n" + "    def m1(self):\n" + //this line, col 9
                "        m1 = 10\n" + "        print m1\n" + "        print self.m1\n" + "";

        RefactoringRequest refactoringRequest = createRefactoringRequest(new Document(str), "foo", 1, 9);

        refactoringRequest.setAdditionalInfo(RefactoringRequest.FIND_DEFINITION_IN_ADDITIONAL_INFO,
                false);
        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(1, pointers[0].start.line);
    }

    public void testOnParam() throws Exception {
        String str = "" + "tok = 10\n" + "def m1(tok=tok):\n" + //parameter tok (left side)
                "    '@param tok: this is tok'\n" + "    #checking tok right?\n" + "";

        RefactoringRequest refactoringRequest = createRefactoringRequest(new Document(str), "foo", 1, 9);

        refactoringRequest.setAdditionalInfo(RefactoringRequest.FIND_DEFINITION_IN_ADDITIONAL_INFO,
                false);
        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(1, pointers.length);
        assertEquals(1, pointers[0].start.line);
    }

    public void testOnSameName2() throws Exception {
        String str = "" + "class Foo:\n" + "    def m1():\n" + //this line, col 9
                "        pass\n" + "    m1 = staticmethod(m1)\n" + //we will find this definition too
                "";

        RefactoringRequest refactoringRequest = createRefactoringRequest(new Document(str), "foo", 1, 9);

        refactoringRequest.setAdditionalInfo(RefactoringRequest.FIND_DEFINITION_IN_ADDITIONAL_INFO,
                false);
        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);

        assertEquals(2, pointers.length);
        assertEquals(1, pointers[0].start.line);
        assertEquals(3, pointers[1].start.line);
    }
}
