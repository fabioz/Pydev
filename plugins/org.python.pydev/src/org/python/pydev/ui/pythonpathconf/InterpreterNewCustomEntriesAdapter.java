package org.python.pydev.ui.pythonpathconf;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Adapter for {@link IInterpreterNewCustomEntries} that provides no additional
 * entries for any item.
 * 
 * This class can be subclassed in preference to implementing
 * IInterpreterNewCustomEntries directly if not all additions need to be made.
 */
public class InterpreterNewCustomEntriesAdapter implements IInterpreterNewCustomEntries {

	public Collection<String> getAdditionalLibraries() {
		return Collections.emptyList();
	}

	public Collection<String> getAdditionalEnvVariables() {
		return Collections.emptyList();
	}

	public Collection<String> getAdditionalBuiltins() {
		return Collections.emptyList();
	}

	public Map<String, String> getAdditionalStringSubstitutionVariables() {
		return Collections.emptyMap();
	}

}
