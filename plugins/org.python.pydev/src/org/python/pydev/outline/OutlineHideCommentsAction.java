/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.outline;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.outline.AbstractOutlineFilterAction;

/**
 * Action that will hide comments in the outline
 * 
 * @author Fabio
 */
public class OutlineHideCommentsAction extends AbstractOutlineFilterAction {

    private static final String PREF_HIDE_COMMENTS = "org.python.pydev.OUTLINE_HIDE_COMMENTS";

    public OutlineHideCommentsAction(PyOutlinePage page, ImageCache imageCache) {
        super("Hide Comments", page, imageCache, PREF_HIDE_COMMENTS, UIConstants.COMMENT_BLACK);
    }

    /**
     * @return the filter used to hide comments
     */
    @Override
    protected ViewerFilter createFilter() {
        return new ViewerFilter() {

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                if (element instanceof ParsedItem) {
                    ParsedItem item = (ParsedItem) element;
                    if (item == null || item.getAstThis() == null || !(item.getAstThis().node instanceof commentType)) {
                        return true;
                    }

                }
                return false;
            }

        };
    }

}
