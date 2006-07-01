/*
 * Created on Jul 1, 2006
 * @author Fabio
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationPresenter;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

public class PyContextInformationValidator implements IContextInformationValidator, IContextInformationPresenter{

    private PythonCompletionProcessor processor;
    private PyContextInformation info;
    private ITextViewer viewer;
    private int offset;

    public PyContextInformationValidator(PythonCompletionProcessor processor) {
        this.processor = processor;
    }

    //--- interface from IContextInformationValidator
    public void install(IContextInformation info, ITextViewer viewer, int offset) {
        this.info = (PyContextInformation) info;
        this.viewer = viewer;
        this.offset = offset;
    }

    public boolean isContextInformationValid(int offset) {
        return true;
    }

    //--- interface from IContextInformationPresenter
    public boolean updatePresentation(int offset, TextPresentation presentation) {
        return false;
    }

}
