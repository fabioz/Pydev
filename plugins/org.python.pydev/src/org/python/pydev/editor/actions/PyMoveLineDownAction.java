package org.python.pydev.editor.actions;

import java.util.ResourceBundle;

import org.python.pydev.editor.PyEdit;

public class PyMoveLineDownAction extends PyMoveLineAction{
	
	
	public PyMoveLineDownAction(ResourceBundle bundle, String prefix, PyEdit editor) {
		super(bundle, prefix, editor);
	}


	protected boolean getMoveUp() {
		return false;
	}

}
