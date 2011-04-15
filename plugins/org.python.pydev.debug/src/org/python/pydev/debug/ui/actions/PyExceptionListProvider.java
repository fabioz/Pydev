package org.python.pydev.debug.ui.actions;

import java.util.ArrayList;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.editor.PyEdit;

public class PyExceptionListProvider implements IStructuredContentProvider {

	private PyEdit pyEdit;

	private Object newInput;
	private Object[] elementsForCurrentInput;
	private static final String[] EMPTY = new String[0];

	public static final String ERROR = "error";
	public static final String EXCEPRION = "exception";
	public static final String WARNING = "warning";

	public PyExceptionListProvider(PyEdit pyEdit) {
		this.pyEdit = pyEdit;
	}

	public Object[] getElements(Object inputElement) {

		if (this.newInput == null) {
			this.inputChanged(null, null, inputElement);
		}
		return elementsForCurrentInput == null ? EMPTY
				: elementsForCurrentInput;
	}

	public void dispose() {
		elementsForCurrentInput = null;
		newInput = null;
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == this.newInput) {
			return;
		}

		// let go of the old info before getting the new info
		dispose();
		this.newInput = newInput;

		if (newInput == null) {
			return;
		}

		ArrayList<String> list = getPyExceptionList();

		elementsForCurrentInput = getPyExceptionList().toArray(new String[0]);
	}

	private ArrayList<String> getPyExceptionList() {
		ArrayList<String> list = new ArrayList<String>();
		IPythonNature pythonNature;
		try {
			pythonNature = this.pyEdit.getPythonNature();
		} catch (MisconfigurationException e1) {
			elementsForCurrentInput = EMPTY;
			return list;
		}

		IToken[] pythonTokens = pythonNature.getBuiltinMod().getGlobalTokens();
		for (IToken token : pythonTokens) {
			String[] pyTokenArr = token.toString().split("-");
			if (pyTokenArr.length > 0) {
				String pyToken = pyTokenArr[0];
				if (pyToken.toLowerCase().contains(ERROR)
						|| pyToken.toLowerCase().contains(EXCEPRION)
						|| pyToken.toLowerCase().contains(WARNING)) {
					list.add(pyToken.trim());
				}
			}
		}
		
		ArrayList<String> testlist = new ArrayList<String>();
		testlist.add("UnicodeError");
		testlist.add("UnicodeEncodeError");
		return list;
	}
}
