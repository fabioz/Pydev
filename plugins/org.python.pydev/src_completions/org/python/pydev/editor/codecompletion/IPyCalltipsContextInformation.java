package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.contentassist.IContextInformation;

public interface IPyCalltipsContextInformation extends IContextInformation{

    int getShowCalltipsOffset();

}
