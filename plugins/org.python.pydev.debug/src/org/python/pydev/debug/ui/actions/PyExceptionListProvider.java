package org.python.pydev.debug.ui.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.core.FileUtils;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.interpreters.ChooseInterpreterManager;

public class PyExceptionListProvider implements IStructuredContentProvider {

	private Object newInput;
	private Object[] elementsForCurrentInput;
	private List<String> userConfiguredException = new ArrayList<String>();
	private static final String[] EMPTY = new String[0];

	public static final String ERROR = "error";
	public static final String EXCEPRION = "exception";
	public static final String WARNING = "warning";

	public PyExceptionListProvider() {

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
		list.addAll(getUserConfiguredException());

		elementsForCurrentInput = list.toArray(new String[0]);
	}

	/**
	 * if the userConfiguredException is empty, then adds the custom exception
	 * from the custom_exceptions.prefs file
	 * 
	 * @return
	 */
	public ArrayList<String> getUserConfiguredException() {
		if (userConfiguredException.isEmpty()) {
			List customExceptions = FileUtils
					.getConfiguredExceptions(Constants.CUSTOM_EXCEPTION_FILE_NAME);
			for (Object object : customExceptions) {
				userConfiguredException.add(object.toString());
			}
		}

		Collections.sort(userConfiguredException);
		return (ArrayList<String>) userConfiguredException;
	}

	/**
	 * 
	 * @param userConfiguredException
	 * 
	 *            Add the new custom exception in the userConfiguredException
	 *            list. if custom_exceptions.prefs file does not exist, create a
	 *            new file and write the exception else appends the new
	 *            exception in the existing custom_exceptions.prefs
	 */
	public void addUserConfiguredException(String userConfiguredException) {
		boolean isAppend = false;
		this.userConfiguredException.add(userConfiguredException);

		IPath path = FileUtils
				.getFilePathFromWorkSpace(Constants.CUSTOM_EXCEPTION_FILE_NAME);
		if (FileUtils.isFileExists(path.toString())) {
			isAppend = true;
			userConfiguredException = File.pathSeparator
					+ userConfiguredException;
		}
		FileUtils.writeExceptionsToFile(Constants.CUSTOM_EXCEPTION_FILE_NAME,
				userConfiguredException, isAppend);
		this.newInput = null;
	}

	/**
	 * Fetch built-in python exceptions from the pythonTokens
	 * 
	 * @return
	 */
	private ArrayList<String> getPyExceptionList() {
		IPythonNature pythonNature;
		ArrayList<String> list = new ArrayList<String>();
		IInterpreterManager useManager = ChooseInterpreterManager
				.chooseInterpreterManager();
		List<IPythonNature> natures = PythonNature
				.getPythonNaturesRelatedTo(useManager.getInterpreterType());
		if (natures.size() > 0) {
			pythonNature = natures.get(0);
		} else {
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
		Collections.sort(list);
		return list;
	}
}
