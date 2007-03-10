package org.python.pydev.navigator;

import java.io.File;
import java.util.HashSet;

import junit.framework.TestCase;

import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.TestDependent;
import org.python.pydev.plugin.nature.PythonNature;

public class PythonModelProviderTest extends TestCase {

    public void testIt() throws Exception {
        PythonNature nature = new PythonNature(){
            @Override
            public IPythonPathNature getPythonPathNature() {
                HashSet<String> set = new HashSet<String>();
                set.add(TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot/source/python");
                return new PythonPathNatureStub(set);
            }
        };
        
        ProjectStub project = new ProjectStub(new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot"), nature);
        FileStub file = new FileStub(project, new File(TestDependent.TEST_PYSRC_NAVIGATOR_LOC+"projroot/source/python/pack1/pack2/mod2.py"));
        
        
        PythonModelProvider provider = new PythonModelProvider();
        HashSet<Object> files = new HashSet<Object>();
        files.add(file);
        provider.interceptAdd(new PipelinedShapeModification(file.getParent(), files));
        assertEquals(1, files.size());
        Object wrappedResource = files.iterator().next();
        assertTrue(wrappedResource instanceof IWrappedResource);
    }
}
