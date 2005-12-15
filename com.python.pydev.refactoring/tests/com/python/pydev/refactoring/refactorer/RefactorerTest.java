package com.python.pydev.refactoring.refactorer;

import java.io.File;

import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.RefactoringRequest;

public class RefactorerTest extends CodeCompletionTestsBase {

	public static void main(String[] args) {
		try {
			RefactorerTest test = new RefactorerTest();
			test.setUp();
			test.testSearch4();
			test.tearDown();

			junit.textui.TestRunner.run(RefactorerTest.class);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private Refactorer refactorer;

	protected void setUp() throws Exception {
		super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);
        refactorer = new Refactorer();
	}

	protected void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
	}
	
	public void testSearch1() throws Exception {
		//searching for import.
		//Line contents (1):
		//from toimport import Test1
		String line = "from toimport import Test1";
		RefactoringRequest refactoringRequest = new RefactoringRequest(new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/relative/testrelative.py"));
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, line.length());
		refactoringRequest.nature = nature;
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/relative/toimport.py"), pointers[0].file);
		assertEquals(0, pointers[0].start.line);
		assertEquals(6, pointers[0].start.column);
	}

	public void testSearch2() throws Exception {
		String line = "from testlib.unittest import testcase as t";
		RefactoringRequest refactoringRequest = new RefactoringRequest(new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/anothertest.py"));
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, line.length());
		refactoringRequest.nature = nature;
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/testcase.py"), pointers[0].file);
		//found the module
		assertEquals(0, pointers[0].start.line);
		assertEquals(0, pointers[0].start.column);
		
	}
	
	public void testSearch3() throws Exception {
		String line = "from testlib.unittest import testcase as t";
		//            "from testlib.unittest import test" < -- that's the cursor pos
		RefactoringRequest refactoringRequest = new RefactoringRequest(new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/anothertest.py"));
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, line.length()-9);
		refactoringRequest.nature = nature;
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
		RefactoringRequest refactoringRequest = new RefactoringRequest(new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/anothertest.py"));
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, line.length()-24);
		refactoringRequest.nature = nature;
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
		RefactoringRequest refactoringRequest = new RefactoringRequest(new File(TestDependent.TEST_PYSRC_LOC+"testlib/unittest/anothertest.py"));
		refactoringRequest.ps = new PySelection(refactoringRequest.doc, 6);
		refactoringRequest.nature = nature;
		ItemPointer[] pointers = refactorer.findDefinition(refactoringRequest);
		
		assertEquals(1, pointers.length);
		assertEquals(new File(TestDependent.TEST_PYSRC_LOC+"testlib/__init__.py"), pointers[0].file);
		//found the module
		assertEquals(0, pointers[0].start.line);
		assertEquals(0, pointers[0].start.column);
		
	}
}
