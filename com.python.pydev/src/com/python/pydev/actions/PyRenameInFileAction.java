/*
 * Created on May 21, 2006
 */
package com.python.pydev.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;

import com.python.pydev.PydevPlugin;

public class PyRenameInFileAction extends Action{

    private PyEdit pyEdit;


    public PyRenameInFileAction(PyEdit edit) {
        this.pyEdit = edit;
    }


    public void run() {
        try {
            ISourceViewer viewer= pyEdit.getPySourceViewer();
            IDocument document= viewer.getDocument();
            PySelection ps = new PySelection(pyEdit);
            int offset = ps.getAbsoluteCursorOffset();
            LinkedPositionGroup group= new LinkedPositionGroup();
            
            List<Annotation> occurrenceAnnotationsInPyEdit = PydevPlugin.getOccurrenceAnnotationsInPyEdit(pyEdit);
            IAnnotationModel annotationModel= pyEdit.getDocumentProvider().getAnnotationModel(pyEdit.getEditorInput());
            if(occurrenceAnnotationsInPyEdit != null){
                int i = 0;
                for (Annotation annotation : occurrenceAnnotationsInPyEdit) {
                    i++;
                    Position position = annotationModel.getPosition(annotation);
                    group.addPosition(new ProposalPosition(document, position.offset, position.length, i , new ICompletionProposal[0]));
                }
            }

            if (group.isEmpty()) {
                return;         
            }
            LinkedModeModel model= new LinkedModeModel();
            model.addGroup(group);
            model.forceInstall();
            LinkedModeUI ui= new EditorLinkedModeUI(model, viewer);
            ui.setExitPosition(viewer, offset, 0, Integer.MAX_VALUE);
            ui.enter();
        } catch (BadLocationException e) {
            Log.log(e);
        } catch (Exception e) {
            Log.log(e);
        }
    }

}
