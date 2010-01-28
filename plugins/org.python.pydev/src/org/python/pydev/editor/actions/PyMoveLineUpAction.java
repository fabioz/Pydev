package org.python.pydev.editor.actions;

import java.util.ResourceBundle;

import org.python.pydev.editor.PyEdit;

public class PyMoveLineUpAction extends PyMoveLineAction{
	
	
	public PyMoveLineUpAction(ResourceBundle bundle, String prefix, PyEdit editor) {
		super(bundle, prefix, editor);
	}


	protected boolean getMoveUp() {
		return true;
	}

}
