/*
 * Created on Nov 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.parser.SimpleNode;
import org.python.pydev.parser.PyParser;

/**
 * @author Fabio Zadrozny
 */
public class ModuleTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ModuleTest.class);
    }
    
    public void testMod1(){
        Object[] obj = PyParser.reparseDocument(new Document(getDoc1()), false);
        SimpleNode n = (SimpleNode)obj[0];
        AbstractModule module = AbstractModule.createModule(n);
       
        assertEquals(5, module.getGlobalTokens().length);

        assertEquals(1, module.getWildImportedModules().length);

        assertEquals(4, module.getTokenImportedModules().length);

        assertEquals("docstring for module", module.getDocString());
        
    }
    
    public String getDoc1(){
        //damn, I really miss python when writing this...
        return
"'''" +
"docstring for module" +
"'''\n" +
"from m1 import a1\n" +
"from mm import aa as xx , gg as yy\n" + //this is only 1 import, but it has 2 'aliasType'.
"from m2 import a2 as aa\n" +
"import m3\n" +
"from m4 import *\n" +
"\n" +
"class C: \n" +
"    '''\n" +
"    this is the class c\n" +
"    '''\n" +
"    pass\n" +
"\n" +
"c = C()\n" +
"\n" +
"class D:pass\n" +
"\n" +
"d = D()\n" +
"\n" +
"def a():\n" +
"    '''\n" +
"    method a" +
"    '''\n" +
"    return 1";
    }
}
