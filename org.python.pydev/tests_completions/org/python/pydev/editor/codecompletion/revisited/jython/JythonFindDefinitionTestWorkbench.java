package org.python.pydev.editor.codecompletion.revisited.jython;

import java.util.ArrayList;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.FindInfo;
import org.python.pydev.core.IModule;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JavaClassModule;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JavaDefinition;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;

public class JythonFindDefinitionTestWorkbench extends JythonCodeCompletionTestsBase{

    

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        CompiledModule.COMPILED_MODULES_ENABLED = true;
        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion();
    
    }

    public void testFind() throws Exception {
        String d = ""+
        "from javax import swing\n" +
        "print swing.JFrame()";

        Document doc = new Document(d);
        IModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, 2);
        Definition[] defs = (Definition[]) module.findDefinition(CompletionStateFactory.getEmptyCompletionState("swing.JFrame", nature), 2, 7, nature, new ArrayList<FindInfo>());
        
        assertEquals(1, defs.length);
        assertEquals("", defs[0].value);
        assertTrue(defs[0].module instanceof JavaClassModule);
        assertTrue(((JavaDefinition)defs[0]).javaElement != null);
        assertTrue(defs[0] instanceof JavaDefinition);
        assertEquals("javax.swing.JFrame", defs[0].module.getName());
    }
    
    public void testFind2() throws Exception {
        String d = ""+
        "import java.lang.Class";
        
        Document doc = new Document(d);
        IModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, 2);
        Definition[] defs = (Definition[]) module.findDefinition(CompletionStateFactory.getEmptyCompletionState("java.lang.Class", nature), 2, 7, nature, new ArrayList<FindInfo>());
        
        assertEquals(1, defs.length);
        assertEquals("", defs[0].value);
        assertTrue(defs[0].module instanceof JavaClassModule);
        assertTrue(((JavaDefinition)defs[0]).javaElement != null);
        assertTrue(defs[0] instanceof JavaDefinition);
        assertEquals("java.lang.Class", defs[0].module.getName());
    }

}
