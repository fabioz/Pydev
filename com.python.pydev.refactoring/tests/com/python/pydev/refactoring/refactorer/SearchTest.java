package com.python.pydev.refactoring.refactorer;

import java.io.File;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.RefactoringRequest;

import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;

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

	protected void setUp() throws Exception {
		super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(false);
        refactorer = new Refactorer();
	}

	protected void tearDown() throws Exception {
        super.tearDown();
	}
	
	public void testSearch1() throws Exception {
		//searching for import.
		//Line contents (1):
		//from toimport import Test1
		String line = "from toimport import Test1";
		final File file = new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/relative/testrelative.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/relative/toimport.py"), pointers[0].file);
		assertEquals(0, pointers[0].start.line);
		assertEquals(6, pointers[0].start.column);
	}

	public void testSearch2() throws Exception {
		String line = "from testlib.unittest import testcase as t";
		final File file = new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/anothertest.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/testcase.py"), pointers[0].file);
		//found the module
		assertEquals(0, pointers[0].start.line);
		assertEquals(0, pointers[0].start.column);
		
	}

	private RefactoringRequest createRefactoringRequest(String line, final File file) {
		return new RefactoringRequest(	file, new PySelection(new Document(REF.getFileContents(file)), line.length()),	nature);
	}
	
	public void testSearch3() throws Exception {
		String line = "from testlib.unittest import testcase as t";
		//            "from testlib.unittest import test" < -- that's the cursor pos
		final File file = new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/anothertest.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, line.length()-9);
		
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/testcase.py"), pointers[0].file);
		//found the module
		assertEquals(0, pointers[0].start.line);
		assertEquals(0, pointers[0].start.column);
		
	}
	
	public void testSearch4() throws Exception {
		String line = "from testlib.unittest import testcase as t";
		//            "from testlib.unitt" < -- that's the cursor pos
		final File file = new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/anothertest.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, line.length()-24);
		
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/__init__.py"), pointers[0].file);
		//found the module
		assertEquals(0, pointers[0].start.line);
		assertEquals(0, pointers[0].start.column);
		
	}
	
	public void testSearch5() throws Exception {
		//ring line = "from testlib.unittest import testcase as t";
		//            "from " < -- that's the cursor pos
		final File file = new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/anothertest.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest("", file);
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, 6);
		
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"testlib/__init__.py"), pointers[0].file);
		//found the module
		assertEquals(0, pointers[0].start.line);
		assertEquals(0, pointers[0].start.column);
	}
	
	public void testSearch6() throws Exception {
		//from testlib.unittest import testcase as t
		String line = "class AnotherTest(t.TestCase):";
		//            "from " < -- that's the cursor pos
		final File file = new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/anothertest.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, 2, line.length() - 5);
		
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/testcase.py"), pointers[0].file);
		//found the module
		assertEquals(8, pointers[0].start.line);
		assertEquals(6, pointers[0].start.column);
	}

	public void testSearch7() throws Exception {
//		from static import TestStatic
//		print TestStatic.static1
//		class TestStaticExt(TestStatic):
//		    def __init__(self):
//		        print self.static1
		String line = "print TestStatic.static1";
		final File file = new File(TestDependent.TEST_PYSRC_LOC+"extendable/static2.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, 1, line.length());
		
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"extendable/static.py"), pointers[0].file);
		//found the module
		assertEquals(3, pointers[0].start.line);
		assertEquals(8, pointers[0].start.column);
	}
	
	public void testSearch8() throws Exception {
//		from static import TestStatic
//		print TestStatic.static1
//		class TestStaticExt(TestStatic):
//		    def __init__(self):
//		        print self.static1
		String line = "        print self.static1";
		final File file = new File(TestDependent.TEST_PYSRC_LOC+"extendable/static2.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, 4, line.length());
		
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"extendable/static.py"), pointers[0].file);
		//found the module
		assertEquals(3, pointers[0].start.line);
		assertEquals(8, pointers[0].start.column);
	}
	
	public void testSearch9() throws Exception {
//		from static import TestStatic
//		print TestStatic.static1
//		class TestStaticExt(TestStatic):
//			def __init__(self):
//				print self.static1
//      		from extendable.dependencies.file2 import Test
        String line = "        from extendable.dependencies.file2 import Test";
		final File file = new File(TestDependent.TEST_PYSRC_LOC+"extendable/static2.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, 5, line.length());
		
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"extendable/dependencies/file2.py"), pointers[0].file);
		//found the module
		assertEquals(0, pointers[0].start.line);
		assertEquals(6, pointers[0].start.column);
	}
	
	public void testSearch10() throws Exception {
//		from static import TestStatic
//		print TestStatic.static1
//		class TestStaticExt(TestStatic):
//		    def __init__(self):
//		        print self.static1
//		        from extendable.dependencies.file2 import Test
//		        import extendable.dependencies.file2.Test
		String line = "        import extendable.dependencies.file2.Test";
		final File file = new File(TestDependent.TEST_PYSRC_LOC+"extendable/static2.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, 6, line.length());
		
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"extendable/dependencies/file2.py"), pointers[0].file);
		//found the module
		assertEquals(0, pointers[0].start.line);
		assertEquals(6, pointers[0].start.column);
	}
	

	public void testSearch11() throws Exception {
//		from static import TestStatic
//		print TestStatic.static1
//		class TestStaticExt(TestStatic):
//		    def __init__(self):
//		        print self.static1
//		        from extendable.dependencies.file2 import Test
//		        import extendable.dependencies.file2.Test
		String line = "        import extendable.dependencies.file2.Test";
		final File file = new File(TestDependent.TEST_PYSRC_LOC+"extendable/static2.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, 6, line.length()-7); //find the file2 module itself
		
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"extendable/dependencies/file2.py"), pointers[0].file);
		//found the module
		assertEquals(0, pointers[0].start.line);
		assertEquals(0, pointers[0].start.column);
	}

	public void testSearch12() throws Exception {
//		from static import TestStatic
//		print TestStatic.static1
//		class TestStaticExt(TestStatic):
//		    def __init__(self):
//		        print self.static1
//		        from extendable.dependencies.file2 import Test
//		        import extendable.dependencies.file2.Test
		String line = "        import extendable.dependencies.file2.Test";
		final File file = new File(TestDependent.TEST_PYSRC_LOC+"extendable/static2.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, 6, line.length()-16); //find the dependencies module itself
		
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"extendable/dependencies/__init__.py"), pointers[0].file);
		//found the module
		assertEquals(0, pointers[0].start.line);
		assertEquals(0, pointers[0].start.column);
	}
	

	public void testSearch13() throws Exception {
//		from f1 import *
//		print Class1
		
		String line = "print Class1";
		final File file = new File(TestDependent.TEST_PYSRC_LOC+"testrecwild/__init__.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, 1, line.length()); 
		
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"testrecwild/f2.py"), pointers[0].file);
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
        
        String line = "print aa.static1()";
        final File file = new File(TestDependent.TEST_PYSRC_LOC+"extendable/parameters.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.doc, 4, line.length()); 
        
        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
        
        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"extendable/static.py"), pointers[0].file);
        //found the module
        assertEquals(3, pointers[0].start.line);
        assertEquals(8, pointers[0].start.column);
    }

    public void testBuiltinSearch() throws Exception {
//      import os
        String line = "import os";
        final File file = new File(TestDependent.TEST_PYSRC_LOC+"simpleosimport.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.doc, 0, line.length()); //find the os module
        
        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
        
        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.PYTHON_LIB+"os.py"), pointers[0].file);
        //found the module
        assertEquals(0, pointers[0].start.line);
        assertEquals(0, pointers[0].start.column);
    }
    
    public void testBuiltinSearch2() throws Exception {
//      import os.path.normpath
        String line = "import os.path.normpath";
        final File file = new File(TestDependent.TEST_PYSRC_LOC+"definitions/__init__.py");
		RefactoringRequest refactoringRequest = createRefactoringRequest(line, file);
        refactoringRequest.ps = new PySelection(refactoringRequest.doc, 0, line.length()); //find the os.path.normpath func pos
        
        ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
        
        assertEquals(1, pointers.length);
        assertEquals(new File(TestDependent.PYTHON_LIB+"ntpath.py"), pointers[0].file);
        //found the module
        assertTrue("Expecting to find it at line 439 or 440, found it at:"+pointers[0].start.line, 440 == pointers[0].start.line || 439 == pointers[0].start.line); //depends on python version
        assertEquals(0, pointers[0].start.column);
    }
}
