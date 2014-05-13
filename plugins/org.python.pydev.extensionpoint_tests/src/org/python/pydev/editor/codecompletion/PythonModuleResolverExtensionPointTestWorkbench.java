/*******************************************************************************
 * Copyright (c) 2014 Google, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Danny Yoo (Google) - initial API and implementation
 *******************************************************************************/
package org.python.pydev.editor.codecompletion;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.revisited.ModulesFoundStructure;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;

/**
 * Tests the org.python.pydev.pydev_python_module_resolver extension point.
 * <p>
 * These tests must be executed in a JUnit Plug-in Test context.
 */
public class PythonModuleResolverExtensionPointTestWorkbench extends TestCase {
    @Override
    public void tearDown() throws Exception {
        MockPythonModuleResolver.setTestDelegate(null);
        super.tearDown();
    }

    /**
     * Checks to see that module resolver extensions can be used in resolveModule().
     */
    public void testResolvePathWithResolver() {
        PythonPathHelper helper = new PythonPathHelper();
        String path = TestDependent.GetCompletePythonLib(true) + "|" + TestDependent.TEST_PYSRC_LOC;
        helper.setPythonPath(path);
        final IPath stubbedModulePath1 = Path.fromOSString("/this/is/a/path/to/a/file1.py");
        final IPath stubbedModulePath2 = Path.fromOSString("/this/is/a/path/to/a/file2.py");
        final IPath stubbedNegativeCase = Path.fromOSString("/this/is/a/path/to/another/file.py");
        setTestingModuleResolver(new IPythonModuleResolver() {
            @Override
            public String resolveModule(IProject project, IPath moduleLocation,
                    List<IPath> baseLocations) {
                if (moduleLocation.equals(stubbedModulePath1)) {
                    return "stubbed.library";
                }
                if (moduleLocation.equals(stubbedModulePath2)) {
                    return "stubbed.second_library";
                }
                if (moduleLocation.equals(stubbedNegativeCase)) {
                    return "";
                }
                // Otherwise, delegate.
                return null;
            }

            @Override
            public Collection<IPath> findAllModules(IProject project, IProgressMonitor monitor) {
                throw new UnsupportedOperationException();
            }
        });
        // Check normal resolution:
        assertEquals("stubbed.library",
                helper.resolveModule(stubbedModulePath1.toOSString()));
        assertEquals("stubbed.second_library",
                helper.resolveModule(stubbedModulePath2.toOSString()));

        // Check to see that delegation also works:
        assertEquals("unittest", helper.resolveModule(TestDependent.PYTHON_LIB + "unittest.py"));

        // Check the negative case:
        assertNull(helper.resolveModule(stubbedNegativeCase.toOSString()));
    }

    /**
     * Checks that participants can explicitly curtail the results collected by
     * {@link PythonPathHelper#getModulesFoundStructure(IProject, IProgressMonitor)}.
     */
    public void testGetModulesFoundStructureWithResolver() {
        PythonPathHelper helper = new PythonPathHelper();
        String path = TestDependent.GetCompletePythonLib(true) + "|" + TestDependent.TEST_PYSRC_LOC;
        helper.setPythonPath(path);
        setTestingModuleResolver(new IPythonModuleResolver() {
            @Override
            public String resolveModule(IProject project, IPath moduleLocation,
                    List<IPath> baseLocations) {
                if (moduleLocation.equals(Path.fromOSString("/root/x/y.py"))) {
                    return "x.y";
                }
                return null;
            }

            @Override
            public Collection<IPath> findAllModules(IProject project, IProgressMonitor monitor) {
                List<IPath> modules = new ArrayList<>();
                modules.add(Path.fromOSString("/root/x/y.py"));
                return modules;
            }
        });

        ModulesFoundStructure modulesFoundStructure = helper.getModulesFoundStructure(null, null);
        Map<File, String> regularModules = modulesFoundStructure.regularModules;
        assertEquals(1, regularModules.keySet().size());
        assertEquals(new File("/root/x/y.py"), regularModules.keySet().iterator().next());
        assertEquals("x.y", regularModules.values().iterator().next());
    }

    private void setTestingModuleResolver(IPythonModuleResolver testDelegate) {
        MockPythonModuleResolver.setTestDelegate(testDelegate);
    }
}
