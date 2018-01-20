package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ITokenCompletionRequest;
import org.python.pydev.core.MisconfigurationException;

public class TokenCompletionRequest implements ITokenCompletionRequest {

    public TokenCompletionRequest(String activationToken, IModule module, IPythonNature nature, String qualifier,
            int line, int col) {
        super();
        this.activationToken = activationToken;
        this.module = module;
        this.nature = nature;
        this.qualifier = qualifier;
        this.line = line; //0-based
        this.col = col; //0-based
    }

    private String activationToken;
    private final IModule module;
    private final IPythonNature nature;
    private final String qualifier;
    private final int line;
    private final int col;

    @Override
    public String getActivationToken() {
        return activationToken;
    }

    @Override
    public void setActivationToken(String activationToken) {
        this.activationToken = activationToken;

    }

    @Override
    public IModule getModule() throws MisconfigurationException {
        return this.module;
    }

    @Override
    public IPythonNature getNature() throws MisconfigurationException {
        return this.nature;
    }

    @Override
    public String getQualifier() {
        return this.qualifier;
    }

    //0-based
    @Override
    public int getLine() throws BadLocationException {
        return this.line;
    }

    //0-based
    @Override
    public int getCol() throws BadLocationException {
        return this.col;
    }

}
