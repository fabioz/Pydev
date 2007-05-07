package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.TestDependent;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.stmtType;

import com.python.pydev.analysis.additionalinfo.AdditionalInfoIntegrityChecker.IntegrityInfo;

/**
 * TODO: Still doesn't check:
 * 1. additional info -- where we also have to check if a module was changed (and the cache is old)
 * 
 * @author Fabio
 *
 */
public class AdditionalInfoIntegrityCheckerTest extends AdditionalInfoTestsBase {
    
    private static final String MOD_NAME = "extendable.initially_not_existant";
    
    private IProgressMonitor monitor = new NullProgressMonitor();
    
    public static void main(String[] args) {
        try {
            AdditionalInfoIntegrityCheckerTest test = new AdditionalInfoIntegrityCheckerTest();
            test.setUp();
            test.testIntegrityFileHasNoMemory();
            test.tearDown();

            junit.textui.TestRunner.run(AdditionalInfoIntegrityCheckerTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testIntegrityInModuleHasNoFile() {
        IntegrityInfo info = AdditionalInfoIntegrityChecker.checkIntegrity(nature, monitor);
        assertTrue(info.desc.toString(), info.allOk);
        
        addFooModule(new Module(new stmtType[0]));
        info = AdditionalInfoIntegrityChecker.checkIntegrity(nature, monitor);
        assertFalse(info.allOk);
        assertEquals(1, info.modulesNotInDisk.size());
        assertEquals(info.modulesNotInDisk.get(0), new ModulesKey("foo", null));
        
        fixAndCheckAllOk(info);
    }

    public void testIntegrityFileHasNoMemory() throws IOException {
        File file = new File(TestDependent.TEST_PYSRC_LOC+"extendable/initially_not_existant.py");
        file.createNewFile();
        
        try{
            IntegrityInfo info = AdditionalInfoIntegrityChecker.checkIntegrity(nature, monitor);
            assertFalse(info.allOk);
            assertEquals(1, info.modulesNotInMemory.size());
            assertEquals(info.modulesNotInMemory.get(0), new ModulesKey(MOD_NAME, file));

            fixAndCheckAllOk(info);
        }finally{
            file.delete();
        }
        
        IntegrityInfo info = AdditionalInfoIntegrityChecker.checkIntegrity(nature, monitor);
        assertFalse(info.allOk);
        assertEquals(1, info.modulesNotInDisk.size());
        assertEquals(info.modulesNotInDisk.get(0), new ModulesKey(MOD_NAME, null));

        fixAndCheckAllOk(info);
        
    }

    private void fixAndCheckAllOk(IntegrityInfo info) {
        AdditionalInfoIntegrityChecker.fix(info);
        info = AdditionalInfoIntegrityChecker.checkIntegrity(nature, monitor);
        assertTrue(info.desc.toString(), info.allOk);
    }
    

}
