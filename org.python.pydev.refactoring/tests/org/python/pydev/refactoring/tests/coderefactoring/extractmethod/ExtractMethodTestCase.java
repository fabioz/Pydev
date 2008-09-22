/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.coderefactoring.extractmethod;

import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.text.edits.MultiTextEdit;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.coderefactoring.extractmethod.edit.ExtractCallEdit;
import org.python.pydev.refactoring.coderefactoring.extractmethod.edit.ExtractMethodEdit;
import org.python.pydev.refactoring.coderefactoring.extractmethod.edit.ParameterReturnDeduce;
import org.python.pydev.refactoring.coderefactoring.extractmethod.request.ExtractMethodRequest;
import org.python.pydev.refactoring.core.RefactoringInfo;
import org.python.pydev.refactoring.tests.adapter.PythonNatureStub;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

import com.thoughtworks.xstream.XStream;

public class ExtractMethodTestCase extends AbstractIOTestCase {

	public ExtractMethodTestCase(String name) {
		super(name);
	}

	@Override
	public void runTest() throws Throwable {
	    REF.IN_TESTS = true;
		MockupExtractMethodConfig config = initConfig();

		IDocument doc = new Document(getSource());
		Module astModule = VisitorFactory.getRootNode(doc);
		String name = getFile().getName();
		name = name.substring(0, name.length()-4);
		ModuleAdapter module = VisitorFactory.createModuleAdapter(null, new SourceModule(name, getFile(), astModule, null), new PythonNatureStub());

		ITextSelection selection = new TextSelection(doc, config.getOffset(), config.getSelectionLength());

		RefactoringInfo info = new RefactoringInfo(null, doc, selection, null);

		MockupExtractMethodRequestProcessor requestProcessor = setupRequestProcessor(config, module, info);

		IDocument refactoringDoc = applyExtractMethod(info, requestProcessor);

		this.setTestGenerated(refactoringDoc.get());
		assertEquals(getExpected(), getGenerated());
		REF.IN_TESTS = false;
	}

	private IDocument applyExtractMethod(RefactoringInfo info, MockupExtractMethodRequestProcessor requestProcessor)
			throws BadLocationException {
		ExtractMethodRequest req = requestProcessor.getRefactoringRequests().get(0);

		ExtractMethodEdit extractMethodEdit = new ExtractMethodEdit(req);
		ExtractCallEdit extractCallEdit = new ExtractCallEdit(req);

		MultiTextEdit edit = new MultiTextEdit();
		edit.addChild(extractMethodEdit.getEdit());
		edit.addChild(extractCallEdit.getEdit());

		IDocument refactoringDoc = new Document(getSource());
		edit.apply(refactoringDoc);
		return refactoringDoc;
	}

	private MockupExtractMethodRequestProcessor setupRequestProcessor(MockupExtractMethodConfig config, ModuleAdapter module,
			RefactoringInfo info) {
		ModuleAdapter parsedSelection = info.getParsedExtendedSelection();

		AbstractScopeNode<?> scope = module.getScopeAdapter(info.getExtendedSelection());
		ParameterReturnDeduce deducer = new ParameterReturnDeduce(scope, info.getExtendedSelection(), module);

		SortedMap<String, String> renameMap = new TreeMap<String, String>();
		for (String variable : deducer.getParameters()) {
			String newName = variable;
			if (config.getRenameMap().containsKey(variable)) {
				newName = config.getRenameMap().get(variable);
			}
			renameMap.put(variable, newName);
		}

		return new MockupExtractMethodRequestProcessor(scope, info.getExtendedSelection(), parsedSelection, deducer, renameMap, config
				.getOffsetStrategy());
	}

	private MockupExtractMethodConfig initConfig() {
		MockupExtractMethodConfig config = null;
		XStream xstream = new XStream();
		xstream.alias("config", MockupExtractMethodConfig.class);

		if (getConfig().length() > 0) {
			config = (MockupExtractMethodConfig) xstream.fromXML(getConfig());
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
