/*******************************************************************************
 * Copyright (c) 2008 Syntax Consulting, Inc. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.python.pydev.runalltests2;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

/**
 * This class allows you harvest unit tests from resolved bundles based on
 * filters you supply. It can harvest tests from both bundles and fragments, and
 * can also be used during automated builds using the Eclipse Testing Framework.
 * <p>
 * This class is similar to the JUnit TestCollector class, except that it takes
 * responsibility for both loading the classes and adding them to the test
 * suite. The collector must load the classes using the appropriate bundle
 * classloader for each test, so this work cannot be done in the suite.
 * <p>
 * To use this collector, simply create a JUnit test suite with a method like
 * this:
 * <p>
 * 
 * <pre>
 * 
 * 
 * public static Test suite() {
 *   TestSuite suite = new TestSuite( &quot;All Tests&quot; );
 *   PluginTestCollector testCollector = new PluginTestCollector();
 *   testCollector.collectTests( suite,
 *                               &quot;com.rcpquickstart.&quot;,
 *                               &quot;com.rcpquickstart.mypackage.&quot;,
 *                               &quot;*Test&quot; );
 *   return suite;
 * }
 * </pre>
 * <p>
 * Comments and suggestions can be sent to patrick@rcpquickstart.com.
 * 
 * This is a modified version that has some more assertions (bundles that match the name filter must have tests) and
 * works with JUnit4 tests. The test suite is still JUnit 3 style though.
 * 
 * @author Patrick Paulin
 * @author Matthias Kempka
 */
public class BundleTestCollector {

    private final class TestCollectionFailure implements Test {

        private final String message;

        private TestCollectionFailure(final String message) {
            this.message = message;
        }

        public void run(final TestResult result) {
            result.addError(this, new IllegalArgumentException(message));
        }

        public int countTestCases() {
            return 1;
        }
    }

    private static final String PLUGIN_ID = "com.python.pydev.runalltests";

    /**
     * Create a list of test classes for the bundles currently resolved by the
     * framework. This method works with JUnit 3.x test cases only, meaning that
     * it searches for classes that subclass the TestCase class.
     * 
     * @param suite to which tests should be added
     * @param bundleRoot root string that a bundle id needs to start with in order
     *          for the bundle to be included in the search
     * @param packageRoot root string that a package needs to start with in order
     *          for the package to be included in the search
     * @param testClassFilter filter string that will be used to search for test
     *          cases. The filter applies to the unqualified class name only (not
     *          including the package name). Wildcards are allowed, as defined by
     *          the {@link Activator Bundle#findEntries(String, String, boolean)}
     *          method.
     * @return list of test classes that match the roots and filter passed in
     */
    public void collectTests(final TestSuite suite,
            final String bundleRoot,
            final String packageRoot,
            final String testClassFilter)
    {
        try {
            if ("".equals(packageRoot)) {
                throw new IllegalArgumentException("Must specify a package root");
            }
            Bundle[] bundles = Activator.getDefault().getBundle().getBundleContext().getBundles();
            boolean found = false;
            for (Bundle bundle : bundles) {
                if (!isFragment(bundle)
                        && bundle.getSymbolicName().startsWith(bundleRoot))
                {
                    List<Class> testClasses = getTestClasesInBundle(bundle,
                            packageRoot,
                            testClassFilter);
                    String msg = "Found "
                            + testClasses.size()
                            + " tests in "
                            + bundle.getSymbolicName();
                    IStatus status = new Status(IStatus.INFO, Activator.getDefault().getBundle().getSymbolicName(), msg);
                    Activator.getDefault().getLog().log(status);
                    for (Class clazz : testClasses) {
                        //            suite.addTestSuite( clazz ); // that works only with JUnit 3
                        suite.addTest(new JUnit4TestAdapter(clazz)); // this works for both
                    }
                    found = true;
                }
            }
            if (!found) {
                suite.addTest(new TestCollectionFailure("No Bundle found starting with "
                        + bundleRoot));
            }
        } catch (final Exception caught) {
            suite.addTest(new Test() {

                public void run(TestResult result) {
                    result.addError(this, caught);
                }

                public int countTestCases() {
                    return 1;
                }
            });
        }
    }

    private List<Class> getTestClasesInBundle(final Bundle bundle,
            final String packageRoot,
            final String testClassFilter)
    {
        List<Class> testClassesInBundle = new ArrayList<Class>();
        Enumeration testClassNames = bundle.findEntries("/", testClassFilter + ".class", true); //$NON-NLS-1$
        if (testClassNames != null) {
            while (testClassNames.hasMoreElements()) {
                /*
                 * Take relative path produced by findEntries method and convert it into
                 * a properly formatted class name. The package root is used to
                 * determine the start of the qualified class name in the path.
                 */
                String testClassPath = ((URL) testClassNames.nextElement()).getPath();
                testClassPath = testClassPath.replace('/', '.');
                int packageRootStart = testClassPath.indexOf(packageRoot);
                /* if class does not begin with package root, just ignore it */
                if (packageRootStart == -1) {
                    continue;
                }
                String testClassName = testClassPath.substring(packageRootStart);
                testClassName = testClassName.substring(0, testClassName.length()
                        - ".class".length()); //$NON-NLS-1$
                /* Attempt to load the class using the bundle classloader. */
                Class testClass = null;
                try {
                    testClass = bundle.loadClass(testClassName);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("Could not load class: " //$NON-NLS-1$
                            + testClassName, e);
                }
                /*
                 * If the class is not abstract, add it to list
                 */
                if (!Modifier.isAbstract(testClass.getModifiers())) {
                    testClassesInBundle.add(testClass);
                }
            }
        }
        return testClassesInBundle;
    }

    private boolean isFragment(final Bundle bundle) {
        Enumeration headerKeys = bundle.getHeaders().keys();
        boolean result = false;
        while (headerKeys.hasMoreElements()) {
            if (headerKeys.nextElement().toString().equals("Fragment-Host")) { //$NON-NLS-1$
                result = true;
            }
        }
        return result;
    }
}
