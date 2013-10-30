/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 28, 2006
 * @author Fabio
 */
package org.python.pydev.plugin.nature;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.python.pydev.editor.actions.PySelectionTest;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.ui.BundleInfoStub;

public class PythonNatureStoreTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PythonNatureStoreTest.class);
    }

    private String contents1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
            +
            "<?eclipse-pydev version=\"1.0\"?>\r\n"
            +
            "\r\n"
            +
            "<pydev_project>\r\n"
            +
            "<pydev_property name=\"PyDevPluginID(null plugin).PYTHON_PROJECT_VERSION\">python 2.5</pydev_property>\r\n"
            +
            "<pydev_pathproperty name=\"PyDevPluginID(null plugin).PROJECT_SOURCE_PATH\">\r\n"
            +
            "<path>/test</path>\r\n" +
            "</pydev_pathproperty>\r\n"
            +
            "<pydev_pathproperty name=\"PyDevPluginID(null plugin).PROJECT_EXTERNAL_SOURCE_PATH\"/>\r\n"
            +
            "</pydev_project>\r\n" +
            "";

    private String contents2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
            +
            "<?eclipse-pydev version=\"1.0\"?>\r\n"
            +
            "\r\n"
            +
            "<pydev_project>\r\n"
            +
            "<pydev_property name=\"PyDevPluginID(null plugin).PYTHON_PROJECT_VERSION\">python 2.5</pydev_property>\r\n"
            +
            "<pydev_pathproperty name=\"PyDevPluginID(null plugin).PROJECT_SOURCE_PATH\">\r\n"
            +
            "<path>/test/foo</path>\r\n" +
            "<path>/bar/kkk</path>\r\n" +
            "</pydev_pathproperty>\r\n"
            +
            "<pydev_pathproperty name=\"PyDevPluginID(null plugin).PROJECT_EXTERNAL_SOURCE_PATH\"/>\r\n"
            +
            "</pydev_project>\r\n" +
            "";

    private String contents3 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
            +
            "<?eclipse-pydev version=\"1.0\"?>\r\n"
            +
            "\r\n"
            +
            "<pydev_project>\r\n"
            +
            "<pydev_property name=\"PyDevPluginID(null plugin).PYTHON_PROJECT_VERSION\">python 2.5</pydev_property>\r\n"
            +
            "<pydev_pathproperty name=\"PyDevPluginID(null plugin).PROJECT_SOURCE_PATH\">\r\n"
            +
            "<path>/test/foo</path>\r\n" +
            "<path>/bar/kkk</path>\r\n" +
            "</pydev_pathproperty>\r\n"
            +
            "<pydev_pathproperty name=\"PyDevPluginID(null plugin).PROJECT_EXTERNAL_SOURCE_PATH\"/>\r\n"
            +
            "<pydev_variables_property name=\"PyDevPluginID(null plugin).PROJECT_VARIABLE_SUBSTITUTION\">\r\n"
            +
            "<key>MY_KEY</key>\r\n" +
            "<value>MY_VALUE</value>\r\n" +
            "</pydev_variables_property>\r\n"
            +
            "</pydev_project>\r\n" +
            "";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ProjectModulesManager.IN_TESTS = true;
        PydevPlugin.setBundleInfo(new BundleInfoStub());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testLoad() throws Exception {
        // This test fails because of whitespace comparison problems. It may be better to
        // use something like XMLUnit to compare the two XML files?
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

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
        assertNull(store.getPathProperty(PythonPathNature.getProjectExternalSourcePathQualifiedName()));

        Map<String, String> map = new HashMap<String, String>();
        map.put("MY_KEY", "MY_VALUE");
        store.setMapProperty(PythonPathNature.getProjectVariableSubstitutionQualifiedName(), map);

        strContents = store.getLastLoadedContents();
        PySelectionTest.checkStrEquals(contents3, strContents.replaceFirst(" standalone=\"no\"", "")); //depending on the java version, standalone="no" may be generated
        assertEquals(map, store.getMapProperty(PythonPathNature.getProjectVariableSubstitutionQualifiedName()));
    }
}
