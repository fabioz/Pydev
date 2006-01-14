/*
 * Created on Apr 13, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.ArrayList;

import org.eclipse.jface.text.Document;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule.FindInfo;

/**
 * @author Fabio Zadrozny
 */
public class FindDefinitionModelVisitorTest  extends CodeCompletionTestsBase{

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FindDefinitionModelVisitorTest.class);
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
		AbstractModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, 2);
		Definition[] defs = module.findDefinition("ex", 2, 2, nature, new ArrayList<FindInfo>());
		
		assertEquals(1, defs.length);
		assertEquals("ex", ((AssignDefinition)defs[0]).target);
		assertEquals("assist.ExistingClass", defs[0].value);
		assertSame(module, defs[0].module);
		
		defs = module.findDefinition("assist.ExistingClass", 1, 5, nature, new ArrayList<FindInfo>());
		assertEquals(1, defs.length);
		assertEquals("ExistingClass", defs[0].value);
		assertNotSame(module, defs[0].module);
		
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
		AbstractModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, 9);
		//self.c is found as an assign
		Definition[] defs = module.findDefinition("self.c", 9, 8, nature, new ArrayList<FindInfo>());
		
		assertEquals(1, defs.length);
		assertEquals("self.c", ((AssignDefinition)defs[0]).target);
		assertEquals("C", defs[0].value);
		assertSame(module, defs[0].module);
		
		defs = module.findDefinition("C", 6, 17, nature, new ArrayList<FindInfo>());
		assertEquals(1, defs.length);
		assertEquals("C", defs[0].value);
		assertSame(module, defs[0].module);
		
    }
}
