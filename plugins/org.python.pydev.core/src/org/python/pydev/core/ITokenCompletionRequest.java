package org.python.pydev.core;

import org.eclipse.jface.text.BadLocationException;

public interface ITokenCompletionRequest {

    String getActivationToken();

    void setActivationToken(String activationToken);

    IModule getModule() throws MisconfigurationException;

    IPythonNature getNature() throws MisconfigurationException;

    String getQualifier();

    //0-based
    int getLine() throws BadLocationException;

    //0-based
    int getCol() throws BadLocationException;

}
