/*
 * Created on 01/10/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;

import com.python.pydev.analysis.AnalysisTestsBase;
import com.python.pydev.analysis.OcurrencesAnalyzer;
import com.python.pydev.analysis.additionalinfo.dependencies.PyStructuralChange;


public class DependencyInfoTest2 extends AnalysisTestsBase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(DependencyInfoTest2.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * The scenario is the following:
     * 
     * file1: from file2 import Test #direct dep
     * 
     * file2: class Test:pass
     * 
     * file3: from file1 import Test #no direct dep
     * 
     * file4: from file2 import * #no dep because no token was used
     * 
     * file5: from file2 import *; print Test #dep because tok was used
     * 
     * @throws Exception
     */
    public void testDependencyInfoGenerated() throws Exception {
        String[] files = new String[]{"file1", "file2", "file3", "file4", "file5"};
        analyzeFiles(files);

        //check if the data for the changes is in the information for the project
        AbstractAdditionalDependencyInfo infoForProject = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
        
        //make a change (delete the class Test from module 'file2'
        PyStructuralChange change = new PyStructuralChange();
        
        change.setModule("extendable.dependencies.file2");
        change.addRemovedToken("Test"); //nested tokens are not added to the removed tokens
        
        Set<String> recalculate = infoForProject.calculateDependencies(change);
        assertTrue(infoForProject.hasWildImportPath("extendable.dependencies.file5", "extendable.dependencies.file2"));
        assertTrue(recalculate.contains("extendable.dependencies.file1"));
        
        //the info we want can be found at the example at MessagesManager.addAdditionalInfoToUnusedWildImport
        assertTrue(recalculate.contains("extendable.dependencies.file5"));
        assertEquals(2, recalculate.size());
    }

    /**
     * @param files
     * @throws FileNotFoundException
     */
    protected void analyzeFiles(String[] files) throws FileNotFoundException {
        for (String string : files) {
            analyzer = new OcurrencesAnalyzer();
            //analyze the files involved
            File file = new File(TestDependent.TEST_PYSRC_LOC+"extendable/dependencies/"+string+".py");
            Document doc = new Document(REF.getFileContents(file));
            msgs = analyzer.analyzeDocument(nature, (SourceModule) AbstractModule.createModule("extendable.dependencies."+string, file, nature, 0), prefs, doc);
            
        }
    }


}
