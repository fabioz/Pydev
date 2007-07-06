package org.python.pydev.navigator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import junit.framework.TestCase;

import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.plugin.nature.PythonNature;

public class PythonModelProviderTest extends TestCase {


    public static void main(String[] args) {
        try {
            PythonModelProviderTest test = new PythonModelProviderTest();
            test.setUp();
            test.testIt2();
            test.tearDown();
            
//            junit.textui.TestRunner.run(PythonModelProviderTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

	private ProjectStub project;
	private FileStub file;
	private PythonModelProvider provider;

	
    public void testIt() throws Exception {
    	PythonNature nature = new PythonNature(){
    		@Override
    		public IPythonPathNature getPythonPathNature() {
    			HashSet<String> set = new HashSet<String>();
    			set.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot/source/python");
    			return new PythonPathNatureStub(set);
    		}
    	};
    	
    	project = new ProjectStub(new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot"), nature);
    	file = new FileStub(project, new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot/source/python/pack1/pack2/mod2.py"));
    	provider = new PythonModelProvider();
    	
    	
        HashSet<Object> files = new HashSet<Object>();
        files.add(file);
        provider.interceptAdd(new PipelinedShapeModification(file.getParent(), files));
        assertEquals(1, files.size());
        Object wrappedResource = files.iterator().next();
        assertTrue(wrappedResource instanceof IWrappedResource);
    }
    
    public void testIt2() throws Exception {
    	final HashSet<String> pythonPathSet = new HashSet<String>();
    	pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot/source");
    	
    	PythonNature nature = new PythonNature(){
    		@Override
    		public IPythonPathNature getPythonPathNature() {
    			return new PythonPathNatureStub(pythonPathSet);
    		}
    	};
    	
    	project = new ProjectStub(new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot"), nature);
    	provider = new PythonModelProvider();
		Object[] children1 = provider.getChildren(project);
		assertTrue(children1[0] instanceof PythonSourceFolder);
		
		//no changes in the pythonpath
		provider.notifyPythonPathRebuilt(project, new ArrayList<String>(pythonPathSet));//still the same
		
		Object[] children2 = provider.getChildren(project);
		assertEquals(1, children1.length);
		assertEquals(1, children2.length);
		assertSame(children1[0], children2[0]);
		
		//changed pythonpath (source folders should be removed)
		pythonPathSet.clear();
		pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot/source/python");
		provider.notifyPythonPathRebuilt(project, new ArrayList<String>(pythonPathSet));
		Object[] children3 = provider.getChildren(project);
		assertFalse(children3[0] instanceof PythonSourceFolder);
		
		//restore initial
		pythonPathSet.clear();
		pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot/source");
		Object[] children4 = provider.getChildren(project);
		assertTrue(children4[0] instanceof PythonSourceFolder);
		assertNotSame(children1[0], children4[0]); //because it was removed
		
	}
}
