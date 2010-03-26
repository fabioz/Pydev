package org.python.pydev.editor;

import java.util.List;

/**
 * This interface should be implemented by actions used in Ctrl+2 that may receive parameters in
 * the command the user entered.
 * 
 * E.g.: if the command is activated with 's', the user may type 's do something' and 'do', 'something'
 * will be received as parameters (in a list with those 2 items). 
 */
public interface IOfflineActionWithParameters {

	/**
	 * The parameters the user typed for the action -- cannot be null.
	 */
	void setParameters(List<String> parameters);

}
