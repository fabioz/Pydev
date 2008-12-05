/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;

public class AdditionalInterpreterInfoTest extends AdditionalInfoTestsBase {

    private AbstractAdditionalDependencyInfo info;

    public static void main(String[] args) {
        try {
            AdditionalInterpreterInfoTest test = new AdditionalInterpreterInfoTest();
            test.setUp();
            test.testMap();
            test.tearDown();

            junit.textui.TestRunner.run(AdditionalInterpreterInfoTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void setUp() throws Exception {
        super.setUp();
        info = new AbstractAdditionalDependencyInfo(){

            @Override
            protected File getPersistingLocation() {
                return null;
            }

            @Override
            protected void setAsDefaultInfo() {
            }

            @Override
            protected File getPersistingFolder() {
                return null;
            }
            
        };
    }

    
    public void testMap() {
        info.addMethod(createFuncDef("metz" ), "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        info.addMethod(createFuncDef("metZ" ), "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        info.addMethod(createFuncDef("met9" ), "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        info.addMethod(createFuncDef("met0" ), "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        info.addMethod(createFuncDef("meta" ), "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        info.addMethod(createFuncDef("metA" ), "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        
        List<IInfo> tokensStartingWith = info.getTokensStartingWith("met", AbstractAdditionalInterpreterInfo.TOP_LEVEL);
        assertEquals(6, tokensStartingWith.size());
        
        List<IInfo> tokensEqualTo = info.getTokensEqualTo("metz", AbstractAdditionalInterpreterInfo.TOP_LEVEL);
        assertEquals(1, tokensEqualTo.size());
    }
    
    public void testMap2() {
        info.addMethod(createFuncDef("m" )   , "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        info.addMethod(createFuncDef("mm" )  , "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        info.addMethod(createFuncDef("mmm" ) , "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        info.addMethod(createFuncDef("mmmm" ), "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        
        List<IInfo> tokensStartingWith = info.getTokensStartingWith("m", AbstractAdditionalInterpreterInfo.TOP_LEVEL);
        assertEquals(4, tokensStartingWith.size());
        
        tokensStartingWith = info.getTokensStartingWith("mm", AbstractAdditionalInterpreterInfo.TOP_LEVEL);
        assertEquals(3, tokensStartingWith.size());
        
        tokensStartingWith = info.getTokensStartingWith("mmm", AbstractAdditionalInterpreterInfo.TOP_LEVEL);
        assertEquals(2, tokensStartingWith.size());
        
        tokensStartingWith = info.getTokensStartingWith("mmmm", AbstractAdditionalInterpreterInfo.TOP_LEVEL);
        assertEquals(1, tokensStartingWith.size());
    }
    
    public void testAddFunc() {
        info.addMethod(createFuncDef("met1" ), "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        info.addMethod(createFuncDef("met2" ), "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        info.addMethod(createFuncDef("func1"), "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        info.addMethod(createFuncDef("func2"), "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        
        List<IInfo> tokensStartingWith = info.getTokensStartingWith("me", AbstractAdditionalInterpreterInfo.TOP_LEVEL);
        assertEquals(2, tokensStartingWith.size());
        assertIsIn("met1", tokensStartingWith);
        assertIsIn("met2", tokensStartingWith);
        
        tokensStartingWith = info.getTokensStartingWith("func", AbstractAdditionalInterpreterInfo.TOP_LEVEL);
        assertEquals(2, tokensStartingWith.size());
        assertIsIn("func1", tokensStartingWith);
        assertIsIn("func2", tokensStartingWith);
        for (IInfo info : tokensStartingWith) {
            assertEquals("mod1", info.getDeclaringModuleName());
        }
    }

    public void testAddClass() {
        info.addClass(createClassDef("cls1" ) , "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        info.addClass(createClassDef("cls2" ) , "mod1", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        info.addClass(createClassDef("class1"), "mod2", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        info.addClass(createClassDef("class2"), "mod2", false, AbstractAdditionalInterpreterInfo.TOP_LEVEL, null);
        
        List<IInfo> tokensStartingWith = info.getTokensStartingWith("cls", AbstractAdditionalInterpreterInfo.TOP_LEVEL);
        assertEquals(2, tokensStartingWith.size());
        assertIsIn("cls1", tokensStartingWith);
        assertIsIn("cls2", tokensStartingWith);
        
        info.removeInfoFromModule("mod2", false);
        tokensStartingWith = info.getTokensStartingWith("class", AbstractAdditionalInterpreterInfo.TOP_LEVEL);
        assertEquals(0, tokensStartingWith.size());
    }
    
    
    public void testAddInner() {
        String doc = 
        "class Test:\n" +
        "    def m1(self):\n" +
        "        pass";
        SourceModule module = (SourceModule) AbstractModule.createModuleFromDoc("test", null, new Document(doc), nature, 0);
        info.addSourceModuleInfo(module, nature, false);

        List<IInfo> tokensStartingWith = info.getTokensStartingWith("Tes", AbstractAdditionalInterpreterInfo.TOP_LEVEL | AbstractAdditionalInterpreterInfo.INNER);
        assertEquals(1, tokensStartingWith.size());
        assertIsIn("Test", tokensStartingWith);
        
        tokensStartingWith = info.getTokensStartingWith("m1", AbstractAdditionalInterpreterInfo.TOP_LEVEL | AbstractAdditionalInterpreterInfo.INNER);
        assertEquals(1, tokensStartingWith.size());
        assertIsIn("m1", tokensStartingWith);
        IInfo i = tokensStartingWith.get(0);
        assertEquals("Test", i.getPath());
        
    }
    
    public void testAddAttrs() {
        String doc = 
"GLOBAL_ATTR = 1\n" +
"GLOBAL2.IGNORE_THIS = 2\n" +
"" +
"class Test:\n" +
"    test_attr = 1\n" +
"    test_attr.ignore = 2\n" +
"    test_attr2.ignore_this = 3\n" +
"" +
"    class Test2:\n" +
"        def mmm(self):\n" +
"            self.attr1 = 10";
        
        SourceModule module = (SourceModule) AbstractModule.createModuleFromDoc("test", null, new Document(doc), nature, 0);
        info.addSourceModuleInfo(module, nature, false);
        
        List<IInfo> tokensStartingWith = null;
        IInfo i = null;
        
        tokensStartingWith = info.getTokensStartingWith("global", AbstractAdditionalInterpreterInfo.TOP_LEVEL | AbstractAdditionalInterpreterInfo.INNER);
//        assertEquals(2, tokensStartingWith.size());
        assertIsIn("GLOBAL_ATTR", tokensStartingWith);
        assertIsIn("GLOBAL2", tokensStartingWith);
        
        tokensStartingWith = info.getTokensStartingWith("", AbstractAdditionalInterpreterInfo.TOP_LEVEL | AbstractAdditionalInterpreterInfo.INNER);
//        assertEquals(2, tokensStartingWith.size());
        i = assertIsIn("Test", tokensStartingWith);
        assertEquals(null, i.getPath());
        
        i = assertIsIn("Test2", tokensStartingWith);
        assertEquals("Test", i.getPath());
        
        i = assertIsIn("test_attr", tokensStartingWith);
        assertEquals("Test", i.getPath());
        
        i = assertIsIn("test_attr2", tokensStartingWith);
        assertEquals("Test", i.getPath());
        
        i = assertIsIn("attr1", tokensStartingWith);
        assertEquals("Test.Test2.mmm", i.getPath());

    }

    public void testAddInner2() {
        String doc = 
            "class Test:\n" +
            "    class Test2:\n" +
            "        def mmm(self):\n" +
            "            pass";
        SourceModule module = (SourceModule) AbstractModule.createModuleFromDoc("test", null, new Document(doc), nature, 0);
        info.addSourceModuleInfo(module, nature, false);
        
        List<IInfo> tokensStartingWith = null;
        
        tokensStartingWith = info.getTokensStartingWith("m", AbstractAdditionalInterpreterInfo.TOP_LEVEL | AbstractAdditionalInterpreterInfo.INNER);
        assertEquals(1, tokensStartingWith.size());
        assertIsIn("mmm", tokensStartingWith);
        IInfo i = tokensStartingWith.get(0);
        assertEquals("Test.Test2", i.getPath());
        
        tokensStartingWith = info.getTokensStartingWith("Test", AbstractAdditionalInterpreterInfo.TOP_LEVEL | AbstractAdditionalInterpreterInfo.INNER);
        assertEquals(2, tokensStartingWith.size());
        i = assertIsIn("Test", tokensStartingWith);
        assertEquals(null, i.getPath());
        i = assertIsIn("Test2", tokensStartingWith);
        assertEquals("Test", i.getPath());
        
    }
    

    public void testCompleteIndex() {
        String doc = 
            "class Test:\n" +
            "    class Test2:\n" +
            "        def mmm(self):\n" +
            "            a = mmm1\n" +
            "            print mmm1";
        SourceModule module = (SourceModule) AbstractModule.createModuleFromDoc("test", null, new Document(doc), nature, 0);
        info.addSourceModuleInfo(module, nature, false);
        
        List<IInfo> tokensStartingWith = null;
        
        tokensStartingWith = info.getTokensStartingWith("m", AbstractAdditionalDependencyInfo.COMPLETE_INDEX);
        assertEquals(2, tokensStartingWith.size()); //only 2, one for mmm and one for mm1 (even appearing twice).
        assertIsIn("mmm", tokensStartingWith);
        assertIsIn("mmm1", tokensStartingWith);
    }
    

    private ClassDef createClassDef(String name) {
        return new ClassDef(new NameTok(name, NameTok.FunctionName), null, null, null, null, null, null);
    }

    private IInfo assertIsIn(String req, List<IInfo> tokensStartingWith) {
        for (IInfo info : tokensStartingWith) {
            if(info.getName().equals(req)){
                return info;
            }
        }
        fail("The token requested ("+req+") was not found.");
        return null;
    }

    private FunctionDef createFuncDef(String metName) {
        return new FunctionDef(new NameTok(metName, NameTok.FunctionName), null, null, null);
    }
    
}
