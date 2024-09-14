package org.python.pydev.editor.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.text.edits.ReplaceEdit;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.wrap_paragraph.Paragrapher;
import org.python.pydev.editor.IOfflineActionWithParameters;
import org.python.pydev.editor.PyEdit;

public class PyWrapParagraphAction extends Action implements IOfflineActionWithParameters {

    protected List<String> parameters;
    protected PyEdit edit;

    public PyWrapParagraphAction(PyEdit edit) {
        this.edit = edit;
    }

    @Override
    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public void run() {
        try {
            PySelection ps = new PySelection(edit);
            Paragrapher p = new Paragrapher(ps, edit.getPrintMarginColums());
            String errorMsg = p.getValidErrorInPos();
            if (errorMsg == null) {
                ReplaceEdit replaceEdit = p.getReplaceEdit();
                replaceEdit.apply(ps.getDoc());
            } else {
                edit.setMessage(false, errorMsg);
            }
        } catch (Exception e) {
            Log.log(e);
        }
    }

}
