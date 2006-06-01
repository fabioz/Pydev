package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;

public class PySelectWord extends PyAction{

	public void run(IAction action) {
		PyEdit pyEdit = getPyEdit();
		PySelection ps = new PySelection(pyEdit);
		try {
			Tuple<String,Integer> currToken = ps.getCurrToken();
			if(currToken.o1 != null){
				int len = currToken.o1.length();
				if(len > 0){
					pyEdit.selectAndReveal(currToken.o2, len);
				}
			}
		} catch (Exception e) {
			PydevPlugin.log(e);
		}
	}

}
