/*
 * Created on Apr 13, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.ArrayList;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.FindInfo;
import org.python.pydev.core.IModule;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;

/**
 * @author Fabio Zadrozny
 */
public class FindDefinitionModelVisitorTest  extends CodeCompletionTestsBase{

    public static void main(String[] args) {
        try{
            FindDefinitionModelVisitorTest test = new FindDefinitionModelVisitorTest();
            test.setUp();
            test.testFind();
            test.tearDown();
            junit.textui.TestRunner.run(FindDefinitionModelVisitorTest.class);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        restorePythonPath(false);
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * @throws Exception
     * 
     */
    public void testFind() throws Exception {
		String d = ""+
		"from testAssist import assist\n" +
		"ex = assist.ExistingClass()\n" +
		"ex.newMethod(c,d)";

		Document doc = new Document(d);
		IModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, 2);
		Definition[] defs = (Definition[]) module.findDefinition(CompletionState.getEmptyCompletionState("ex", nature), 3, 3, nature, new ArrayList<FindInfo>());
		
		assertEquals(1, defs.length);
		assertEquals("ex", ((AssignDefinition)defs[0]).target);
		assertEquals("assist.ExistingClass", defs[0].value);
		assertSame(module, defs[0].module);
		
		defs = (Definition[]) module.findDefinition(CompletionState.getEmptyCompletionState("assist.ExistingClass", nature), 2, 6, nature, new ArrayList<FindInfo>());
		assertEquals(1, defs.length);
		assertEquals("ExistingClass", defs[0].value);
		assertNotSame(module, defs[0].module);
		assertEquals("testAssist.assist", defs[0].module.getName());
		
    }

    /**
     * @throws Exception
     * 
     */
    public void testFind2() throws Exception {
        String d;
        d = "class C:            \n" +
			"    def met1(self): \n" +
			"        pass        \n" +
			"                    \n" +
			"class B:            \n" +
			"    def met2(self): \n" +
			"        self.c = C()\n" +
			"                    \n" +
			"    def met3(self): \n" +
			"        self.c.";

		Document doc = new Document(d);
		IModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, 9);
		//self.c is found as an assign
		Definition[] defs = (Definition[]) module.findDefinition(CompletionState.getEmptyCompletionState("self.c",nature), 10, 9, nature, new ArrayList<FindInfo>());
		
		assertEquals(1, defs.length);
		assertEquals("self.c", ((AssignDefinition)defs[0]).target);
		assertEquals("C", defs[0].value);
		assertSame(module, defs[0].module);
		
		defs = (Definition[]) module.findDefinition(CompletionState.getEmptyCompletionState("C", nature), 7, 18, nature, new ArrayList<FindInfo>());
		assertEquals(1, defs.length);
		assertEquals("C", defs[0].value);
		assertSame(module, defs[0].module);
		
    }
}
