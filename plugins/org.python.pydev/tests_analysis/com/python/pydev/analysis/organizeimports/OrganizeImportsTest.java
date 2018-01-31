/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 25/09/2005
 */
package com.python.pydev.analysis.organizeimports;

import org.eclipse.jface.text.Document;
import org.python.pydev.editor.codefolding.MarkerAnnotationAndPosition;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;

public class OrganizeImportsTest extends AdditionalInfoTestsBase {

    private OrganizeImports organizer;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(OrganizeImportsTest.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        organizer = new OrganizeImports();
    }

    public void testOrganizeImports() throws Exception {
        String s = "import xxx"; //unused import
        Document document = new Document(s);
        MarkerAnnotationAndPosition stub = createMarkerStub(0, s.length(), IAnalysisPreferences.TYPE_UNUSED_IMPORT);

        //        organizer.performArrangeImports(new PySelection(document), stub);

    }
}
