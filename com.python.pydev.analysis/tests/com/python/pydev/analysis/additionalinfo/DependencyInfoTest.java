/*
 * Created on 01/10/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.util.Set;

import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;

import com.python.pydev.analysis.AnalysisTestsBase;


public class DependencyInfoTest extends AnalysisTestsBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DependencyInfoTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testDependencyInfoGenerated() throws Exception {
        String file = TestDependent.TEST_PYSRC_LOC+"testOtherImports/f1.py";
        String file2 = TestDependent.TEST_PYSRC_LOC+"testOtherImports/f2.py";
        
        //it is dependent on f2...
        analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModule("testOtherImports.f1", new File(file), nature, 0), prefs);
        analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModule("testOtherImports.f2", new File(file2), nature, 0), prefs);

        //check if the dependency info was correctly generated
        AbstractAdditionalDependencyInfo infoForProject = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());

        Set<String> dependencies = infoForProject.getDependencies("testOtherImports.f1");
        assertEquals(1, dependencies.size());
        assertEquals("testOtherImports.f2", dependencies.iterator().next());
        
        Set<String> dependenciesOn = infoForProject.getModulesThatHaveDependenciesOn("testOtherImports.f2");
        assertEquals(1, dependenciesOn.size());
        assertEquals("testOtherImports.f1", dependenciesOn.iterator().next());
    }

}
