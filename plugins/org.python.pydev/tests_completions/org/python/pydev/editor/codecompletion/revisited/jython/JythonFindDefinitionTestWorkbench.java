/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited.jython;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.IModule;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JavaDefinition;
import org.python.pydev.editor.codecompletion.revisited.javaintegration.JavaZipModule;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;

public class JythonFindDefinitionTestWorkbench extends AbstractJythonWorkbenchTests {

    public void testFind() throws Exception {
        String d = "" +
                "from javax import swing\n" +
                "print swing.JFrame()";

        Document doc = new Document(d);
        IModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, true);
        Definition[] defs = (Definition[]) module.findDefinition(
                CompletionStateFactory.getEmptyCompletionState("swing.JFrame", nature, new CompletionCache()), 2, 7,
                nature);

        assertEquals(1, defs.length);
        assertEquals("", defs[0].value);
        assertTrue(defs[0].module instanceof JavaZipModule);
        assertTrue(((JavaDefinition) defs[0]).javaElement != null);
        assertTrue(defs[0] instanceof JavaDefinition);
        assertEquals("javax.swing.JFrame", defs[0].module.getName());
    }

    public void testFind2() throws Exception {
        String d = "" +
                "import java.lang.Class";

        Document doc = new Document(d);
        IModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, true);
        Definition[] defs = (Definition[]) module.findDefinition(
                CompletionStateFactory.getEmptyCompletionState("java.lang.Class", nature, new CompletionCache()), 1,
                20, nature);

        assertEquals(1, defs.length);
        assertEquals("", defs[0].value);
        assertTrue(defs[0].module instanceof JavaZipModule);
        IJavaElement javaElement = ((JavaDefinition) defs[0]).javaElement;
        assertTrue(javaElement != null);
        assertTrue(defs[0] instanceof JavaDefinition);
        assertEquals("java.lang.Class", defs[0].module.getName());
        assertEquals("Class", javaElement.getElementName());
    }

    public void testFind3() throws Exception {
        String d = "" +
                "import java.lang.Class\n" +
                "java.lang.Class.asSubclass";

        Document doc = new Document(d);
        IModule module = AbstractModule.createModuleFromDoc("", null, doc, nature, true);
        Definition[] defs = (Definition[]) module.findDefinition(CompletionStateFactory.getEmptyCompletionState(
                "java.lang.Class.asSubclass", nature, new CompletionCache()), 2, 20, nature);

        assertEquals(1, defs.length);
        assertEquals("asSubclass", defs[0].value);
        assertTrue(defs[0].module instanceof JavaZipModule);
        IJavaElement javaElement = ((JavaDefinition) defs[0]).javaElement;
        assertTrue(javaElement != null);
        assertEquals("asSubclass", javaElement.getElementName());
        assertTrue(defs[0] instanceof JavaDefinition);
        assertEquals("java.lang.Class", defs[0].module.getName());
    }

}
