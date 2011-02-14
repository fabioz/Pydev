/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.hover;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codefolding.PySourceViewer;

/**
 * This interface should be implemented by clients that are providing the org.python.pydev.pydev_hover extension.
 * 
 * It should enable users to return a given text to appear on the hover on the current circumstances.
 * 
 * @author Fabio
 */
public interface IPyHoverParticipant {

    /**
     * This method should return the text to be added to the hover.
     * 
     * Text that should be made bold can be returned with <pydev_hint_bold>xxx</pydev_hint_bold>
     * 
     * @param hoverRegion the region for the hover
     * @param s the source viewer that's making the hover
     * @param ps determines the location of the cursor as if it was a text-selection
     * @param textSelection the text selection for the current hover
     * 
     * @return the text that should appear in the hover or null if this extension should provide nothing to the hover.
     */
    String getHoverText(IRegion hoverRegion, PySourceViewer s, PySelection ps, ITextSelection textSelection);

}
