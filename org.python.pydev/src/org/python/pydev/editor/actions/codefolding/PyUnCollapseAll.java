/*
 * Created on Jul 22, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.codefolding;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;

/**
 * @author Fabio Zadrozny
 */
public class PyUnCollapseAll extends PyAction {

    /* (non-Javadoc)
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
		PySelection ps = new PySelection ( getTextEditor ( ), false );

		ProjectionAnnotationModel model = (ProjectionAnnotationModel) getTextEditor ( )
                .getAdapter(ProjectionAnnotationModel.class);

        if (model != null) {
            model.expandAll(0, ps.doc.getLength());
        }
        
    }

}
