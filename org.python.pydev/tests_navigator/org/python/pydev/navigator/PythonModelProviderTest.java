package org.python.pydev.navigator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.navigator.elements.IWrappedResource;
import org.python.pydev.navigator.elements.PythonSourceFolder;
import org.python.pydev.plugin.nature.PythonNature;

public class PythonModelProviderTest extends TestCase {


    public static void main(String[] args) {
        try {
            PythonModelProviderTest test = new PythonModelProviderTest();
            test.setUp();
            test.testProjectIsRoot();
            test.tearDown();
            
            junit.textui.TestRunner.run(PythonModelProviderTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

	private ProjectStub project;
	private FileStub file;
	private PythonModelProvider provider;

	/**
	 * Test if intercepting an add deep within the pythonpath structure will correctly return an object
	 * from the python model. 
	 */
    public void testInterceptAdd() throws Exception {
    	PythonNature nature = createNature(TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot/source/python");
    	
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
    
    /**
     * Test if setting the project root as a source folder will return an object from the python model.
     */
    public void testProjectIsRoot() throws Exception {
        String pythonpathLoc = TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot";
        final HashSet<String> pythonPathSet = new HashSet<String>();
        pythonPathSet.add(pythonpathLoc);

        PythonNature nature = createNature(pythonPathSet);
        
        WorkspaceRootStub workspaceRootStub = new WorkspaceRootStub();
        project = new ProjectStub(new File(pythonpathLoc), nature);
        provider = new PythonModelProvider();
        
        workspaceRootStub.addChild(project);
        project.setParent(workspaceRootStub);
        
        Object[] children1 = provider.getChildren(workspaceRootStub);
        assertEquals(1, children1.length);
        assertTrue("Expecting source folder. Received: "+children1[0].getClass().getName(), children1[0] instanceof PythonSourceFolder);
        
        //now, let's go and change the pythonpath location to a folder within the project and see if it changes...
        pythonPathSet.clear();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot/source/python");
        IResource refreshObject = provider.internalDoNotifyPythonPathRebuilt(project, new ArrayList<String>(pythonPathSet));
        assertTrue("Expecting the refresh object to be the root and not the project", refreshObject instanceof IWorkspaceRoot);
        
        children1 = provider.getChildren(workspaceRootStub);
        assertEquals(1, children1.length);
        assertTrue("Expecting IProject. Received: "+children1[0].getClass().getName(), children1[0] instanceof IProject);

        //set to be the root again
        pythonPathSet.clear();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot");
        refreshObject = provider.internalDoNotifyPythonPathRebuilt(project, new ArrayList<String>(pythonPathSet));
        assertTrue("Expecting the refresh object to be the root and not the project", refreshObject instanceof IWorkspaceRoot);
    }

    /**
     * Creates a nature that has the passed pythonpath location in its pythonpath.
     */
    private PythonNature createNature(String pythonpathLoc) {
        final HashSet<String> pythonPathSet = new HashSet<String>();
        pythonPathSet.add(pythonpathLoc);
        return createNature(pythonPathSet);
    }
    
    /**
     * Creates a nature that has the given set as its underlying pythonpath paths. The reference
     * is kept inside as a reference, so, changing that reference will affect the pythonpath
     * that is set in the nature.
     */
    private PythonNature createNature(final HashSet<String> pythonPathSet) {
        
        PythonNature nature = new PythonNature(){
            @Override
            public IPythonPathNature getPythonPathNature() {
                return new PythonPathNatureStub(pythonPathSet);
            }
        };
        return nature;
    }
    
    /**
     * Test if changing the pythonpath has the desired effects in the python model.
     */
    public void testPythonpathChanges() throws Exception {
        final HashSet<String> pythonPathSet = new HashSet<String>();
        pythonPathSet.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot/source");
    	PythonNature nature = createNature(pythonPathSet);
    	
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
