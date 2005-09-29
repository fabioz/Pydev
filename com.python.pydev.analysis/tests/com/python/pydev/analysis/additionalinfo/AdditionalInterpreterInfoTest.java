/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.util.List;
import java.util.Set;

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

    
    
    public void testMap() {
        info.addMethod(createFuncDef("metz" ), "mod1");
        info.addMethod(createFuncDef("metZ" ), "mod1");
        info.addMethod(createFuncDef("met9" ), "mod1");
        info.addMethod(createFuncDef("met0" ), "mod1");
        info.addMethod(createFuncDef("meta" ), "mod1");
        info.addMethod(createFuncDef("metA" ), "mod1");
        
        List<IInfo> tokensStartingWith = info.getTokensStartingWith("met");
        assertEquals(6, tokensStartingWith.size());
        
        List<IInfo> tokensEqualTo = info.getTokensEqualTo("metz");
        assertEquals(1, tokensEqualTo.size());
    }
    
    public void testMap2() {
        info.addMethod(createFuncDef("m" ), "mod1");
        info.addMethod(createFuncDef("mm" ), "mod1");
        info.addMethod(createFuncDef("mmm" ), "mod1");
        info.addMethod(createFuncDef("mmmm" ), "mod1");
        
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
    
    public void testDependencyInfo() throws Exception {
        String analyzedModule = "mod1";
        String dependsOn = "mod2";
        info.addDependency(analyzedModule, dependsOn);
        
        //flat dependencies
        Set dependencies = info.getDependencies("mod1");
        assertEquals(1, dependencies.size());
        assertEquals("mod2", dependencies.iterator().next());
        
        Set<String> dependenciesOn = info.getModulesThatHaveDependenciesOn("mod2");
        assertEquals(1, dependenciesOn.size());
        assertEquals("mod1", dependenciesOn.iterator().next());
        

        //deep dependencies
        info.addDependency("mod2", "mod3");
        dependencies = info.getDependencies("mod1");
        assertEquals(2, dependencies.size());
        assertTrue(dependencies.contains("mod2"));
        assertTrue(dependencies.contains("mod3"));

        dependenciesOn = info.getModulesThatHaveDependenciesOn("mod3");
        assertEquals(2, dependenciesOn.size());
        assertTrue(dependenciesOn.contains("mod1"));
        assertTrue(dependenciesOn.contains("mod2"));
        
        
        //let's see how it goes with circular dependencies
        info.addDependency("mod3", "mod1");
        dependencies = info.getDependencies("mod1"); //does not return itself
        assertEquals(2, dependencies.size());
        assertTrue(dependencies.contains("mod2"));
        assertTrue(dependencies.contains("mod3"));
        

        dependenciesOn = info.getModulesThatHaveDependenciesOn("mod3"); //does not return itself
        assertEquals(2, dependenciesOn.size());
        assertTrue(dependenciesOn.contains("mod1"));
        assertTrue(dependenciesOn.contains("mod2"));

        info.removeInfoFromModule("mod2"); //the other modules should still depend on it, altough its information is lost
        dependencies = info.getDependencies("mod1"); //does return mod2, even though it does not exist anymore
        assertEquals(1, dependencies.size());
        
    }
}
