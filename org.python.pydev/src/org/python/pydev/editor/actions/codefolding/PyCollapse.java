/*
 * Created on Jul 19, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions.codefolding;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codefolding.PyProjectionAnnotation;

/**
 * @author Fabio Zadrozny
 */
public class PyCollapse extends PyAction {

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run(IAction action) {
		PySelection ps = new PySelection ( getTextEditor ( ), false );

        IAnnotationModel model = (IAnnotationModel) getTextEditor ( )
                .getAdapter(ProjectionAnnotationModel.class);
        try {
            if (model != null) {
                ArrayList collapsed = new ArrayList();
                //put annotations in array list.
                Iterator iter = model.getAnnotationIterator();
                while (iter != null && iter.hasNext()) {
                    PyProjectionAnnotation element = (PyProjectionAnnotation) iter.next();
                    Position position = model.getPosition(element);
                    
                    int line = ps.doc.getLineOfOffset(position.offset);
                    if(ps.startLineIndex == line){
                        model.removeAnnotation(element);
                        element.markCollapsed();
                        model.addAnnotation(element, position);
                        break;
                    }
                }
                
            }
        } catch (BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}