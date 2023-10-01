/**
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.outline;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.visitors.scope.ASTEntryWithChildren;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_ui.outline.AbstractOutlineFilterAction;

public class OutlineHideOverloadsAction extends AbstractOutlineFilterAction {

    private static final String PREF_HIDE_OVERLOADS = "org.python.pydev.OUTLINE_HIDE_OVERLOADS";

    public OutlineHideOverloadsAction(PyOutlinePage page, IImageCache imageCache) {
        super("Hide overload Methods", page, imageCache, PREF_HIDE_OVERLOADS,
                UIConstants.STATIC_MEMBER_HIDE_OVERLOADS);
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

                    ASTEntryWithChildren astThis = item.getAstThis();
                    if (astThis == null) {
                        return true;
                    }
                    SimpleNode token = astThis.node;

                    if (token instanceof FunctionDef) {
                        FunctionDef functionDefToken = (FunctionDef) token;
                        if (functionDefToken.decs != null) {
                            for (decoratorsType decorator : functionDefToken.decs) {
                                if (decorator.func instanceof Name) {
                                    Name decoratorFuncName = (Name) decorator.func;
                                    if (decoratorFuncName.id.equals("overload")) {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
                return true;
            }
        };
    }
}
