/******************************************************************************
* Copyright (C) 2007-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.tests.coderefactoring.extractlocal;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.refactoring.coderefactoring.extractlocal.ExtractLocalRefactoring;
import org.python.pydev.refactoring.coderefactoring.extractlocal.ExtractLocalRequestProcessor;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;
import org.python.pydev.shared_core.io.FileUtils;

public class ExtractLocalTestCase extends AbstractIOTestCase {

    public ExtractLocalTestCase(String name) {
        super(name);
    }

    @Override
    public void runTest() throws Throwable {
        FileUtils.IN_TESTS = true;

        IDocument document = new Document(data.source);
        ITextSelection selection = new TextSelection(document, data.sourceSelection.getOffset(),
                data.sourceSelection.getLength());
        IGrammarVersionProvider versionProvider = createVersionProvider();
        RefactoringInfo info = new RefactoringInfo(document, selection, versionProvider);
        ExtractLocalRefactoring refactoring = new ExtractLocalRefactoring(info);

        ExtractLocalRequestProcessor requestProcessor = refactoring.getRequestProcessor();
        requestProcessor.setVariableName("extracted_variable");
        requestProcessor.setReplaceDuplicates(true);

        NullProgressMonitor monitor = new NullProgressMonitor();
        RefactoringStatus result = refactoring.checkAllConditions(monitor);

        assertTrue("Refactoring is not ok: " + result.getMessageMatchingSeverity(RefactoringStatus.WARNING),
                result.isOK());

        Change change = refactoring.createChange(monitor);
        change.perform(monitor);

        assertContentsEqual(data.result, document.get());

        FileUtils.IN_TESTS = false;
    }

}
