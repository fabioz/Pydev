/*
 * Created on Nov 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IToken;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;

/**
 * @author Fabio Zadrozny
 */
public class ModuleTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ModuleTest.class);
    }
    
    public void testMod1(){
        Tuple<SimpleNode, Throwable> obj = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(getDoc1()), false, null));
        SimpleNode n = obj.o1;
        IModule module = AbstractModule.createModule(n);
       
        IToken[] globalTokens = module.getGlobalTokens();
        assertEquals(5, globalTokens.length); //C c D d a
        compareReps(globalTokens, "C c D d a");

        IToken[] wildImportedModules = module.getWildImportedModules();
        assertEquals(1, wildImportedModules.length); //m4
        compareReps(wildImportedModules, "m4");

        IToken[] tokenImportedModules = module.getTokenImportedModules();
        assertEquals(5, tokenImportedModules.length); //a1, xx, yy, aa
        compareReps(tokenImportedModules, "a1 xx yy aa m3");

        assertEquals("docstring for module", module.getDocString());
        
    }
    
    /**
     * @param globalTokens
     * @param string
     */
    private void compareReps(IToken[] globalTokens, String string) {
        String[] strings = string.split(" ");
        assertEquals(strings.length , globalTokens.length);
        for (int i = 0; i < globalTokens.length; i++) {
            assertEquals(strings[i], globalTokens[i].getRepresentation());
        }
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
