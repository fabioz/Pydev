/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.util.List;

import junit.framework.TestCase;

import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.NameTok;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

public class AdditionalInterpreterInfoTest extends TestCase {

    private AbstractAdditionalInterpreterInfo info;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AdditionalInterpreterInfoTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
        info = new AbstractAdditionalInterpreterInfo(){

            @Override
            protected String getPersistingLocation() {
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

    public void testAddFunc() {
        info.addMethod(createFuncDef("met1" ), "mod1");
        info.addMethod(createFuncDef("met2" ), "mod1");
        info.addMethod(createFuncDef("func1"), "mod1");
        info.addMethod(createFuncDef("func2"), "mod1");
        
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
        info.addClass(createClassDef("cls1" ) , "mod1");
        info.addClass(createClassDef("cls2" ) , "mod1");
        info.addClass(createClassDef("class1"), "mod2");
        info.addClass(createClassDef("class2"), "mod2");
        
        List<IInfo> tokensStartingWith = info.getTokensStartingWith("cls");
        assertEquals(2, tokensStartingWith.size());
        assertIsIn("cls1", tokensStartingWith);
        assertIsIn("cls2", tokensStartingWith);
        
        info.removeInfoFromModule("mod2");
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
