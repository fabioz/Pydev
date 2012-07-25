/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.outline;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.IPyEditListener2;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.visitors.scope.ASTEntryWithChildren;
import org.python.pydev.ui.UIConstants;

/**
 * This action keeps the outline synched with the text selected in the text
 * editor.
 * 
 * Design notes:
 * It's linked on the constructor and unlinked in the destructor.
 * 
 * It considers that it's always linked, even if the action is inactive, but before executing it, a
 * check is done to see if it's active or not.
 * 
 * @author Fabio
 */
public class OutlineLinkWithEditorAction extends AbstractOutlineFilterAction implements IPyEditListener,
        IPyEditListener2 {

    private static final String PREF_LINK_WITH_EDITOR = "org.python.pydev.PREF_LINK_WITH_EDITOR";

    private WeakReference<PyEdit> pyEdit;

    public OutlineLinkWithEditorAction(PyOutlinePage page, ImageCache imageCache) {
        super("Link With Editor", page, imageCache, PREF_LINK_WITH_EDITOR, UIConstants.SYNC_WITH_EDITOR);

        pyEdit = new WeakReference<PyEdit>(page.editorView);
        relink();
    }

    /**
     * When called, it STOPS hearing notifications to update the outline when the cursor changes positions.
     */
    public void unlink() {
        PyEdit edit = pyEdit.get();
        if (edit != null) {
            edit.removePyeditListener(this);
        }
    }

    /**
     * When called, it STARTS hearing notifications to update the outline when the cursor changes positions.
     */
    public void relink() {
        PyEdit edit = pyEdit.get();
        if (edit != null) {
            edit.addPyeditListener(this);
        }
    }

    public void dispose() {
        unlink();
    }

    @Override
    protected ViewerFilter createFilter() {
        throw new RuntimeException(
                "Not implemented: as setActionEnabled is overriden, this action is not needed (as this is not a filter action).");
    }

    /**
     * Overridden to enable the linking with the editor instead of having a filter created.
     */
    @Override
    protected void setActionEnabled(boolean enableAction) {
        PyOutlinePage p = this.page.get();
        if (p != null) {
            p.getStore().setValue(PREF_LINK_WITH_EDITOR, enableAction);
            if (enableAction && pyEdit != null) {
                PyEdit edit = pyEdit.get();
                if (edit != null) {
                    handleCursorPositionChanged(edit, new PySelection(edit));
                }
            }
        }
    }

    public void onCreateActions(ListResourceBundle resources, PyEdit edit, IProgressMonitor monitor) {

    }

    public void onDispose(PyEdit edit, IProgressMonitor monitor) {

    }

    public void onSave(PyEdit edit, IProgressMonitor monitor) {

    }

    public void onSetDocument(IDocument document, PyEdit edit, IProgressMonitor monitor) {

    }

    /**
     * Hear mouse selections to update the selection in the outline
     */
    public void handleCursorPositionChanged(PyEdit edit, PySelection ps) {
        PyOutlinePage p = this.page.get();
        if (p != null && edit != null) {
            if (isChecked()) {
                doLinkOutlinePosition(edit, p, ps);
            }
        }
    }

    /**
     * Keeps the outline linked with the editor.
     */
    protected void doLinkOutlinePosition(PyEdit edit, PyOutlinePage p, PySelection ps) {
        ITextSelection t = ps.getTextSelection();
        IOutlineModel outlineModel = p.model;
        if (outlineModel != null) {
            StructuredSelection sel = getSelectionPosition(outlineModel.getRoot(), t);
            if (sel != null) {
                // we don't want to hear our own selections
                p.unlinkAll();
                try {
                    p.setSelection(sel);
                } finally {
                    p.relinkAll();
                }
            }
        }
    }

    /**
     * Convert the text selection to a model node in the outline (parsed item tree path).
     */
    private StructuredSelection getSelectionPosition(ParsedItem r, ITextSelection t) {
        try {
            ArrayList<ParsedItem> sel = new ArrayList<ParsedItem>();

            if (r != null) {
                do {
                    ParsedItem item = findSel(r, t.getStartLine() + 1);
                    if (item != null) {
                        sel.add(item);
                    }
                    r = item;
                } while (r != null);
            }
            TreePath treePath = null;
            if (sel != null && sel.size() > 0) {
                treePath = new TreePath(sel.toArray());
            }
            if (treePath != null) {
                return new TreeSelection(treePath);
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }

    /**
     * @return The parsed item that should be selected given the startLine passed.
     */
    private ParsedItem findSel(ParsedItem r, int startLine) {
        ParsedItem prev = null;

        ParsedItem[] children = r.getChildren();
        if (children != null) {
            for (ParsedItem i : children) {
                ASTEntryWithChildren astThis = i.getAstThis();
                if (astThis != null && astThis.node != null) {
                    if (astThis.node.beginLine == startLine) {
                        prev = i;
                        break;
                    }
                    if (astThis.node.beginLine > startLine) {
                        break;
                    }
                }
                prev = i;
            }
        }
        return prev;
    }

}
