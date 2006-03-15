/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public interface IAssistProps {

    /**
     * Gets the completion proposals related to the common assists
     * 
     * @param ps this is the selection
     * @param imageCache this is a cache for images (from the pydev plugin)
     * @param f this is the file related to the editor
     * @param nature this is the nature related to this file
     * @param edit this is the edit where the request took place
     * @param offset this is the offset of the edit 
     * 
     * @return a list of completions with proposals to fix things
     * @throws BadLocationException
     */
    List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, PythonNature nature, PyEdit edit, int offset) throws BadLocationException;

    /**
     * Gets wether this assist proposal is valid to be applied at the current line
     * 
     * @param ps the current selection
     * @param sel is the current string without any comments
     * @param edit this is the edit where the request took place
     * @param offset this is the offset of the edit 
     * 
     * @return whether the assist can be applied at the current line
     */
    boolean isValid(PySelection ps, String sel, PyEdit edit, int offset);
}
