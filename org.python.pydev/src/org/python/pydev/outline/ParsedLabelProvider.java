/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.outline;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.If;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.UIConstants;

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
		return ((ParsedItem)element).toString();
	}

	// returns images based upon element type
	public Image getImage(Object element) {
		SimpleNode token = ((ParsedItem)element).getToken();
		if (token instanceof ClassDef) {
			return imageCache.get(UIConstants.CLASS_ICON);
		}
		else if (token instanceof FunctionDef) {
			if (((FunctionDef)token).name.startsWith("_")) {
				return imageCache.get(UIConstants.PRIVATE_METHOD_ICON);
			}
			else
				return imageCache.get(UIConstants.PUBLIC_METHOD_ICON);
		}
		else if (token instanceof Import) {
			return imageCache.get(UIConstants.IMPORT_ICON);
		}
		else if (token instanceof ImportFrom) {
			return imageCache.get(UIConstants.IMPORT_ICON);
		}
		else if (token instanceof If) {
			return imageCache.get(UIConstants.MAIN_FUNCTION_ICON);
		}
		else {
			return imageCache.get("ERROR");
		}
	}
}
