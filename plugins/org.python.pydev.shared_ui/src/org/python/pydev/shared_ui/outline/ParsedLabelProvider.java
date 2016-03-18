/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Jul 25, 2003
 */
package org.python.pydev.shared_ui.outline;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.shared_ui.ImageCache;

/**
 * 
 * LabelProvider for ParsedItems inside an outline view
 */
public class ParsedLabelProvider extends LabelProvider {

    ImageCache imageCache;

    public ParsedLabelProvider(ImageCache imageCache) {
        this.imageCache = imageCache;
    }

    @Override
    public String getText(Object element) {
        return element.toString();
    }

    // returns images based upon element type
    @Override
    public Image getImage(Object element) {
        return ((IParsedItem) element).getImage();
    }
}
