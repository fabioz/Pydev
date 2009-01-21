/*
 * Created on Oct 28, 2006
 * @author Fabio
 */
package org.python.pydev.plugin.nature;

import org.python.pydev.editor.actions.PySelectionTest;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.BundleInfoStub;

import junit.framework.TestCase;

public class PythonNatureStoreTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PythonNatureStoreTest.class);
    }


    private String contents1= 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"+
        "<?eclipse-pydev version=\"1.0\"?>\r\n"+
        "\r\n" +
        "<pydev_project>\r\n" +
        "<pydev_property name=\"plugin_id.PYTHON_PROJECT_VERSION\">python 2.5</pydev_property>\r\n"+
        "<pydev_pathproperty name=\"plugin_id.PROJECT_SOURCE_PATH\">\r\n"+
        "<path>/test</path>\r\n"+
        "</pydev_pathproperty>\r\n"+
        "<pydev_pathproperty name=\"plugin_id.PROJECT_EXTERNAL_SOURCE_PATH\">\r\n"+
        "<path/>\r\n"+
        "</pydev_pathproperty>\r\n"+
        "</pydev_project>\r\n"+
        "";
    
    private String contents2= 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"+
        "<?eclipse-pydev version=\"1.0\"?>\r\n"+
        "\r\n" +
        "<pydev_project>\r\n"+
        "<pydev_property name=\"plugin_id.PYTHON_PROJECT_VERSION\">python 2.5</pydev_property>\r\n"+
        "<pydev_pathproperty name=\"plugin_id.PROJECT_SOURCE_PATH\">\r\n"+
        "<path>/test/foo</path>\r\n"+
        "<path>/bar/kkk</path>\r\n"+
        "</pydev_pathproperty>\r\n"+
        "<pydev_pathproperty name=\"plugin_id.PROJECT_EXTERNAL_SOURCE_PATH\">\r\n"+
        "<path/>\r\n"+
        "</pydev_pathproperty>\r\n"+
        "</pydev_project>\r\n"+
        "";
    
    protected void setUp() throws Exception {
        super.setUp();
        ProjectModulesManager.IN_TESTS = true;
        PydevPlugin.setBundleInfo(new BundleInfoStub());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }


    public void testLoad() throws Exception {
        PythonNatureStore store = new PythonNatureStore();
        ProjectStub2 projectStub2 = new ProjectStub2("test");
        
        //when setting the project, a side-effect must be that we create the xml file if it still does not exist
        store.setProject(projectStub2);

        //check the contents
        String strContents = store.getLastLoadedContents();
        PySelectionTest.checkStrEquals(contents1, strContents.replaceFirst(" standalone=\"no\"", "")); //depending on the java version, standalone="no" may be generated
            
        //in ProjectStub2, the initial setting is /test (see the getPersistentProperty)
        assertEquals("/test", store.getPathProperty(PythonPathNature.getProjectSourcePathQualifiedName()));
        store.setPathProperty(PythonPathNature.getProjectSourcePathQualifiedName(), "/test/foo|/bar/kkk");
        assertEquals("/test/foo|/bar/kkk", store.getPathProperty(PythonPathNature.getProjectSourcePathQualifiedName()));
        
        strContents = store.getLastLoadedContents();
        PySelectionTest.checkStrEquals(contents2, strContents.replaceFirst(" standalone=\"no\"", "")); //depending on the java version, standalone="no" may be generated
        assertEquals("", store.getPathProperty(PythonPathNature.getProjectExternalSourcePathQualifiedName()));
    }
}
