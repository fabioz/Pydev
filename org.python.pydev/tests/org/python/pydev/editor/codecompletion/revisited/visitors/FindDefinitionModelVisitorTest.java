/*
 * Created on Apr 13, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import org.eclipse.jface.text.Document;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;

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
		AssignDefinition[] definitions = module.findDefinition("ex", 2, 2, nature.getAstManager());
		
    }
}
