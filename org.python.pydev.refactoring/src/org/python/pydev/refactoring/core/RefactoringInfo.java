/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.refactoring.ast.PythonModuleManager;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;

public class RefactoringInfo {

	private IFile sourceFile;

	private IDocument doc;

	private ITextSelection userSelection;

	private ITextSelection extendedSelection;

	private ModuleAdapter moduleAdapter;

	private IPythonNature nature;

	private PythonModuleManager moduleManager;

	private AbstractScopeNode<?> scopeAdapter;

	public RefactoringInfo(ITextEditor edit, IPythonNature nature)  {
		this(((IFileEditorInput) edit.getEditorInput()).getFile(), edit.getDocumentProvider().getDocument(edit.getEditorInput()),
				(ITextSelection) edit.getSelectionProvider().getSelection(), nature);
	}

	public RefactoringInfo(IFile sourceFile, IDocument doc, ITextSelection selection, IPythonNature nature) {
		this.sourceFile = sourceFile;
		this.doc = doc;
		this.nature = nature;

		initInfo(selection, userSelection);
	}

	private void initInfo(ITextSelection selection, ITextSelection userSelection) {
		if (this.nature != null) {
			this.moduleManager = new PythonModuleManager(nature);
		}

		File realFile = null;
		if (sourceFile != null) {
			realFile = sourceFile.getRawLocation().toFile();
		}

		try {
			this.moduleAdapter = VisitorFactory.createModuleAdapter(moduleManager, realFile, doc, nature);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

		this.extendedSelection = null;
		this.userSelection = moduleAdapter.normalizeSelection(selection);
	}

	public ModuleAdapter getModule() {
		return moduleAdapter;
	}

	public List<IClassDefAdapter> getClasses() {
		return moduleAdapter.getClasses();
	}

	public IFile getSourceFile() {
		return this.sourceFile;
	}

	public IDocument getDocument() {
		return this.doc;
	}

	public ITextSelection getExtendedSelection() {
		if (this.extendedSelection == null) {
			this.extendedSelection = new TextSelection(this.doc, this.getUserSelection().getOffset(), this.userSelection.getLength());

			if (getScopeAdapter() != null) {
				this.extendedSelection = moduleAdapter.normalizeSelection(VisitorFactory.createSelectionExtension(getScopeAdapter(),
						this.extendedSelection));
			}

		}
		return extendedSelection;
	}

	public ITextSelection getUserSelection() {
		return userSelection;
	}

	public ModuleAdapter getParsedExtendedSelection() {
		return getParsedExtendedSelection(getScopeAdapter());
	}

	public ModuleAdapter getParsedUserSelection() {
		ModuleAdapter parsedAdapter = null;
		String source = normalizeSourceSelection(getScopeAdapter(), this.userSelection);

		if (this.userSelection != null && source.length() > 0) {
			try {
				parsedAdapter = VisitorFactory.createModuleAdapter(moduleManager, null, new Document(source), nature);
			} catch (ParseException e) {
				/* Parse Exception means the current selection is invalid, discard and return null */
			}
		}
		return parsedAdapter;
	}

	private ModuleAdapter getParsedExtendedSelection(AbstractScopeNode<?> scopeNode) {
		ModuleAdapter parsedAdapter = null;

		String source = normalizeSourceSelection(scopeNode, this.getExtendedSelection());

		if (this.getExtendedSelection() != null && source.length() > 0) {

			try {
				parsedAdapter = VisitorFactory.createModuleAdapter(moduleManager, null, new Document(source), nature);
			} catch (ParseException e) {
				/* Parse Exception means the current selection is invalid, discard and return null */
			}
		}
		return parsedAdapter;
	}

	public String normalizeSourceSelection(AbstractScopeNode<?> scopeNode, ITextSelection selection) {
		String selectedText = "";

		if (selection.getText() != null) {
			selectedText = selection.getText().trim();
		}
		if (selectedText.length() == 0) {
			return "";
		}

		try {
			return normalizeBlockIndentation(selection, selectedText);
		} catch (Throwable e) {
			/* TODO: uncommented empty exception catch all */
		}
		return selectedText;

	}

	private String normalizeBlockIndentation(ITextSelection selection, String selectedText) throws Throwable {
		String[] lines = selectedText.split("\\n");
		if (lines.length < 2) {
			return selectedText;
		}

		String firstLine = doc.get(doc.getLineOffset(selection.getStartLine()), doc.getLineLength(selection.getStartLine()));
        String lineDelimiter = TextUtilities.getDefaultLineDelimiter(doc);
        
		String indentation = "";
		int bodyIndent = 0;
		while (firstLine.startsWith(" ")) {
			indentation += " ";
			firstLine = firstLine.substring(1);
			bodyIndent += 1;
		}
		if (bodyIndent > 0) {
			StringBuffer selectedCode = new StringBuffer();
			for (String line : lines) {
				if (line.startsWith(indentation)) {
					selectedCode.append(line.substring(bodyIndent) + lineDelimiter);
				} else {
					selectedCode.append(line + lineDelimiter);
				}

			}
			selectedText = selectedCode.toString();
		}
		return selectedText;
	}

	public IClassDefAdapter getScopeClass() {
		return moduleAdapter.getScopeClass(getUserSelection());
	}

	public IPythonNature getNature() {
		return nature;
	}

	public List<IClassDefAdapter> getScopeClassAndBases() {
		return moduleAdapter.getClassHierarchy(getScopeClass());
	}

	public AbstractScopeNode<?> getScopeAdapter() {
		if (scopeAdapter == null) {
			scopeAdapter = moduleAdapter.getScopeAdapter(getUserSelection());
		}
		return scopeAdapter;
	}

	public boolean isSelectionExtensionRequired() {
		return !(this.getUserSelection().getOffset() == this.getExtendedSelection().getOffset() && this.getUserSelection().getLength() == this
				.getExtendedSelection().getLength());
	}

    public String getNewLineDelim() {
        return TextUtilities.getDefaultLineDelimiter(this.doc);
    }

}
