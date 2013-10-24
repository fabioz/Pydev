/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.TestDependent;
import org.python.pydev.shared_core.SharedCorePlugin;

/**
 * @author Fabio
 *
 */
public class OccurrencesAnalyzerOpenGLTest extends AnalysisTestsBase {

    public static void main(String[] args) {
        try {
            OccurrencesAnalyzerOpenGLTest analyzer2 = new OccurrencesAnalyzerOpenGLTest();
            analyzer2.setUp();
            analyzer2.testGlu();
            analyzer2.tearDown();
            System.out.println("finished");

            junit.textui.TestRunner.run(OccurrencesAnalyzerOpenGLTest.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (TestDependent.PYTHON_OPENGL_PACKAGES == null) {
            restorePythonPathWithSitePackages(false);
        } else {
            restorePythonPathWithCustomSystemPath(false, TestDependent.GetCompletePythonLib(true) + "|"
                    + TestDependent.PYTHON_OPENGL_PACKAGES);
        }
    }

    public void testGlu() {
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }
        if (TestDependent.PYTHON_OPENGL_PACKAGES != null) {
            doc = new Document("from OpenGL.GL import glPushMatrix\n" + "print glPushMatrix\n" + "");
            checkNoError();
        }

    }

    public void testGlu2() {
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        if (TestDependent.PYTHON_OPENGL_PACKAGES != null) {
            doc = new Document("from OpenGL.GL import * #@UnusedWildImport\n" + "print glPushMatrix\n" + "");
            checkNoError();
        }

    }

    public void testGlu3() {
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        if (TestDependent.PYTHON_OPENGL_PACKAGES != null) {
            doc = new Document("from OpenGL.GL import glRotatef\n" + "print glRotatef\n" + "");
            checkNoError();
        }

    }

    public void testGlu4() {
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        if (TestDependent.PYTHON_OPENGL_PACKAGES != null) {
            doc = new Document("from OpenGL.GLU import gluLookAt\n" + "print gluLookAt" + "");
            checkNoError();
        }

    }
}
