/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.outline;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.bundle.ImageCache;

/**
 * 
 * LabelProvider for ParsedItems inside an outline view
 */
public class ParsedLabelProvider extends LabelProvider {
	
	ImageCache imageCache;
	
	public ParsedLabelProvider(ImageCache imageCache) {
		this.imageCache = imageCache;
	}

	public String getText(Object element) {
		return element.toString();
	}

	// returns images based upon element type
	public Image getImage(Object element) {
		return((ParsedItem)element).getImage();
	}
}
