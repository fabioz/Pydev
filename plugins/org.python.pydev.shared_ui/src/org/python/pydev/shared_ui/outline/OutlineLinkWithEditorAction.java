/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.outline;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.ui.progress.UIJob;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.editor.BaseEditor;
import org.python.pydev.shared_ui.editor.IPyEditListener;
import org.python.pydev.shared_ui.editor.IPyEditListener2;

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

    private WeakReference<BaseEditor> pyEdit;

    public OutlineLinkWithEditorAction(BaseOutlinePage page, ImageCache imageCache, String pluginId) {
        super("Link With Editor", page, imageCache, pluginId + ".PREF_LINK_WITH_EDITOR", UIConstants.SYNC_WITH_EDITOR);

        pyEdit = new WeakReference<BaseEditor>(page.getEditor());
        relink();
    }

    /**
     * When called, it STOPS hearing notifications to update the outline when the cursor changes positions.
     */
    public void unlink() {
        BaseEditor edit = pyEdit.get();
        if (edit != null) {
            edit.removePyeditListener(this);
        }
    }

    /**
     * When called, it STARTS hearing notifications to update the outline when the cursor changes positions.
     */
    public void relink() {
        BaseEditor edit = pyEdit.get();
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
        BaseOutlinePage p = this.page.get();
        if (p != null) {
            p.getStore().setValue(this.preference, enableAction);
            if (enableAction && pyEdit != null) {
                BaseEditor edit = pyEdit.get();
                if (edit != null) {
                    handleCursorPositionChanged(edit, EditorUtils.createTextSelectionUtils(edit));
                }
            }
        }
    }

    @Override
    public void onCreateActions(ListResourceBundle resources, BaseEditor baseEditor, IProgressMonitor monitor) {

    }

    @Override
    public void onDispose(BaseEditor baseEditor, IProgressMonitor monitor) {

    }

    @Override
    public void onSave(BaseEditor baseEditor, IProgressMonitor monitor) {

    }

    @Override
    public void onSetDocument(IDocument document, BaseEditor baseEditor, IProgressMonitor monitor) {

    }

    /**
     * Hear mouse selections to update the selection in the outline
     */
    @Override
    public void handleCursorPositionChanged(BaseEditor edit, TextSelectionUtils ps) {
        BaseOutlinePage p = this.page.get();
        if (p != null && edit != null) {
            if (isChecked()) {
                doLinkOutlinePosition(edit, p, ps);
            }
        }
    }

    private static class UpdateSelection extends UIJob {

        private WeakReference<IOutlineModel> outlineModel;
        private final Object lock = new Object();
        private WeakReference<BaseOutlinePage> outlinePage;
        private ITextSelection ts;

        public UpdateSelection() {
            super("Link outline selection");
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            try {
                IOutlineModel model = null;
                IParsedItem parsedItem = null;
                BaseOutlinePage p = null;
                ITextSelection localTextSelection = ts;
                synchronized (lock) {
                    if (outlineModel != null) {
                        model = outlineModel.get();
                    }
                    if (model != null) {
                        parsedItem = model.getRoot();
                    }
                    p = outlinePage.get();
                }
                if (parsedItem == null || p == null || localTextSelection == null) {
                    return null;
                }
                StructuredSelection sel = getSelectionPosition(parsedItem, localTextSelection);
                if (sel != null) {
                    // we don't want to hear our own selections
                    p.unlinkAll();
                    try {
                        p.setSelection(sel);
                    } finally {
                        p.relinkAll();
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }
            return Status.OK_STATUS;
        }

        public void setOutline(IOutlineModel outlineModel, BaseOutlinePage p, ITextSelection ts) {
            synchronized (lock) {
                this.outlinePage = new WeakReference<BaseOutlinePage>(p);
                this.ts = ts;
                this.outlineModel = new WeakReference<IOutlineModel>(outlineModel);
            }
        }

        /**
         * Convert the text selection to a model node in the outline (parsed item tree path).
         */
        private StructuredSelection getSelectionPosition(IParsedItem r, ITextSelection t) {
            try {
                ArrayList<IParsedItem> sel = new ArrayList<IParsedItem>();

                if (r != null) {
                    do {
                        IParsedItem item = findSel(r, t.getStartLine() + 1);
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
        private IParsedItem findSel(IParsedItem r, int startLine) {
            IParsedItem prev = null;

            IParsedItem[] children = r.getChildren();
            if (children != null) {
                for (IParsedItem i : children) {
                    int beginLine = i.getBeginLine();
                    if (beginLine >= 0) {
                        if (beginLine == startLine) {
                            prev = i;
                            break;
                        }
                        if (beginLine > startLine) {
                            break;
                        }
                    }
                    prev = i;
                }
            }
            return prev;
        }
    };

    private final UpdateSelection updateSelection = new UpdateSelection();

    /**
     * Keeps the outline linked with the editor.
     */
    protected void doLinkOutlinePosition(BaseEditor edit, BaseOutlinePage p, TextSelectionUtils ps) {
        IOutlineModel outlineModel = p.getOutlineModel();
        if (outlineModel != null) {
            updateSelection.setOutline(outlineModel, p, ps.getTextSelection());
            updateSelection.schedule(50);
        }
    }

}
