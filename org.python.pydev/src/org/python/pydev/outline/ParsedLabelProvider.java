/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.outline;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.visitors.NodeUtils;
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
		return element.toString();
	}

	// returns images based upon element type
	public Image getImage(Object element) {
		SimpleNode token = ((ParsedItem)element).astThis.node;
		if (token instanceof ClassDef) {
			return imageCache.get(UIConstants.CLASS_ICON);
		}
		else if (token instanceof FunctionDef) {
			if (NodeUtils.getNameFromNameTok((NameTok) ((FunctionDef)token).name).startsWith("_")) {
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
		else if (token instanceof commentType) {
		    return imageCache.get(UIConstants.COMMENT);
		}
		else if (token instanceof Attribute || token instanceof Name || token instanceof NameTok) {
		    return imageCache.get(UIConstants.PUBLIC_ATTR_ICON);
		}
		else {
			return imageCache.get("ERROR");
		}
	}
}
