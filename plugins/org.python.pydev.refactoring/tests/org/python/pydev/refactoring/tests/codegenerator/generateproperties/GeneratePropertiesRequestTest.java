package org.python.pydev.refactoring.tests.codegenerator.generateproperties;

import org.python.pydev.plugin.preferences.PyCodeStylePreferencesPage;
import org.python.pydev.refactoring.codegenerator.generateproperties.request.GeneratePropertiesRequest;

import junit.framework.TestCase;

public class GeneratePropertiesRequestTest extends TestCase {

    public static void main(String[] args) {
        try {
            GeneratePropertiesRequestTest test = new GeneratePropertiesRequestTest();
            test.setUp();
            test.testCodingStd();
            test.tearDown();
            junit.textui.TestRunner.run(GeneratePropertiesRequestTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testCodingStd() {
        PyCodeStylePreferencesPage.TESTING_METHOD_FORMAT = PyCodeStylePreferencesPage.METHODS_FORMAT_CAMELCASE_FIRST_LOWER;
        assertEquals("delMyAttr", GeneratePropertiesRequest.getAccessorName("del", "my_attr"));

        PyCodeStylePreferencesPage.TESTING_METHOD_FORMAT = PyCodeStylePreferencesPage.METHODS_FORMAT_CAMELCASE_FIRST_UPPER;
        assertEquals("DelMyAttr", GeneratePropertiesRequest.getAccessorName("del", "my_attr"));

        PyCodeStylePreferencesPage.TESTING_METHOD_FORMAT = PyCodeStylePreferencesPage.METHODS_FORMAT_UNDERSCORE_SEPARATED;
        assertEquals("del_my_attr", GeneratePropertiesRequest.getAccessorName("del", "my_attr"));

    }

}
