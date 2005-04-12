/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.plugin.PythonNature;
import org.python.pydev.ui.ImageCache;

/**
 * @author Fabio Zadrozny
 */
public class AssistCreateInModule implements IAssistProps{

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.editor.actions.PySelection, org.python.pydev.ui.ImageCache, java.io.File, org.python.pydev.plugin.PythonNature, org.python.pydev.editor.model.AbstractNode)
     */
    public List getProps(PySelection ps, ImageCache imageCache, File f, PythonNature nature, AbstractNode root) throws BadLocationException {
        List l = new ArrayList();
        
        return l;
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.editor.actions.PySelection, java.lang.String)
     */
    public boolean isValid(PySelection ps, String sel) {
        try {
            String lineToCursor = ps.getLineContentsToCursor();
	        return lineToCursor.indexOf('.') != -1;
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return false;
    }

}
