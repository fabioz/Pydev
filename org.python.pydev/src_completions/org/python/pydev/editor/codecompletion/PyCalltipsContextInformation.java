/*
 * Created on Jul 1, 2006
 * @author Fabio
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;

public class PyCalltipsContextInformation implements IContextInformation{


    /** The name of the context. */
    private final String fContextDisplayString;
    /** The information to be displayed. */
    private final String fInformationDisplayString;
    /** The image to be displayed. */
    private final Image fImage;

    /**
     * Creates a new context information without an image.
     *
     * @param contextDisplayString the string to be used when presenting the context
     * @param informationDisplayString the string to be displayed when presenting the context information
     */
    public PyCalltipsContextInformation(String contextDisplayString, String informationDisplayString, CompletionRequest request) {
        this(null, contextDisplayString, informationDisplayString);
    }

    /**
     * Creates a new context information with an image.
     *
     * @param image the image to display when presenting the context information
     * @param contextDisplayString the string to be used when presenting the context
     * @param informationDisplayString the string to be displayed when presenting the context information,
     *      may not be <code>null</code>
     */
    public PyCalltipsContextInformation(Image image, String contextDisplayString, String informationDisplayString) {

        Assert.isNotNull(informationDisplayString);

        fImage= image;
        fContextDisplayString= contextDisplayString;
        fInformationDisplayString= informationDisplayString;
    }

    /*
     * @see IContextInformation#equals(Object)
     */
    public boolean equals(Object object) {
        if (object instanceof IContextInformation) {
            IContextInformation contextInformation= (IContextInformation) object;
            boolean equals= fInformationDisplayString.equalsIgnoreCase(contextInformation.getInformationDisplayString());
            if (fContextDisplayString != null)
                equals= equals && fContextDisplayString.equalsIgnoreCase(contextInformation.getContextDisplayString());
            return equals;
        }
        return false;
    }

    /*
     * @see java.lang.Object#hashCode()
     * @since 3.1
     */
    public int hashCode() {
        int low= fContextDisplayString != null ? fContextDisplayString.hashCode() : 0;
        return (fInformationDisplayString.hashCode() << 16) | low;
    }

    /*
     * @see IContextInformation#getInformationDisplayString()
     */
    public String getInformationDisplayString() {
        return fInformationDisplayString;
    }

    /*
     * @see IContextInformation#getImage()
     */
    public Image getImage() {
        return fImage;
    }

    /*
     * @see IContextInformation#getContextDisplayString()
     */
    public String getContextDisplayString() {
        if (fContextDisplayString != null)
            return fContextDisplayString;
        return fInformationDisplayString;
    }



}
