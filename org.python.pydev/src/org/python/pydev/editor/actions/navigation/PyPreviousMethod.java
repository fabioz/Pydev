/*
 * @author: fabioz
 * Created: February 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.editor.actions.navigation;

import org.python.pydev.outline.ParsedItem;
import org.python.pydev.outline.IOutlineModel.SelectThis;

/**
 * @author Fabio Zadrozny
 */
public class PyPreviousMethod extends PyMethodNavigation{

	public SelectThis getSelect(Visitor v) {
		if (v.prevNode != null){
			return ParsedItem.getPosition(v.prevNode);
		}
		return null;
	}

}
