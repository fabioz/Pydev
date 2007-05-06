package org.python.pydev.outline;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.ListResourceBundle;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.IPyEditListener;
import org.python.pydev.editor.IPyEditListener2;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

/**
 * This action keeps the outline synched with the text selected in the text
 * editor.
 * 
 * @author Fabio
 */
public class OutlineLinkWithEditorAction extends Action implements IPyEditListener, IPyEditListener2 {

    private static final String PREF_LINK_WITH_EDITOR = "org.python.pydev.PREF_LINK_WITH_EDITOR";

    private WeakReference<PyOutlinePage> page;

    public boolean synchingWithEditor = false;

    private WeakReference<PyEdit> pyEdit;

    public OutlineLinkWithEditorAction(PyOutlinePage page, ImageCache imageCache) {
        super("Link With Editor", IAction.AS_CHECK_BOX);

        this.page = new WeakReference<PyOutlinePage>(page);

        setChecked(page.getStore().getBoolean(PREF_LINK_WITH_EDITOR));
        setLinkWithEditor(isChecked());

        try {
            setImageDescriptor(imageCache.getDescriptor(UIConstants.SYNC_WITH_EDITOR));
        } catch (MalformedURLException e) {
            PydevPlugin.log("Missing Icon", e);
        }
        page.editorView.addPyeditListener(this);
        pyEdit = new WeakReference<PyEdit>(page.editorView);
    }

    public void dispose() {
        PyEdit edit = pyEdit.get();
        if(edit != null){
            edit.removePyeditListener(this);
        }
    }

    public void run() {
        setLinkWithEditor(isChecked());
    }

    private void setLinkWithEditor(boolean doLink) {
        PyOutlinePage p = this.page.get();
        if (p != null) {
            p.getStore().setValue(PREF_LINK_WITH_EDITOR, doLink);
            if (doLink) {
                handleCursorPositionChanged(p.editorView);
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
    public void handleCursorPositionChanged(PyEdit edit) {
        PyOutlinePage p = this.page.get();
        if (p != null && edit != null) {
            if (isChecked()) {
                doLinkOutlinePosition(edit, p);
            }
        }
    }

    public void doLinkOutlinePosition(PyEdit edit, PyOutlinePage p) {
        PySelection ps = new PySelection(edit);
        ITextSelection t = ps.getTextSelection();
        IOutlineModel outlineModel = p.model;
        if(outlineModel != null){
            StructuredSelection sel = getSelectionPosition(outlineModel.getRoot(), t);
            if (sel != null) {
                // we don't want to hear our own selections
                synchingWithEditor = true;
                try {
                    p.getTreeViewer().setSelection(sel);
                } finally {
                    synchingWithEditor = false;
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
            PydevPlugin.log(e);
        }            
        return null;
    }

    
    private ParsedItem findSel(ParsedItem r, int startLine) {
        ParsedItem prev = null;

        if (r.children != null) {
            for (ParsedItem i : r.children) {
                if (i.astThis != null && i.astThis.node != null) {
                    if (i.astThis.node.beginLine == startLine) {
                        prev = i;
                        break;
                    }
                    if (i.astThis.node.beginLine > startLine) {
                        break;
                    }
                }
                prev = i;
            }
        }
        return prev;
    }


}
