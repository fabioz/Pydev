/* 
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.tests.codegenerator.generatedocstring;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.refactoring.codegenerator.generatedocstring.GenerateDocstringOperation;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

public class GenerateDocstringTestCase extends AbstractIOTestCase {

	public GenerateDocstringTestCase(String name) {
		super(name);
	}
	
	@Override
	public void runTest() throws Throwable {
		IDocument document = new Document(data.source);
		TextSelection selection = new TextSelection(document, data.sourceSelection.getOffset(), data.sourceSelection.getLength());
		TextEditorStub editor = new TextEditorStub(document, selection);
		GenerateDocstringOperation operation = new GenerateDocstringOperation(editor);
		
		operation.run(new NullProgressMonitor());

		setTestGenerated(document.get());
		assertEquals(data.result, getGenerated());
		
		ITextSelection expected = data.resultSelection;
		assertEquals(expected.getOffset(), editor.getSelectionOffset());
		assertEquals(expected.getLength(), editor.getSelectionLength());
	}

}
