package org.python.pydev.navigator.actions;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.actions.PyOpenAction;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editorinput.PydevFileEditorInput;
import org.python.pydev.navigator.PythonpathTreeNode;

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
    

    @Override
	protected void openFiles(PythonpathTreeNode[] pythonPathFilesSelected) {
		for(PythonpathTreeNode n:pythonPathFilesSelected){
			try {
				if(PythonPathHelper.isValidSourceFile(n.file.getName())){
					new PyOpenAction().run(new ItemPointer(n.file));
				}else{
					IEditorRegistry editorReg = PlatformUI.getWorkbench().getEditorRegistry();
					IEditorDescriptor defaultEditor = editorReg.getDefaultEditor(n.file.getName());
					if(defaultEditor != null){
						IDE.openEditor(page, new PydevFileEditorInput(n.file), defaultEditor.getId());
					}else{
						IDE.openEditor(page, new PydevFileEditorInput(n.file), EditorsUI.DEFAULT_TEXT_EDITOR_ID);
					}
				}
			} catch (PartInitException e) {
				Log.log(e);
			}
		}
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
