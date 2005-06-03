/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.ImageCache;

/**
 * @author Fabio Zadrozny
 */
public interface IAssistProps {

    /**
     * 
     * @param ps
     * @param imageCache
     * @return
     * @throws BadLocationException
     */
    List getProps(PySelection ps, ImageCache imageCache, File f, PythonNature nature, AbstractNode root) throws BadLocationException;

    /**
     * @param ps
     * @param sel
     * @return
     */
    boolean isValid(PySelection ps, String sel);
}
