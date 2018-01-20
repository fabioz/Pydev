/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jul 1, 2006
 * @author Fabio
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.NodeUtils;

public class PyCalltipsContextInformationFromIToken implements IPyCalltipsContextInformation {

    private final IToken token;

    /** 
     * The arguments to be displayed. 
     */
    private String argumentsWithParens;

    /** 
     * The information to be displayed (calculated when requested)
     */
    private String argumentsWithoutParens;

    /** 
     * The image to be displayed.
     */
    private final Image fImage;

    /**
     * The place where the replacement started.
     */
    private final int fReplacementOffset;

    private final String defaultArguments;

    /**
     * Creates a new context information without an image.
     *
     * @param argumentsWithParens the arguments available.
     * @param replacementOffset the offset where the replacement for the arguments started (the place right after the
     * parenthesis start)
     * @param i 
     */
    public PyCalltipsContextInformationFromIToken(IToken token, String defaultArguments, int replacementOffset) {
        Assert.isNotNull(token);
        fImage = null;
        fReplacementOffset = replacementOffset;
        this.defaultArguments = defaultArguments;
        this.token = token;
    }

    /*
     * @see IContextInformation#equals(Object)
     */
    @Override
    public boolean equals(Object object) {
        if (object instanceof PyCalltipsContextInformationFromIToken) {
            PyCalltipsContextInformationFromIToken contextInformation = (PyCalltipsContextInformationFromIToken) object;
            contextInformation.calculateArgumentsWithParens();
            this.calculateArgumentsWithParens();
            return argumentsWithParens.equalsIgnoreCase(contextInformation.argumentsWithParens);
        }
        return false;
    }

    private void calculateArgumentsWithParens() {
        if (argumentsWithParens == null) {
            if (token instanceof SourceToken) {
                SourceToken sourceToken = (SourceToken) token;
                SimpleNode ast = sourceToken.getAst();
                String fullArgs = NodeUtils.getFullArgs(ast);
                if (fullArgs.length() == 0) {
                    argumentsWithParens = null;
                } else {
                    argumentsWithParens = fullArgs;
                }
            }
            if (argumentsWithParens == null) {
                //still not found: use default
                argumentsWithParens = defaultArguments;
            }
        }

    }

    /*
     * @see java.lang.Object#hashCode()
     * @since 3.1
     */
    @Override
    public int hashCode() {
        calculateArgumentsWithParens();
        return argumentsWithParens.hashCode();
    }

    /*
     * @see IContextInformation#getInformationDisplayString()
     */
    @Override
    public String getInformationDisplayString() {
        if (argumentsWithoutParens == null) {
            calculateArgumentsWithParens();
            argumentsWithoutParens = argumentsWithParens.substring(1, argumentsWithParens.length() - 1); //remove the parenthesis
        }
        return argumentsWithoutParens;
    }

    /*
     * @see IContextInformation#getImage()
     */
    @Override
    public Image getImage() {
        return fImage;
    }

    /*
     * @see IContextInformation#getContextDisplayString()
     */
    @Override
    public String getContextDisplayString() {
        return getInformationDisplayString();
    }

    @Override
    public int getShowCalltipsOffset() {
        return this.fReplacementOffset;
    }

}
