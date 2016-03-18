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

public class PyCalltipsContextInformation implements IPyCalltipsContextInformation {

    /** 
     * The arguments to be displayed. 
     */
    private final String argumentsWithParens;

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

    /**
     * Creates a new context information without an image.
     *
     * @param argumentsWithParens the arguments available.
     * @param replacementOffset the offset where the replacement for the arguments started (the place right after the
     * parenthesis start)
     */
    public PyCalltipsContextInformation(String arguments, int replacementOffset) {
        this(null, arguments, replacementOffset);
    }

    /**
     * Creates a new context information with an image.
     *
     * @param image the image to display when presenting the context information
     * @param argumentsWithParens the arguments available.
     * @param replacementOffset the offset where the replacement started
     */
    private PyCalltipsContextInformation(Image image, String argumentsWithParens, int replacementOffset) {
        Assert.isNotNull(argumentsWithParens);

        fImage = image;
        this.argumentsWithParens = argumentsWithParens;
        fReplacementOffset = replacementOffset;
    }

    /*
     * @see IContextInformation#equals(Object)
     */
    @Override
    public boolean equals(Object object) {
        if (object instanceof PyCalltipsContextInformation) {
            PyCalltipsContextInformation contextInformation = (PyCalltipsContextInformation) object;
            return argumentsWithParens.equalsIgnoreCase(contextInformation.argumentsWithParens);
        }
        return false;
    }

    /*
     * @see java.lang.Object#hashCode()
     * @since 3.1
     */
    @Override
    public int hashCode() {
        return argumentsWithParens.hashCode();
    }

    /*
     * @see IContextInformation#getInformationDisplayString()
     */
    @Override
    public String getInformationDisplayString() {
        if (argumentsWithoutParens == null) {
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
