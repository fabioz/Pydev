package com.python.pydev.refactoring.wizards;

import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;

/**
 * Currently the same as the class rename.
 */
public class PyRenameImportProcess extends PyRenameClassProcess{

	public PyRenameImportProcess(Definition definition) {
		super(definition);
	}

}
