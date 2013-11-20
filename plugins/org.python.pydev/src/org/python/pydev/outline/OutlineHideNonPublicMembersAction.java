/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.outline;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntryWithChildren;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.outline.AbstractOutlineFilterAction;

/**
 * Action that will hide the non-public members in the outline
 * 
 * @author laurent.dore
 */
public class OutlineHideNonPublicMembersAction extends AbstractOutlineFilterAction {

    private static final String PREF_HIDE_NONPUBLICMEMBERS = "org.python.pydev.OUTLINE_HIDE_NONPUBLICMEMBERS";

    public OutlineHideNonPublicMembersAction(PyOutlinePage page, ImageCache imageCache) {
        super("Hide Non-Public Members", page, imageCache, PREF_HIDE_NONPUBLICMEMBERS, UIConstants.PUBLIC_ATTR_ICON);
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
                    if (item == null) {
                        return true;
                    }

                    ASTEntryWithChildren astThis = item.getAstThis();
                    if (astThis == null) {
                        return true;
                    }

                    SimpleNode token = astThis.node;
                    if (token == null) {
                        return true;
                    }

                    String name = NodeUtils.getRepresentationString(token);

                    if (name != null) {
                        return (!name.startsWith("_")) || (name.startsWith("__") && name.endsWith("__"));
                    }
                }
                return true;
            }

        };
    }
}
