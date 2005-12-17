/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.NameTok;

public class AdditionalInterpreterInfoTest extends TestCase {

    private AbstractAdditionalDependencyInfo info;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AdditionalInterpreterInfoTest.class);
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
        info.addMethod(createFuncDef("metz" ), "mod1", false);
        info.addMethod(createFuncDef("metZ" ), "mod1", false);
        info.addMethod(createFuncDef("met9" ), "mod1", false);
        info.addMethod(createFuncDef("met0" ), "mod1", false);
        info.addMethod(createFuncDef("meta" ), "mod1", false);
        info.addMethod(createFuncDef("metA" ), "mod1", false);
        
        List<IInfo> tokensStartingWith = info.getTokensStartingWith("met");
        assertEquals(6, tokensStartingWith.size());
        
        List<IInfo> tokensEqualTo = info.getTokensEqualTo("metz");
        assertEquals(1, tokensEqualTo.size());
    }
    
    public void testMap2() {
        info.addMethod(createFuncDef("m" )   , "mod1", false);
        info.addMethod(createFuncDef("mm" )  , "mod1", false);
        info.addMethod(createFuncDef("mmm" ) , "mod1", false);
        info.addMethod(createFuncDef("mmmm" ), "mod1", false);
        
        List<IInfo> tokensStartingWith = info.getTokensStartingWith("m");
        assertEquals(4, tokensStartingWith.size());
        
        tokensStartingWith = info.getTokensStartingWith("mm");
        assertEquals(3, tokensStartingWith.size());
        
        tokensStartingWith = info.getTokensStartingWith("mmm");
        assertEquals(2, tokensStartingWith.size());
        
        tokensStartingWith = info.getTokensStartingWith("mmmm");
        assertEquals(1, tokensStartingWith.size());
    }
    
    public void testAddFunc() {
        info.addMethod(createFuncDef("met1" ), "mod1", false);
        info.addMethod(createFuncDef("met2" ), "mod1", false);
        info.addMethod(createFuncDef("func1"), "mod1", false);
        info.addMethod(createFuncDef("func2"), "mod1", false);
        
        List<IInfo> tokensStartingWith = info.getTokensStartingWith("me");
        assertEquals(2, tokensStartingWith.size());
        assertIsIn("met1", tokensStartingWith);
        assertIsIn("met2", tokensStartingWith);
        
        tokensStartingWith = info.getTokensStartingWith("func");
        assertEquals(2, tokensStartingWith.size());
        assertIsIn("func1", tokensStartingWith);
        assertIsIn("func2", tokensStartingWith);
        for (IInfo info : tokensStartingWith) {
            assertEquals("mod1", info.getDeclaringModuleName());
        }
    }

    public void testAddClass() {
        info.addClass(createClassDef("cls1" ) , "mod1", false);
        info.addClass(createClassDef("cls2" ) , "mod1", false);
        info.addClass(createClassDef("class1"), "mod2", false);
        info.addClass(createClassDef("class2"), "mod2", false);
        
        List<IInfo> tokensStartingWith = info.getTokensStartingWith("cls");
        assertEquals(2, tokensStartingWith.size());
        assertIsIn("cls1", tokensStartingWith);
        assertIsIn("cls2", tokensStartingWith);
        
        info.removeInfoFromModule("mod2", false);
        tokensStartingWith = info.getTokensStartingWith("class");
        assertEquals(0, tokensStartingWith.size());
    }
    
    private ClassDef createClassDef(String name) {
        return new ClassDef(new NameTok(name, NameTok.FunctionName), null, null);
    }

    private void assertIsIn(String req, List<IInfo> tokensStartingWith) {
        for (IInfo info : tokensStartingWith) {
            if(info.getName().equals(req)){
                return;
            }
        }
        fail("The token requested ("+req+") was not found.");
    }

    private FunctionDef createFuncDef(String metName) {
        return new FunctionDef(new NameTok(metName, NameTok.FunctionName), null, null, null);
    }
    
    

}
