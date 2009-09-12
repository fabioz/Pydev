/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.visitors;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.refactoring.ast.visitors.rewriter.RewriterVisitor;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

import com.thoughtworks.xstream.XStream;

public class SelectionExtensionTestCase extends AbstractIOTestCase {

	public SelectionExtensionTestCase(String name) {
		super(name);
	}

	@Override
	public void runTest() throws Throwable {
		MockupSelectionConfig config = initConfig();

		RefactoringInfo info = setupInfo(config);
		runSelectionExtension(info);

		assertEquals(getExpected(), getGenerated());
	}

	private void runSelectionExtension(RefactoringInfo info) {
		StringBuilder buffer = new StringBuilder();
		try {
			String source = RewriterVisitor.createSourceFromAST(info.getParsedExtendedSelection().getASTParent(), "\n");
			buffer.append(source); // normalized source
		} catch (Throwable e) {
			buffer.append("# Invalid selection:\n");
			String[] lines = info.normalizeSourceSelection(info.getExtendedSelection()).split("\\n"); // normalized

			for (String line : lines) {
				if (line.trim().length() != 0) {
					buffer.append("# " + line + "\n");
				}
			}
		}

		setTestGenerated(buffer.toString());
	}

	private RefactoringInfo setupInfo(MockupSelectionConfig config) throws Throwable {
		IDocument doc = new Document(data.source);

		ITextSelection selection = new TextSelection(doc, data.sourceSelection.getOffset(), data.sourceSelection.getLength());
		RefactoringInfo info = new RefactoringInfo(doc, selection);

		return info;
	}

	private MockupSelectionConfig initConfig() {
		MockupSelectionConfig config = null;
		XStream xstream = new XStream();
		xstream.alias("config", MockupSelectionConfig.class);
		
		if (data.config.length() > 0) {
			config = (MockupSelectionConfig) xstream.fromXML(data.config);
		} else {
			config = new MockupSelectionConfig(0, 0, 0);
		}
		return config;
	}
}
