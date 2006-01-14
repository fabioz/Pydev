/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.NameTok;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;

public class AdditionalInterpreterInfoTest extends AdditionalInfoTestsBase {

    private AbstractAdditionalDependencyInfo info;

    public static void main(String[] args) {
        try {
            AdditionalInterpreterInfoTest test = new AdditionalInterpreterInfoTest();
            test.setUp();
            test.testAddInner2();
            test.tearDown();

            junit.textui.TestRunner.run(AdditionalInterpreterInfoTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected void setUp() throws Exception {
        super.setUp();
        info = new AbstractAdditionalDependencyInfo(){

            @Override
            protected File getPersistingLocation() {
                return null;
            }

            @Override
            protected void setAsDefaultInfo() {
            }
            
        };
    }

    protected void tearDown() throws Exception {
        super.tearDown();
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
    
    private ClassDef createClassDef(String name) {
        return new ClassDef(new NameTok(name, NameTok.FunctionName), null, null);
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
