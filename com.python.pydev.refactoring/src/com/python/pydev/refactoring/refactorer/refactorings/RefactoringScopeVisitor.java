package com.python.pydev.refactoring.refactorer.refactorings;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IModule;
import org.python.pydev.plugin.nature.PythonNature;

import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.visitors.OcurrencesVisitor;

public class RefactoringScopeVisitor extends OcurrencesVisitor{

	public RefactoringScopeVisitor(PythonNature nature, String moduleName, IModule current, IAnalysisPreferences prefs, IDocument document) {
		super(nature, moduleName, current, prefs, document);
	}

}
