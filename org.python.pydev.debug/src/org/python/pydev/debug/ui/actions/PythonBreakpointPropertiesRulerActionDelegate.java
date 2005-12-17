/**
 * 
 */
package org.python.pydev.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * @author nierbeck
 *
 */
public class PythonBreakpointPropertiesRulerActionDelegate extends
		AbstractRulerActionDelegate {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractRulerActionDelegate#createAction(org.eclipse.ui.texteditor.ITextEditor, org.eclipse.jface.text.source.IVerticalRulerInfo)
	 */
	@Override
	protected IAction createAction(ITextEditor editor,
			IVerticalRulerInfo rulerInfo) {
		return new PythonBreakpointPropertiesRulerAction(editor, rulerInfo);
	}

}
