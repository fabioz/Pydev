/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.TestDependent;

/**
 * @author Fabio
 *
 */
public class OccurrencesAnalyzerTestOpenGL extends AnalysisTestsBase {

    public static void main(String[] args) {
        try {
            OccurrencesAnalyzerTestOpenGL analyzer2 = new OccurrencesAnalyzerTestOpenGL();
            analyzer2.setUp();
            analyzer2.testGlu();
            analyzer2.tearDown();
            System.out.println("finished");

            junit.textui.TestRunner.run(OccurrencesAnalyzerTestOpenGL.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

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
        if (TestDependent.PYTHON_OPENGL_PACKAGES != null) {
            doc = new Document("from OpenGL.GL import glPushMatrix\n" + "print glPushMatrix\n" + "");
            checkNoError();
        }

    }

    public void testGlu2() {
        if (TestDependent.PYTHON_OPENGL_PACKAGES != null) {
            doc = new Document("from OpenGL.GL import * #@UnusedWildImport\n" + "print glPushMatrix\n" + "");
            checkNoError();
        }

    }

    public void testGlu3() {
        if (TestDependent.PYTHON_OPENGL_PACKAGES != null) {
            doc = new Document("from OpenGL.GL import glRotatef\n" + "print glRotatef\n" + "");
            checkNoError();
        }

    }

    public void testGlu4() {
        if (TestDependent.PYTHON_OPENGL_PACKAGES != null) {
            doc = new Document("from OpenGL.GLU import gluLookAt\n" + "print gluLookAt" + "");
            checkNoError();
        }

    }
}
