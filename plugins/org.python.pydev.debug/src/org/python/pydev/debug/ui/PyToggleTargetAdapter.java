package org.python.pydev.debug.ui;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.debug.model.PyBreakpoint;
import org.python.pydev.debug.ui.actions.PyBreakpointRulerAction;
import org.python.pydev.shared_ui.editor.BaseEditor;

public class PyToggleTargetAdapter implements IAdapterFactory {

    @Override
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adaptableObject instanceof ITextEditor && adapterType == IToggleBreakpointsTarget.class) {
            ITextEditor iTextEditor = (ITextEditor) adaptableObject;
            if (canToggleFor(iTextEditor)) {
                return new PyDjangoToggleBreakpointsTarget();
            }
            return null;
        }

        return null;
    }

    public static boolean canToggleFor(ITextEditor iTextEditor) {
        if (iTextEditor instanceof BaseEditor) {
            IEditorInput editorInput = iTextEditor.getEditorInput();
            String name = editorInput.getName();
            if (name != null) {
                if (name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".djhtml")) {
                    //System.err.println("PyToggleTargetAdapter.getAdapter: " + iTextEditor);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Class[] getAdapterList() {
        return new Class[] { IToggleBreakpointsTarget.class };
    }

}

class PyDjangoToggleBreakpointsTarget implements IToggleBreakpointsTarget, IToggleBreakpointsTargetExtension,
        IPyToggleBreakpointsTarget {

    PyDjangoToggleBreakpointsTarget() {
    }

    // --------------- All others point to this 2 methods!
    public void toggleBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
        if (part instanceof BaseEditor && selection instanceof TextSelection
                && PyToggleTargetAdapter.canToggleFor((BaseEditor) part)) {
            TextSelection textSelection = (TextSelection) selection;
            BaseEditor pyEdit = (BaseEditor) part;
            int startLine = textSelection.getStartLine();

            List<IMarker> markersFromCurrentFile = PyBreakpointRulerAction.getMarkersFromCurrentFile(pyEdit, startLine);
            if (markersFromCurrentFile.size() > 0) {
                PyBreakpointRulerAction.removeMarkers(markersFromCurrentFile);
            } else {
                PyBreakpointRulerAction.addBreakpointMarker(pyEdit.getDocument(), startLine + 1, pyEdit,
                        PyBreakpoint.PY_BREAK_TYPE_DJANGO);
            }
        }
    }

    public boolean canToggleBreakpoints(IWorkbenchPart part, ISelection selection) {
        return selection instanceof TextSelection && part instanceof ITextEditor
                && PyToggleTargetAdapter.canToggleFor((ITextEditor) part);
    }

    public void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
        toggleBreakpoints(part, selection);
    }

    public boolean canToggleLineBreakpoints(IWorkbenchPart part, ISelection selection) {
        return canToggleBreakpoints(part, selection);
    }

    public void toggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
        toggleBreakpoints(part, selection);
    }

    public boolean canToggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) {
        return canToggleBreakpoints(part, selection);
    }

    public void toggleWatchpoints(IWorkbenchPart part, ISelection selection) throws CoreException {
        toggleBreakpoints(part, selection);
    }

    public boolean canToggleWatchpoints(IWorkbenchPart part, ISelection selection) {
        return canToggleBreakpoints(part, selection);
    }

    @Override
    public void addBreakpointMarker(IDocument document, int line, ITextEditor fTextEditor) {
        PyBreakpointRulerAction.addBreakpointMarker(document, line, fTextEditor, PyBreakpoint.PY_BREAK_TYPE_DJANGO);
    }

}
