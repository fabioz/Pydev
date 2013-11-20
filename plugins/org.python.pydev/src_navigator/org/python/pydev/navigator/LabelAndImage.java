/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator;

import org.eclipse.swt.graphics.Image;

public final class LabelAndImage {

    public final String label;
    public final Image image;

    public LabelAndImage(String o1, Image o2) {
        this.label = o1;
        this.image = o2;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LabelAndImage)) {
            return false;
        }

        LabelAndImage t2 = (LabelAndImage) obj;
        if (label == t2.label && image == t2.image) { //all the same 
            return true;
        }

        if (label == null && t2.label != null) {
            return false;
        }
        if (image == null && t2.image != null) {
            return false;
        }
        if (label != null && t2.label == null) {
            return false;
        }
        if (image != null && t2.image == null) {
            return false;
        }

        if (!label.equals(t2.label)) {
            return false;
        }
        if (!image.equals(t2.image)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        if (label != null && image != null) {
            return label.hashCode() * image.hashCode();
        }
        if (label != null) {
            return label.hashCode();
        }
        if (image != null) {
            return image.hashCode();
        }
        return 7;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("LabelAndImage [");
        buffer.append(label);
        buffer.append(" -- ");
        buffer.append(image);
        buffer.append("]");
        return buffer.toString();
    }

}
