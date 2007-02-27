package org.python.pydev.refactoring.tests.visitors;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.core.RefactoringInfo;
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
			String source = VisitorFactory.createSourceFromAST(info.getParsedExtendedSelection().getASTParent());
			buffer.append(source); // normalized source
		} catch (Throwable e) {
			buffer.append("# Invalid selection:\n");
			String[] lines = info.normalizeSourceSelection(info.getScopeAdapter(), info.getExtendedSelection()).split("\\n"); // normalized

			for (String line : lines) {
				if (line.trim().length() != 0)
					buffer.append("# " + line + "\n");
			}
		}

		setTestGenerated(buffer.toString());
	}

	private RefactoringInfo setupInfo(MockupSelectionConfig config) throws Throwable {
		IDocument doc = new Document(getSource());

		ITextSelection selection = new TextSelection(doc, config.getOffset(), config.getSelectionLength());
		RefactoringInfo info = new RefactoringInfo(null, doc, selection, null);
		selection = info.getExtendedSelection();

		selection = VisitorFactory.createSelectionExtension(info.getScopeAdapter(), selection);
		info = new RefactoringInfo(null, doc, selection, null);
		return info;
	}

	private MockupSelectionConfig initConfig() {
		MockupSelectionConfig config = null;
		XStream xstream = new XStream();
		xstream.alias("config", MockupSelectionConfig.class);
		if (getConfig().length() > 0) {
			config = (MockupSelectionConfig) xstream.fromXML(getConfig());
		} else {
			fail("Could not unserialize configuration");
		}
		return config;
	}

	@Override
	public String getExpected() {
		return getResult();
	}

}
