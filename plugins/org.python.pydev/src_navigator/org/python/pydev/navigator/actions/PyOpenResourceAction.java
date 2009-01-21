package org.python.pydev.navigator.actions;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.python.pydev.core.log.Log;

/**
 * This open action extends the action that tries to open files with the Pydev Editor, just changing the implementation
 * to try to open the files with the 'correct' editor in the ide.
 */
public class PyOpenResourceAction extends PyOpenPythonFileAction{

    private IWorkbenchPage page;

    public PyOpenResourceAction(IWorkbenchPage page, ISelectionProvider selectionProvider) {
        super(page, selectionProvider);
        this.page = page;
        this.setText("Open");
    }
    
    /**
     * Overridden to open the given files with the match provided by the platform.
     */
    @Override
    protected void openFiles(List<IFile> filesSelected) {
        for (IFile f : filesSelected) {
            try {
                IDE.openEditor(page, f);
            } catch (PartInitException e) {
                Log.log(e);
            }
        }
    }


}
